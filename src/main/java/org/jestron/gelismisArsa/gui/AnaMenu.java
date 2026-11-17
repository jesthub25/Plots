package org.jestron.gelismisArsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.util.Mesaj;
import org.jestron.gelismisArsa.util.OyuncuBul;
import org.jestron.gelismisArsa.util.Sure;

/**
 * Arsa yönetim paneli (3 satır, dolgu camı yok).
 *   10 Bilgi · 12 Üyeler · 14 Ziyaretçi izinleri · 16 Kira · 22 Kapat
 */
public final class AnaMenu extends Menu {

    public AnaMenu(GelismisArsa plugin, Player oyuncu, Arsa arsa) {
        super(plugin, oyuncu, arsa, 3, "menu.ana.baslik", arsa.id());
    }

    @Override
    protected void ciz() {
        koy(10, bilgi());

        koy(12, Ikon.kafa(arsa.sahibiVar() ? Bukkit.getOfflinePlayer(arsa.sahip()) : null)
                .ad(t("menu.ana.uyeler.ad"))
                .satirlar(l("menu.ana.uyeler.lore", arsa.uyeler().size()))
                .yap(), tik -> {
            Mesaj.sesSayfa(oyuncu);
            new UyelerMenu(plugin, oyuncu, arsa, 0).ac();
        });

        int acik = arsa.ziyaretciIzinleri().size();
        String acikYazi = acik == 0 ? t("menu.ana.ziyaretci.hicbiri")
                : t("menu.ana.ziyaretci.sayi", acik, Izin.ZIYARETCI_IZINLERI.size());
        koy(14, Ikon.of(Material.OAK_FENCE_GATE)
                .ad(t("menu.ana.ziyaretci.ad"))
                .satirlar(l("menu.ana.ziyaretci.lore", acikYazi))
                .yap(), tik -> {
            Mesaj.sesSayfa(oyuncu);
            new ZiyaretciMenu(plugin, oyuncu, arsa).ac();
        });

        koy(16, kira(), this::kiraTik);

        koy(22, Ikon.of(Material.BARRIER).ad(t("menu.kapat")).yap(), tik -> oyuncu.closeInventory());
    }

    private ItemStack bilgi() {
        Ikon i = Ikon.of(Material.FILLED_MAP)
                .ad(t("menu.ana.bilgi.ad", arsa.id()))
                .satirlar(l("menu.ana.bilgi.lore",
                        arsa.dunya(),
                        arsa.genislikX() + "x" + arsa.genislikZ(),
                        arsa.yukseklik(),
                        arsa.konumYazisi(),
                        arsa.sahibiVar() ? OyuncuBul.isim(arsa.sahip()) : t("genel.sahipsiz"),
                        arsa.uyeler().size()));
        if (arsa.kiralik()) {
            i.satirlar(l("menu.ana.bilgi.kira", Sure.tarih(arsa.kiraBitis()), Sure.yaz(dil, arsa.kalanMs())));
        }
        return i.yap();
    }

    private ItemStack kira() {
        Ikon i = Ikon.of(Material.CLOCK).ad(t("menu.ana.kira.ad"));
        if (!arsa.kiralik()) {
            return i.satir(t("menu.ana.kira.kiralik-degil")).yap();
        }
        i.satirlar(l("menu.ana.kira.durum", Sure.yaz(dil, arsa.kalanMs()), Sure.tarih(arsa.kiraBitis())));
        if (arsa.sahipMi(oyuncu.getUniqueId()) && plugin.kira() != null) {
            i.satirlar(l("menu.ana.kira.uzat",
                    plugin.kira().bicim(plugin.kira().ucret(86400L)),
                    plugin.kira().bicim(plugin.kira().ucret(604800L))));
        } else {
            i.satirlar(l("menu.ana.kira.sadece-kiraci"));
        }
        return i.yap();
    }

    private void kiraTik(ClickType tip) {
        if (!arsa.kiralik() || !arsa.sahipMi(oyuncu.getUniqueId()) || plugin.kira() == null) {
            Mesaj.sesHata(oyuncu);
            return;
        }
        long saniye = tip.isRightClick() ? 604800L : 86400L;
        if (plugin.kira().uzat(oyuncu, arsa, saniye)) yenidenCiz();
    }
}
