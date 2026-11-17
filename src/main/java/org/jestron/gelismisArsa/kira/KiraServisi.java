package org.jestron.gelismisArsa.kira;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jestron.gelismisArsa.Ayarlar;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.entegrasyon.Ekonomi;
import org.jestron.gelismisArsa.entegrasyon.WorldEditKopru;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.util.Mesaj;
import org.jestron.gelismisArsa.util.Sure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kiralama: kiralama, süre uzatma/kısaltma, terk (orantılı iade), süre bitimi ve sıfırlama.
 * Süre kontrolü ana thread'de 20 saniyede bir yapılır (thread güvenli, maliyetsiz).
 */
public final class KiraServisi implements Listener {

    private static final long MIN_KALAN_SANIYE = 3600L;

    private final GelismisArsa plugin;
    private final Ekonomi ekonomi;

    public KiraServisi(GelismisArsa plugin, Ekonomi ekonomi) {
        this.plugin = plugin;
        this.ekonomi = ekonomi;
    }

    private Ayarlar ayar() {
        return plugin.ayarlar();
    }

    public double ucret(long saniye) {
        return saniye * ayar().saniyeUcreti();
    }

    public String bicim(double miktar) {
        return ekonomi != null ? ekonomi.bicim(miktar) : String.format("%.2f", miktar);
    }

    private boolean ekonomiHazir(Player p) {
        if (!ayar().kiralamaAktif) {
            Mesaj.hata(p, "Kiralama sistemi şu anda kapalı.");
            return false;
        }
        if (ekonomi == null || !ekonomi.hazir()) {
            Mesaj.hata(p, "Ekonomi sistemi bulunamadı (Vault + ekonomi eklentisi gerekli).");
            return false;
        }
        return true;
    }

    public long maxSaniye() {
        return ayar().maxHafta * 604800L;
    }

    // ── Kiralama ────────────────────────────────────────────────────────────

    public boolean kirala(Player p, Arsa arsa, long saniye) {
        if (!ekonomiHazir(p)) return false;
        if (arsa.sahibiVar()) {
            Mesaj.hata(p, "Bu arsanın zaten bir sahibi var.");
            return false;
        }
        if (saniye < 86400L || saniye > maxSaniye()) {
            Mesaj.hata(p, "Kiralama süresi en az 1 gün, en fazla " + ayar().maxHafta + " hafta olabilir.");
            return false;
        }
        int limit = ayar().oyuncuBasiMaxArsa;
        if (limit > 0 && plugin.arsalar().sahipArsalari(p.getUniqueId()).size() >= limit) {
            Mesaj.hata(p, "En fazla " + limit + " arsa kiralayabilirsin.");
            return false;
        }
        double tutar = ucret(saniye);
        if (!ekonomi.cek(p, tutar)) {
            Mesaj.hata(p, "Yeterli paran yok! Gerekli: &f" + bicim(tutar));
            return false;
        }
        // Arsanın kiralanmadan önceki hali şablon olarak saklanır (yoksa)
        WorldEditKopru we = plugin.worldEdit();
        File sablon = sablonDosyasi(arsa);
        if (we != null && !sablon.exists()) we.sablonKaydet(arsa, sablon);

        arsa.sahipsizYap();
        arsa.sahipAyarla(p.getUniqueId());
        arsa.kiraAyarla(System.currentTimeMillis() + saniye * 1000L, ayar().saniyeUcreti());
        for (Izin i : ayar().ziyaretciVarsayilan) arsa.ziyaretciAyarla(i, true);
        plugin.depo().kaydetPlanla();

        Mesaj.basari(p, "Tebrikler! &f#" + arsa.id() + " &anumaralı arsayı &f" + Sure.saniyeYaz(saniye)
                + " &asüreyle &f" + bicim(tutar) + " &akarşılığında kiraladın.");
        Mesaj.bilgi(p, "Bitiş: &f" + Sure.tarih(arsa.kiraBitis()) + " &8• &7Yönetim: &f/arsa gui");
        return true;
    }

    public boolean uzat(Player p, Arsa arsa, long saniye) {
        if (!sahipKontrol(p, arsa) || !ekonomiHazir(p)) return false;
        long yeniKalan = arsa.kalanMs() / 1000L + saniye;
        if (yeniKalan > maxSaniye()) {
            Mesaj.hata(p, "Kalan süre en fazla " + ayar().maxHafta + " hafta olabilir. Şu an kalan: &f"
                    + Sure.yaz(arsa.kalanMs()));
            return false;
        }
        double tutar = ucret(saniye);
        if (!ekonomi.cek(p, tutar)) {
            Mesaj.hata(p, "Yeterli paran yok! Gerekli: &f" + bicim(tutar));
            return false;
        }
        arsa.kiraBitisAyarla(Math.max(arsa.kiraBitis(), System.currentTimeMillis()) + saniye * 1000L);
        plugin.depo().kaydetPlanla();
        Mesaj.basari(p, "Kira süresi &f" + Sure.saniyeYaz(saniye) + " &auzatıldı (&f" + bicim(tutar)
                + "&a). Yeni bitiş: &f" + Sure.tarih(arsa.kiraBitis()));
        return true;
    }

    public boolean kisalt(Player p, Arsa arsa, long saniye) {
        if (!sahipKontrol(p, arsa) || !ekonomiHazir(p)) return false;
        long kalan = arsa.kalanMs() / 1000L;
        if (kalan - saniye < MIN_KALAN_SANIYE) {
            Mesaj.hata(p, "En az 1 saat süre kalmalı. Arsayı bırakmak için: &f/arsa terket");
            return false;
        }
        double iade = saniye * iadeOrani(arsa);
        arsa.kiraBitisAyarla(arsa.kiraBitis() - saniye * 1000L);
        ekonomi.yatir(p, iade);
        plugin.depo().kaydetPlanla();
        Mesaj.basari(p, "Kira süresi &f" + Sure.saniyeYaz(saniye) + " &akısaltıldı, &f" + bicim(iade)
                + " &aiade edildi. Yeni bitiş: &f" + Sure.tarih(arsa.kiraBitis()));
        return true;
    }

    /** Kalan süre karşılığı iade tutarı (terk etmeden önce gösterim için). */
    public double terkIadesi(Arsa arsa) {
        return (arsa.kalanMs() / 1000D) * iadeOrani(arsa);
    }

    public boolean terket(Player p, Arsa arsa) {
        if (!arsa.sahipMi(p.getUniqueId())) {
            Mesaj.hata(p, "Bu arsanın sahibi sen değilsin.");
            return false;
        }
        double iade = arsa.kiralik() ? terkIadesi(arsa) : 0D;
        if (iade > 0 && ekonomi != null && ekonomi.hazir()) {
            ekonomi.yatir(p, iade);
            Mesaj.basari(p, "Arsayı terk ettin. Hesabına &f" + bicim(iade) + " &aiade edildi.");
        } else {
            Mesaj.basari(p, "Arsayı terk ettin.");
        }
        bitir(arsa, true, false);
        return true;
    }

    /**
     * Kirayı bitirir: arsa sahipsiz olur, üyeler ve izinler silinir, ayara göre sıfırlanır.
     */
    public void bitir(Arsa arsa, boolean sifirla, boolean sureDoldu) {
        UUID eskiSahip = arsa.sahip();
        arsa.sahipsizYap();
        plugin.depo().kaydetPlanla();
        if (sifirla && ayar().bitinceSifirla) sifirla(arsa);
        if (sureDoldu && eskiSahip != null) {
            Player p = Bukkit.getPlayer(eskiSahip);
            if (p != null) Mesaj.hata(p, "&f#" + arsa.id() + " &cnumaralı arsanın kira süresi doldu, arsa boşaltıldı.");
        }
    }

    public void sifirla(Arsa arsa) {
        WorldEditKopru we = plugin.worldEdit();
        if (we == null) {
            plugin.getLogger().warning("#" + arsa.id() + " sıfırlanamadı: WorldEdit yüklü değil.");
            return;
        }
        File sablon = sablonDosyasi(arsa);
        if (sablon.exists()) {
            if (we.sablonYukle(arsa, sablon)) return;
        }
        if (ayar().sablonYoksaHavayaCevir) {
            we.havayaCevir(arsa);
        } else {
            plugin.getLogger().info("#" + arsa.id() + " için şablon yok, sıfırlama atlandı (/arsa sablon ile oluşturabilirsin).");
        }
    }

    public File sablonDosyasi(Arsa arsa) {
        return new File(new File(plugin.getDataFolder(), "sablonlar"), arsa.id() + ".schem");
    }

    // ── Zamanlayıcı ─────────────────────────────────────────────────────────

    public void baslat() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::sureKontrol, 200L, 400L);
    }

    private void sureKontrol() {
        long simdi = System.currentTimeMillis();
        List<Arsa> biten = new ArrayList<>();
        for (Arsa a : plugin.arsalar().tumu()) {
            if (a.kiralik() && a.kiraBitis() <= simdi) biten.add(a);
        }
        for (Arsa a : biten) {
            plugin.getLogger().info("#" + a.id() + " arsasının kira süresi doldu, boşaltılıyor.");
            bitir(a, true, true);
        }
    }

    @EventHandler
    public void giris(PlayerJoinEvent e) {
        int saat = ayar().hatirlatmaSaat;
        if (saat <= 0) return;
        UUID u = e.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(u);
            if (p == null) return;
            for (Arsa a : plugin.arsalar().sahipArsalari(u)) {
                if (a.kiralik() && a.kalanMs() < saat * 3600_000L) {
                    Mesaj.gonder(p, "&e⚠ &f#" + a.id() + " &enumaralı arsanın kirası &f" + Sure.yaz(a.kalanMs())
                            + " &esonra bitiyor. Uzatmak için: &f/arsa sure arttır 1w");
                }
            }
        }, 60L);
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    private boolean sahipKontrol(Player p, Arsa arsa) {
        if (!arsa.kiralik()) {
            Mesaj.hata(p, "Bu arsa kiralık değil.");
            return false;
        }
        if (!arsa.sahipMi(p.getUniqueId())) {
            Mesaj.hata(p, "Kira süresini sadece arsanın kiracısı değiştirebilir.");
            return false;
        }
        return true;
    }

    /** İade oranı: kiralama anındaki ve şu anki fiyattan düşük olanı (fiyat değişimi istismarını önler). */
    private double iadeOrani(Arsa arsa) {
        double simdiki = ayar().saniyeUcreti();
        double kayitli = arsa.saniyeUcreti() > 0 ? arsa.saniyeUcreti() : simdiki;
        return Math.min(kayitli, simdiki);
    }
}
