package org.jestron.gelismisArsa.dil;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Oyuncu başına dil tercihi. Oyuncu /arsa kullanınca TR, /plot kullanınca EN olur ve kalıcı kaydedilir.
 * Hiç komut kullanmamış oyuncu için config'deki varsayılan (otomatik = oyun istemcisinin dili) geçerlidir.
 */
public final class DilYoneticisi {

    private final Map<UUID, Dil> tercihler = new HashMap<>();
    private Dil varsayilan; // null = otomatik (istemci dili)
    private Dil konsol = Dil.TR;
    private Runnable degisti = () -> { };

    public void varsayilanAyarla(String ayar) {
        this.varsayilan = Dil.bul(ayar); // "otomatik" → null
    }

    public void degisinceCalistir(Runnable r) {
        this.degisti = r;
    }

    public Dil dil(CommandSender s) {
        if (s instanceof Player p) return dil(p);
        return konsol;
    }

    public Dil dil(Player p) {
        Dil d = tercihler.get(p.getUniqueId());
        if (d != null) return d;
        if (varsayilan != null) return varsayilan;
        try {
            Dil istemci = Dil.bul(p.getLocale());
            if (istemci == Dil.TR) return Dil.TR;
        } catch (Throwable ignored) {
            // istemci dili okunamazsa İngilizce
        }
        return Dil.EN;
    }

    /** Çevrimdışı oyuncu için (bildirimler): kayıtlı tercih yoksa varsayılan. */
    public Dil dil(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return dil(p);
        Dil d = tercihler.get(uuid);
        if (d != null) return d;
        return varsayilan != null ? varsayilan : Dil.TR;
    }

    /** Komut kullanıldığında çağrılır. */
    public void kullan(CommandSender s, Dil d) {
        if (s instanceof Player p) {
            Dil eski = tercihler.put(p.getUniqueId(), d);
            if (eski != d) degisti.run();
        } else {
            konsol = d;
        }
    }

    public Map<UUID, Dil> tumu() {
        return Collections.unmodifiableMap(tercihler);
    }

    public void yukle(Map<UUID, Dil> kayitli) {
        tercihler.clear();
        tercihler.putAll(kayitli);
    }
}
