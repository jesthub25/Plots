package org.jestron.gelismisArsa.komut;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.Yetki;
import org.jestron.gelismisArsa.entegrasyon.WorldEditKopru;
import org.jestron.gelismisArsa.gui.AnaMenu;
import org.jestron.gelismisArsa.gui.ZiyaretciMenu;
import org.jestron.gelismisArsa.kira.KiraServisi;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.ArsaUye;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.model.Rutbe;
import org.jestron.gelismisArsa.util.Mesaj;
import org.jestron.gelismisArsa.util.OyuncuBul;
import org.jestron.gelismisArsa.util.Sure;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Tek ana komut: /arsa <alt komut>. Tüm alt komutlar ve tab tamamlama burada. */
public final class ArsaKomut implements TabExecutor {

    private static final List<String> OYUNCU_KOMUTLARI = Arrays.asList(
            "kirala", "terket", "sure", "bilgi", "sahip", "gui", "yetkiver", "yetkial", "izinler", "ziyaretci", "yardim");
    private static final List<String> ADMIN_KOMUTLARI = Arrays.asList(
            "olustur", "sil", "liste", "bypass", "sablon", "yenile");

    private static final String P_KIRALA = "gelismisarsa.kiralama.kirala";
    private static final String P_TERKET = "gelismisarsa.kiralama.terket";
    private static final String P_SURE = "gelismisarsa.kiralama.sure";
    private static final String P_GUI = "gelismisarsa.gui";
    private static final String P_OLUSTUR = "gelismisarsa.admin.olustur";
    private static final String P_SIL = "gelismisarsa.admin.sil";
    private static final String P_LISTE = "gelismisarsa.admin.liste";
    private static final String P_SABLON = "gelismisarsa.admin.sablon";
    private static final String P_YENILE = "gelismisarsa.admin.yenile";

    private final GelismisArsa plugin;

    public ArsaKomut(GelismisArsa plugin) {
        this.plugin = plugin;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Komut yönlendirme
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String alt = a.length == 0 ? "yardim" : normal(a[0]);

        // Konsoldan da çalışabilenler
        if (alt.equals("liste")) { liste(s, a); return true; }
        if (alt.equals("yenile")) { yenile(s); return true; }

        if (!(s instanceof Player p)) {
            Mesaj.gonder(s, "&cBu komut sadece oyuncular tarafından kullanılabilir.");
            return true;
        }

        switch (alt) {
            case "kirala": case "sahiplen": kirala(p, a); break;
            case "terket": case "birak": terket(p, a); break;
            case "sure": sure(p, a); break;
            case "bilgi": case "info": bilgi(p); break;
            case "sahip": sahip(p); break;
            case "gui": case "menu": case "panel": gui(p); break;
            case "yetkiver": case "davet": case "ekle": yetkiver(p, a); break;
            case "yetkial": case "at": yetkial(p, a); break;
            case "izinler": izinler(p); break;
            case "ziyaretci": ziyaretci(p, a); break;
            case "olustur": olustur(p, a); break;
            case "sil": sil(p, a); break;
            case "bypass": bypass(p); break;
            case "sablon": sablon(p); break;
            default: yardim(p); break;
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Oyuncu komutları
    // ═══════════════════════════════════════════════════════════════════════

    private void kirala(Player p, String[] a) {
        if (!yetkiVar(p, P_KIRALA)) return;
        KiraServisi kira = plugin.kira();
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "Kiralamak için bir arsanın içinde durmalısın.");
            return;
        }
        if (a.length < 2) {
            Mesaj.hata(p, "Kullanım: &f/arsa kirala <süre> &8(ör: 1w, 2w, 3d)");
            Mesaj.bilgi(p, "Haftalık ücret: &f" + kira.bicim(kira.ucret(604800L))
                    + " &8• &7En fazla: &f" + plugin.ayarlar().maxHafta + " hafta");
            return;
        }
        long saniye = Sure.saniyeyeCevir(a[1], "w");
        if (saniye <= 0) {
            Mesaj.hata(p, "Geçersiz süre. Örnek: &f1w &c(1 hafta), &f3d &c(3 gün)");
            return;
        }
        kira.kirala(p, arsa, saniye);
    }

    private void terket(Player p, String[] a) {
        if (!yetkiVar(p, P_TERKET)) return;
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null || !arsa.sahipMi(p.getUniqueId())) {
            Mesaj.hata(p, "Terk etmek için kendi arsanın içinde durmalısın.");
            return;
        }
        if (a.length < 2 || !normal(a[1]).equals("onayla")) {
            Mesaj.gonder(p, "&e⚠ &f#" + arsa.id() + " &enumaralı arsayı terk etmek üzeresin.");
            if (arsa.kiralik()) {
                Mesaj.satir(p, "&7  İade edilecek tutar: &f" + plugin.kira().bicim(plugin.kira().terkIadesi(arsa)));
            }
            Mesaj.satir(p, "&7  Tüm üyeler çıkarılır" + (plugin.ayarlar().bitinceSifirla ? " ve arsa sıfırlanır." : "."));
            Mesaj.satir(p, "&7  Onaylamak için: &f/arsa terket onayla");
            return;
        }
        plugin.kira().terket(p, arsa);
    }

    private void sure(Player p, String[] a) {
        if (!yetkiVar(p, P_SURE)) return;
        if (a.length < 3) {
            Mesaj.hata(p, "Kullanım: &f/arsa sure <arttır|azalt> <süre> &8(ör: 1w, 2d, 12h)");
            return;
        }
        Arsa arsa = hedefArsa(p);
        if (arsa == null) return;
        long saniye = Sure.saniyeyeCevir(a[2], "d");
        if (saniye <= 0) {
            Mesaj.hata(p, "Geçersiz süre. Örnek: &f1w&c, &f2d&c, &f12h");
            return;
        }
        String islem = normal(a[1]);
        if (islem.equals("arttir") || islem.equals("uzat") || islem.equals("ekle")) {
            plugin.kira().uzat(p, arsa, saniye);
        } else if (islem.equals("azalt") || islem.equals("kisalt")) {
            plugin.kira().kisalt(p, arsa, saniye);
        } else {
            Mesaj.hata(p, "Geçersiz işlem. &farttır &cveya &fazalt &ckullan.");
        }
    }

    private void bilgi(Player p) {
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "Bir arsanın içinde değilsin.");
            return;
        }
        Mesaj.satir(p, "&8&m          &r &6&lArsa #" + arsa.id() + " &8&m          ");
        Mesaj.satir(p, "&7Dünya: &f" + arsa.dunya() + " &8• &7Boyut: &f" + arsa.genislikX() + "x" + arsa.genislikZ()
                + " &8(y " + arsa.yukseklik() + ")");
        Mesaj.satir(p, "&7Konum: &f" + arsa.konumYazisi());
        if (!arsa.sahibiVar()) {
            Mesaj.satir(p, "&7Durum: &aSahipsiz &8• &7Kiralamak için: &f/arsa kirala <süre>");
            Mesaj.satir(p, "&7Haftalık ücret: &f" + plugin.kira().bicim(plugin.kira().ucret(604800L)));
            return;
        }
        Mesaj.satir(p, "&7Sahip: &f" + OyuncuBul.isim(arsa.sahip()));
        if (arsa.kiralik()) {
            Mesaj.satir(p, "&7Kira bitişi: &f" + Sure.tarih(arsa.kiraBitis()) + " &8(" + Sure.yaz(arsa.kalanMs()) + ")");
        }
        if (arsa.uyeler().isEmpty()) {
            Mesaj.satir(p, "&7Üyeler: &8yok");
        } else {
            List<String> l = new ArrayList<>();
            for (Map.Entry<UUID, ArsaUye> e : arsa.uyeler().entrySet()) {
                l.add("&f" + OyuncuBul.isim(e.getKey()) + " &8(" + e.getValue().rutbe().gorunenAd() + "&8)");
            }
            Mesaj.satir(p, "&7Üyeler: " + String.join("&7, ", l));
        }
        List<String> ziyaretci = new ArrayList<>();
        for (Izin i : arsa.ziyaretciIzinleri()) ziyaretci.add(i.ad());
        Mesaj.satir(p, "&7Ziyaretçi izinleri: " + (ziyaretci.isEmpty() ? "&8yok (tam koruma)" : "&f" + String.join(", ", ziyaretci)));

        List<String> benim = new ArrayList<>();
        for (Izin i : Izin.values()) if (plugin.yetki().izinli(p, arsa, i)) benim.add(i.anahtar());
        Mesaj.satir(p, "&7Senin izinlerin: " + (benim.isEmpty() ? "&cyok" : "&a" + String.join("&7, &a", benim)));
    }

    private void sahip(Player p) {
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "Bir arsanın içinde değilsin.");
        } else if (arsa.sahibiVar()) {
            Mesaj.bilgi(p, "&f#" + arsa.id() + " &7numaralı arsanın sahibi: &f" + OyuncuBul.isim(arsa.sahip()));
        } else {
            Mesaj.bilgi(p, "Bu arsa sahipsiz. Kiralamak için: &f/arsa kirala <süre>");
        }
    }

    private void gui(Player p) {
        if (!yetkiVar(p, P_GUI)) return;
        Arsa arsa = hedefArsa(p);
        if (arsa == null) return;
        if (!plugin.yetki().panel(p, arsa)) {
            Mesaj.hata(p, "Yönetim panelini sadece arsa sahibi ve MOD'lar açabilir.");
            return;
        }
        Mesaj.sesSayfa(p);
        new AnaMenu(plugin, p, arsa).ac();
    }

    /**
     * /arsa yetkiver <oyuncu>                → üye olarak davet et
     * /arsa yetkiver <oyuncu> <rütbe>        → rütbe ver (üye değilse ekler)
     * /arsa yetkiver <oyuncu> <izin|hepsi>   → izin aç (üye değilse ekler)
     */
    private void yetkiver(Player p, String[] a) {
        if (a.length < 2) {
            Mesaj.hata(p, "Kullanım: &f/arsa yetkiver <oyuncu> [rütbe|izin|hepsi]");
            Mesaj.bilgi(p, "Rütbeler: &f" + rutbeListesi() + " &8• &7İzinler: &f/arsa izinler");
            return;
        }
        Arsa arsa = hedefArsa(p);
        if (arsa == null) return;
        OfflinePlayer hedef = OyuncuBul.bul(a[1]);
        if (hedef == null) {
            Mesaj.hata(p, "'" + a[1] + "' adında bir oyuncu bulunamadı (sunucuya en az bir kez girmiş olmalı).");
            return;
        }
        if (a.length == 2) {
            plugin.uyeServisi().davet(p, arsa, hedef);
            return;
        }
        String deger = a[2];
        if (normal(deger).equals("hepsi") || normal(deger).equals("tumu")) {
            plugin.uyeServisi().tumIzinler(p, arsa, hedef, Boolean.TRUE);
            return;
        }
        Rutbe r = Rutbe.bul(deger);
        if (r != null) {
            plugin.uyeServisi().rutbeVer(p, arsa, hedef, r);
            return;
        }
        Izin i = Izin.bul(deger);
        if (i != null) {
            plugin.uyeServisi().izinAyarla(p, arsa, hedef, i, true, false);
            return;
        }
        Mesaj.hata(p, "'" + deger + "' geçerli bir rütbe veya izin değil.");
        Mesaj.bilgi(p, "Rütbeler: &f" + rutbeListesi() + " &8• &7İzinler: &f" + String.join(", ", Izin.anahtarlar()));
    }

    /**
     * /arsa yetkial <oyuncu>                 → arsadan at
     * /arsa yetkial <oyuncu> <izin|hepsi>    → izni kapat
     */
    private void yetkial(Player p, String[] a) {
        if (a.length < 2) {
            Mesaj.hata(p, "Kullanım: &f/arsa yetkial <oyuncu> [izin|hepsi]");
            return;
        }
        Arsa arsa = hedefArsa(p);
        if (arsa == null) return;
        OfflinePlayer hedef = OyuncuBul.bul(a[1]);
        if (hedef == null) {
            UUID uyeUuid = uyeIsmindenBul(arsa, a[1]);
            if (uyeUuid != null) hedef = Bukkit.getOfflinePlayer(uyeUuid);
        }
        if (hedef == null) {
            Mesaj.hata(p, "'" + a[1] + "' adında bir oyuncu bulunamadı.");
            return;
        }
        if (a.length == 2) {
            plugin.uyeServisi().at(p, arsa, hedef.getUniqueId());
            return;
        }
        String deger = a[2];
        if (normal(deger).equals("hepsi") || normal(deger).equals("tumu")) {
            plugin.uyeServisi().tumIzinler(p, arsa, hedef, Boolean.FALSE);
            return;
        }
        Izin i = Izin.bul(deger);
        if (i == null) {
            Mesaj.hata(p, "'" + deger + "' geçerli bir izin değil. İzinler: &f" + String.join(", ", Izin.anahtarlar()));
            return;
        }
        plugin.uyeServisi().izinAyarla(p, arsa, hedef, i, false, false);
    }

    private void izinler(Player p) {
        Mesaj.satir(p, "&8&m        &r &6&lArsa İzinleri &8&m        ");
        for (Izin i : Izin.values()) {
            Mesaj.satir(p, "&e" + i.anahtar() + " &8- &f" + i.ad() + " &8» &7" + String.join(" ", i.aciklama()));
        }
        Mesaj.satir(p, "&6Rütbeler: &f" + rutbeListesi());
        Mesaj.satir(p, "&7Ver: &f/arsa yetkiver <oyuncu> <izin|rütbe|hepsi>");
        Mesaj.satir(p, "&7Al: &f/arsa yetkial <oyuncu> <izin|hepsi> &8(izin yazmazsan arsadan atar)");
    }

    /** /arsa ziyaretci → menü, /arsa ziyaretci <izin|hepsi> <ac|kapat> */
    private void ziyaretci(Player p, String[] a) {
        Arsa arsa = hedefArsa(p);
        if (arsa == null) return;
        if (a.length < 3) {
            if (plugin.yetki().ziyaretciDuzenleyebilir(p, arsa)) {
                new ZiyaretciMenu(plugin, p, arsa).ac();
            } else {
                Mesaj.hata(p, "Ziyaretçi izinlerini sadece arsa sahibi ve MOD'lar değiştirebilir.");
            }
            return;
        }
        String d = normal(a[2]);
        boolean deger;
        if (d.equals("ac") || d.equals("acik") || d.equals("evet") || d.equals("on")) deger = true;
        else if (d.equals("kapat") || d.equals("kapali") || d.equals("hayir") || d.equals("off")) deger = false;
        else {
            Mesaj.hata(p, "Kullanım: &f/arsa ziyaretci <izin|hepsi> <aç|kapat>");
            return;
        }
        if (normal(a[1]).equals("hepsi")) {
            for (Izin i : Izin.ZIYARETCI_IZINLERI) {
                if (!plugin.uyeServisi().ziyaretciAyarla(p, arsa, i, deger, true)) return;
            }
            Mesaj.basari(p, "Ziyaretçiler için tüm izinler " + (deger ? "&2AÇILDI" : "&cKAPATILDI") + "&a.");
            return;
        }
        Izin i = Izin.bul(a[1]);
        if (i == null) {
            Mesaj.hata(p, "'" + a[1] + "' geçerli bir izin değil.");
            return;
        }
        plugin.uyeServisi().ziyaretciAyarla(p, arsa, i, deger, false);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Admin komutları
    // ═══════════════════════════════════════════════════════════════════════

    private void olustur(Player p, String[] a) {
        if (!yetkiVar(p, P_OLUSTUR)) return;
        int[] k;
        if (a.length == 7) {
            k = new int[6];
            try {
                for (int i = 0; i < 6; i++) k[i] = koordinat(a[i + 1], p, i % 3);
            } catch (NumberFormatException e) {
                Mesaj.hata(p, "Koordinatlar tam sayı olmalı (veya ~ kullan).");
                return;
            }
        } else if (a.length == 1) {
            WorldEditKopru we = plugin.worldEdit();
            if (we == null) {
                Mesaj.hata(p, "Kullanım: &f/arsa olustur <x1> <y1> <z1> <x2> <y2> <z2>");
                return;
            }
            k = we.secim(p);
            if (k == null) {
                Mesaj.hata(p, "Önce WorldEdit ile bir bölge seç (//wand) veya koordinat gir.");
                return;
            }
        } else {
            Mesaj.hata(p, "Kullanım: &f/arsa olustur &8(WorldEdit seçimi) &cveya &f/arsa olustur <x1> <y1> <z1> <x2> <y2> <z2>");
            return;
        }
        String dunya = p.getWorld().getName();
        Arsa cakisan = plugin.arsalar().cakisan(dunya, k[0], k[1], k[2], k[3], k[4], k[5]);
        if (cakisan != null) {
            Mesaj.hata(p, "Bu bölge &f#" + cakisan.id() + " &cnumaralı arsa ile çakışıyor.");
            return;
        }
        Arsa arsa = plugin.arsalar().olustur(dunya, k[0], k[1], k[2], k[3], k[4], k[5]);
        plugin.depo().kaydetPlanla();
        boolean sablon = false;
        if (plugin.worldEdit() != null) {
            sablon = plugin.worldEdit().sablonKaydet(arsa, plugin.kira().sablonDosyasi(arsa));
        }
        Mesaj.basari(p, "&f#" + arsa.id() + " &anumaralı arsa oluşturuldu &8(" + arsa.genislikX() + "x"
                + arsa.genislikZ() + "x" + arsa.yukseklik() + ")" + (sablon ? " &7• şablon kaydedildi" : ""));
    }

    private void sil(Player p, String[] a) {
        if (!yetkiVar(p, P_SIL)) return;
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "Silinecek arsanın içinde durmalısın.");
            return;
        }
        if (a.length < 2 || !normal(a[1]).equals("onayla")) {
            Mesaj.gonder(p, "&e⚠ &f#" + arsa.id() + " &earsası " + (arsa.sahibiVar()
                    ? "(sahibi: &f" + OyuncuBul.isim(arsa.sahip()) + "&e) " : "") + "kalıcı olarak silinecek.");
            Mesaj.satir(p, "&7  Onaylamak için: &f/arsa sil onayla");
            return;
        }
        UUID eskiSahip = arsa.sahip();
        plugin.arsalar().sil(arsa);
        File sablon = plugin.kira().sablonDosyasi(arsa);
        if (sablon.exists() && !sablon.delete()) {
            plugin.getLogger().warning("Şablon dosyası silinemedi: " + sablon.getName());
        }
        plugin.depo().kaydetPlanla();
        Mesaj.basari(p, "&f#" + arsa.id() + " &anumaralı arsa silindi.");
        if (eskiSahip != null) {
            Player sahip = Bukkit.getPlayer(eskiSahip);
            if (sahip != null && !sahip.equals(p)) {
                Mesaj.hata(sahip, "&f#" + arsa.id() + " &cnumaralı arsan bir yetkili tarafından silindi.");
            }
        }
    }

    private void liste(CommandSender s, String[] a) {
        if (!yetkiVar(s, P_LISTE)) return;
        List<Arsa> tum = new ArrayList<>(plugin.arsalar().tumu());
        if (tum.isEmpty()) {
            Mesaj.hata(s, "Kayıtlı arsa yok.");
            return;
        }
        int sayfaBoyutu = 10;
        int toplam = (tum.size() + sayfaBoyutu - 1) / sayfaBoyutu;
        int sayfa = 1;
        if (a.length > 1) {
            try {
                sayfa = Integer.parseInt(a[1]);
            } catch (NumberFormatException e) {
                Mesaj.hata(s, "Geçersiz sayfa numarası.");
                return;
            }
        }
        sayfa = Math.max(1, Math.min(toplam, sayfa));
        Mesaj.satir(s, "&8&m      &r &6&lArsalar &8(" + sayfa + "/" + toplam + ") &8&m      ");
        for (int i = (sayfa - 1) * sayfaBoyutu; i < Math.min(tum.size(), sayfa * sayfaBoyutu); i++) {
            Arsa x = tum.get(i);
            String durum = !x.sahibiVar() ? "&aSahipsiz"
                    : "&f" + OyuncuBul.isim(x.sahip()) + (x.kiralik() ? " &8(" + Sure.yaz(x.kalanMs()) + ")" : "");
            Mesaj.satir(s, "&e#" + x.id() + " &8» " + durum + " &8• &7" + x.dunya() + " &8" + x.konumYazisi());
        }
        if (sayfa < toplam) Mesaj.satir(s, "&7Sonraki sayfa: &f/arsa liste " + (sayfa + 1));
    }

    private void bypass(Player p) {
        if (!yetkiVar(p, Yetki.PERM_BYPASS)) return;
        boolean acik = plugin.yetki().bypassDegistir(p);
        if (acik) Mesaj.basari(p, "Koruma atlatma &2AÇIK&a. Tüm arsalarda her şeyi yapabilirsin.");
        else Mesaj.basari(p, "Koruma atlatma &cKAPALI&a. Arsa korumaları sana da uygulanır.");
    }

    private void sablon(Player p) {
        if (!yetkiVar(p, P_SABLON)) return;
        WorldEditKopru we = plugin.worldEdit();
        if (we == null) {
            Mesaj.hata(p, "Bu özellik için WorldEdit gerekli.");
            return;
        }
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "Şablonu kaydedilecek arsanın içinde durmalısın.");
            return;
        }
        if (we.sablonKaydet(arsa, plugin.kira().sablonDosyasi(arsa))) {
            Mesaj.basari(p, "&f#" + arsa.id() + " &aarsasının şu anki hali sıfırlama şablonu olarak kaydedildi.");
        } else {
            Mesaj.hata(p, "Şablon kaydedilemedi, konsolu kontrol et.");
        }
    }

    private void yenile(CommandSender s) {
        if (!yetkiVar(s, P_YENILE)) return;
        plugin.ayarlariYenile();
        Mesaj.basari(s, "config.yml yeniden yüklendi.");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Yardım
    // ═══════════════════════════════════════════════════════════════════════

    private void yardim(Player p) {
        Mesaj.satir(p, "&8&m          &r &6&lGelismisArsa &8&m          ");
        Mesaj.satir(p, "&e/arsa kirala <süre> &8» &7Bulunduğun sahipsiz arsayı kirala");
        Mesaj.satir(p, "&e/arsa terket &8» &7Arsanı bırak, kalan süre iade edilir");
        Mesaj.satir(p, "&e/arsa sure <arttır|azalt> <süre> &8» &7Kira süresini yönet");
        Mesaj.satir(p, "&e/arsa bilgi &8» &7Arsa bilgileri ve senin izinlerin");
        Mesaj.satir(p, "&e/arsa gui &8» &7Yönetim paneli (sahip ve MOD)");
        Mesaj.satir(p, "&e/arsa yetkiver <oyuncu> [rütbe|izin|hepsi] &8» &7Davet / yetki ver");
        Mesaj.satir(p, "&e/arsa yetkial <oyuncu> [izin|hepsi] &8» &7Yetki al / arsadan at");
        Mesaj.satir(p, "&e/arsa ziyaretci [izin] [aç|kapat] &8» &7Ziyaretçi izinleri");
        Mesaj.satir(p, "&e/arsa izinler &8» &7Tüm izinlerin listesi");
        if (adminMi(p)) {
            Mesaj.satir(p, "&c/arsa olustur [x1 y1 z1 x2 y2 z2] &8» &7Arsa oluştur (boş = WorldEdit seçimi)");
            Mesaj.satir(p, "&c/arsa sil onayla &8» &7Bulunduğun arsayı sil");
            Mesaj.satir(p, "&c/arsa liste [sayfa] &8» &7Tüm arsalar");
            Mesaj.satir(p, "&c/arsa sablon &8» &7Arsanın sıfırlama şablonunu kaydet");
            Mesaj.satir(p, "&c/arsa bypass &8» &7Koruma atlatmayı aç/kapat");
            Mesaj.satir(p, "&c/arsa yenile &8» &7config.yml yeniden yükle");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tab tamamlama
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 1) {
            List<String> l = new ArrayList<>(OYUNCU_KOMUTLARI);
            for (String k : ADMIN_KOMUTLARI) if (adminKomutIzni(s, k)) l.add(k);
            return eslesen(a[0], l);
        }
        String alt = normal(a[0]);
        Player p = s instanceof Player pl ? pl : null;
        switch (alt) {
            case "kirala":
                if (a.length == 2) return eslesen(a[1], Arrays.asList("1w", "2w", "3w", "4w", "1d", "3d"));
                break;
            case "sure":
                if (a.length == 2) return eslesen(a[1], Arrays.asList("arttır", "azalt"));
                if (a.length == 3) return eslesen(a[2], Arrays.asList("1d", "3d", "1w", "2w", "12h"));
                break;
            case "terket":
            case "sil":
                if (a.length == 2) return eslesen(a[1], Collections.singletonList("onayla"));
                break;
            case "yetkiver":
                if (a.length == 2) return eslesen(a[1], cevrimiciIsimler(p));
                if (a.length == 3) {
                    List<String> l = new ArrayList<>();
                    for (Rutbe r : Rutbe.values()) l.add(r.anahtar());
                    l.addAll(Izin.anahtarlar());
                    l.add("hepsi");
                    return eslesen(a[2], l);
                }
                break;
            case "yetkial":
                if (a.length == 2) return eslesen(a[1], uyeIsimleri(p));
                if (a.length == 3) {
                    List<String> l = new ArrayList<>(Izin.anahtarlar());
                    l.add("hepsi");
                    return eslesen(a[2], l);
                }
                break;
            case "ziyaretci":
                if (a.length == 2) {
                    List<String> l = new ArrayList<>();
                    for (Izin i : Izin.ZIYARETCI_IZINLERI) l.add(i.anahtar());
                    l.add("hepsi");
                    return eslesen(a[1], l);
                }
                if (a.length == 3) return eslesen(a[2], Arrays.asList("aç", "kapat"));
                break;
            case "olustur":
                if (p != null && a.length >= 2 && a.length <= 7) {
                    int eksen = (a.length - 2) % 3;
                    int deger = eksen == 0 ? p.getLocation().getBlockX()
                            : eksen == 1 ? p.getLocation().getBlockY() : p.getLocation().getBlockZ();
                    return eslesen(a[a.length - 1], Arrays.asList("~", String.valueOf(deger)));
                }
                break;
            case "liste":
                if (a.length == 2) {
                    int toplam = Math.max(1, (plugin.arsalar().tumu().size() + 9) / 10);
                    List<String> l = new ArrayList<>();
                    for (int i = 1; i <= Math.min(toplam, 20); i++) l.add(String.valueOf(i));
                    return eslesen(a[1], l);
                }
                break;
            default:
                break;
        }
        return Collections.emptyList();
    }

    private List<String> cevrimiciIsimler(Player haric) {
        List<String> l = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (haric != null && (o.equals(haric) || !haric.canSee(o))) continue;
            l.add(o.getName());
        }
        return l;
    }

    private List<String> uyeIsimleri(Player p) {
        if (p == null) return Collections.emptyList();
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            List<Arsa> sahip = plugin.arsalar().sahipArsalari(p.getUniqueId());
            if (sahip.size() == 1) arsa = sahip.get(0);
        }
        if (arsa == null) return Collections.emptyList();
        List<String> l = new ArrayList<>();
        for (UUID u : arsa.uyeler().keySet()) l.add(OyuncuBul.isim(u));
        return l;
    }

    private static List<String> eslesen(String girdi, Collection<String> secenekler) {
        return StringUtil.copyPartialMatches(girdi, secenekler, new ArrayList<>());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Yardımcılar
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * İşlem yapılacak arsa: önce bulunduğun arsa; değilse ve tek bir arsan varsa o.
     */
    private Arsa hedefArsa(Player p) {
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa != null) return arsa;
        List<Arsa> sahip = plugin.arsalar().sahipArsalari(p.getUniqueId());
        if (sahip.size() == 1) return sahip.get(0);
        Mesaj.hata(p, sahip.isEmpty() ? "Bir arsanın içinde durmalısın."
                : "Birden fazla arsan var; işlem yapmak istediğin arsanın içinde dur.");
        return null;
    }

    private UUID uyeIsmindenBul(Arsa arsa, String ad) {
        for (UUID u : arsa.uyeler().keySet()) {
            if (OyuncuBul.isim(u).equalsIgnoreCase(ad)) return u;
        }
        return null;
    }

    private static int koordinat(String g, Player p, int eksen) {
        int taban = eksen == 0 ? p.getLocation().getBlockX()
                : eksen == 1 ? p.getLocation().getBlockY() : p.getLocation().getBlockZ();
        if (g.startsWith("~")) {
            return g.length() == 1 ? taban : taban + Integer.parseInt(g.substring(1));
        }
        return Integer.parseInt(g);
    }

    private static boolean yetkiVar(CommandSender s, String perm) {
        if (s.hasPermission(perm)) return true;
        Mesaj.hata(s, "Bu komutu kullanma yetkin yok.");
        return false;
    }

    private static boolean adminMi(CommandSender s) {
        for (String k : ADMIN_KOMUTLARI) if (adminKomutIzni(s, k)) return true;
        return false;
    }

    private static boolean adminKomutIzni(CommandSender s, String komut) {
        return s.hasPermission(komut.equals("bypass") ? Yetki.PERM_BYPASS : "gelismisarsa.admin." + komut);
    }

    private static String rutbeListesi() {
        List<String> l = new ArrayList<>();
        for (Rutbe r : Rutbe.values()) l.add(r.anahtar());
        return String.join(", ", l);
    }

    private static String normal(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replace("i̇", "i").replace("ı", "i").replace("ş", "s").replace("ğ", "g")
                .replace("ü", "u").replace("ö", "o").replace("ç", "c");
    }
}
