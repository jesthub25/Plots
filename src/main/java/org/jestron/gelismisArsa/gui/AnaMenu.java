package org.jestron.gelismisArsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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
        super(plugin, oyuncu, arsa, 3, "&8Arsa &7#" + arsa.id() + " &8» &0Yönetim Paneli");
    }

    @Override
    protected void ciz() {
        koy(10, bilgi());

        boolean sahip = plugin.yetki().sahipGibi(oyuncu, arsa);

        koy(12, Ikon.kafa(arsa.sahibiVar() ? Bukkit.getOfflinePlayer(arsa.sahip()) : null)
                .ad("&b&lÜyeler")
                .satir("&7Arsa üyelerini görüntüle, rütbe", "&7ve izinlerini tek tek düzenle,", "&7yeni üye davet et.")
                .bosluk()
                .satir("&7Üye sayısı: &f" + arsa.uyeler().size())
                .bosluk()
                .satir("&e▶ Tıkla")
                .yap(), t -> {
            Mesaj.sesSayfa(oyuncu);
            new UyelerMenu(plugin, oyuncu, arsa, 0).ac();
        });

        int acik = arsa.ziyaretciIzinleri().size();
        koy(14, Ikon.of(Material.OAK_FENCE_GATE)
                .ad("&a&lZiyaretçi İzinleri")
                .satir("&7Üye olmayan oyuncuların arsanda", "&7neleri yapabileceğini belirle.")
                .bosluk()
                .satir("&7Açık izin: " + (acik == 0 ? "&cHiçbiri &8(tam koruma)" : "&f" + acik + "&7/&f" + Izin.ZIYARETCI_IZINLERI.size()))
                .bosluk()
                .satir("&e▶ Tıkla")
                .yap(), t -> {
            Mesaj.sesSayfa(oyuncu);
            new ZiyaretciMenu(plugin, oyuncu, arsa).ac();
        });

        koy(16, kira(sahip), this::kiraTik);

        koy(22, Ikon.of(Material.BARRIER).ad("&c&lKapat").yap(), t -> oyuncu.closeInventory());
    }

    private org.bukkit.inventory.ItemStack bilgi() {
        Ikon i = Ikon.of(Material.FILLED_MAP)
                .ad("&e&lArsa #" + arsa.id())
                .satir("&7Dünya: &f" + arsa.dunya(),
                        "&7Boyut: &f" + arsa.genislikX() + "x" + arsa.genislikZ() + " &8(yükseklik " + arsa.yukseklik() + ")",
                        "&7Konum: &f" + arsa.konumYazisi())
                .bosluk()
                .satir("&7Sahip: &f" + (arsa.sahibiVar() ? OyuncuBul.isim(arsa.sahip()) : "&cSahipsiz"),
                        "&7Üye: &f" + arsa.uyeler().size());
        if (arsa.kiralik()) {
            i.satir("&7Kira bitişi: &f" + Sure.tarih(arsa.kiraBitis()),
                    "&7Kalan: &f" + Sure.yaz(arsa.kalanMs()));
        }
        return i.yap();
    }

    private org.bukkit.inventory.ItemStack kira(boolean sahip) {
        Ikon i = Ikon.of(Material.CLOCK).ad("&6&lKira Süresi");
        if (!arsa.kiralik()) {
            return i.satir("&7Bu arsa kiralık değil.").yap();
        }
        i.satir("&7Kalan: &f" + Sure.yaz(arsa.kalanMs()),
                "&7Bitiş: &f" + Sure.tarih(arsa.kiraBitis()));
        if (sahip && arsa.sahipMi(oyuncu.getUniqueId()) && plugin.kira() != null) {
            double gun = plugin.kira().ucret(86400L);
            double hafta = plugin.kira().ucret(604800L);
            i.bosluk()
                    .satir("&eSol Tık &8» &7+1 gün uzat &8(&f" + plugin.kira().bicim(gun) + "&8)",
                            "&eSağ Tık &8» &7+1 hafta uzat &8(&f" + plugin.kira().bicim(hafta) + "&8)")
                    .bosluk()
                    .satir("&8Kısaltmak / terk için: /arsa sure azalt, /arsa terket");
        } else {
            i.bosluk().satir("&8Süreyi sadece kiracı uzatabilir.");
        }
        return i.yap();
    }

    private void kiraTik(ClickType t) {
        if (!arsa.kiralik() || !arsa.sahipMi(oyuncu.getUniqueId()) || plugin.kira() == null) {
            Mesaj.sesHata(oyuncu);
            return;
        }
        long saniye = t.isRightClick() ? 604800L : 86400L;
        if (plugin.kira().uzat(oyuncu, arsa, saniye)) yenidenCiz();
    }
}
