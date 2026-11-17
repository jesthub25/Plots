package org.jestron.gelismisArsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.ArsaUye;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.model.Rutbe;
import org.jestron.gelismisArsa.util.Mesaj;
import org.jestron.gelismisArsa.util.OyuncuBul;

import java.util.UUID;

/**
 * Tek bir üyenin rütbe ve izin ayarları (27 slot, dolgu camı yok).
 *
 *  0 Geri · 2 Rütbe düşür · 4 Üye bilgisi · 6 Rütbe yükselt · 8 Arsadan at (onaylı)
 *  9-19  izinler (11 adet, parlayan = açık)
 *  24 Tümünü aç · 25 Tümünü kapat · 26 Rütbe varsayılanına sıfırla
 */
public final class UyeIzinMenu extends Menu {

    private static final int ILK_IZIN_SLOTU = 9;

    private final UUID hedef;
    private final OfflinePlayer hedefOyuncu;
    private boolean atmaOnayi;

    public UyeIzinMenu(GelismisArsa plugin, Player oyuncu, Arsa arsa, UUID hedef) {
        super(plugin, oyuncu, arsa, 3, "&8Üye &8» &0" + OyuncuBul.isim(hedef));
        this.hedef = hedef;
        this.hedefOyuncu = Bukkit.getOfflinePlayer(hedef);
    }

    @Override
    protected boolean gecerli() {
        return super.gecerli() && arsa.uyeMi(hedef) && plugin.yetki().yonetebilir(oyuncu, arsa, hedef);
    }

    @Override
    protected void ciz() {
        ArsaUye uye = arsa.uye(hedef);
        if (uye == null) return;
        Rutbe rutbe = uye.rutbe();
        boolean mod = rutbe == Rutbe.MOD;

        koy(0, Ikon.of(Material.ARROW).ad("&e« Geri").satir("&7Üye listesine dön.").yap(), t -> {
            Mesaj.sesSayfa(oyuncu);
            new UyelerMenu(plugin, oyuncu, arsa, 0).ac();
        });

        // Rütbe düşür
        Rutbe onceki = rutbe.onceki();
        if (onceki != rutbe) {
            koy(2, Ikon.of(Material.RED_DYE)
                    .ad("&c&l▼ Rütbe Düşür")
                    .satir("&7Şu an: " + rutbe.gorunenAd(), "&7Yeni: " + onceki.gorunenAd())
                    .bosluk().satir("&8Özel izin ayarları sıfırlanır.").bosluk().satir("&e▶ Tıkla").yap(), t -> {
                if (plugin.uyeServisi().rutbeKaydir(oyuncu, arsa, hedef, false)) yenidenCiz();
            });
        } else {
            koy(2, Ikon.of(Material.GRAY_DYE).ad("&7▼ Rütbe Düşür").satir("&8Zaten en düşük rütbe.").yap());
        }

        // Bilgi
        Ikon bilgi = Ikon.kafa(hedefOyuncu).ad("&f&l" + OyuncuBul.isim(hedef))
                .satir("&7Rütbe: " + rutbe.gorunenAd());
        if (mod) {
            bilgi.satir("&6Tüm izinlere sahiptir.", "&7Üyeleri yönetebilir ve davet edebilir.");
        } else {
            int acik = 0;
            for (Izin i : Izin.values()) if (arsa.uyeIzni(uye, i)) acik++;
            bilgi.satir("&7Açık izin: &f" + acik + "&7/&f" + Izin.values().length);
        }
        bilgi.bosluk().satir("&8Parlayan izinler açıktır.", "&8Bir izne tıklayarak aç/kapat.");
        koy(4, bilgi.yap());

        // Rütbe yükselt
        Rutbe sonraki = rutbe.sonraki();
        if (sonraki == rutbe) {
            koy(6, Ikon.of(Material.GRAY_DYE).ad("&7▲ Rütbe Yükselt").satir("&8Zaten en yüksek rütbe.").yap());
        } else if (!plugin.yetki().rutbeVerebilir(oyuncu, arsa, hedef, sonraki)) {
            koy(6, Ikon.of(Material.GRAY_DYE).ad("&7▲ Rütbe Yükselt")
                    .satir("&8" + sonraki.gorunenAd() + " &8rütbesini sadece", "&8arsa sahibi verebilir.").yap());
        } else {
            koy(6, Ikon.of(Material.LIME_DYE)
                    .ad("&a&l▲ Rütbe Yükselt")
                    .satir("&7Şu an: " + rutbe.gorunenAd(), "&7Yeni: " + sonraki.gorunenAd())
                    .bosluk().satir("&8Özel izin ayarları sıfırlanır.").bosluk().satir("&e▶ Tıkla").yap(), t -> {
                if (plugin.uyeServisi().rutbeKaydir(oyuncu, arsa, hedef, true)) yenidenCiz();
            });
        }

        // Arsadan at (iki tık onaylı)
        if (atmaOnayi) {
            koy(8, Ikon.of(Material.TNT).ad("&4&lEMİN MİSİN?")
                    .satir("&c" + OyuncuBul.isim(hedef) + " arsadan atılacak.", "", "&eOnaylamak için tekrar tıkla.")
                    .parla(true).yap(), t -> {
                if (plugin.uyeServisi().at(oyuncu, arsa, hedef)) {
                    new UyelerMenu(plugin, oyuncu, arsa, 0).ac();
                }
            });
        } else {
            koy(8, Ikon.of(Material.BARRIER).ad("&c&lArsadan At")
                    .satir("&7Üyeyi arsadan tamamen çıkarır.", "", "&e▶ Tıkla").yap(), t -> {
                atmaOnayi = true;
                Mesaj.sesTik(oyuncu);
                yenidenCiz();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (atmaOnayi) {
                        atmaOnayi = false;
                        if (plugin.arsalar().kayitli(arsa) && arsa.uyeMi(hedef)) yenidenCiz();
                    }
                }, 100L);
            });
        }

        // İzinler
        int slot = ILK_IZIN_SLOTU;
        for (Izin izin : Izin.values()) {
            final Izin iz = izin;
            if (mod) {
                koy(slot++, Ikon.of(iz.ikon()).ad("&6" + iz.ad())
                        .satirlar(gri(iz)).bosluk().satir("&6✔ MOD rütbesi ile açık").parla(true).yap());
                continue;
            }
            koy(slot++, izinIkonu(uye, iz), t -> {
                boolean simdi = arsa.uyeIzni(uye, iz);
                if (plugin.uyeServisi().izinAyarla(oyuncu, arsa, hedefOyuncu, iz, !simdi, true)) {
                    Mesaj.sesTik(oyuncu);
                    yenidenCiz();
                }
            });
        }

        if (!mod) {
            koy(24, Ikon.of(Material.LIME_CONCRETE).ad("&a&lTümünü Aç")
                    .satir("&7Tüm izinleri bu üye için açar.").yap(), t -> {
                if (plugin.uyeServisi().tumIzinler(oyuncu, arsa, hedefOyuncu, Boolean.TRUE)) yenidenCiz();
            });
            koy(25, Ikon.of(Material.RED_CONCRETE).ad("&c&lTümünü Kapat")
                    .satir("&7Tüm izinleri bu üye için kapatır.", "&8(Üye sadece arsaya girebilir.)").yap(), t -> {
                if (plugin.uyeServisi().tumIzinler(oyuncu, arsa, hedefOyuncu, Boolean.FALSE)) yenidenCiz();
            });
            koy(26, Ikon.of(Material.NETHER_STAR).ad("&e&lVarsayılana Döndür")
                    .satir("&7Özel ayarları siler, " + rutbe.gorunenAd(), "&7rütbesinin varsayılanlarını uygular.")
                    .parla(uye.ozelAyarVar()).yap(), t -> {
                if (plugin.uyeServisi().tumIzinler(oyuncu, arsa, hedefOyuncu, null)) yenidenCiz();
            });
        }
    }

    private ItemStack izinIkonu(ArsaUye uye, Izin iz) {
        boolean acik = arsa.uyeIzni(uye, iz);
        Boolean ozel = uye.ozel(iz);
        String kaynak;
        if (ozel != null) kaynak = "&e(özel ayar)";
        else if (uye.rutbe().varsayilanIzin(iz)) kaynak = "&8(rütbe varsayılanı)";
        else if (acik) kaynak = "&8(ziyaretçi izni)";
        else kaynak = "&8(rütbe varsayılanı)";
        return Ikon.of(iz.ikon())
                .ad((acik ? "&a" : "&c") + iz.ad())
                .satirlar(gri(iz))
                .bosluk()
                .satir("&7Durum: " + (acik ? "&a✔ AÇIK" : "&c✘ KAPALI") + " " + kaynak)
                .bosluk()
                .satir("&e▶ Tıkla: " + (acik ? "kapat" : "aç"))
                .parla(acik)
                .yap();
    }

    private static java.util.List<String> gri(Izin iz) {
        java.util.List<String> l = new java.util.ArrayList<>();
        for (String s : iz.aciklama()) l.add("&7" + s);
        return l;
    }
}
