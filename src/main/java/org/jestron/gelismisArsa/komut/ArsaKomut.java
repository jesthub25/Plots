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
import org.jestron.gelismisArsa.dil.Dil;
import org.jestron.gelismisArsa.dil.L;
import org.jestron.gelismisArsa.entegrasyon.WorldEditKopru;
import org.jestron.gelismisArsa.gui.AnaMenu;
import org.jestron.gelismisArsa.gui.ArsaListeMenu;
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
import java.util.Map;
import java.util.UUID;

/**
 * İki dilli ana komut: /arsa (Türkçe) ve /plot (İngilizce).
 * Kullanılan komut oyuncunun dilini belirler; tüm mesajlar, GUI'ler ve tab tamamlama o dilde olur.
 * Alt komutların iki dildeki adları da her iki komutta kabul edilir.
 */
public final class ArsaKomut implements TabExecutor {

    private enum Alt {
        KIRALA("kirala", "rent", false, "sahiplen", "claim"),
        TERKET("terket", "leave", false, "birak", "abandon"),
        SURE("sure", "time", false, "süre"),
        BILGI("bilgi", "info", false),
        SAHIP("sahip", "owner", false),
        GUI("gui", "gui", false, "menu", "panel"),
        YETKIVER("yetkiver", "trust", false, "davet", "ekle", "add", "invite"),
        YETKIAL("yetkial", "untrust", false, "at", "kick", "remove"),
        ZIYARETCI("ziyaretci", "visitor", false, "ziyaretçi", "visitors"),
        IZINLER("izinler", "perms", false, "permissions", "flags"),
        YARDIM("yardim", "help", false, "yardım"),
        OLUSTUR("olustur", "create", true, "oluştur"),
        SIL("sil", "delete", true),
        LISTE("liste", "list", true),
        BYPASS("bypass", "bypass", true),
        SABLON("sablon", "template", true, "şablon"),
        YENILE("yenile", "reload", true);

        final String tr;
        final String en;
        final boolean admin;
        final String[] diger;

        Alt(String tr, String en, boolean admin, String... diger) {
            this.tr = tr;
            this.en = en;
            this.admin = admin;
            this.diger = diger;
        }

        String ad(Dil d) {
            return d == Dil.EN ? en : tr;
        }

        String yetki() {
            switch (this) {
                case KIRALA: return "gelismisarsa.kiralama.kirala";
                case TERKET: return "gelismisarsa.kiralama.terket";
                case SURE: return "gelismisarsa.kiralama.sure";
                case GUI: return "gelismisarsa.gui";
                case BYPASS: return Yetki.PERM_BYPASS;
                case OLUSTUR: return "gelismisarsa.admin.olustur";
                case SIL: return "gelismisarsa.admin.sil";
                case LISTE: return ArsaListeMenu.YETKI;
                case SABLON: return "gelismisarsa.admin.sablon";
                case YENILE: return "gelismisarsa.admin.yenile";
                default: return null;
            }
        }

        static Alt bul(String s) {
            String g = normal(s);
            for (Alt a : values()) {
                if (normal(a.tr).equals(g) || normal(a.en).equals(g)) return a;
                for (String d : a.diger) if (normal(d).equals(g)) return a;
            }
            return null;
        }
    }

    private final GelismisArsa plugin;

    public ArsaKomut(GelismisArsa plugin) {
        this.plugin = plugin;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Yönlendirme
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        Dil dil = Dil.komuttan(cmd.getName());
        plugin.diller().kullan(s, dil);

        Alt alt = a.length == 0 ? Alt.YARDIM : Alt.bul(a[0]);
        if (alt == null) alt = Alt.YARDIM;

        String yetki = alt.yetki();
        if (yetki != null && !s.hasPermission(yetki)) {
            Mesaj.hata(s, "genel.yetki-yok");
            return true;
        }

        if (alt == Alt.LISTE && !(s instanceof Player)) { listeSohbet(s, a); return true; }
        if (alt == Alt.YENILE) { yenile(s); return true; }
        if (alt == Alt.YARDIM) { yardim(s, dil); return true; }

        if (!(s instanceof Player p)) {
            Mesaj.hata(s, "genel.sadece-oyuncu");
            return true;
        }

        switch (alt) {
            case KIRALA: kirala(p, a); break;
            case TERKET: terket(p, a, dil); break;
            case SURE: sure(p, a); break;
            case BILGI: bilgi(p, dil); break;
            case SAHIP: sahip(p); break;
            case GUI: gui(p); break;
            case YETKIVER: yetkiver(p, a, dil); break;
            case YETKIAL: yetkial(p, a, dil); break;
            case ZIYARETCI: ziyaretci(p, a); break;
            case IZINLER: izinler(p, dil); break;
            case OLUSTUR: olustur(p, a); break;
            case SIL: sil(p, a, dil); break;
            case LISTE: liste(p); break;
            case BYPASS: bypass(p); break;
            case SABLON: sablon(p); break;
            default: yardim(p, dil); break;
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Oyuncu komutları
    // ═══════════════════════════════════════════════════════════════════════

    private void kirala(Player p, String[] a) {
        KiraServisi kira = plugin.kira();
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "komut.kirala.arsada-degil");
            return;
        }
        if (a.length < 2) {
            Mesaj.hata(p, "komut.kirala.kullanim");
            Mesaj.bilgi(p, "komut.kirala.ucret", kira.bicim(kira.ucret(604800L)), plugin.ayarlar().maxHafta);
            return;
        }
        long saniye = Sure.saniyeyeCevir(a[1], "w");
        if (saniye <= 0) {
            Mesaj.hata(p, "komut.kirala.gecersiz");
            return;
        }
        kira.kirala(p, arsa, saniye);
    }

    private void terket(Player p, String[] a, Dil dil) {
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null || !arsa.sahipMi(p.getUniqueId())) {
            Mesaj.hata(p, "komut.terket.arsada-degil");
            return;
        }
        if (a.length < 2 || !onayMi(a[1])) {
            Mesaj.gonder(p, "komut.terket.uyari", arsa.id());
            if (arsa.kiralik()) Mesaj.satir(p, "komut.terket.iade", plugin.kira().bicim(plugin.kira().terkIadesi(arsa)));
            Mesaj.satir(p, plugin.ayarlar().bitinceSifirla ? "komut.terket.sifirlanir" : "komut.terket.uyeler");
            Mesaj.satir(p, "komut.terket.onayla", dil.komut());
            return;
        }
        plugin.kira().terket(p, arsa);
    }

    private void sure(Player p, String[] a) {
        if (a.length < 3) {
            Mesaj.hata(p, "komut.sure.kullanim");
            return;
        }
        Arsa arsa = hedefArsa(p);
        if (arsa == null) return;
        long saniye = Sure.saniyeyeCevir(a[2], "d");
        if (saniye <= 0) {
            Mesaj.hata(p, "komut.sure.gecersiz");
            return;
        }
        String islem = normal(a[1]);
        if (islem.equals("arttir") || islem.equals("uzat") || islem.equals("ekle")
                || islem.equals("add") || islem.equals("extend")) {
            plugin.kira().uzat(p, arsa, saniye);
        } else if (islem.equals("azalt") || islem.equals("kisalt")
                || islem.equals("remove") || islem.equals("reduce")) {
            plugin.kira().kisalt(p, arsa, saniye);
        } else {
            Mesaj.hata(p, "komut.sure.gecersiz-islem");
        }
    }

    private void bilgi(Player p, Dil dil) {
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "genel.arsada-degilsin");
            return;
        }
        Mesaj.satir(p, "komut.bilgi.baslik", arsa.id());
        Mesaj.satir(p, "komut.bilgi.boyut", arsa.dunya(), arsa.genislikX() + "x" + arsa.genislikZ(), arsa.yukseklik());
        Mesaj.satir(p, "komut.bilgi.konum", arsa.konumYazisi());
        if (!arsa.sahibiVar()) {
            Mesaj.satir(p, "komut.bilgi.sahipsiz");
            Mesaj.satir(p, "komut.bilgi.ucret", plugin.kira().bicim(plugin.kira().ucret(604800L)));
            return;
        }
        Mesaj.satir(p, "komut.bilgi.sahip", OyuncuBul.isim(arsa.sahip()));
        if (arsa.kiralik()) {
            Mesaj.satir(p, "komut.bilgi.kira", Sure.tarih(arsa.kiraBitis()), Sure.yaz(dil, arsa.kalanMs()));
        }
        if (arsa.uyeler().isEmpty()) {
            Mesaj.satir(p, "komut.bilgi.uye-yok");
        } else {
            List<String> l = new ArrayList<>();
            for (Map.Entry<UUID, ArsaUye> e : arsa.uyeler().entrySet()) {
                l.add(L.t(dil, "komut.bilgi.uye-oge", OyuncuBul.isim(e.getKey()), e.getValue().rutbe().ad(dil)));
            }
            Mesaj.satir(p, "komut.bilgi.uyeler", String.join(L.t(dil, "komut.bilgi.ayrac"), l));
        }
        List<String> ziyaretci = new ArrayList<>();
        for (Izin i : arsa.ziyaretciIzinleri()) ziyaretci.add(i.ad(dil));
        if (ziyaretci.isEmpty()) Mesaj.satir(p, "komut.bilgi.ziyaretci-yok");
        else Mesaj.satir(p, "komut.bilgi.ziyaretci", String.join(", ", ziyaretci));

        List<String> benim = new ArrayList<>();
        for (Izin i : Izin.values()) if (plugin.yetki().izinli(p, arsa, i)) benim.add(i.anahtar(dil));
        if (benim.isEmpty()) Mesaj.satir(p, "komut.bilgi.benim-yok");
        else Mesaj.satir(p, "komut.bilgi.benim", String.join(", ", benim));
    }

    private void sahip(Player p) {
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "genel.arsada-degilsin");
        } else if (arsa.sahibiVar()) {
            Mesaj.bilgi(p, "komut.sahip.sahip", arsa.id(), OyuncuBul.isim(arsa.sahip()));
        } else {
            Mesaj.bilgi(p, "komut.sahip.sahipsiz");
        }
    }

    private void gui(Player p) {
        Arsa arsa = hedefArsa(p);
        if (arsa == null) return;
        if (!plugin.yetki().panel(p, arsa)) {
            Mesaj.hata(p, "komut.gui.yetki-yok");
            return;
        }
        Mesaj.sesSayfa(p);
        new AnaMenu(plugin, p, arsa).ac();
    }

    /**
     * yetkiver &lt;oyuncu&gt;                 → üye olarak davet et
     * yetkiver &lt;oyuncu&gt; &lt;rütbe&gt;        → rütbe ver (üye değilse ekler)
     * yetkiver &lt;oyuncu&gt; &lt;izin|hepsi&gt;   → izin aç (üye değilse ekler)
     */
    private void yetkiver(Player p, String[] a, Dil dil) {
        if (a.length < 2) {
            Mesaj.hata(p, "komut.yetkiver.kullanim");
            Mesaj.bilgi(p, "komut.liste-rutbe-izin", rutbeListesi(dil), dil.komut());
            return;
        }
        Arsa arsa = hedefArsa(p);
        if (arsa == null) return;
        OfflinePlayer hedef = OyuncuBul.bul(a[1]);
        if (hedef == null) {
            Mesaj.hata(p, "genel.oyuncu-yok-detay", a[1]);
            return;
        }
        if (a.length == 2) {
            plugin.uyeServisi().davet(p, arsa, hedef);
            return;
        }
        String deger = a[2];
        if (hepsiMi(deger)) {
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
        Mesaj.hata(p, "komut.yetkiver.gecersiz", deger);
        Mesaj.bilgi(p, "komut.liste-rutbe-izin", rutbeListesi(dil), dil.komut());
    }

    /**
     * yetkial &lt;oyuncu&gt;                 → arsadan at
     * yetkial &lt;oyuncu&gt; &lt;izin|hepsi&gt;    → izni kapat
     */
    private void yetkial(Player p, String[] a, Dil dil) {
        if (a.length < 2) {
            Mesaj.hata(p, "komut.yetkial.kullanim");
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
            Mesaj.hata(p, "genel.oyuncu-yok", a[1]);
            return;
        }
        if (a.length == 2) {
            plugin.uyeServisi().at(p, arsa, hedef.getUniqueId());
            return;
        }
        if (hepsiMi(a[2])) {
            plugin.uyeServisi().tumIzinler(p, arsa, hedef, Boolean.FALSE);
            return;
        }
        Izin i = Izin.bul(a[2]);
        if (i == null) {
            Mesaj.hata(p, "komut.izin-gecersiz", a[2], String.join(", ", Izin.anahtarlar(dil)));
            return;
        }
        plugin.uyeServisi().izinAyarla(p, arsa, hedef, i, false, false);
    }

    private void izinler(Player p, Dil dil) {
        Mesaj.satir(p, "komut.izinler.baslik");
        for (Izin i : Izin.values()) {
            Mesaj.satir(p, "komut.izinler.satir", i.anahtar(dil), i.ad(dil), String.join(" ", i.aciklama(dil)));
        }
        Mesaj.satir(p, "komut.izinler.rutbeler", rutbeListesi(dil));
        for (String satir : L.l(dil, "komut.izinler.ipucu")) Mesaj.hamSatir(p, satir);
    }

    /** ziyaretci → menü, ziyaretci &lt;izin|hepsi&gt; &lt;ac|kapat&gt; */
    private void ziyaretci(Player p, String[] a) {
        Arsa arsa = hedefArsa(p);
        if (arsa == null) return;
        if (a.length < 3) {
            if (plugin.yetki().ziyaretciDuzenleyebilir(p, arsa)) {
                new ZiyaretciMenu(plugin, p, arsa).ac();
            } else {
                Mesaj.hata(p, "ziyaretci.yetki-yok");
            }
            return;
        }
        Boolean deger = acikMi(a[2]);
        if (deger == null) {
            Mesaj.hata(p, "komut.ziyaretci.kullanim");
            return;
        }
        if (hepsiMi(a[1])) {
            for (Izin i : Izin.ZIYARETCI_IZINLERI) {
                if (!plugin.uyeServisi().ziyaretciAyarla(p, arsa, i, deger, true)) return;
            }
            Mesaj.basari(p, deger ? "ziyaretci.tum-acildi" : "ziyaretci.tum-kapandi");
            return;
        }
        Izin i = Izin.bul(a[1]);
        if (i == null) {
            Mesaj.hata(p, "komut.izin-gecersiz", a[1], String.join(", ", Izin.anahtarlar(Mesaj.dil(p))));
            return;
        }
        plugin.uyeServisi().ziyaretciAyarla(p, arsa, i, deger, false);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Admin komutları
    // ═══════════════════════════════════════════════════════════════════════

    private void olustur(Player p, String[] a) {
        int[] k;
        if (a.length == 7) {
            k = new int[6];
            try {
                for (int i = 0; i < 6; i++) k[i] = koordinat(a[i + 1], p, i % 3);
            } catch (NumberFormatException e) {
                Mesaj.hata(p, "komut.olustur.koordinat");
                return;
            }
        } else if (a.length == 1) {
            WorldEditKopru we = plugin.worldEdit();
            if (we == null) {
                Mesaj.hata(p, "komut.olustur.kullanim-koordinat");
                return;
            }
            k = we.secim(p);
            if (k == null) {
                Mesaj.hata(p, "komut.olustur.secim-yok");
                return;
            }
        } else {
            Mesaj.hata(p, "komut.olustur.kullanim");
            return;
        }
        String dunya = p.getWorld().getName();
        Arsa cakisan = plugin.arsalar().cakisan(dunya, k[0], k[1], k[2], k[3], k[4], k[5]);
        if (cakisan != null) {
            Mesaj.hata(p, "komut.olustur.cakisma", cakisan.id());
            return;
        }
        Arsa arsa = plugin.arsalar().olustur(dunya, k[0], k[1], k[2], k[3], k[4], k[5]);
        plugin.depo().kaydetPlanla();
        boolean sablon = plugin.worldEdit() != null
                && plugin.worldEdit().sablonKaydet(arsa, plugin.kira().sablonDosyasi(arsa));
        Mesaj.basari(p, sablon ? "komut.olustur.basari-sablon" : "komut.olustur.basari",
                arsa.id(), arsa.genislikX() + "x" + arsa.genislikZ() + "x" + arsa.yukseklik());
    }

    private void sil(Player p, String[] a, Dil dil) {
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "komut.sil.arsada-degil");
            return;
        }
        if (a.length < 2 || !onayMi(a[1])) {
            if (arsa.sahibiVar()) Mesaj.gonder(p, "komut.sil.uyari-sahipli", arsa.id(), OyuncuBul.isim(arsa.sahip()));
            else Mesaj.gonder(p, "komut.sil.uyari", arsa.id());
            Mesaj.satir(p, "komut.sil.onayla", dil.komut());
            return;
        }
        UUID eskiSahip = arsa.sahip();
        plugin.arsalar().sil(arsa);
        File sablon = plugin.kira().sablonDosyasi(arsa);
        if (sablon.exists() && !sablon.delete()) {
            plugin.getLogger().warning("Sablon dosyasi silinemedi: " + sablon.getName());
        }
        plugin.depo().kaydetPlanla();
        Mesaj.basari(p, "komut.sil.basari", arsa.id());
        if (eskiSahip != null) {
            Player sahip = Bukkit.getPlayer(eskiSahip);
            if (sahip != null && !sahip.equals(p)) Mesaj.hata(sahip, "komut.sil.bildirim", arsa.id());
        }
    }

    private void liste(Player p) {
        Mesaj.sesSayfa(p);
        new ArsaListeMenu(plugin, p, 0, ArsaListeMenu.Filtre.TUMU).ac();
    }

    /** Konsol için sohbet listesi. */
    private void listeSohbet(CommandSender s, String[] a) {
        Dil dil = Mesaj.dil(s);
        List<Arsa> tum = new ArrayList<>(plugin.arsalar().tumu());
        if (tum.isEmpty()) {
            Mesaj.hata(s, "komut.liste.bos");
            return;
        }
        int boyut = 10;
        int toplam = (tum.size() + boyut - 1) / boyut;
        int sayfa = 1;
        if (a.length > 1) {
            try {
                sayfa = Integer.parseInt(a[1]);
            } catch (NumberFormatException e) {
                Mesaj.hata(s, "komut.liste.gecersiz-sayfa");
                return;
            }
        }
        sayfa = Math.max(1, Math.min(toplam, sayfa));
        Mesaj.satir(s, "komut.liste.baslik", sayfa, toplam);
        for (int i = (sayfa - 1) * boyut; i < Math.min(tum.size(), sayfa * boyut); i++) {
            Arsa x = tum.get(i);
            String durum = !x.sahibiVar() ? L.t(dil, "genel.sahipsiz")
                    : OyuncuBul.isim(x.sahip()) + (x.kiralik() ? " (" + Sure.yaz(dil, x.kalanMs()) + ")" : "");
            Mesaj.satir(s, "komut.liste.satir", x.id(), durum, x.dunya(), x.konumYazisi(), x.uyeler().size());
        }
        if (sayfa < toplam) Mesaj.satir(s, "komut.liste.sonraki", dil.komut(), Alt.LISTE.ad(dil), sayfa + 1);
    }

    private void bypass(Player p) {
        boolean acik = plugin.yetki().bypassDegistir(p);
        Mesaj.basari(p, acik ? "komut.bypass.acik" : "komut.bypass.kapali");
    }

    private void sablon(Player p) {
        WorldEditKopru we = plugin.worldEdit();
        if (we == null) {
            Mesaj.hata(p, "komut.sablon.we-yok");
            return;
        }
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa == null) {
            Mesaj.hata(p, "komut.sablon.arsada-degil");
            return;
        }
        if (we.sablonKaydet(arsa, plugin.kira().sablonDosyasi(arsa))) {
            Mesaj.basari(p, "komut.sablon.basari", arsa.id());
        } else {
            Mesaj.hata(p, "komut.sablon.hata");
        }
    }

    private void yenile(CommandSender s) {
        plugin.ayarlariYenile();
        Mesaj.basari(s, "komut.yenile.basari");
    }

    private void yardim(CommandSender s, Dil dil) {
        for (String satir : L.l(dil, "yardim.oyuncu")) Mesaj.hamSatir(s, satir);
        if (adminMi(s)) {
            for (String satir : L.l(dil, "yardim.admin")) Mesaj.hamSatir(s, satir);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tab tamamlama
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        Dil dil = Dil.komuttan(cmd.getName());
        if (a.length == 1) {
            List<String> l = new ArrayList<>();
            for (Alt alt : Alt.values()) {
                String y = alt.yetki();
                if (alt.admin && (y == null || !s.hasPermission(y))) continue;
                if (!l.contains(alt.ad(dil))) l.add(alt.ad(dil));
            }
            return eslesen(a[0], l);
        }
        Alt alt = Alt.bul(a[0]);
        if (alt == null) return Collections.emptyList();
        Player p = s instanceof Player pl ? pl : null;
        String hepsi = dil == Dil.EN ? "all" : "hepsi";
        switch (alt) {
            case KIRALA:
                if (a.length == 2) return eslesen(a[1], Arrays.asList("1w", "2w", "3w", "4w", "1d", "3d"));
                break;
            case SURE:
                if (a.length == 2) return eslesen(a[1], dil == Dil.EN ? Arrays.asList("add", "remove") : Arrays.asList("arttır", "azalt"));
                if (a.length == 3) return eslesen(a[2], Arrays.asList("1d", "3d", "1w", "2w", "12h"));
                break;
            case TERKET:
            case SIL:
                if (a.length == 2) return eslesen(a[1], Collections.singletonList(dil == Dil.EN ? "confirm" : "onayla"));
                break;
            case YETKIVER:
                if (a.length == 2) return eslesen(a[1], cevrimiciIsimler(p));
                if (a.length == 3) {
                    List<String> l = new ArrayList<>();
                    for (Rutbe r : Rutbe.values()) l.add(r.anahtar(dil));
                    l.addAll(Izin.anahtarlar(dil));
                    l.add(hepsi);
                    return eslesen(a[2], l);
                }
                break;
            case YETKIAL:
                if (a.length == 2) return eslesen(a[1], uyeIsimleri(p));
                if (a.length == 3) {
                    List<String> l = new ArrayList<>(Izin.anahtarlar(dil));
                    l.add(hepsi);
                    return eslesen(a[2], l);
                }
                break;
            case ZIYARETCI:
                if (a.length == 2) {
                    List<String> l = new ArrayList<>();
                    for (Izin i : Izin.ZIYARETCI_IZINLERI) l.add(i.anahtar(dil));
                    l.add(hepsi);
                    return eslesen(a[1], l);
                }
                if (a.length == 3) return eslesen(a[2], dil == Dil.EN ? Arrays.asList("on", "off") : Arrays.asList("aç", "kapat"));
                break;
            case OLUSTUR:
                if (p != null && a.length >= 2 && a.length <= 7) {
                    int eksen = (a.length - 2) % 3;
                    int deger = eksen == 0 ? p.getLocation().getBlockX()
                            : eksen == 1 ? p.getLocation().getBlockY() : p.getLocation().getBlockZ();
                    return eslesen(a[a.length - 1], Arrays.asList("~", String.valueOf(deger)));
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
        Arsa arsa = sessizHedefArsa(p);
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

    /** İşlem yapılacak arsa: bulunduğun arsa; değilse adının geçtiği tek arsa. */
    private Arsa hedefArsa(Player p) {
        Arsa arsa = sessizHedefArsa(p);
        if (arsa != null) return arsa;
        boolean birden = plugin.arsalar().iliskiliArsalar(p.getUniqueId()).size() > 1;
        Mesaj.hata(p, birden ? "genel.birden-fazla-arsa" : "genel.arsada-durmalisin");
        return null;
    }

    private Arsa sessizHedefArsa(Player p) {
        Arsa arsa = plugin.arsalar().bul(p.getLocation());
        if (arsa != null) return arsa;
        List<Arsa> sahip = plugin.arsalar().sahipArsalari(p.getUniqueId());
        if (sahip.size() == 1) return sahip.get(0);
        if (sahip.isEmpty()) {
            List<Arsa> iliskili = plugin.arsalar().iliskiliArsalar(p.getUniqueId());
            if (iliskili.size() == 1) return iliskili.get(0);
        }
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
        if (g.startsWith("~")) return g.length() == 1 ? taban : taban + Integer.parseInt(g.substring(1));
        return Integer.parseInt(g);
    }

    private static boolean adminMi(CommandSender s) {
        for (Alt a : Alt.values()) {
            if (a.admin && a.yetki() != null && s.hasPermission(a.yetki())) return true;
        }
        return false;
    }

    private static boolean onayMi(String s) {
        String g = normal(s);
        return g.equals("onayla") || g.equals("onay") || g.equals("confirm");
    }

    private static boolean hepsiMi(String s) {
        String g = normal(s);
        return g.equals("hepsi") || g.equals("tumu") || g.equals("all");
    }

    private static Boolean acikMi(String s) {
        switch (normal(s)) {
            case "ac": case "acik": case "evet": case "on": case "allow": case "true": case "yes":
                return Boolean.TRUE;
            case "kapat": case "kapali": case "hayir": case "off": case "deny": case "false": case "no":
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    private static String rutbeListesi(Dil dil) {
        List<String> l = new ArrayList<>();
        for (Rutbe r : Rutbe.values()) l.add(r.anahtar(dil));
        return String.join(", ", l);
    }

    private static String normal(String s) {
        return Izin.normalize(s);
    }
}
