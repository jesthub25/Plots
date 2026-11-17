package org.jestron.gelismisArsa.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jestron.gelismisArsa.util.Mesaj;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * GUI'den sohbete metin isteme (ör. davet edilecek oyuncu adı).
 * Yanıt ana thread'de işlenir; 30 saniye sonra veya "iptal" yazılınca iptal olur.
 */
public final class SohbetGirdisi implements Listener {

    private static final long SURE_MS = 30_000L;

    private final Plugin plugin;
    private final Map<UUID, Bekleyen> bekleyenler = new ConcurrentHashMap<>();

    private record Bekleyen(Consumer<String> isleyici, long bitis) {}

    public SohbetGirdisi(Plugin plugin) {
        this.plugin = plugin;
    }

    public void iste(Player p, String soru, Consumer<String> isleyici) {
        bekleyenler.put(p.getUniqueId(), new Bekleyen(isleyici, System.currentTimeMillis() + SURE_MS));
        Mesaj.gonder(p, soru);
        Mesaj.satir(p, "&8(İptal etmek için &7iptal &8yaz • 30 saniye)");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void sohbet(AsyncPlayerChatEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        Bekleyen b = bekleyenler.remove(u);
        if (b == null) return;
        e.setCancelled(true);
        String metin = e.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(u);
            if (p == null) return;
            if (System.currentTimeMillis() > b.bitis()) {
                Mesaj.hata(p, "Süre doldu, işlem iptal edildi.");
                return;
            }
            if (metin.equalsIgnoreCase("iptal") || metin.equalsIgnoreCase("cancel")) {
                Mesaj.bilgi(p, "İşlem iptal edildi.");
                return;
            }
            b.isleyici().accept(metin);
        });
    }

    @EventHandler
    public void cikis(PlayerQuitEvent e) {
        bekleyenler.remove(e.getPlayer().getUniqueId());
    }
}
