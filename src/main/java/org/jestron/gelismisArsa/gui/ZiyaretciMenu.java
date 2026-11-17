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
        super(plugin, oyuncu, arsa, 3, "&8Arsa &7#" + arsa.id() + " &8» &0Ziyaretçiler");
    }

    @Override
    protected void ciz() {
        koy(0, Ikon.of(Material.ARROW).ad("&e« Geri").satir("&7Yönetim paneline dön.").yap(), t -> {
            Mesaj.sesSayfa(oyuncu);
            new AnaMenu(plugin, oyuncu, arsa).ac();
        });

        int acikSayi = arsa.ziyaretciIzinleri().size();
        koy(4, Ikon.of(Material.OAK_FENCE_GATE).ad("&a&lZiyaretçi İzinleri")
                .satir("&7Arsanın üyesi olmayan HERKES için", "&7geçerlidir. Varsayılan: tam koruma.")
                .bosluk()
                .satir("&7Açık izin: &f" + acikSayi + "&7/&f" + Izin.ZIYARETCI_IZINLERI.size())
                .bosluk()
                .satir("&c⚠ Açtığın izinleri sunucudaki", "&cherkes kullanabilir!")
                .yap());

        int slot = 9;
        for (Izin izin : Izin.values()) {
            if (!Izin.ZIYARETCI_IZINLERI.contains(izin)) continue;
            final Izin iz = izin;
            boolean acik = arsa.ziyaretciIzni(iz);
            List<String> aciklama = new ArrayList<>();
            for (String s : iz.aciklama()) aciklama.add("&7" + s);
            koy(slot++, Ikon.of(iz.ikon())
                    .ad((acik ? "&a" : "&c") + iz.ad())
                    .satirlar(aciklama)
                    .bosluk()
                    .satir("&7Ziyaretçiler: " + (acik ? "&a✔ YAPABİLİR" : "&c✘ YAPAMAZ"))
                    .bosluk()
                    .satir("&e▶ Tıkla: " + (acik ? "kapat" : "aç"))
                    .parla(acik)
                    .yap(), t -> {
                if (plugin.uyeServisi().ziyaretciAyarla(oyuncu, arsa, iz, !arsa.ziyaretciIzni(iz), true)) {
                    Mesaj.sesTik(oyuncu);
                    yenidenCiz();
                }
            });
        }

        koy(24, Ikon.of(Material.LIME_CONCRETE).ad("&a&lTümünü Aç")
                .satir("&7Ziyaretçiler arsada her şeyi", "&7yapabilir hale gelir.").yap(), t -> topluAyarla(true));
        koy(25, Ikon.of(Material.RED_CONCRETE).ad("&c&lTümünü Kapat")
                .satir("&7Tam koruma: ziyaretçiler sadece", "&7arsaya girebilir.").yap(), t -> topluAyarla(false));
    }

    private void topluAyarla(boolean deger) {
        for (Izin i : Izin.ZIYARETCI_IZINLERI) {
            if (!plugin.uyeServisi().ziyaretciAyarla(oyuncu, arsa, i, deger, true)) return;
        }
        Mesaj.basari(oyuncu, "Ziyaretçiler için tüm izinler " + (deger ? "&2AÇILDI" : "&cKAPATILDI") + "&a.");
        yenidenCiz();
    }
}
