package org.jestron.gelismisArsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.util.Mesaj;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tüm arsa menülerinin temeli. Menü kendi envanterinin sahibidir (InventoryHolder);
 * tıklama tespiti başlığa değil bu nesneye dayanır, bu yüzden oyuncu hiçbir şekilde
 * menü eşyalarını alamaz veya kendi envanterini menü üzerinden kullanamaz.
 */
public abstract class Menu implements InventoryHolder {

    protected final GelismisArsa plugin;
    protected final Player oyuncu;
    protected final Arsa arsa;
    private final Inventory envanter;
    private final Map<Integer, Consumer<ClickType>> eylemler = new HashMap<>();

    protected Menu(GelismisArsa plugin, Player oyuncu, Arsa arsa, int satir, String baslik) {
        this.plugin = plugin;
        this.oyuncu = oyuncu;
        this.arsa = arsa;
        this.envanter = Bukkit.createInventory(this, Math.max(1, Math.min(6, satir)) * 9, Mesaj.renk(baslik));
    }

    @Override
    public Inventory getInventory() {
        return envanter;
    }

    /** Menü içeriğini çizer. */
    protected abstract void ciz();

    /** Her tıklamadan önce: arsa silinmiş veya yetki kaybedilmişse menü kapanır. */
    protected boolean gecerli() {
        return plugin.arsalar().kayitli(arsa) && plugin.yetki().panel(oyuncu, arsa);
    }

    public final void ac() {
        if (!gecerli()) {
            Mesaj.hata(oyuncu, "Bu menüyü açma yetkin yok.");
            return;
        }
        yenidenCiz();
        oyuncu.openInventory(envanter);
        MenuListener.acildi(oyuncu);
    }

    /** Menüyü kapatıp açmadan (imleç sıçramadan) yeniden çizer. */
    public final void yenidenCiz() {
        envanter.clear();
        eylemler.clear();
        ciz();
    }

    protected final void koy(int slot, ItemStack item) {
        if (slot >= 0 && slot < envanter.getSize()) envanter.setItem(slot, item);
    }

    protected final void koy(int slot, ItemStack item, Consumer<ClickType> eylem) {
        if (slot < 0 || slot >= envanter.getSize()) return;
        envanter.setItem(slot, item);
        eylemler.put(slot, eylem);
    }

    final void tiklandi(int slot, ClickType tip) {
        if (!gecerli()) {
            oyuncu.closeInventory();
            Mesaj.hata(oyuncu, "Bu arsa üzerindeki yönetim yetkin artık yok.");
            return;
        }
        Consumer<ClickType> eylem = eylemler.get(slot);
        if (eylem != null) eylem.accept(tip);
    }

    protected int boyut() {
        return envanter.getSize();
    }
}
