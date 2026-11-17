package org.jestron.gelismisArsa.veri;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jestron.gelismisArsa.GelismisArsa;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.ArsaUye;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.model.Rutbe;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * arsalar.yml okuma/yazma.
 * Kayıt: ana thread'de anlık görüntü (YAML metni) alınır, tek iş parçacıklı
 * yazıcıda geçici dosyaya yazılıp atomik olarak yerine taşınır.
 * Böylece ana thread hiçbir zaman disk beklemez ve yarım dosya oluşmaz.
 */
public final class VeriDeposu {

    private static final int SURUM = 3;

    private final GelismisArsa plugin;
    private final ArsaManager arsalar;
    private final File dosya;
    private final ExecutorService yazici;

    private boolean kirli;
    private boolean planli;

    public VeriDeposu(GelismisArsa plugin, ArsaManager arsalar) {
        this.plugin = plugin;
        this.arsalar = arsalar;
        this.dosya = new File(plugin.getDataFolder(), "arsalar.yml");
        this.yazici = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GelismisArsa-Kayit");
            t.setDaemon(true);
            return t;
        });
    }

    // ── Yükleme ─────────────────────────────────────────────────────────────

    public void yukle() {
        if (!dosya.exists()) {
            if (eskiVeriVarMi()) {
                eskiVeriyiTasi();
            } else {
                arsalar.yukle(new ArrayList<>(), 1);
            }
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(dosya);
        List<Arsa> liste = new ArrayList<>();
        ConfigurationSection kok = y.getConfigurationSection("arsalar");
        if (kok != null) {
            for (String anahtar : kok.getKeys(false)) {
                ConfigurationSection s = kok.getConfigurationSection(anahtar);
                if (s == null) continue;
                try {
                    liste.add(arsaOku(Integer.parseInt(anahtar), s));
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "arsalar.yml içindeki #" + anahtar + " okunamadı, atlandı.", ex);
                }
            }
        }
        arsalar.yukle(liste, y.getInt("sonraki-id", 1));
        plugin.getLogger().info(liste.size() + " arsa yüklendi.");
    }

    private Arsa arsaOku(int id, ConfigurationSection s) {
        Arsa a = new Arsa(id, s.getString("dunya"),
                s.getInt("minX"), s.getInt("minY"), s.getInt("minZ"),
                s.getInt("maxX"), s.getInt("maxY"), s.getInt("maxZ"));
        String sahip = s.getString("sahip");
        if (sahip != null && !sahip.isEmpty()) {
            a.sahipAyarla(UUID.fromString(sahip));
            a.kiraAyarla(s.getLong("kira-bitis", 0L), s.getDouble("saniye-ucreti", 0D));
        }
        for (Izin i : izinleriOku(s.getStringList("ziyaretci"))) a.ziyaretciAyarla(i, true);
        ConfigurationSection uyeler = s.getConfigurationSection("uyeler");
        if (uyeler != null) {
            for (String uuidStr : uyeler.getKeys(false)) {
                ConfigurationSection us = uyeler.getConfigurationSection(uuidStr);
                if (us == null) continue;
                try {
                    Rutbe r = Rutbe.bul(us.getString("rutbe", "UYE"));
                    ArsaUye uye = new ArsaUye(r == null ? Rutbe.UYE : r);
                    uye.ozelleriYukle(izinleriOku(us.getStringList("acik")), izinleriOku(us.getStringList("kapali")));
                    a.uyeKoy(UUID.fromString(uuidStr), uye);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("#" + id + " arsasında geçersiz üye kaydı atlandı: " + uuidStr);
                }
            }
        }
        return a;
    }

    private static EnumSet<Izin> izinleriOku(List<String> adlar) {
        EnumSet<Izin> s = EnumSet.noneOf(Izin.class);
        for (String ad : adlar) s.addAll(Izin.eskiAddanCevir(ad));
        return s;
    }

    // ── Kaydetme ────────────────────────────────────────────────────────────

    /** Değişiklik sonrası çağrılır; 2 saniye içinde toplu olarak arka planda kaydeder. */
    public void kaydetPlanla() {
        kirli = true;
        if (planli || !plugin.isEnabled()) return;
        planli = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            planli = false;
            if (kirli) {
                kirli = false;
                String veri = anlikGoruntu();
                yazici.execute(() -> yaz(veri));
            }
        }, 40L);
    }

    /** Kapanışta: bekleyen yazımları bitirir, son hali senkron yazar. */
    public void kapat() {
        yazici.shutdown();
        try {
            if (!yazici.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Kayıt iş parçacığı zamanında bitmedi.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        yaz(anlikGoruntu());
        kirli = false;
    }

    private String anlikGoruntu() {
        YamlConfiguration y = new YamlConfiguration();
        y.options().setHeader(List.of("GelismisArsa veri dosyasi. Sunucu acikken elle duzenlemeyin."));
        y.set("surum", SURUM);
        y.set("sonraki-id", arsalar.sonrakiId());
        for (Arsa a : arsalar.tumu()) {
            String k = "arsalar." + a.id() + ".";
            y.set(k + "dunya", a.dunya());
            y.set(k + "minX", a.minX());
            y.set(k + "minY", a.minY());
            y.set(k + "minZ", a.minZ());
            y.set(k + "maxX", a.maxX());
            y.set(k + "maxY", a.maxY());
            y.set(k + "maxZ", a.maxZ());
            if (a.sahibiVar()) {
                y.set(k + "sahip", a.sahip().toString());
                if (a.kiraBitis() > 0) {
                    y.set(k + "kira-bitis", a.kiraBitis());
                    y.set(k + "saniye-ucreti", a.saniyeUcreti());
                }
            }
            if (!a.ziyaretciIzinleri().isEmpty()) y.set(k + "ziyaretci", adlar(a.ziyaretciIzinleri()));
            for (Map.Entry<UUID, ArsaUye> e : a.uyeler().entrySet()) {
                String u = k + "uyeler." + e.getKey() + ".";
                ArsaUye uye = e.getValue();
                y.set(u + "rutbe", uye.rutbe().name());
                if (!uye.acikIzinler().isEmpty()) y.set(u + "acik", adlar(uye.acikIzinler()));
                if (!uye.kapaliIzinler().isEmpty()) y.set(u + "kapali", adlar(uye.kapaliIzinler()));
            }
        }
        return y.saveToString();
    }

    private static List<String> adlar(Iterable<Izin> izinler) {
        List<String> l = new ArrayList<>();
        for (Izin i : izinler) l.add(i.name());
        return l;
    }

    private synchronized void yaz(String veri) {
        try {
            File klasor = dosya.getParentFile();
            if (!klasor.exists() && !klasor.mkdirs()) {
                plugin.getLogger().severe("Veri klasörü oluşturulamadı: " + klasor);
                return;
            }
            File gecici = new File(klasor, dosya.getName() + ".tmp");
            Files.write(gecici.toPath(), veri.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(gecici.toPath(), dosya.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(gecici.toPath(), dosya.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "arsalar.yml kaydedilemedi!", ex);
        }
    }

    // ── Eski sürümden taşıma (config.yml + kiralama.yml) ────────────────────

    private boolean eskiVeriVarMi() {
        File cfg = new File(plugin.getDataFolder(), "config.yml");
        if (!cfg.exists()) return false;
        return YamlConfiguration.loadConfiguration(cfg).isList("arsalar");
    }

    private void eskiVeriyiTasi() {
        File cfgDosya = new File(plugin.getDataFolder(), "config.yml");
        File kiraDosya = new File(plugin.getDataFolder(), "kiralama.yml");
        FileConfiguration eski = YamlConfiguration.loadConfiguration(cfgDosya);
        YamlConfiguration kira = kiraDosya.exists() ? YamlConfiguration.loadConfiguration(kiraDosya) : new YamlConfiguration();
        ConfigurationSection kiraBolumu = kira.getConfigurationSection("kiralik-arsalar");
        double varsayilanSaniye = 10000D / 604800D;

        List<Arsa> liste = new ArrayList<>();
        int id = 1;
        for (Map<?, ?> m : eski.getMapList("arsalar")) {
            try {
                String dunya = String.valueOf(m.get("world"));
                int minX = sayi(m.get("minX")), minY = sayi(m.get("minY")), minZ = sayi(m.get("minZ"));
                int maxX = sayi(m.get("maxX")), maxY = sayi(m.get("maxY")), maxZ = sayi(m.get("maxZ"));
                Arsa a = new Arsa(id, dunya, minX, minY, minZ, maxX, maxY, maxZ);
                Object sahip = m.get("owner");
                if (sahip != null) a.sahipAyarla(UUID.fromString(String.valueOf(sahip)));

                Object uyeObj = m.get("members");
                if (uyeObj instanceof List<?> uyeListesi) {
                    for (Object o : uyeListesi) eskiUyeOku(a, String.valueOf(o));
                }

                String eskiId = dunya + ";" + a.minX() + ";" + a.minY() + ";" + a.minZ() + ";"
                        + a.maxX() + ";" + a.maxY() + ";" + a.maxZ();
                if (kiraBolumu != null && kiraBolumu.isConfigurationSection(eskiId)) {
                    ConfigurationSection k = kiraBolumu.getConfigurationSection(eskiId);
                    String kSahip = k.getString("owner");
                    if (kSahip != null) a.sahipAyarla(UUID.fromString(kSahip));
                    double odenen = k.getDouble("paid", 0D);
                    long sure = k.getLong("duration", 0L);
                    double oran = (odenen > 0 && sure > 0) ? odenen / sure : varsayilanSaniye;
                    a.kiraAyarla(k.getLong("expiration", 0L), oran);
                }
                liste.add(a);
                id++;
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Eski arsa kaydı taşınamadı, atlandı: " + m, ex);
            }
        }
        arsalar.yukle(liste, id);

        // Eski dosyaları yedekle, yeni (yorumlu) config.yml oluştur
        try {
            Files.move(cfgDosya.toPath(), new File(plugin.getDataFolder(), "config-eski.yml").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            if (kiraDosya.exists()) {
                Files.move(kiraDosya.toPath(), new File(plugin.getDataFolder(), "kiralama-eski.yml").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Eski dosyalar yedeklenemedi.", ex);
        }
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        yaz(anlikGoruntu());
        plugin.getLogger().info(liste.size() + " arsa eski sürümden arsalar.yml dosyasına taşındı. "
                + "Eski dosyalar config-eski.yml / kiralama-eski.yml olarak yedeklendi.");
    }

    private void eskiUyeOku(Arsa a, String kayit) {
        int ayrac = kayit.indexOf(':');
        if (ayrac <= 0) return;
        UUID uuid = UUID.fromString(kayit.substring(0, ayrac));
        String[] parcalar = kayit.substring(ayrac + 1).split("\\|");
        Rutbe r = Rutbe.bul(parcalar[0]);
        ArsaUye uye = new ArsaUye(r == null ? Rutbe.UYE : r);
        List<Izin> acik = new ArrayList<>();
        List<Izin> kapali = new ArrayList<>();
        for (int i = 1; i < parcalar.length; i++) {
            String p = parcalar[i];
            if (p.length() < 2) continue;
            List<Izin> cevrilen = Izin.eskiAddanCevir(p.substring(1));
            if (p.charAt(0) == '+') acik.addAll(cevrilen);
            else if (p.charAt(0) == '-') kapali.addAll(cevrilen);
        }
        uye.ozelleriYukle(acik, kapali);
        a.uyeKoy(uuid, uye);
    }

    private static int sayi(Object o) {
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }
}
