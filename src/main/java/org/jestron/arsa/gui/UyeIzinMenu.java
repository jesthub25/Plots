package org.jestron.arsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jestron.arsa.ArsaPlugin;
import org.jestron.arsa.model.Arsa;
import org.jestron.arsa.model.ArsaUye;
import org.jestron.arsa.model.Izin;
import org.jestron.arsa.model.Rutbe;
import org.jestron.arsa.util.Mesaj;
import org.jestron.arsa.util.OyuncuBul;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tek bir üyenin rütbe ve izin ayarları (27 slot, dolgu camı yok).
 *
 *  0 Geri · 2 Rütbe düşür · 4 Üye bilgisi · 6 Rütbe yükselt · 8 Arsadan at (onaylı)
 *  9-19  izinler (11 adet, parlayan = açık)
 *  24 Tümünü aç · 25 Tümünü kapat · 26 Rütbe varsayılanına döndür
 */
public final class UyeIzinMenu extends Menu {

    private static final int ILK_IZIN_SLOTU = 9;

    private final UUID hedef;
    private final OfflinePlayer hedefOyuncu;
    private boolean atmaOnayi;

    public UyeIzinMenu(ArsaPlugin plugin, Player oyuncu, Arsa arsa, UUID hedef) {
        super(plugin, oyuncu, arsa, 3, "menu.uyeizin.baslik", OyuncuBul.isim(hedef));
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
        String isim = OyuncuBul.isim(hedef);

        koy(0, geriIkonu("menu.geri.uyeler"), tik -> {
            Mesaj.sesSayfa(oyuncu);
            new UyelerMenu(plugin, oyuncu, arsa, 0).ac();
        });

        // Rütbe düşür
        Rutbe onceki = rutbe.onceki();
        if (onceki != rutbe) {
            koy(2, Ikon.of(Material.RED_DYE)
                    .ad(t("menu.uyeizin.dusur.ad"))
                    .satirlar(l("menu.uyeizin.rutbe-degisim", rutbe.ad(dil), onceki.ad(dil)))
                    .yap(), tik -> {
                if (plugin.uyeServisi().rutbeKaydir(oyuncu, arsa, hedef, false)) yenidenCiz();
            });
        } else {
            koy(2, Ikon.of(Material.GRAY_DYE).ad(t("menu.uyeizin.dusur.pasif"))
                    .satir(t("menu.uyeizin.en-dusuk")).yap());
        }

        // Bilgi
        Ikon bilgi = Ikon.kafa(hedefOyuncu).ad(t("menu.uye.ad", isim))
                .satir(t("menu.uye.rutbe", rutbe.ad(dil)));
        if (mod) {
            bilgi.satirlar(l("menu.uyeizin.mod-bilgi"));
        } else {
            int acik = 0;
            for (Izin i : Izin.values()) if (arsa.uyeIzni(uye, i)) acik++;
            bilgi.satir(t("menu.uye.acik-sayi", acik, Izin.values().length));
        }
        koy(4, bilgi.bosluk().satirlar(l("menu.uyeizin.ipucu")).yap());

        // Rütbe yükselt
        Rutbe sonraki = rutbe.sonraki();
        if (sonraki == rutbe) {
            koy(6, Ikon.of(Material.GRAY_DYE).ad(t("menu.uyeizin.yukselt.pasif"))
                    .satir(t("menu.uyeizin.en-yuksek")).yap());
        } else if (!plugin.yetki().rutbeVerebilir(oyuncu, arsa, hedef, sonraki)) {
            koy(6, Ikon.of(Material.GRAY_DYE).ad(t("menu.uyeizin.yukselt.pasif"))
                    .satirlar(l("menu.uyeizin.sadece-sahip-verir", sonraki.ad(dil))).yap());
        } else {
            koy(6, Ikon.of(Material.LIME_DYE)
                    .ad(t("menu.uyeizin.yukselt.ad"))
                    .satirlar(l("menu.uyeizin.rutbe-degisim", rutbe.ad(dil), sonraki.ad(dil)))
                    .yap(), tik -> {
                if (plugin.uyeServisi().rutbeKaydir(oyuncu, arsa, hedef, true)) yenidenCiz();
            });
        }

        // Arsadan at (iki tık onaylı)
        if (atmaOnayi) {
            koy(8, Ikon.of(Material.TNT).ad(t("menu.uyeizin.at.onay-ad"))
                    .satirlar(l("menu.uyeizin.at.onay-lore", isim)).parla(true).yap(), tik -> {
                if (plugin.uyeServisi().at(oyuncu, arsa, hedef)) {
                    new UyelerMenu(plugin, oyuncu, arsa, 0).ac();
                }
            });
        } else {
            koy(8, Ikon.of(Material.BARRIER).ad(t("menu.uyeizin.at.ad"))
                    .satirlar(l("menu.uyeizin.at.lore")).yap(), tik -> {
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
                koy(slot++, Ikon.of(iz.ikon()).ad(t("menu.izin.ad-mod", iz.ad(dil)))
                        .satirlar(gri(iz)).bosluk().satir(t("menu.uyeizin.mod-acik")).parla(true).yap());
                continue;
            }
            koy(slot++, izinIkonu(uye, iz), tik -> {
                boolean simdi = arsa.uyeIzni(uye, iz);
                if (plugin.uyeServisi().izinAyarla(oyuncu, arsa, hedefOyuncu, iz, !simdi, true)) {
                    Mesaj.sesTik(oyuncu);
                    yenidenCiz();
                }
            });
        }

        if (!mod) {
            koy(24, Ikon.of(Material.LIME_CONCRETE).ad(t("menu.tumunu-ac"))
                    .satir(t("menu.uyeizin.tumunu-ac-lore")).yap(), tik -> {
                if (plugin.uyeServisi().tumIzinler(oyuncu, arsa, hedefOyuncu, Boolean.TRUE)) yenidenCiz();
            });
            koy(25, Ikon.of(Material.RED_CONCRETE).ad(t("menu.tumunu-kapat"))
                    .satirlar(l("menu.uyeizin.tumunu-kapat-lore")).yap(), tik -> {
                if (plugin.uyeServisi().tumIzinler(oyuncu, arsa, hedefOyuncu, Boolean.FALSE)) yenidenCiz();
            });
            koy(26, Ikon.of(Material.NETHER_STAR).ad(t("menu.uyeizin.sifirla.ad"))
                    .satirlar(l("menu.uyeizin.sifirla.lore", rutbe.ad(dil)))
                    .parla(uye.ozelAyarVar()).yap(), tik -> {
                if (plugin.uyeServisi().tumIzinler(oyuncu, arsa, hedefOyuncu, null)) yenidenCiz();
            });
        }
    }

    private ItemStack izinIkonu(ArsaUye uye, Izin iz) {
        boolean acik = arsa.uyeIzni(uye, iz);
        String kaynak;
        if (uye.ozel(iz) != null) kaynak = t("menu.izin.kaynak.ozel");
        else if (uye.rutbe().varsayilanIzin(iz) || !acik) kaynak = t("menu.izin.kaynak.rutbe");
        else kaynak = t("menu.izin.kaynak.ziyaretci");
        return Ikon.of(iz.ikon())
                .ad(t(acik ? "menu.izin.ad-acik" : "menu.izin.ad-kapali", iz.ad(dil)))
                .satirlar(gri(iz))
                .bosluk()
                .satir(t(acik ? "menu.izin.durum-acik" : "menu.izin.durum-kapali", kaynak))
                .bosluk()
                .satir(t(acik ? "menu.izin.tikla-kapat" : "menu.izin.tikla-ac"))
                .parla(acik)
                .yap();
    }

    private List<String> gri(Izin iz) {
        List<String> l = new ArrayList<>();
        for (String s : iz.aciklama(dil)) l.add("&7" + s);
        return l;
    }
}
