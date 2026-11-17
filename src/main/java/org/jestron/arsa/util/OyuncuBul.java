package org.jestron.arsa.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/** İsimden oyuncu bulma (çevrimiçi → sunucuya daha önce girmiş oyuncular). */
public final class OyuncuBul {

    private OyuncuBul() {}

    public static OfflinePlayer bul(String ad) {
        if (ad == null || ad.isEmpty() || ad.length() > 16) return null;
        Player tam = Bukkit.getPlayerExact(ad);
        if (tam != null) return tam;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(ad)) return p;
        }
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            String n = op.getName();
            if (n != null && n.equalsIgnoreCase(ad)) return op;
        }
        return null;
    }

    /** UUID'den görünen isim; bilinmiyorsa kısaltılmış UUID. */
    public static String isim(UUID uuid) {
        if (uuid == null) return "-";
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        String n = Bukkit.getOfflinePlayer(uuid).getName();
        return n != null ? n : uuid.toString().substring(0, 8);
    }
}
