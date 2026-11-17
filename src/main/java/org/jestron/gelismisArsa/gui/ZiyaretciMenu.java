package org.jestron.gelismisArsa.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.util.Mesaj;

import java.util.ArrayList;
import java.util.List;

/**
 * Ziyaretçi (üye olmayan herkes) izinleri. 27 slot, dolgu camı yok.
 *  0 Geri · 4 Bilgi · 9-18 izinler · 24 Tümünü aç · 25 Tümünü kapat
 */
public final class ZiyaretciMenu extends Menu {

    public ZiyaretciMenu(GelismisArsa plugin, Player oyuncu, Arsa arsa) {
        super(plugin, oyuncu, arsa, 3, "menu.ziyaretci.baslik", arsa.id());
    }

    @Override
    protected void ciz() {
        koy(0, geriIkonu("menu.geri.panel"), tik -> {
            Mesaj.sesSayfa(oyuncu);
            new AnaMenu(plugin, oyuncu, arsa).ac();
        });

        koy(4, Ikon.of(Material.OAK_FENCE_GATE).ad(t("menu.ana.ziyaretci.ad"))
                .satirlar(l("menu.ziyaretci.bilgi", arsa.ziyaretciIzinleri().size(), Izin.ZIYARETCI_IZINLERI.size()))
                .yap());

        int slot = 9;
        for (Izin izin : Izin.values()) {
            if (!Izin.ZIYARETCI_IZINLERI.contains(izin)) continue;
            final Izin iz = izin;
            boolean acik = arsa.ziyaretciIzni(iz);
            List<String> aciklama = new ArrayList<>();
            for (String s : iz.aciklama(dil)) aciklama.add("&7" + s);
            koy(slot++, Ikon.of(iz.ikon())
                    .ad(t(acik ? "menu.izin.ad-acik" : "menu.izin.ad-kapali", iz.ad(dil)))
                    .satirlar(aciklama)
                    .bosluk()
                    .satir(t(acik ? "menu.ziyaretci.yapabilir" : "menu.ziyaretci.yapamaz"))
                    .bosluk()
                    .satir(t(acik ? "menu.izin.tikla-kapat" : "menu.izin.tikla-ac"))
                    .parla(acik)
                    .yap(), tik -> {
                if (plugin.uyeServisi().ziyaretciAyarla(oyuncu, arsa, iz, !arsa.ziyaretciIzni(iz), true)) {
                    Mesaj.sesTik(oyuncu);
                    yenidenCiz();
                }
            });
        }

        koy(24, Ikon.of(Material.LIME_CONCRETE).ad(t("menu.tumunu-ac"))
                .satirlar(l("menu.ziyaretci.tumunu-ac-lore")).yap(), tik -> topluAyarla(true));
        koy(25, Ikon.of(Material.RED_CONCRETE).ad(t("menu.tumunu-kapat"))
                .satirlar(l("menu.ziyaretci.tumunu-kapat-lore")).yap(), tik -> topluAyarla(false));
    }

    private void topluAyarla(boolean deger) {
        for (Izin i : Izin.ZIYARETCI_IZINLERI) {
            if (!plugin.uyeServisi().ziyaretciAyarla(oyuncu, arsa, i, deger, true)) return;
        }
        Mesaj.basari(oyuncu, deger ? "ziyaretci.tum-acildi" : "ziyaretci.tum-kapandi");
        yenidenCiz();
    }
}
