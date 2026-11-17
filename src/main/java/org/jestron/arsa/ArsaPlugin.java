package org.jestron.arsa;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jestron.arsa.dil.DilYoneticisi;
import org.jestron.arsa.dil.L;
import org.jestron.arsa.entegrasyon.Ekonomi;
import org.jestron.arsa.entegrasyon.WorldEditKopru;
import org.jestron.arsa.gui.MenuListener;
import org.jestron.arsa.gui.SohbetGirdisi;
import org.jestron.arsa.kira.KiraServisi;
import org.jestron.arsa.komut.ArsaKomut;
import org.jestron.arsa.koruma.CevreListener;
import org.jestron.arsa.koruma.KorumaListener;
import org.jestron.arsa.util.Mesaj;
import org.jestron.arsa.veri.ArsaManager;
import org.jestron.arsa.veri.VeriDeposu;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;

/**
 * Arsa eklentisinin ana sınıfı.
 * Vault ve WorldEdit isteğe bağlıdır: yoksa ilgili özellikler (kiralama ödemesi, şablon/sıfırlama,
 * seçimden oluşturma) devre dışı kalır, koruma ve GUI her zaman çalışır.
 */
public final class ArsaPlugin extends JavaPlugin implements Listener {

    private final Ayarlar ayarlar = new Ayarlar();
    private final ArsaManager arsalar = new ArsaManager();
    private final Yetki yetki = new Yetki();
    private final DilYoneticisi diller = new DilYoneticisi();
    private VeriDeposu depo;
    private UyeServisi uyeServisi;
    private KiraServisi kira;
    private SohbetGirdisi sohbet;
    private Ekonomi ekonomi;
    private WorldEditKopru worldEdit;

    @Override
    public void onEnable() {
        eskiKlasoruTasi();
        if (getServer().getPluginManager().getPlugin("GelismisArsa") != null) {
            getLogger().severe("Eski 'GelismisArsa' eklentisi de yuklu! Iki eklenti ayni /arsa komutunu kullanir. "
                    + "plugins klasorundeki eski GelismisArsa jar dosyasini silip sunucuyu yeniden baslat.");
        }
        saveDefaultConfig();

        depo = new VeriDeposu(this, arsalar, diller);
        depo.yukle();               // eski sürüm verisi varsa burada taşınır (config.yml yenilenir)
        diller.degisinceCalistir(depo::kaydetPlanla);
        ayarlariYenile();

        PluginManager pm = getServer().getPluginManager();
        if (eklentiVar(pm, "Vault")) {
            ekonomi = new Ekonomi();
            getLogger().info("Vault bulundu" + (ekonomi.hazir() ? ", ekonomi bağlandı." : "; ekonomi sağlayıcısı ilk kullanımda aranacak."));
        } else {
            getLogger().warning("Vault bulunamadı: kiralama ödemeleri devre dışı.");
        }
        if (eklentiVar(pm, "WorldEdit") || eklentiVar(pm, "FastAsyncWorldEdit")) {
            worldEdit = new WorldEditKopru(getLogger());
            getLogger().info("WorldEdit bulundu: seçimden arsa oluşturma ve şablon sıfırlama aktif.");
        } else {
            getLogger().warning("WorldEdit bulunamadı: şablon/sıfırlama ve seçimden oluşturma devre dışı.");
        }

        uyeServisi = new UyeServisi(this);
        kira = new KiraServisi(this, ekonomi);
        sohbet = new SohbetGirdisi(this);

        pm.registerEvents(new KorumaListener(arsalar, yetki), this);
        pm.registerEvents(new CevreListener(arsalar, yetki, () -> ayarlar), this);
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(sohbet, this);
        pm.registerEvents(kira, this);
        pm.registerEvents(this, this);

        ArsaKomut k = new ArsaKomut(this);
        for (String ad : new String[]{"arsa", "plot"}) {
            PluginCommand komut = getCommand(ad);
            if (komut != null) {
                komut.setExecutor(k);
                komut.setTabCompleter(k);
            } else {
                getLogger().severe("plugin.yml icinde '" + ad + "' komutu bulunamadi!");
            }
        }

        kira.baslat();
        getLogger().info("Arsa " + getDescription().getVersion() + " etkin. " + arsalar.tumu().size() + " arsa.");
    }

    @Override
    public void onDisable() {
        MenuListener.tumunuKapat();
        if (depo != null) depo.kapat();
        getLogger().info("Tüm arsa verileri kaydedildi.");
    }

    /**
     * Eklentinin adı "GelismisArsa"dan "Arsa"ya değişti; Bukkit veri klasörü de değişir.
     * plugins/Arsa henüz boşsa plugins/GelismisArsa içeriği (lang hariç, yeni metinler gelsin diye)
     * buraya kopyalanır. Eski klasör silinmez, yedek olarak kalır.
     */
    private void eskiKlasoruTasi() {
        File yeni = getDataFolder();
        File eski = new File(yeni.getParentFile(), "GelismisArsa");
        if (!eski.isDirectory()) return;
        if (new File(yeni, "arsalar.yml").exists() || new File(yeni, "config.yml").exists()) return;
        Path kaynak = eski.toPath();
        Path hedef = yeni.toPath();
        try {
            Files.walkFileTree(kaynak, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path goreli = kaynak.relativize(dir);
                    if (goreli.toString().equals("lang")) return FileVisitResult.SKIP_SUBTREE;
                    Files.createDirectories(hedef.resolve(goreli.toString()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.getFileName().toString().endsWith(".tmp")) {
                        Files.copy(file, hedef.resolve(kaynak.relativize(file).toString()),
                                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            getLogger().info("plugins/GelismisArsa verileri plugins/Arsa klasorune kopyalandi. "
                    + "Her sey dogruysa eski GelismisArsa klasorunu silebilirsin.");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "plugins/GelismisArsa verileri kopyalanamadi! Dosyalari elle plugins/Arsa icine tasi.", e);
        }
    }

    public void ayarlariYenile() {
        reloadConfig();
        ayarlar.yukle(getConfig(), getLogger());
        L.yukle(this);
        diller.varsayilanAyarla(ayarlar.varsayilanDil);
        Mesaj.kur(diller, ayarlar.mesajBeklemeMs);
    }

    private static boolean eklentiVar(PluginManager pm, String ad) {
        Plugin p = pm.getPlugin(ad);
        return p != null && p.isEnabled();
    }

    @EventHandler
    public void cikis(PlayerQuitEvent e) {
        Mesaj.temizle(e.getPlayer().getUniqueId());
        yetki.cikis(e.getPlayer().getUniqueId());
    }

    // ── Erişim ──────────────────────────────────────────────────────────────

    public Ayarlar ayarlar() { return ayarlar; }
    public ArsaManager arsalar() { return arsalar; }
    public Yetki yetki() { return yetki; }
    public DilYoneticisi diller() { return diller; }
    public VeriDeposu depo() { return depo; }
    public UyeServisi uyeServisi() { return uyeServisi; }
    public KiraServisi kira() { return kira; }
    public SohbetGirdisi sohbet() { return sohbet; }
    public Ekonomi ekonomi() { return ekonomi; }
    public WorldEditKopru worldEdit() { return worldEdit; }
}
