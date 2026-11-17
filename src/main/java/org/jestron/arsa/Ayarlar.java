package org.jestron.arsa;

import org.bukkit.configuration.file.FileConfiguration;
import org.jestron.arsa.model.Izin;
import org.jestron.arsa.model.Rutbe;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

/** config.yml'den okunan ayarlar. /arsa yenile ile tekrar yüklenir. */
public final class Ayarlar {

    public String varsayilanDil;
    public long mesajBeklemeMs;
    public boolean tekArsaKurali;

    public boolean kiralamaAktif;
    public double haftalikUcret;
    public int maxHafta;
    public int oyuncuBasiMaxArsa;
    public boolean bitinceSifirla;
    public boolean sablonYoksaHavayaCevir;
    public int hatirlatmaSaat;

    public boolean korumaPatlama;
    public boolean korumaAtes;
    public boolean korumaSivi;
    public boolean korumaPiston;
    public boolean korumaDagitici;
    public boolean korumaHuni;
    public boolean korumaMobTahribat;
    public boolean korumaBuyume;

    public final EnumSet<Izin> ziyaretciVarsayilan = EnumSet.noneOf(Izin.class);

    public void yukle(FileConfiguration c, Logger log) {
        varsayilanDil = c.getString("varsayilan-dil", "otomatik");
        tekArsaKurali = c.getBoolean("tek-arsa-kurali", true);
        mesajBeklemeMs = Math.max(0L, c.getLong("mesaj-bekleme-ms", 1500L));

        kiralamaAktif = c.getBoolean("kiralama.aktif", true);
        haftalikUcret = Math.max(0D, c.getDouble("kiralama.haftalik-ucret", 10000D));
        maxHafta = Math.max(1, c.getInt("kiralama.max-hafta", 4));
        oyuncuBasiMaxArsa = Math.max(0, c.getInt("kiralama.oyuncu-basi-max-arsa", 1));
        bitinceSifirla = c.getBoolean("kiralama.bitince-sifirla", true);
        sablonYoksaHavayaCevir = c.getBoolean("kiralama.sablon-yoksa-havaya-cevir", true);
        hatirlatmaSaat = Math.max(0, c.getInt("kiralama.hatirlatma-saat", 24));

        korumaPatlama = c.getBoolean("koruma.patlama", true);
        korumaAtes = c.getBoolean("koruma.ates", true);
        korumaSivi = c.getBoolean("koruma.sivi", true);
        korumaPiston = c.getBoolean("koruma.piston", true);
        korumaDagitici = c.getBoolean("koruma.dagitici", true);
        korumaHuni = c.getBoolean("koruma.huni", true);
        korumaMobTahribat = c.getBoolean("koruma.mob-tahribat", true);
        korumaBuyume = c.getBoolean("koruma.buyume", true);

        Rutbe.varsayilanlariSifirla();
        for (Rutbe r : Rutbe.values()) {
            if (r == Rutbe.MOD) continue;
            String yol = "rutbe-izinleri." + r.name();
            if (c.isList(yol)) {
                r.varsayilanAyarla(izinListesi(c.getStringList(yol), yol, log));
            }
        }

        ziyaretciVarsayilan.clear();
        for (Izin i : izinListesi(c.getStringList("ziyaretci-varsayilan"), "ziyaretci-varsayilan", log)) {
            if (Izin.ZIYARETCI_IZINLERI.contains(i)) ziyaretciVarsayilan.add(i);
        }
    }

    public double saniyeUcreti() {
        return haftalikUcret / 604800D;
    }

    private static EnumSet<Izin> izinListesi(List<String> liste, String yol, Logger log) {
        EnumSet<Izin> s = EnumSet.noneOf(Izin.class);
        for (String ad : liste) {
            List<Izin> cevrilen = Izin.eskiAddanCevir(ad);
            if (cevrilen.isEmpty()) {
                log.warning("config.yml '" + yol + "' içinde bilinmeyen izin: " + ad);
            }
            s.addAll(cevrilen);
        }
        return s;
    }
}
