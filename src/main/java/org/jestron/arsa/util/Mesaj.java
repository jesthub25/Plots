package org.jestron.arsa.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jestron.arsa.dil.Dil;
import org.jestron.arsa.dil.DilYoneticisi;
import org.jestron.arsa.dil.L;
import org.jestron.arsa.model.Izin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dile duyarlı mesaj, aksiyon çubuğu ve ses yardımcıları (spam korumalı).
 * Tüm metinler dil dosyasındaki anahtarlarla gönderilir; alıcının dili otomatik seçilir.
 */
public final class Mesaj {

    private static DilYoneticisi diller = new DilYoneticisi();
    private static long beklemeMs = 1500L;
    private static final Map<UUID, Map<String, Long>> SON = new HashMap<>();

    private Mesaj() {}

    public static void kur(DilYoneticisi yonetici, long yeniBeklemeMs) {
        diller = yonetici;
        beklemeMs = yeniBeklemeMs;
    }

    public static Dil dil(CommandSender s) {
        return diller.dil(s);
    }

    public static String renk(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    private static String onek(Dil d) {
        return L.t(d, "onek");
    }

    // ── Anahtar ile gönderim ────────────────────────────────────────────────

    public static void gonder(CommandSender s, String anahtar, Object... args) {
        Dil d = dil(s);
        s.sendMessage(onek(d) + L.t(d, anahtar, args));
    }

    public static void bilgi(CommandSender s, String anahtar, Object... args) {
        Dil d = dil(s);
        s.sendMessage(onek(d) + renk("&7") + L.t(d, anahtar, args));
    }

    public static void basari(CommandSender s, String anahtar, Object... args) {
        Dil d = dil(s);
        s.sendMessage(onek(d) + renk("&a") + L.t(d, anahtar, args));
        if (s instanceof Player p) sesBasari(p);
    }

    public static void hata(CommandSender s, String anahtar, Object... args) {
        Dil d = dil(s);
        s.sendMessage(onek(d) + renk("&c") + L.t(d, anahtar, args));
        if (s instanceof Player p) sesHata(p);
    }

    /** Öneksiz satır (listeler, başlıklar). */
    public static void satir(CommandSender s, String anahtar, Object... args) {
        s.sendMessage(L.t(dil(s), anahtar, args));
    }

    /** Önceden çevrilmiş metni öneksiz gönderir. */
    public static void hamSatir(CommandSender s, String metin) {
        s.sendMessage(renk(metin));
    }

    public static void aksiyon(Player p, String metin) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(renk(metin)));
    }

    // ── Koruma reddi ────────────────────────────────────────────────────────

    public static void reddedildi(Player p, Izin izin) {
        if (!izinVer(p, "red:" + izin.name(), beklemeMs)) return;
        Dil d = dil(p);
        p.sendMessage(onek(d) + L.t(d, "koruma.red", izin.ad(d)));
        aksiyon(p, L.t(d, "koruma.red-aksiyon", izin.ad(d)));
        sesHata(p);
    }

    /** Sürekli tetiklenen durumlar (basınç plakası, yerden eşya) için sadece aksiyon çubuğu. */
    public static void sessizRed(Player p, Izin izin) {
        if (!izinVer(p, "sessiz:" + izin.name(), Math.max(beklemeMs, 3000L))) return;
        Dil d = dil(p);
        aksiyon(p, L.t(d, "koruma.red-aksiyon", izin.ad(d)));
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

    // ── Sesler (anahtar ile; tüm sürümlerde uyumlu) ─────────────────────────

    public static void sesTik(Player p) { ses(p, "minecraft:ui.button.click", 0.5f, 1.2f); }
    public static void sesBasari(Player p) { ses(p, "minecraft:entity.experience_orb.pickup", 0.6f, 1.4f); }
    public static void sesHata(Player p) { ses(p, "minecraft:entity.villager.no", 0.6f, 1.0f); }
    public static void sesSayfa(Player p) { ses(p, "minecraft:item.book.page_turn", 0.7f, 1.0f); }

    private static void ses(Player p, String anahtar, float ses, float perde) {
        try {
            p.playSound(p.getLocation(), anahtar, ses, perde);
        } catch (Throwable ignored) {
            // ses çalınamazsa işlem etkilenmez
        }
    }
}
