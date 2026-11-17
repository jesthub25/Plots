package org.jestron.gelismisArsa.model;

import org.bukkit.Material;
import org.jestron.gelismisArsa.dil.Dil;
import org.jestron.gelismisArsa.dil.L;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Arsa üye rütbeleri (düşükten yükseğe).
 * MOD her zaman tüm izinlere sahiptir ve arsa sahibi gibi üyeleri yönetebilir;
 * ancak arsa sahibine ve diğer MOD'lara dokunamaz, kimseye MOD rütbesi veremez.
 * Görünen ad dil dosyasındadır: rutbe.&lt;ENUM&gt;
 */
public enum Rutbe {

    UYE(1, "uye", "member", Material.LEATHER_HELMET),
    GUVENILIR_UYE(2, "guvenilir", "trusted", Material.IRON_HELMET),
    YETKILI(3, "yetkili", "officer", Material.DIAMOND_HELMET),
    MOD(4, "mod", "mod", Material.GOLDEN_HELMET);

    private final int seviye;
    private final String anahtarTr;
    private final String anahtarEn;
    private final Material ikon;
    private final EnumSet<Izin> varsayilan = EnumSet.noneOf(Izin.class);

    Rutbe(int seviye, String anahtarTr, String anahtarEn, Material ikon) {
        this.seviye = seviye;
        this.anahtarTr = anahtarTr;
        this.anahtarEn = anahtarEn;
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
        if (this == MOD) return;
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
    public String anahtar(Dil dil) { return dil == Dil.EN ? anahtarEn : anahtarTr; }
    public String ad(Dil dil) { return L.t(dil, "rutbe." + name()); }
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
            if (r.anahtarTr.equals(g) || r.anahtarEn.equals(g) || Izin.normalize(r.name()).equals(g)) return r;
        }
        switch (g) {
            case "guveniliruye": case "guvenli": return GUVENILIR_UYE;
            case "moderator": return MOD;
            case "manager": return YETKILI;
            default: return null;
        }
    }
}
