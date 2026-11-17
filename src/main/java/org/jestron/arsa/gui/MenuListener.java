package org.jestron.arsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Menü tıklamaları. Üstteki envanter bir {@link Menu} ise HER tıklama ve sürükleme iptal edilir
 * (shift-tık, sayı tuşu, çift tık toplama, yaratıcı mod klonlama dahil). Sadece menü
 * slotlarına yapılan tıklamalar menüye iletilir.
 *
 * Not: InventoryView hiç kullanılmaz (1.21'de sınıftan arayüze dönüştü), bu sayede
 * 1.20.1 ile derlenen eklenti üst sürümlerde de sorunsuz çalışır.
 */
public final class MenuListener implements Listener {

    private static final long TIK_ARALIGI_MS = 120L;
    private static final Set<UUID> ACIK = new HashSet<>();
    private final Plugin plugin;
    private final Map<UUID, Long> sonTik = new HashMap<>();

    public MenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    static void acildi(Player p) {
        ACIK.add(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void tik(InventoryClickEvent e) {
        Inventory ust = e.getInventory();
        InventoryHolder h = ust.getHolder();
        if (!(h instanceof Menu menu)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot();
        if (e.getClickedInventory() == null || slot < 0 || slot >= ust.getSize()) return;
        ClickType tip = e.getClick();
        if (tip == ClickType.DOUBLE_CLICK || tip == ClickType.UNKNOWN) return;

        long simdi = System.currentTimeMillis();
        Long son = sonTik.get(p.getUniqueId());
        if (son != null && simdi - son < TIK_ARALIGI_MS) return;
        sonTik.put(p.getUniqueId(), simdi);

        // Envanter açma/kapama tıklama olayının içinde değil, bir sonraki tick'te yapılır
        // (Spigot'un önerdiği güvenli yol; hayalet eşya / senkron sorunlarını önler).
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isOnline()) menu.tiklandi(slot, tip);
        });
    }

    /** Başka bir eklenti iptali kaldırsa bile menüden eşya alınamaz. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void tikKesinlestir(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void surukle(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void surukleKesinlestir(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    @EventHandler
    public void kapat(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) ACIK.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void cikis(PlayerQuitEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        sonTik.remove(u);
        ACIK.remove(u);
    }

    /** Eklenti kapanırken açık menüleri kapatır. */
    public static void tumunuKapat() {
        for (UUID u : new HashSet<>(ACIK)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.closeInventory();
        }
        ACIK.clear();
    }
}
