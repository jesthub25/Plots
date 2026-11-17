package org.jestron.gelismisArsa;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.ArsaUye;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.model.Rutbe;
import org.jestron.gelismisArsa.util.Mesaj;
import org.jestron.gelismisArsa.util.OyuncuBul;

import java.util.UUID;

/**
 * Üye işlemleri. Komutlar ve GUI aynı metotları kullanır; böylece kurallar tek yerde uygulanır.
 * Her metot yetkiyi kendisi doğrular, mesaj gönderir ve değişikliği kaydeder.
 */
public final class UyeServisi {

    private final GelismisArsa plugin;

    public UyeServisi(GelismisArsa plugin) {
        this.plugin = plugin;
    }

    private Yetki yetki() {
        return plugin.yetki();
    }

    public boolean davet(Player yapan, Arsa arsa, OfflinePlayer hedef) {
        if (!arsa.sahibiVar()) {
            Mesaj.hata(yapan, "Sahipsiz arsaya üye eklenemez.");
            return false;
        }
        if (!yetki().davetEdebilir(yapan, arsa)) {
            Mesaj.hata(yapan, "Bu arsaya üye davet etme yetkin yok.");
            return false;
        }
        UUID h = hedef.getUniqueId();
        String ad = ad(hedef);
        if (h.equals(yapan.getUniqueId())) {
            Mesaj.hata(yapan, "Kendini davet edemezsin.");
            return false;
        }
        if (arsa.sahipMi(h)) {
            Mesaj.hata(yapan, ad + " zaten bu arsanın sahibi.");
            return false;
        }
        ArsaUye mevcut = arsa.uye(h);
        if (mevcut != null) {
            Mesaj.hata(yapan, ad + " zaten üye (" + mevcut.rutbe().gorunenAd() + "&c).");
            return false;
        }
        arsa.uyeEkle(h, Rutbe.UYE);
        kaydet();
        Mesaj.basari(yapan, "&f" + ad + " &aarsaya " + Rutbe.UYE.gorunenAd() + " &aolarak eklendi.");
        bildir(h, "&a" + yapan.getName() + " seni &f#" + arsa.id() + " &anumaralı arsaya üye olarak ekledi.");
        return true;
    }

    public boolean at(Player yapan, Arsa arsa, UUID hedef) {
        ArsaUye uye = arsa.uye(hedef);
        String ad = OyuncuBul.isim(hedef);
        if (uye == null) {
            Mesaj.hata(yapan, ad + " bu arsanın üyesi değil.");
            return false;
        }
        if (!yetki().yonetebilir(yapan, arsa, hedef)) {
            Mesaj.hata(yapan, yonetimRedSebebi(yapan, arsa, hedef));
            return false;
        }
        arsa.uyeCikar(hedef);
        kaydet();
        Mesaj.basari(yapan, "&f" + ad + " &aarsadan atıldı.");
        bildir(hedef, "&c" + yapan.getName() + " seni &f#" + arsa.id() + " &cnumaralı arsadan attı.");
        return true;
    }

    public boolean rutbeVer(Player yapan, Arsa arsa, OfflinePlayer hedefOyuncu, Rutbe rutbe) {
        UUID hedef = hedefOyuncu.getUniqueId();
        String ad = ad(hedefOyuncu);
        if (arsa.sahipMi(hedef)) {
            Mesaj.hata(yapan, "Arsa sahibinin rütbesi değiştirilemez.");
            return false;
        }
        if (!yetki().yonetebilir(yapan, arsa, hedef)) {
            Mesaj.hata(yapan, yonetimRedSebebi(yapan, arsa, hedef));
            return false;
        }
        if (!yetki().rutbeVerebilir(yapan, arsa, hedef, rutbe)) {
            Mesaj.hata(yapan, "MOD rütbesini sadece arsa sahibi verebilir.");
            return false;
        }
        ArsaUye uye = arsa.uye(hedef);
        boolean yeni = uye == null;
        if (yeni) {
            if (!yetki().davetEdebilir(yapan, arsa)) {
                Mesaj.hata(yapan, "Bu arsaya üye davet etme yetkin yok.");
                return false;
            }
            uye = arsa.uyeEkle(hedef, rutbe);
        } else if (uye.rutbe() == rutbe) {
            Mesaj.hata(yapan, ad + " zaten " + rutbe.gorunenAd() + "&c.");
            return false;
        }
        uye.rutbeAyarla(rutbe);
        kaydet();
        Mesaj.basari(yapan, "&f" + ad + " &a" + (yeni ? "arsaya eklendi, rütbesi: " : "rütbesi güncellendi: ")
                + rutbe.gorunenAd());
        bildir(hedef, "&a#" + arsa.id() + " numaralı arsadaki rütben: " + rutbe.gorunenAd());
        return true;
    }

    /** Rütbeyi bir basamak değiştirir (GUI). */
    public boolean rutbeKaydir(Player yapan, Arsa arsa, UUID hedef, boolean yukari) {
        ArsaUye uye = arsa.uye(hedef);
        if (uye == null) return false;
        Rutbe yeni = yukari ? uye.rutbe().sonraki() : uye.rutbe().onceki();
        if (yeni == uye.rutbe()) {
            Mesaj.hata(yapan, yukari ? "Bu üye zaten en yüksek rütbede." : "Bu üye zaten en düşük rütbede.");
            return false;
        }
        return rutbeVer(yapan, arsa, Bukkit.getOfflinePlayer(hedef), yeni);
    }

    public boolean izinAyarla(Player yapan, Arsa arsa, OfflinePlayer hedefOyuncu, Izin izin, boolean deger, boolean sessiz) {
        UUID hedef = hedefOyuncu.getUniqueId();
        String ad = ad(hedefOyuncu);
        if (arsa.sahipMi(hedef)) {
            Mesaj.hata(yapan, "Arsa sahibi zaten tüm izinlere sahiptir.");
            return false;
        }
        if (!yetki().yonetebilir(yapan, arsa, hedef)) {
            Mesaj.hata(yapan, yonetimRedSebebi(yapan, arsa, hedef));
            return false;
        }
        ArsaUye uye = arsa.uye(hedef);
        if (uye == null) {
            if (!deger) {
                Mesaj.hata(yapan, ad + " bu arsanın üyesi değil.");
                return false;
            }
            if (!yetki().davetEdebilir(yapan, arsa)) {
                Mesaj.hata(yapan, "Bu arsaya üye davet etme yetkin yok.");
                return false;
            }
            uye = arsa.uyeEkle(hedef, Rutbe.UYE);
            Mesaj.bilgi(yapan, "&f" + ad + " &7arsaya " + Rutbe.UYE.gorunenAd() + " &7olarak eklendi.");
        }
        if (uye.rutbe() == Rutbe.MOD) {
            Mesaj.hata(yapan, "MOD rütbesi tüm izinlere sahiptir. Önce rütbesini düşür.");
            return false;
        }
        uye.izinAyarla(izin, deger);
        kaydet();
        if (!sessiz) {
            Mesaj.basari(yapan, "&f" + ad + " &aiçin &f" + izin.ad() + " &aizni "
                    + (deger ? "&2AÇILDI" : "&cKAPATILDI") + "&a.");
        }
        return true;
    }

    /** Tüm izinleri aç (true), kapat (false) veya rütbe varsayılanına döndür (null). */
    public boolean tumIzinler(Player yapan, Arsa arsa, OfflinePlayer hedefOyuncu, Boolean deger) {
        UUID hedef = hedefOyuncu.getUniqueId();
        ArsaUye uye = arsa.uye(hedef);
        if (uye == null && deger != Boolean.TRUE) {
            Mesaj.hata(yapan, ad(hedefOyuncu) + " bu arsanın üyesi değil.");
            return false;
        }
        if (deger == null) {
            if (!yetki().yonetebilir(yapan, arsa, hedef)) {
                Mesaj.hata(yapan, yonetimRedSebebi(yapan, arsa, hedef));
                return false;
            }
            uye.ozelleriSifirla();
            kaydet();
            Mesaj.basari(yapan, "&f" + ad(hedefOyuncu) + " &aizinleri rütbe varsayılanına döndürüldü.");
            return true;
        }
        for (Izin i : Izin.values()) {
            if (!izinAyarla(yapan, arsa, hedefOyuncu, i, deger, true)) return false;
        }
        Mesaj.basari(yapan, "&f" + ad(hedefOyuncu) + " &aiçin tüm izinler " + (deger ? "&2AÇILDI" : "&cKAPATILDI") + "&a.");
        return true;
    }

    public boolean ziyaretciAyarla(Player yapan, Arsa arsa, Izin izin, boolean deger, boolean sessiz) {
        if (!yetki().ziyaretciDuzenleyebilir(yapan, arsa)) {
            Mesaj.hata(yapan, "Ziyaretçi izinlerini sadece arsa sahibi ve MOD'lar değiştirebilir.");
            return false;
        }
        if (!Izin.ZIYARETCI_IZINLERI.contains(izin)) {
            Mesaj.hata(yapan, izin.ad() + " izni ziyaretçilere verilemez.");
            return false;
        }
        arsa.ziyaretciAyarla(izin, deger);
        kaydet();
        if (!sessiz) {
            Mesaj.basari(yapan, "Ziyaretçiler için &f" + izin.ad() + " &aizni " + (deger ? "&2AÇILDI" : "&cKAPATILDI") + "&a.");
        }
        return true;
    }

    public String yonetimRedSebebi(Player yapan, Arsa arsa, UUID hedef) {
        if (hedef.equals(yapan.getUniqueId())) return "Kendi yetkilerini değiştiremezsin.";
        if (arsa.sahipMi(hedef)) return "Arsa sahibine hiçbir işlem yapılamaz.";
        ArsaUye h = arsa.uye(hedef);
        if (yetki().modMu(yapan, arsa) && h != null && h.rutbe() == Rutbe.MOD) {
            return "MOD'lar diğer MOD'ları yönetemez.";
        }
        return "Bu işlem için arsa sahibi veya MOD olmalısın.";
    }

    private void bildir(UUID hedef, String mesaj) {
        Player p = Bukkit.getPlayer(hedef);
        if (p != null) Mesaj.gonder(p, mesaj);
    }

    private void kaydet() {
        plugin.depo().kaydetPlanla();
    }

    private static String ad(OfflinePlayer op) {
        String n = op.getName();
        return n != null ? n : OyuncuBul.isim(op.getUniqueId());
    }
}
