package org.jestron.gelismisArsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.util.Mesaj;
import org.jestron.gelismisArsa.util.OyuncuBul;
import org.jestron.gelismisArsa.util.Sure;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin arsa listesi: 54 slot (6 satır), sayfa başına 45 arsa.
 *  0-44 arsalar (sahipli: sahibin kafası, sahipsiz: çimen bloğu) — tıkla: detay
 *  45 Kapat · 47 Önceki · 49 Özet · 51 Sonraki · 53 Filtre
 */
public final class ArsaListeMenu extends Menu {

    public static final String YETKI = "gelismisarsa.admin.liste";
    private static final int SAYFA_BOYUTU = 45;

    public enum Filtre { TUMU, SAHIPLI, SAHIPSIZ;
        Filtre sonraki() { return values()[(ordinal() + 1) % values().length]; }
    }

    private final int sayfa;
    private final Filtre filtre;
    private final List<Arsa> liste;

    public ArsaListeMenu(GelismisArsa plugin, Player oyuncu, int sayfa, Filtre filtre) {
        this(plugin, oyuncu, filtrele(plugin, filtre), sayfa, filtre);
    }

    private ArsaListeMenu(GelismisArsa plugin, Player oyuncu, List<Arsa> liste, int istenen, Filtre filtre) {
        super(plugin, oyuncu, null, 6, "menu.liste.baslik", kisit(istenen, liste.size()) + 1, sayfaSayisi(liste.size()));
        this.liste = liste;
        this.sayfa = kisit(istenen, liste.size());
        this.filtre = filtre;
    }

    private static List<Arsa> filtrele(GelismisArsa plugin, Filtre f) {
        List<Arsa> l = new ArrayList<>();
        for (Arsa a : plugin.arsalar().tumu()) {
            if (f == Filtre.SAHIPLI && !a.sahibiVar()) continue;
            if (f == Filtre.SAHIPSIZ && a.sahibiVar()) continue;
            l.add(a);
        }
        return l;
    }

    private static int sayfaSayisi(int adet) {
        return Math.max(1, (adet + SAYFA_BOYUTU - 1) / SAYFA_BOYUTU);
    }

    private static int kisit(int sayfa, int adet) {
        return Math.max(0, Math.min(sayfa, sayfaSayisi(adet) - 1));
    }

    @Override
    protected boolean gecerli() {
        return oyuncu.hasPermission(YETKI);
    }

    @Override
    protected void ciz() {
        int bas = sayfa * SAYFA_BOYUTU;
        int son = Math.min(liste.size(), bas + SAYFA_BOYUTU);
        int slot = 0;
        for (int i = bas; i < son; i++, slot++) {
            Arsa a = liste.get(i);
            koy(slot, arsaIkonu(a), tik -> {
                if (!plugin.arsalar().kayitli(a)) {
                    Mesaj.hata(oyuncu, "menu.liste.silinmis");
                    new ArsaListeMenu(plugin, oyuncu, sayfa, filtre).ac();
                    return;
                }
                Mesaj.sesSayfa(oyuncu);
                new ArsaDetayMenu(plugin, oyuncu, a, sayfa, filtre).ac();
            });
        }
        if (liste.isEmpty()) {
            koy(22, Ikon.of(Material.PAPER).ad(t("menu.liste.bos")).yap());
        }

        koy(45, Ikon.of(Material.BARRIER).ad(t("menu.kapat")).yap(), tik -> oyuncu.closeInventory());
        if (sayfa > 0) {
            koy(47, Ikon.of(Material.SPECTRAL_ARROW).ad(t("menu.onceki")).yap(), tik -> {
                Mesaj.sesSayfa(oyuncu);
                new ArsaListeMenu(plugin, oyuncu, sayfa - 1, filtre).ac();
            });
        }
        koy(49, ozet());
        if (son < liste.size()) {
            koy(51, Ikon.of(Material.SPECTRAL_ARROW).ad(t("menu.sonraki")).yap(), tik -> {
                Mesaj.sesSayfa(oyuncu);
                new ArsaListeMenu(plugin, oyuncu, sayfa + 1, filtre).ac();
            });
        }
        koy(53, Ikon.of(Material.HOPPER)
                .ad(t("menu.liste.filtre.ad"))
                .satir(filtreSatiri(Filtre.TUMU), filtreSatiri(Filtre.SAHIPLI), filtreSatiri(Filtre.SAHIPSIZ))
                .bosluk().satir(t("menu.liste.filtre.tikla"))
                .yap(), tik -> {
            Mesaj.sesTik(oyuncu);
            new ArsaListeMenu(plugin, oyuncu, 0, filtre.sonraki()).ac();
        });
    }

    private String filtreSatiri(Filtre f) {
        String ad = t("menu.liste.filtre." + f.name().toLowerCase(java.util.Locale.ROOT));
        return t(f == filtre ? "menu.liste.filtre.secili" : "menu.liste.filtre.secili-degil", ad);
    }

    private ItemStack ozet() {
        int toplam = 0, sahipli = 0, kiralik = 0, uye = 0;
        for (Arsa a : plugin.arsalar().tumu()) {
            toplam++;
            if (a.sahibiVar()) sahipli++;
            if (a.kiralik()) kiralik++;
            uye += a.uyeler().size();
        }
        return Ikon.of(Material.BOOK).ad(t("menu.liste.ozet.ad"))
                .satirlar(l("menu.liste.ozet.lore", toplam, sahipli, toplam - sahipli, kiralik, uye, liste.size()))
                .yap();
    }

    private ItemStack arsaIkonu(Arsa a) {
        Ikon i;
        if (a.sahibiVar()) {
            i = Ikon.kafa(Bukkit.getOfflinePlayer(a.sahip()))
                    .ad(t("menu.liste.arsa.ad-sahipli", a.id(), OyuncuBul.isim(a.sahip())));
        } else {
            i = Ikon.of(Material.GRASS_BLOCK).ad(t("menu.liste.arsa.ad-sahipsiz", a.id()));
        }
        i.satirlar(l("menu.liste.arsa.lore",
                a.dunya(),
                a.genislikX() + "x" + a.genislikZ() + "x" + a.yukseklik(),
                a.konumYazisi(),
                a.uyeler().size(),
                a.ziyaretciIzinleri().size()));
        if (a.kiralik()) {
            i.satir(t("menu.liste.arsa.kira", Sure.yaz(dil, a.kalanMs())));
        }
        return i.bosluk().satir(t("menu.liste.arsa.tikla")).yap();
    }
}
