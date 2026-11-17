package org.jestron.arsa.kira;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jestron.arsa.Ayarlar;
import org.jestron.arsa.ArsaPlugin;
import org.jestron.arsa.dil.Dil;
import org.jestron.arsa.entegrasyon.Ekonomi;
import org.jestron.arsa.entegrasyon.WorldEditKopru;
import org.jestron.arsa.model.Arsa;
import org.jestron.arsa.model.Izin;
import org.jestron.arsa.util.Mesaj;
import org.jestron.arsa.util.Sure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Kiralama: kiralama, süre uzatma/kısaltma, terk (orantılı iade), süre bitimi ve sıfırlama.
 * Süre kontrolü ana thread'de 20 saniyede bir yapılır (thread güvenli, maliyetsiz).
 */
public final class KiraServisi implements Listener {

    private static final long MIN_KALAN_SANIYE = 3600L;

    private final ArsaPlugin plugin;
    private final Ekonomi ekonomi;

    public KiraServisi(ArsaPlugin plugin, Ekonomi ekonomi) {
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
        return ekonomi != null ? ekonomi.bicim(miktar) : String.format(Locale.ROOT, "%,.2f", miktar);
    }

    private boolean ekonomiHazir(Player p) {
        if (!ayar().kiralamaAktif) {
            Mesaj.hata(p, "kira.kapali");
            return false;
        }
        if (ekonomi == null || !ekonomi.hazir()) {
            Mesaj.hata(p, "kira.ekonomi-yok");
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
        Dil d = Mesaj.dil(p);
        if (arsa.sahibiVar()) {
            Mesaj.hata(p, "kira.zaten-sahipli");
            return false;
        }
        if (saniye < 86400L || saniye > maxSaniye()) {
            Mesaj.hata(p, "kira.sure-sinir", ayar().maxHafta);
            return false;
        }
        if (ayar().tekArsaKurali) {
            Arsa diger = plugin.arsalar().iliskiliArsa(p.getUniqueId(), null);
            if (diger != null) {
                Mesaj.hata(p, "kira.baska-arsada", diger.id());
                return false;
            }
        }
        int limit = ayar().oyuncuBasiMaxArsa;
        if (limit > 0 && plugin.arsalar().sahipArsalari(p.getUniqueId()).size() >= limit) {
            Mesaj.hata(p, "kira.limit", limit);
            return false;
        }
        double tutar = ucret(saniye);
        if (!ekonomi.cek(p, tutar)) {
            Mesaj.hata(p, "kira.para-yok", bicim(tutar));
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

        Mesaj.basari(p, "kira.kiralandi", arsa.id(), Sure.saniyeYaz(d, saniye), bicim(tutar));
        Mesaj.bilgi(p, "kira.kiralandi-bilgi", Sure.tarih(arsa.kiraBitis()));
        return true;
    }

    public boolean uzat(Player p, Arsa arsa, long saniye) {
        if (!sahipKontrol(p, arsa) || !ekonomiHazir(p)) return false;
        Dil d = Mesaj.dil(p);
        long yeniKalan = arsa.kalanMs() / 1000L + saniye;
        if (yeniKalan > maxSaniye()) {
            Mesaj.hata(p, "kira.max-kalan", ayar().maxHafta, Sure.yaz(d, arsa.kalanMs()));
            return false;
        }
        double tutar = ucret(saniye);
        if (!ekonomi.cek(p, tutar)) {
            Mesaj.hata(p, "kira.para-yok", bicim(tutar));
            return false;
        }
        arsa.kiraBitisAyarla(Math.max(arsa.kiraBitis(), System.currentTimeMillis()) + saniye * 1000L);
        plugin.depo().kaydetPlanla();
        Mesaj.basari(p, "kira.uzatildi", Sure.saniyeYaz(d, saniye), bicim(tutar), Sure.tarih(arsa.kiraBitis()));
        return true;
    }

    public boolean kisalt(Player p, Arsa arsa, long saniye) {
        if (!sahipKontrol(p, arsa) || !ekonomiHazir(p)) return false;
        Dil d = Mesaj.dil(p);
        long kalan = arsa.kalanMs() / 1000L;
        if (kalan - saniye < MIN_KALAN_SANIYE) {
            Mesaj.hata(p, "kira.min-kalan");
            return false;
        }
        double iade = saniye * iadeOrani(arsa);
        arsa.kiraBitisAyarla(arsa.kiraBitis() - saniye * 1000L);
        ekonomi.yatir(p, iade);
        plugin.depo().kaydetPlanla();
        Mesaj.basari(p, "kira.kisaltildi", Sure.saniyeYaz(d, saniye), bicim(iade), Sure.tarih(arsa.kiraBitis()));
        return true;
    }

    /** Kalan süre karşılığı iade tutarı (terk etmeden önce gösterim için). */
    public double terkIadesi(Arsa arsa) {
        return (arsa.kalanMs() / 1000D) * iadeOrani(arsa);
    }

    public boolean terket(Player p, Arsa arsa) {
        if (!arsa.sahipMi(p.getUniqueId())) {
            Mesaj.hata(p, "kira.sahip-degil");
            return false;
        }
        double iade = arsa.kiralik() ? terkIadesi(arsa) : 0D;
        if (iade > 0 && ekonomi != null && ekonomi.hazir()) {
            ekonomi.yatir(p, iade);
            Mesaj.basari(p, "kira.terk-iade", bicim(iade));
        } else {
            Mesaj.basari(p, "kira.terk");
        }
        bitir(arsa, true, false);
        return true;
    }

    /** Kirayı bitirir: arsa sahipsiz olur, üyeler ve izinler silinir, ayara göre sıfırlanır. */
    public void bitir(Arsa arsa, boolean sifirla, boolean sureDoldu) {
        UUID eskiSahip = arsa.sahip();
        arsa.sahipsizYap();
        plugin.depo().kaydetPlanla();
        if (sifirla && ayar().bitinceSifirla) sifirla(arsa);
        if (sureDoldu && eskiSahip != null) {
            Player p = Bukkit.getPlayer(eskiSahip);
            if (p != null) Mesaj.hata(p, "kira.sure-doldu", arsa.id());
        }
    }

    public void sifirla(Arsa arsa) {
        WorldEditKopru we = plugin.worldEdit();
        if (we == null) {
            plugin.getLogger().warning("#" + arsa.id() + " sifirlanamadi: WorldEdit yuklu degil.");
            return;
        }
        File sablon = sablonDosyasi(arsa);
        if (sablon.exists() && we.sablonYukle(arsa, sablon)) return;
        if (ayar().sablonYoksaHavayaCevir) {
            we.havayaCevir(arsa);
        } else {
            plugin.getLogger().info("#" + arsa.id() + " icin sablon yok, sifirlama atlandi (/arsa sablon).");
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
            plugin.getLogger().info("#" + a.id() + " kira suresi doldu, bosaltiliyor.");
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
            Dil d = Mesaj.dil(p);
            for (Arsa a : plugin.arsalar().sahipArsalari(u)) {
                if (a.kiralik() && a.kalanMs() < saat * 3600_000L) {
                    Mesaj.gonder(p, "kira.hatirlatma", a.id(), Sure.yaz(d, a.kalanMs()));
                }
            }
        }, 60L);
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    private boolean sahipKontrol(Player p, Arsa arsa) {
        if (!arsa.kiralik()) {
            Mesaj.hata(p, "kira.kiralik-degil");
            return false;
        }
        if (!arsa.sahipMi(p.getUniqueId())) {
            Mesaj.hata(p, "kira.sadece-kiraci");
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
