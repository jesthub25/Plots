package org.jestron.gelismisArsa.model;

import org.bukkit.Material;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Arsa üye rütbeleri (düşükten yükseğe).
 * MOD her zaman tüm izinlere sahiptir ve arsa sahibi gibi üyeleri yönetebilir;
 * ancak arsa sahibine ve diğer MOD'lara dokunamaz, kimseye MOD rütbesi veremez.
 */
public enum Rutbe {

    UYE(1, "uye", "§7Üye", Material.LEATHER_HELMET),
    GUVENILIR_UYE(2, "guvenilir", "§aGüvenilir Üye", Material.IRON_HELMET),
    YETKILI(3, "yetkili", "§bYetkili", Material.DIAMOND_HELMET),
    MOD(4, "mod", "§6Mod", Material.GOLDEN_HELMET);

    private final int seviye;
    private final String anahtar;
    private final String gorunenAd;
    private final Material ikon;
    private final EnumSet<Izin> varsayilan = EnumSet.noneOf(Izin.class);

    Rutbe(int seviye, String anahtar, String gorunenAd, Material ikon) {
        this.seviye = seviye;
        this.anahtar = anahtar;
        this.gorunenAd = gorunenAd;
        this.ikon = ikon;
    }

    static {
        varsayilanlariSifirla();
    }

    /** Kod içi varsayılanlar (config.yml ile değiştirilebilir). */
    public static void varsayilanlariSifirla() {
        UYE.varsayilan.clear();
        GUVENILIR_UYE.varsayilan.clear();
        GUVENILIR_UYE.varsayilan.addAll(EnumSet.of(Izin.BLOK_KOY, Izin.BLOK_KIR, Izin.SANDIK,
                Izin.KAPI, Izin.MEKANIZMA, Izin.ITEM_TOPLA, Izin.ITEM_AT));
        YETKILI.varsayilan.clear();
        YETKILI.varsayilan.addAll(EnumSet.of(Izin.BLOK_KOY, Izin.BLOK_KIR, Izin.SANDIK,
                Izin.KAPI, Izin.MEKANIZMA, Izin.ETKILESIM, Izin.ESYA_KULLAN, Izin.VARLIK,
                Izin.ITEM_TOPLA, Izin.ITEM_AT));
        MOD.varsayilan.clear();
        MOD.varsayilan.addAll(EnumSet.allOf(Izin.class));
    }

    public void varsayilanAyarla(Collection<Izin> izinler) {
        if (this == MOD) return; // MOD her zaman tam yetkilidir
        varsayilan.clear();
        varsayilan.addAll(izinler);
    }

    public boolean varsayilanIzin(Izin izin) {
        return this == MOD || varsayilan.contains(izin);
    }

    public Set<Izin> varsayilanlar() {
        return Collections.unmodifiableSet(varsayilan);
    }

    public int seviye() { return seviye; }
    public String anahtar() { return anahtar; }
    public String gorunenAd() { return gorunenAd; }
    public Material ikon() { return ikon; }

    public Rutbe sonraki() {
        return this == MOD ? MOD : values()[ordinal() + 1];
    }

    public Rutbe onceki() {
        return this == UYE ? UYE : values()[ordinal() - 1];
    }

    public static Rutbe bul(String girdi) {
        if (girdi == null) return null;
        String g = Izin.normalize(girdi);
        for (Rutbe r : values()) {
            if (r.anahtar.equals(g) || Izin.normalize(r.name()).equals(g)) return r;
        }
        if (g.equals("guveniliruye") || g.equals("guvenli")) return GUVENILIR_UYE;
        if (g.equals("moderator")) return MOD;
        return null;
    }
}
