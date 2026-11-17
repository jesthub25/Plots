package org.jestron.gelismisArsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jestron.gelismisArsa.GelismisArsa;
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
                baslik(arsa, kisit(istenen, sirali.size()), sayfaSayisi(sirali.size())));
        this.sayfa = kisit(istenen, sirali.size());
        this.sirali = sirali;
    }

    private static int kisit(int sayfa, int adet) {
        return Math.max(0, Math.min(sayfa, sayfaSayisi(adet) - 1));
    }

    private static String baslik(Arsa arsa, int sayfa, int toplam) {
        return "&8Arsa &7#" + arsa.id() + " &8» &0Üyeler" + (toplam > 1 ? " &8(" + (sayfa + 1) + "/" + toplam + ")" : "");
    }

    /** Sıra: sahip, sonra rütbe (yüksekten düşüğe), sonra isim. Sahip listede null olmayan ilk eleman. */
    private static List<UUID> sirala(Arsa arsa) {
        List<UUID> l = new ArrayList<>();
        if (arsa.sahibiVar()) l.add(arsa.sahip());
        List<Map.Entry<UUID, ArsaUye>> uyeler = new ArrayList<>(arsa.uyeler().entrySet());
        uyeler.sort(Comparator.<Map.Entry<UUID, ArsaUye>>comparingInt(e -> -e.getValue().rutbe().seviye())
                .thenComparing(e -> OyuncuBul.isim(e.getKey()), String.CASE_INSENSITIVE_ORDER));
        for (Map.Entry<UUID, ArsaUye> e : uyeler) l.add(e.getKey());
        return l;
    }

    private static int sayfaSayisi(int adet) {
        return Math.max(1, (adet + SAYFA_BOYUTU - 1) / SAYFA_BOYUTU);
    }

    private static int satirSayisi(int adet, int sayfa) {
        int sayfadaki = Math.min(SAYFA_BOYUTU, Math.max(0, adet - sayfa * SAYFA_BOYUTU));
        int izgara = Math.max(1, (sayfadaki + 8) / 9);
        return izgara + 1;
    }

    @Override
    protected void ciz() {
        int baslangic = sayfa * SAYFA_BOYUTU;
        int bitis = Math.min(sirali.size(), baslangic + SAYFA_BOYUTU);
        int slot = 0;
        for (int i = baslangic; i < bitis; i++, slot++) {
            UUID u = sirali.get(i);
            if (arsa.sahipMi(u)) {
                koy(slot, Ikon.kafa(Bukkit.getOfflinePlayer(u))
                        .ad("&6&l" + OyuncuBul.isim(u))
                        .satir("&6★ Arsa Sahibi", "&7Tüm izinlere sahiptir.")
                        .yap());
                continue;
            }
            ArsaUye uye = arsa.uye(u);
            if (uye == null) continue;
            koy(slot, uyeIkonu(u, uye), t -> {
                if (!plugin.yetki().yonetebilir(oyuncu, arsa, u)) {
                    Mesaj.hata(oyuncu, plugin.uyeServisi().yonetimRedSebebi(oyuncu, arsa, u));
                    return;
                }
                Mesaj.sesSayfa(oyuncu);
                new UyeIzinMenu(plugin, oyuncu, arsa, u).ac();
            });
        }

        int alt = boyut() - 9;
        koy(alt, Ikon.of(Material.ARROW).ad("&e« Geri").satir("&7Yönetim paneline dön.").yap(), t -> {
            Mesaj.sesSayfa(oyuncu);
            new AnaMenu(plugin, oyuncu, arsa).ac();
        });

        if (sayfa > 0) {
            koy(alt + 3, Ikon.of(Material.SPECTRAL_ARROW).ad("&e‹ Önceki Sayfa").yap(), t -> {
                Mesaj.sesSayfa(oyuncu);
                new UyelerMenu(plugin, oyuncu, arsa, sayfa - 1).ac();
            });
        }

        if (plugin.yetki().davetEdebilir(oyuncu, arsa)) {
            koy(alt + 4, Ikon.of(Material.NAME_TAG)
                    .ad("&a&l+ Üye Davet Et")
                    .satir("&7Tıkla ve davet etmek istediğin", "&7oyuncunun adını sohbete yaz.")
                    .bosluk()
                    .satir("&8Komut: /arsa yetkiver <oyuncu> [rütbe|izin]")
                    .yap(), t -> davetIste());
        }

        if (bitis < sirali.size()) {
            koy(alt + 5, Ikon.of(Material.SPECTRAL_ARROW).ad("&eSonraki Sayfa ›").yap(), t -> {
                Mesaj.sesSayfa(oyuncu);
                new UyelerMenu(plugin, oyuncu, arsa, sayfa + 1).ac();
            });
        }

        koy(alt + 8, rehber());
    }

    private org.bukkit.inventory.ItemStack uyeIkonu(UUID u, ArsaUye uye) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(u);
        Ikon i = Ikon.kafa(op).ad("&f&l" + OyuncuBul.isim(u))
                .satir("&7Rütbe: " + uye.rutbe().gorunenAd());
        if (uye.rutbe() == Rutbe.MOD) {
            i.satir("&6Tüm izinlere sahip &8(yönetici)");
        } else {
            int acik = 0;
            List<String> satirlar = new ArrayList<>();
            for (Izin iz : Izin.values()) {
                boolean var = arsa.uyeIzni(uye, iz);
                if (var) acik++;
                satirlar.add((var ? "&a✔ " : "&c✘ ") + "&7" + iz.ad() + (uye.ozel(iz) != null ? " &e*" : ""));
            }
            i.satir("&7Açık izin: &f" + acik + "&7/&f" + Izin.values().length).bosluk().satirlar(satirlar);
            if (uye.ozelAyarVar()) i.satir("&e* &8= özel ayar");
        }
        i.bosluk();
        if (plugin.yetki().yonetebilir(oyuncu, arsa, u)) {
            i.satir("&e▶ Tıkla: rütbe ve izinleri düzenle");
        } else {
            i.satir("&8Bu üyeyi yönetemezsin.");
        }
        return i.yap();
    }

    private org.bukkit.inventory.ItemStack rehber() {
        Ikon i = Ikon.of(Material.BOOK).ad("&6&lRütbe Rehberi");
        for (Rutbe r : Rutbe.values()) {
            i.bosluk().satir(r.gorunenAd());
            if (r == Rutbe.MOD) {
                i.satir("&8• Tüm izinler + üye yönetimi", "&8• Sahibe ve MOD'lara dokunamaz", "&8• MOD rütbesi veremez");
            } else if (r.varsayilanlar().isEmpty()) {
                i.satir("&8• Sadece arsaya girebilir");
            } else {
                List<String> adlar = new ArrayList<>();
                for (Izin iz : r.varsayilanlar()) adlar.add(iz.ad());
                for (int k = 0; k < adlar.size(); k += 2) {
                    i.satir("&8• " + String.join("&8, ", adlar.subList(k, Math.min(adlar.size(), k + 2))));
                }
            }
        }
        i.bosluk().satir("&8Rütbe değişince özel izin ayarları sıfırlanır.");
        return i.yap();
    }

    private void davetIste() {
        oyuncu.closeInventory();
        plugin.sohbet().iste(oyuncu, "&aDavet etmek istediğin oyuncunun adını sohbete yaz.", ad -> {
            if (!plugin.arsalar().kayitli(arsa)) return;
            OfflinePlayer hedef = OyuncuBul.bul(ad);
            if (hedef == null) {
                Mesaj.hata(oyuncu, "'" + ad + "' adında bir oyuncu bulunamadı.");
            } else {
                plugin.uyeServisi().davet(oyuncu, arsa, hedef);
            }
            if (plugin.yetki().panel(oyuncu, arsa)) new UyelerMenu(plugin, oyuncu, arsa, 0).ac();
        });
    }
}
