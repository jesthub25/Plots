package org.jestron.gelismisArsa.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jestron.gelismisArsa.model.Izin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mesaj, aksiyon çubuğu ve ses yardımcıları (spam korumalı). */
public final class Mesaj {

    private static String onEk = renk("&6&lArsa &8» &7");
    private static long beklemeMs = 1500L;
    private static final Map<UUID, Map<String, Long>> SON = new HashMap<>();

    private Mesaj() {}

    public static void ayarla(String yeniOnEk, long yeniBeklemeMs) {
        onEk = renk(yeniOnEk);
        beklemeMs = yeniBeklemeMs;
    }

    public static String renk(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void gonder(CommandSender alici, String mesaj) {
        alici.sendMessage(onEk + renk(mesaj));
    }

    public static void bilgi(CommandSender alici, String mesaj) {
        gonder(alici, "&7" + mesaj);
    }

    public static void basari(CommandSender alici, String mesaj) {
        gonder(alici, "&a" + mesaj);
        if (alici instanceof Player p) sesBasari(p);
    }

    public static void hata(CommandSender alici, String mesaj) {
        gonder(alici, "&c" + mesaj);
        if (alici instanceof Player p) sesHata(p);
    }

    /** Önek olmadan düz satır (listeler için). */
    public static void satir(CommandSender alici, String mesaj) {
        alici.sendMessage(renk(mesaj));
    }

    public static void aksiyon(Player p, String mesaj) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(renk(mesaj)));
    }

    /** Koruma reddi: sohbet + aksiyon çubuğu, oyuncu ve izin başına bekleme süreli. */
    public static void reddedildi(Player p, Izin izin) {
        if (!izinVer(p, "red:" + izin.name())) return;
        gonder(p, "&cBu arsada &f" + izin.ad() + " &cyetkin yok.");
        aksiyon(p, "&c✘ " + izin.ad() + " yetkin yok");
        sesHata(p);
    }

    /** Sessiz reddetme (basınç plakası, yerden eşya alma vb. sürekli tetiklenenler). */
    public static void sessizRed(Player p, Izin izin) {
        if (!izinVer(p, "sessiz:" + izin.name(), Math.max(beklemeMs, 3000L))) return;
        aksiyon(p, "&c✘ Bu arsada " + izin.ad() + " yetkin yok");
    }

    private static boolean izinVer(Player p, String anahtar) {
        return izinVer(p, anahtar, beklemeMs);
    }

    private static boolean izinVer(Player p, String anahtar, long ms) {
        long simdi = System.currentTimeMillis();
        Map<String, Long> m = SON.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        Long son = m.get(anahtar);
        if (son != null && simdi - son < ms) return false;
        m.put(anahtar, simdi);
        return true;
    }

    public static void temizle(UUID uuid) {
        SON.remove(uuid);
    }

    // ── Sesler (anahtar ile çalınır; tüm sürümlerde uyumlu) ─────────────────

    public static void sesTik(Player p) {
        ses(p, "minecraft:ui.button.click", 0.5f, 1.2f);
    }

    public static void sesBasari(Player p) {
        ses(p, "minecraft:entity.experience_orb.pickup", 0.6f, 1.4f);
    }

    public static void sesHata(Player p) {
        ses(p, "minecraft:entity.villager.no", 0.6f, 1.0f);
    }

    public static void sesSayfa(Player p) {
        ses(p, "minecraft:item.book.page_turn", 0.7f, 1.0f);
    }

    private static void ses(Player p, String anahtar, float ses, float perde) {
        try {
            p.playSound(p.getLocation(), anahtar, ses, perde);
        } catch (Throwable ignored) {
            // ses çalınamazsa işlem etkilenmez
        }
    }
}
