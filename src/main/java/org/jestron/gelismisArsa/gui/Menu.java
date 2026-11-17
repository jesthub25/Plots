package org.jestron.gelismisArsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.dil.Dil;
import org.jestron.gelismisArsa.dil.L;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.util.Mesaj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tüm menülerin temeli. Menü kendi envanterinin sahibidir (InventoryHolder);
 * tıklama tespiti başlığa değil bu nesneye dayanır, bu yüzden oyuncu hiçbir şekilde
 * menü eşyalarını alamaz veya kendi envanterini menü üzerinden kullanamaz.
 * Menü dili, açan oyuncunun diliyle (son kullandığı /arsa veya /plot) belirlenir.
 */
public abstract class Menu implements InventoryHolder {

    protected final GelismisArsa plugin;
    protected final Player oyuncu;
    protected final Arsa arsa;
    protected final Dil dil;
    private final Inventory envanter;
    private final Map<Integer, Consumer<ClickType>> eylemler = new HashMap<>();

    protected Menu(GelismisArsa plugin, Player oyuncu, Arsa arsa, int satir, String baslikAnahtari, Object... args) {
        this.plugin = plugin;
        this.oyuncu = oyuncu;
        this.arsa = arsa;
        this.dil = Mesaj.dil(oyuncu);
        this.envanter = Bukkit.createInventory(this, Math.max(1, Math.min(6, satir)) * 9,
                L.t(dil, baslikAnahtari, args));
    }

    @Override
    public Inventory getInventory() {
        return envanter;
    }

    /** Menü içeriğini çizer. */
    protected abstract void ciz();

    /** Her tıklamadan önce: arsa silinmiş veya yetki kaybedilmişse menü kapanır. */
    protected boolean gecerli() {
        return arsa != null && plugin.arsalar().kayitli(arsa) && plugin.yetki().panel(oyuncu, arsa);
    }

    public final void ac() {
        if (!gecerli()) {
            Mesaj.hata(oyuncu, "menu.acamazsin");
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
            Mesaj.hata(oyuncu, "menu.yetki-kalmadi");
            return;
        }
        Consumer<ClickType> eylem = eylemler.get(slot);
        if (eylem != null) eylem.accept(tip);
    }

    protected int boyut() {
        return envanter.getSize();
    }

    /** Menünün dilinde metin. */
    protected String t(String anahtar, Object... args) {
        return L.t(dil, anahtar, args);
    }

    /** Menünün dilinde çok satırlı metin. */
    protected List<String> l(String anahtar, Object... args) {
        return L.l(dil, anahtar, args);
    }

    /** Ortak "Geri" düğmesi. */
    protected ItemStack geriIkonu(String aciklamaAnahtari) {
        return Ikon.of(org.bukkit.Material.ARROW).ad(t("menu.geri-ad")).satir(t(aciklamaAnahtari)).yap();
    }
}
