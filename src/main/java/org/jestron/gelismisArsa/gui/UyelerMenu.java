package org.jestron.gelismisArsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.dil.Dil;
import org.jestron.gelismisArsa.dil.L;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.ArsaUye;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.model.Rutbe;
import org.jestron.gelismisArsa.util.Mesaj;
import org.jestron.gelismisArsa.util.OyuncuBul;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Üye listesi. Boyut üye sayısına göre ayarlanır (dolgu camı yok), 45'ten fazlası sayfalanır.
 * Son satır: Geri · Önceki · Davet · Sonraki · Rütbe rehberi
 */
public final class UyelerMenu extends Menu {

    private static final int SAYFA_BOYUTU = 45;

    private final int sayfa;
    private final List<UUID> sirali;

    public UyelerMenu(GelismisArsa plugin, Player oyuncu, Arsa arsa, int sayfa) {
        this(plugin, oyuncu, arsa, sirala(arsa), sayfa);
    }

    private UyelerMenu(GelismisArsa plugin, Player oyuncu, Arsa arsa, List<UUID> sirali, int istenen) {
        super(plugin, oyuncu, arsa, satirSayisi(sirali.size(), kisit(istenen, sirali.size())),
                sayfaSayisi(sirali.size()) > 1 ? "menu.uyeler.baslik-sayfa" : "menu.uyeler.baslik",
                arsa.id(), kisit(istenen, sirali.size()) + 1, sayfaSayisi(sirali.size()));
        this.sayfa = kisit(istenen, sirali.size());
        this.sirali = sirali;
    }

    /** Sıra: sahip, sonra rütbe (yüksekten düşüğe), sonra isim. */
    static List<UUID> sirala(Arsa arsa) {
        List<UUID> l = new ArrayList<>();
        if (arsa.sahibiVar()) l.add(arsa.sahip());
        List<Map.Entry<UUID, ArsaUye>> uyeler = new ArrayList<>(arsa.uyeler().entrySet());
        uyeler.sort(Comparator.<Map.Entry<UUID, ArsaUye>>comparingInt(e -> -e.getValue().rutbe().seviye())
                .thenComparing(e -> OyuncuBul.isim(e.getKey()), String.CASE_INSENSITIVE_ORDER));
        for (Map.Entry<UUID, ArsaUye> e : uyeler) l.add(e.getKey());
        return l;
    }

    private static int kisit(int sayfa, int adet) {
        return Math.max(0, Math.min(sayfa, sayfaSayisi(adet) - 1));
    }

    private static int sayfaSayisi(int adet) {
        return Math.max(1, (adet + SAYFA_BOYUTU - 1) / SAYFA_BOYUTU);
    }

    private static int satirSayisi(int adet, int sayfa) {
        int sayfadaki = Math.min(SAYFA_BOYUTU, Math.max(0, adet - sayfa * SAYFA_BOYUTU));
        return Math.max(1, (sayfadaki + 8) / 9) + 1;
    }

    @Override
    protected void ciz() {
        int baslangic = sayfa * SAYFA_BOYUTU;
        int bitis = Math.min(sirali.size(), baslangic + SAYFA_BOYUTU);
        int slot = 0;
        for (int i = baslangic; i < bitis; i++, slot++) {
            UUID u = sirali.get(i);
            if (arsa.sahipMi(u)) {
                koy(slot, sahipIkonu(dil, u));
                continue;
            }
            ArsaUye uye = arsa.uye(u);
            if (uye == null) continue;
            boolean yonetebilir = plugin.yetki().yonetebilir(oyuncu, arsa, u);
            koy(slot, uyeIkonu(dil, arsa, u, uye, yonetebilir ? "menu.uye.tikla-duzenle" : "menu.uye.yonetemezsin"), tik -> {
                if (!plugin.yetki().yonetebilir(oyuncu, arsa, u)) {
                    plugin.uyeServisi().yonetimRed(oyuncu, arsa, u);
                    return;
                }
                Mesaj.sesSayfa(oyuncu);
                new UyeIzinMenu(plugin, oyuncu, arsa, u).ac();
            });
        }

        int alt = boyut() - 9;
        koy(alt, geriIkonu("menu.geri.panel"), tik -> {
            Mesaj.sesSayfa(oyuncu);
            new AnaMenu(plugin, oyuncu, arsa).ac();
        });
        if (sayfa > 0) {
            koy(alt + 3, Ikon.of(Material.SPECTRAL_ARROW).ad(t("menu.onceki")).yap(), tik -> {
                Mesaj.sesSayfa(oyuncu);
                new UyelerMenu(plugin, oyuncu, arsa, sayfa - 1).ac();
            });
        }
        if (plugin.yetki().davetEdebilir(oyuncu, arsa)) {
            koy(alt + 4, Ikon.of(Material.NAME_TAG)
                    .ad(t("menu.uyeler.davet.ad"))
                    .satirlar(l("menu.uyeler.davet.lore"))
                    .yap(), tik -> davetIste());
        }
        if (bitis < sirali.size()) {
            koy(alt + 5, Ikon.of(Material.SPECTRAL_ARROW).ad(t("menu.sonraki")).yap(), tik -> {
                Mesaj.sesSayfa(oyuncu);
                new UyelerMenu(plugin, oyuncu, arsa, sayfa + 1).ac();
            });
        }
        koy(alt + 8, rehber(dil));
    }

    // ── Ortak ikonlar (liste/detay menüleri de kullanır) ────────────────────

    static ItemStack sahipIkonu(Dil dil, UUID u) {
        return Ikon.kafa(Bukkit.getOfflinePlayer(u))
                .ad(L.t(dil, "menu.uye.sahip.ad", OyuncuBul.isim(u)))
                .satirlar(L.l(dil, "menu.uye.sahip.lore"))
                .yap();
    }

    /** Üye kafası: rütbe + her iznin durumu. {@code altSatirAnahtari} null ise alt satır eklenmez. */
    static ItemStack uyeIkonu(Dil dil, Arsa arsa, UUID u, ArsaUye uye, String altSatirAnahtari) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(u);
        Ikon i = Ikon.kafa(op).ad(L.t(dil, "menu.uye.ad", OyuncuBul.isim(u)))
                .satir(L.t(dil, "menu.uye.rutbe", uye.rutbe().ad(dil)));
        if (uye.rutbe() == Rutbe.MOD) {
            i.satir(L.t(dil, "menu.uye.mod-tum"));
        } else {
            int acik = 0;
            List<String> satirlar = new ArrayList<>();
            for (Izin iz : Izin.values()) {
                boolean var = arsa.uyeIzni(uye, iz);
                if (var) acik++;
                satirlar.add(L.t(dil, var ? "menu.uye.izin-acik" : "menu.uye.izin-kapali", iz.ad(dil))
                        + (uye.ozel(iz) != null ? L.t(dil, "menu.uye.ozel-isaret") : ""));
            }
            i.satir(L.t(dil, "menu.uye.acik-sayi", acik, Izin.values().length)).bosluk().satirlar(satirlar);
            if (uye.ozelAyarVar()) i.satir(L.t(dil, "menu.uye.ozel-aciklama"));
        }
        if (altSatirAnahtari != null) i.bosluk().satir(L.t(dil, altSatirAnahtari));
        return i.yap();
    }

    static ItemStack rehber(Dil dil) {
        Ikon i = Ikon.of(Material.BOOK).ad(L.t(dil, "menu.rehber.ad"));
        for (Rutbe r : Rutbe.values()) {
            i.bosluk().satir(r.ad(dil));
            if (r == Rutbe.MOD) {
                i.satirlar(L.l(dil, "menu.rehber.mod"));
            } else if (r.varsayilanlar().isEmpty()) {
                i.satir(L.t(dil, "menu.rehber.sadece-giris"));
            } else {
                List<String> adlar = new ArrayList<>();
                for (Izin iz : r.varsayilanlar()) adlar.add(iz.ad(dil));
                for (int k = 0; k < adlar.size(); k += 2) {
                    i.satir(L.t(dil, "menu.rehber.madde",
                            String.join(", ", adlar.subList(k, Math.min(adlar.size(), k + 2)))));
                }
            }
        }
        i.bosluk().satir(L.t(dil, "menu.rehber.not"));
        return i.yap();
    }

    private void davetIste() {
        oyuncu.closeInventory();
        plugin.sohbet().iste(oyuncu, "menu.uyeler.davet.soru", ad -> {
            if (!plugin.arsalar().kayitli(arsa)) return;
            OfflinePlayer hedef = OyuncuBul.bul(ad);
            if (hedef == null) {
                Mesaj.hata(oyuncu, "genel.oyuncu-yok", ad);
            } else {
                plugin.uyeServisi().davet(oyuncu, arsa, hedef);
            }
            if (plugin.yetki().panel(oyuncu, arsa)) new UyelerMenu(plugin, oyuncu, arsa, 0).ac();
        });
    }
}
