package org.jestron.arsa;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jestron.arsa.dil.Dil;
import org.jestron.arsa.model.Arsa;
import org.jestron.arsa.model.ArsaUye;
import org.jestron.arsa.model.Izin;
import org.jestron.arsa.model.Rutbe;
import org.jestron.arsa.util.Mesaj;
import org.jestron.arsa.util.OyuncuBul;

import java.util.UUID;

/**
 * Üye işlemleri. Komutlar ve GUI aynı metotları kullanır; kurallar tek yerde uygulanır.
 * Her metot yetkiyi ve tek arsa kuralını kendisi doğrular, mesaj gönderir ve kaydeder.
 */
public final class UyeServisi {

    private final ArsaPlugin plugin;

    public UyeServisi(ArsaPlugin plugin) {
        this.plugin = plugin;
    }

    private Yetki yetki() {
        return plugin.yetki();
    }

    private static Dil d(Player p) {
        return Mesaj.dil(p);
    }

    /**
     * Tek arsa kuralı: oyuncu başka bir arsanın sahibi veya üyesiyse bu arsaya eklenemez.
     * @return engel varsa true (mesaj gönderilmiştir)
     */
    private boolean baskaArsadaMi(Player yapan, Arsa arsa, UUID hedef, String ad) {
        if (!plugin.ayarlar().tekArsaKurali) return false;
        Arsa diger = plugin.arsalar().iliskiliArsa(hedef, arsa);
        if (diger == null) return false;
        Mesaj.hata(yapan, "uye.baska-arsada", ad, diger.id());
        return true;
    }

    public boolean davet(Player yapan, Arsa arsa, OfflinePlayer hedef) {
        if (!arsa.sahibiVar()) {
            Mesaj.hata(yapan, "uye.sahipsiz-arsa");
            return false;
        }
        if (!yetki().davetEdebilir(yapan, arsa)) {
            Mesaj.hata(yapan, "uye.davet-yetkisi-yok");
            return false;
        }
        UUID h = hedef.getUniqueId();
        String ad = ad(hedef);
        if (h.equals(yapan.getUniqueId())) {
            Mesaj.hata(yapan, "uye.kendini-davet");
            return false;
        }
        if (arsa.sahipMi(h)) {
            Mesaj.hata(yapan, "uye.zaten-sahip", ad);
            return false;
        }
        ArsaUye mevcut = arsa.uye(h);
        if (mevcut != null) {
            Mesaj.hata(yapan, "uye.zaten-uye", ad, mevcut.rutbe().ad(d(yapan)));
            return false;
        }
        if (baskaArsadaMi(yapan, arsa, h, ad)) return false;
        arsa.uyeEkle(h, Rutbe.UYE);
        kaydet();
        Mesaj.basari(yapan, "uye.davet-basari", ad, Rutbe.UYE.ad(d(yapan)));
        Player hp = Bukkit.getPlayer(h);
        if (hp != null) Mesaj.gonder(hp, "uye.davet-bildirim", yapan.getName(), arsa.id());
        return true;
    }

    public boolean at(Player yapan, Arsa arsa, UUID hedef) {
        ArsaUye uye = arsa.uye(hedef);
        String ad = OyuncuBul.isim(hedef);
        if (uye == null) {
            Mesaj.hata(yapan, "uye.uye-degil", ad);
            return false;
        }
        if (!yetki().yonetebilir(yapan, arsa, hedef)) {
            yonetimRed(yapan, arsa, hedef);
            return false;
        }
        arsa.uyeCikar(hedef);
        kaydet();
        Mesaj.basari(yapan, "uye.atildi", ad);
        Player hp = Bukkit.getPlayer(hedef);
        if (hp != null) Mesaj.gonder(hp, "uye.atildi-bildirim", yapan.getName(), arsa.id());
        return true;
    }

    public boolean rutbeVer(Player yapan, Arsa arsa, OfflinePlayer hedefOyuncu, Rutbe rutbe) {
        UUID hedef = hedefOyuncu.getUniqueId();
        String ad = ad(hedefOyuncu);
        if (arsa.sahipMi(hedef)) {
            Mesaj.hata(yapan, "uye.sahip-rutbe");
            return false;
        }
        if (!yetki().yonetebilir(yapan, arsa, hedef)) {
            yonetimRed(yapan, arsa, hedef);
            return false;
        }
        if (!yetki().rutbeVerebilir(yapan, arsa, hedef, rutbe)) {
            Mesaj.hata(yapan, "uye.mod-sadece-sahip");
            return false;
        }
        ArsaUye uye = arsa.uye(hedef);
        boolean yeni = uye == null;
        if (yeni) {
            if (!yetki().davetEdebilir(yapan, arsa)) {
                Mesaj.hata(yapan, "uye.davet-yetkisi-yok");
                return false;
            }
            if (baskaArsadaMi(yapan, arsa, hedef, ad)) return false;
            uye = arsa.uyeEkle(hedef, rutbe);
        } else if (uye.rutbe() == rutbe) {
            Mesaj.hata(yapan, "uye.zaten-rutbe", ad, rutbe.ad(d(yapan)));
            return false;
        }
        uye.rutbeAyarla(rutbe);
        kaydet();
        Mesaj.basari(yapan, yeni ? "uye.eklendi-rutbe" : "uye.rutbe-guncellendi", ad, rutbe.ad(d(yapan)));
        Player hp = Bukkit.getPlayer(hedef);
        if (hp != null) Mesaj.gonder(hp, "uye.rutbe-bildirim", arsa.id(), rutbe.ad(d(hp)));
        return true;
    }

    /** Rütbeyi bir basamak değiştirir (GUI). */
    public boolean rutbeKaydir(Player yapan, Arsa arsa, UUID hedef, boolean yukari) {
        ArsaUye uye = arsa.uye(hedef);
        if (uye == null) return false;
        Rutbe yeni = yukari ? uye.rutbe().sonraki() : uye.rutbe().onceki();
        if (yeni == uye.rutbe()) {
            Mesaj.hata(yapan, yukari ? "uye.en-yuksek" : "uye.en-dusuk");
            return false;
        }
        return rutbeVer(yapan, arsa, Bukkit.getOfflinePlayer(hedef), yeni);
    }

    public boolean izinAyarla(Player yapan, Arsa arsa, OfflinePlayer hedefOyuncu, Izin izin, boolean deger, boolean sessiz) {
        UUID hedef = hedefOyuncu.getUniqueId();
        String ad = ad(hedefOyuncu);
        if (arsa.sahipMi(hedef)) {
            Mesaj.hata(yapan, "uye.sahip-tum-izinler");
            return false;
        }
        if (!yetki().yonetebilir(yapan, arsa, hedef)) {
            yonetimRed(yapan, arsa, hedef);
            return false;
        }
        ArsaUye uye = arsa.uye(hedef);
        if (uye == null) {
            if (!deger) {
                Mesaj.hata(yapan, "uye.uye-degil", ad);
                return false;
            }
            if (!yetki().davetEdebilir(yapan, arsa)) {
                Mesaj.hata(yapan, "uye.davet-yetkisi-yok");
                return false;
            }
            if (baskaArsadaMi(yapan, arsa, hedef, ad)) return false;
            uye = arsa.uyeEkle(hedef, Rutbe.UYE);
            Mesaj.bilgi(yapan, "uye.otomatik-eklendi", ad, Rutbe.UYE.ad(d(yapan)));
        }
        if (uye.rutbe() == Rutbe.MOD) {
            Mesaj.hata(yapan, "uye.mod-tum-izinler");
            return false;
        }
        uye.izinAyarla(izin, deger);
        kaydet();
        if (!sessiz) {
            Mesaj.basari(yapan, deger ? "uye.izin-acildi" : "uye.izin-kapandi", ad, izin.ad(d(yapan)));
        }
        return true;
    }

    /** Tüm izinleri aç (true), kapat (false) veya rütbe varsayılanına döndür (null). */
    public boolean tumIzinler(Player yapan, Arsa arsa, OfflinePlayer hedefOyuncu, Boolean deger) {
        UUID hedef = hedefOyuncu.getUniqueId();
        String ad = ad(hedefOyuncu);
        ArsaUye uye = arsa.uye(hedef);
        if (uye == null && deger != Boolean.TRUE) {
            Mesaj.hata(yapan, "uye.uye-degil", ad);
            return false;
        }
        if (deger == null) {
            if (!yetki().yonetebilir(yapan, arsa, hedef)) {
                yonetimRed(yapan, arsa, hedef);
                return false;
            }
            uye.ozelleriSifirla();
            kaydet();
            Mesaj.basari(yapan, "uye.varsayilana-dondu", ad);
            return true;
        }
        for (Izin i : Izin.values()) {
            if (!izinAyarla(yapan, arsa, hedefOyuncu, i, deger, true)) return false;
        }
        Mesaj.basari(yapan, deger ? "uye.tum-acildi" : "uye.tum-kapandi", ad);
        return true;
    }

    public boolean ziyaretciAyarla(Player yapan, Arsa arsa, Izin izin, boolean deger, boolean sessiz) {
        if (!yetki().ziyaretciDuzenleyebilir(yapan, arsa)) {
            Mesaj.hata(yapan, "ziyaretci.yetki-yok");
            return false;
        }
        if (!Izin.ZIYARETCI_IZINLERI.contains(izin)) {
            Mesaj.hata(yapan, "ziyaretci.verilemez", izin.ad(d(yapan)));
            return false;
        }
        arsa.ziyaretciAyarla(izin, deger);
        kaydet();
        if (!sessiz) {
            Mesaj.basari(yapan, deger ? "ziyaretci.acildi" : "ziyaretci.kapandi", izin.ad(d(yapan)));
        }
        return true;
    }

    /** Yönetim reddinin sebebini oyuncuya bildirir. */
    public void yonetimRed(Player yapan, Arsa arsa, UUID hedef) {
        if (hedef.equals(yapan.getUniqueId())) {
            Mesaj.hata(yapan, "uye.red.kendin");
            return;
        }
        if (arsa.sahipMi(hedef)) {
            Mesaj.hata(yapan, "uye.red.sahip");
            return;
        }
        ArsaUye h = arsa.uye(hedef);
        if (yetki().modMu(yapan, arsa) && h != null && h.rutbe() == Rutbe.MOD) {
            Mesaj.hata(yapan, "uye.red.mod-mod");
            return;
        }
        Mesaj.hata(yapan, "uye.red.genel");
    }

    private void kaydet() {
        plugin.depo().kaydetPlanla();
    }

    private static String ad(OfflinePlayer op) {
        String n = op.getName();
        return n != null ? n : OyuncuBul.isim(op.getUniqueId());
    }
}
