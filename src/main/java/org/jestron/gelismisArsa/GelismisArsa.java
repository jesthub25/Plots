package org.jestron.gelismisArsa;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jestron.gelismisArsa.dil.DilYoneticisi;
import org.jestron.gelismisArsa.dil.L;
import org.jestron.gelismisArsa.entegrasyon.Ekonomi;
import org.jestron.gelismisArsa.entegrasyon.WorldEditKopru;
import org.jestron.gelismisArsa.gui.MenuListener;
import org.jestron.gelismisArsa.gui.SohbetGirdisi;
import org.jestron.gelismisArsa.kira.KiraServisi;
import org.jestron.gelismisArsa.komut.ArsaKomut;
import org.jestron.gelismisArsa.koruma.CevreListener;
import org.jestron.gelismisArsa.koruma.KorumaListener;
import org.jestron.gelismisArsa.util.Mesaj;
import org.jestron.gelismisArsa.veri.ArsaManager;
import org.jestron.gelismisArsa.veri.VeriDeposu;

/**
 * GelismisArsa ana sınıfı.
 * Vault ve WorldEdit isteğe bağlıdır: yoksa ilgili özellikler (kiralama ödemesi, şablon/sıfırlama,
 * seçimden oluşturma) devre dışı kalır, koruma ve GUI her zaman çalışır.
 */
public final class GelismisArsa extends JavaPlugin implements Listener {

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
        getLogger().info("GelismisArsa " + getDescription().getVersion() + " etkin. " + arsalar.tumu().size() + " arsa.");
    }

    @Override
    public void onDisable() {
        MenuListener.tumunuKapat();
        if (depo != null) depo.kapat();
        getLogger().info("Tüm arsa verileri kaydedildi.");
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
