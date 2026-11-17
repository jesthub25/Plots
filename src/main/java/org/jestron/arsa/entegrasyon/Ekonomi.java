package org.jestron.arsa.entegrasyon;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Locale;

/**
 * Vault ekonomi köprüsü. Bu sınıf yalnızca Vault yüklüyse oluşturulur, böylece Vault
 * olmadan da eklenti (koruma, GUI) çalışır; sadece kiralama devre dışı kalır.
 * Ekonomi sağlayıcısı geç yüklenirse ilk kullanımda tekrar aranır.
 */
public final class Ekonomi {

    private Economy eco;

    private Economy al() {
        if (eco == null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) eco = rsp.getProvider();
        }
        return eco;
    }

    public boolean hazir() {
        return al() != null;
    }

    public double bakiye(OfflinePlayer p) {
        Economy e = al();
        return e == null ? 0D : e.getBalance(p);
    }

    public boolean cek(OfflinePlayer p, double miktar) {
        if (miktar <= 0) return true;
        Economy e = al();
        if (e == null || !e.has(p, miktar)) return false;
        EconomyResponse r = e.withdrawPlayer(p, miktar);
        return r != null && r.transactionSuccess();
    }

    public boolean yatir(OfflinePlayer p, double miktar) {
        if (miktar <= 0) return true;
        Economy e = al();
        if (e == null) return false;
        EconomyResponse r = e.depositPlayer(p, miktar);
        return r != null && r.transactionSuccess();
    }

    public String bicim(double miktar) {
        Economy e = al();
        if (e != null) {
            try {
                return e.format(miktar);
            } catch (Throwable ignored) {
                // bazı sağlayıcılar format desteklemez
            }
        }
        return String.format(Locale.ROOT, "%,.2f", miktar);
    }
}
