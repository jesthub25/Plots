package org.jestron.gelismisArsa.dil;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Dil dosyaları: plugins/GelismisArsa/lang/tr.yml ve en.yml (düzenlenebilir).
 * Dosyada olmayan anahtarlar jar içindeki varsayılandan, o da yoksa diğer dilden alınır;
 * böylece eklenti güncellemelerinde yeni metinler hiçbir zaman boş kalmaz.
 * Yer tutucular: {0}, {1}, ... — & renk kodları desteklenir.
 */
public final class L {

    private static final EnumMap<Dil, YamlConfiguration> DOSYALAR = new EnumMap<>(Dil.class);

    private L() {}

    public static void yukle(JavaPlugin plugin) {
        File klasor = new File(plugin.getDataFolder(), "lang");
        if (!klasor.exists() && !klasor.mkdirs()) {
            plugin.getLogger().warning("lang klasörü oluşturulamadı.");
        }
        for (Dil d : Dil.values()) {
            String yol = "lang/" + d.kod() + ".yml";
            File dosya = new File(plugin.getDataFolder(), yol);
            if (!dosya.exists()) plugin.saveResource(yol, false);
            YamlConfiguration y = YamlConfiguration.loadConfiguration(dosya);
            try (InputStream in = plugin.getResource(yol)) {
                if (in != null) {
                    y.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, yol + " varsayılanları okunamadı.", e);
            }
            DOSYALAR.put(d, y);
        }
    }

    /** Tek satırlık çevrilmiş ve renklendirilmiş metin. */
    public static String t(Dil dil, String anahtar, Object... args) {
        return renk(doldur(ham(dil, anahtar), args));
    }

    /** Çok satırlı metin (YAML listesi veya \n ile ayrılmış). */
    public static List<String> l(Dil dil, String anahtar, Object... args) {
        YamlConfiguration y = DOSYALAR.get(dil);
        List<String> satirlar = null;
        if (y != null && y.isList(anahtar)) satirlar = y.getStringList(anahtar);
        if (satirlar == null) {
            YamlConfiguration diger = DOSYALAR.get(dil == Dil.TR ? Dil.EN : Dil.TR);
            if ((y == null || !y.contains(anahtar)) && diger != null && diger.isList(anahtar)) {
                satirlar = diger.getStringList(anahtar);
            }
        }
        if (satirlar == null) {
            String tek = ham(dil, anahtar);
            satirlar = new ArrayList<>();
            Collections.addAll(satirlar, tek.split("\n", -1));
        }
        List<String> sonuc = new ArrayList<>(satirlar.size());
        for (String s : satirlar) sonuc.add(renk(doldur(s, args)));
        return sonuc;
    }

    private static String ham(Dil dil, String anahtar) {
        YamlConfiguration y = DOSYALAR.get(dil);
        String s = y == null ? null : y.getString(anahtar);
        if (s == null) {
            YamlConfiguration diger = DOSYALAR.get(dil == Dil.TR ? Dil.EN : Dil.TR);
            if (diger != null) s = diger.getString(anahtar);
        }
        return s != null ? s : anahtar;
    }

    private static String doldur(String s, Object... args) {
        if (args == null || args.length == 0 || s.indexOf('{') < 0) return s;
        String r = s;
        for (int i = 0; i < args.length; i++) {
            r = r.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return r;
    }

    private static String renk(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
