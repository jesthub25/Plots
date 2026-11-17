package org.jestron.arsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jestron.arsa.ArsaPlugin;
import org.jestron.arsa.Yetki;
import org.jestron.arsa.model.Arsa;
import org.jestron.arsa.model.ArsaUye;
import org.jestron.arsa.model.Izin;
import org.jestron.arsa.util.Mesaj;
import org.jestron.arsa.util.OyuncuBul;
import org.jestron.arsa.util.Sure;

import java.util.List;
import java.util.UUID;

/**
 * Admin arsa detayı (54 slot): sahip, tüm üyeler, rütbeleri ve her bir izinleri.
 *  0 Geri · 2 Bilgi · 4 Sahip · 6 Ziyaretçi izinleri · 8 Işınlan
 *  9-44 üyeler (36/sayfa) · 47 Önceki · 49 Yönet · 51 Sonraki · 53 Kapat
 */
public final class ArsaDetayMenu extends Menu {

    private static final int SAYFA_BOYUTU = 36;

    private final int listeSayfasi;
    private final ArsaListeMenu.Filtre filtre;
    private final int sayfa;
    private final List<UUID> uyeler;

    public ArsaDetayMenu(ArsaPlugin plugin, Player oyuncu, Arsa arsa, int listeSayfasi, ArsaListeMenu.Filtre filtre) {
        this(plugin, oyuncu, arsa, listeSayfasi, filtre, 0);
    }

    private ArsaDetayMenu(ArsaPlugin plugin, Player oyuncu, Arsa arsa, int listeSayfasi,
                          ArsaListeMenu.Filtre filtre, int sayfa) {
        super(plugin, oyuncu, arsa, 6, "menu.detay.baslik", arsa.id());
        this.listeSayfasi = listeSayfasi;
        this.filtre = filtre;
        List<UUID> l = UyelerMenu.sirala(arsa);
        if (arsa.sahibiVar()) l.remove(0); // sahip ayrı gösterilir
        this.uyeler = l;
        int toplam = Math.max(1, (l.size() + SAYFA_BOYUTU - 1) / SAYFA_BOYUTU);
        this.sayfa = Math.max(0, Math.min(sayfa, toplam - 1));
    }

    @Override
    protected boolean gecerli() {
        return oyuncu.hasPermission(ArsaListeMenu.YETKI) && plugin.arsalar().kayitli(arsa);
    }

    private boolean yonetebilir() {
        return oyuncu.hasPermission(Yetki.PERM_YONET);
    }

    @Override
    protected void ciz() {
        koy(0, geriIkonu("menu.geri.liste"), tik -> {
            Mesaj.sesSayfa(oyuncu);
            new ArsaListeMenu(plugin, oyuncu, listeSayfasi, filtre).ac();
        });

        Ikon bilgi = Ikon.of(Material.FILLED_MAP).ad(t("menu.ana.bilgi.ad", arsa.id()))
                .satirlar(l("menu.ana.bilgi.lore",
                        arsa.dunya(),
                        arsa.genislikX() + "x" + arsa.genislikZ(),
                        arsa.yukseklik(),
                        arsa.konumYazisi(),
                        arsa.sahibiVar() ? OyuncuBul.isim(arsa.sahip()) : t("genel.sahipsiz"),
                        arsa.uyeler().size()));
        if (arsa.kiralik()) {
            bilgi.satirlar(l("menu.ana.bilgi.kira", Sure.tarih(arsa.kiraBitis()), Sure.yaz(dil, arsa.kalanMs())));
        }
        koy(2, bilgi.yap());

        if (arsa.sahibiVar()) {
            koy(4, UyelerMenu.sahipIkonu(dil, arsa.sahip()));
        } else {
            koy(4, Ikon.of(Material.GRASS_BLOCK).ad(t("menu.detay.sahipsiz")).yap());
        }

        koy(6, ziyaretciIkonu());

        koy(8, Ikon.of(Material.ENDER_PEARL).ad(t("menu.detay.isinlan.ad"))
                .satir(t("menu.detay.isinlan.lore")).yap(), tik -> isinla());

        // Üyeler
        int bas = sayfa * SAYFA_BOYUTU;
        int son = Math.min(uyeler.size(), bas + SAYFA_BOYUTU);
        int slot = 9;
        boolean yonet = yonetebilir();
        for (int i = bas; i < son; i++, slot++) {
            UUID u = uyeler.get(i);
            ArsaUye uye = arsa.uye(u);
            if (uye == null) continue;
            if (yonet) {
                koy(slot, UyelerMenu.uyeIkonu(dil, arsa, u, uye, "menu.uye.tikla-duzenle"), tik -> {
                    if (!arsa.uyeMi(u)) return;
                    Mesaj.sesSayfa(oyuncu);
                    new UyeIzinMenu(plugin, oyuncu, arsa, u).ac();
                });
            } else {
                koy(slot, UyelerMenu.uyeIkonu(dil, arsa, u, uye, null));
            }
        }
        if (uyeler.isEmpty()) {
            koy(22, Ikon.of(Material.PAPER).ad(t("menu.detay.uye-yok")).yap());
        }

        if (sayfa > 0) {
            koy(47, Ikon.of(Material.SPECTRAL_ARROW).ad(t("menu.onceki")).yap(), tik -> {
                Mesaj.sesSayfa(oyuncu);
                new ArsaDetayMenu(plugin, oyuncu, arsa, listeSayfasi, filtre, sayfa - 1).ac();
            });
        }
        if (yonet) {
            koy(49, Ikon.of(Material.COMPARATOR).ad(t("menu.detay.yonet.ad"))
                    .satir(t("menu.detay.yonet.lore")).yap(), tik -> {
                Mesaj.sesSayfa(oyuncu);
                new AnaMenu(plugin, oyuncu, arsa).ac();
            });
        }
        if (son < uyeler.size()) {
            koy(51, Ikon.of(Material.SPECTRAL_ARROW).ad(t("menu.sonraki")).yap(), tik -> {
                Mesaj.sesSayfa(oyuncu);
                new ArsaDetayMenu(plugin, oyuncu, arsa, listeSayfasi, filtre, sayfa + 1).ac();
            });
        }
        koy(53, Ikon.of(Material.BARRIER).ad(t("menu.kapat")).yap(), tik -> oyuncu.closeInventory());
    }

    private ItemStack ziyaretciIkonu() {
        Ikon i = Ikon.of(Material.OAK_FENCE_GATE).ad(t("menu.ana.ziyaretci.ad"));
        for (Izin iz : Izin.ZIYARETCI_IZINLERI) {
            i.satir(t(arsa.ziyaretciIzni(iz) ? "menu.uye.izin-acik" : "menu.uye.izin-kapali", iz.ad(dil)));
        }
        return i.yap();
    }

    private void isinla() {
        World w = Bukkit.getWorld(arsa.dunya());
        if (w == null) {
            Mesaj.hata(oyuncu, "menu.detay.dunya-yok", arsa.dunya());
            return;
        }
        int x = (arsa.minX() + arsa.maxX()) / 2;
        int z = (arsa.minZ() + arsa.maxZ()) / 2;
        int y = w.getHighestBlockYAt(x, z) + 1;
        Location hedef = new Location(w, x + 0.5, y, z + 0.5,
                oyuncu.getLocation().getYaw(), oyuncu.getLocation().getPitch());
        oyuncu.closeInventory();
        oyuncu.teleport(hedef);
        Mesaj.basari(oyuncu, "menu.detay.isinlandi", arsa.id());
    }
}
