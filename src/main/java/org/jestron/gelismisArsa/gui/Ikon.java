package org.jestron.gelismisArsa.gui;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jestron.gelismisArsa.util.Mesaj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** GUI eşya oluşturucu. Parıltı için büyü kayıt defterinden anahtarla alınır (sürüm bağımsız). */
public final class Ikon {

    private static final Enchantment PARILTI = parilti();

    private final ItemStack item;
    private final ItemMeta meta;
    private final List<String> lore = new ArrayList<>();

    private Ikon(ItemStack item) {
        this.item = item;
        this.meta = item.getItemMeta();
    }

    public static Ikon of(Material m) {
        return new Ikon(new ItemStack(m == null ? Material.STONE : m));
    }

    public static Ikon kafa(OfflinePlayer sahip) {
        Ikon i = new Ikon(new ItemStack(Material.PLAYER_HEAD));
        if (sahip != null && i.meta instanceof SkullMeta sm) {
            try {
                sm.setOwningPlayer(sahip);
            } catch (Throwable ignored) {
                // doku alınamazsa düz kafa kalır
            }
        }
        return i;
    }

    public Ikon ad(String ad) {
        if (meta != null) meta.setDisplayName(Mesaj.renk("&r" + ad));
        return this;
    }

    public Ikon satir(String... satirlar) {
        for (String s : satirlar) lore.add(Mesaj.renk(s));
        return this;
    }

    public Ikon satirlar(List<String> satirlar) {
        for (String s : satirlar) lore.add(Mesaj.renk(s));
        return this;
    }

    public Ikon bosluk() {
        lore.add("");
        return this;
    }

    public Ikon parla(boolean parla) {
        if (parla && meta != null && PARILTI != null) {
            meta.addEnchant(PARILTI, 1, true);
        }
        return this;
    }

    public Ikon adet(int adet) {
        item.setAmount(Math.max(1, Math.min(64, adet)));
        return this;
    }

    public ItemStack yap() {
        if (meta != null) {
            if (!lore.isEmpty()) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Enchantment parilti() {
        for (String anahtar : Arrays.asList("unbreaking", "luck_of_the_sea", "mending")) {
            try {
                Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(anahtar));
                if (e != null) return e;
            } catch (Throwable ignored) {
                // sonraki anahtarı dene
            }
        }
        return null;
    }
}
