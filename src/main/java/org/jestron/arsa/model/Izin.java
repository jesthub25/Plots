package org.jestron.arsa.model;

import org.bukkit.Material;
import org.jestron.arsa.dil.Dil;
import org.jestron.arsa.dil.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Arsa üzerindeki her bir ayrı işlem izni. Sıralama GUI sıralamasıdır.
 * Görünen ad ve açıklama dil dosyasındadır: izin.&lt;ENUM&gt;.ad / izin.&lt;ENUM&gt;.aciklama
 */
public enum Izin {

    BLOK_KOY("blokkoy", "place", Material.BRICKS),
    BLOK_KIR("blokkir", "break", Material.CRACKED_STONE_BRICKS),
    SANDIK("sandik", "container", Material.CHEST),
    KAPI("kapi", "door", Material.OAK_DOOR),
    MEKANIZMA("mekanizma", "redstone", Material.LEVER),
    ETKILESIM("etkilesim", "interact", Material.CRAFTING_TABLE),
    ESYA_KULLAN("esyakullan", "useitem", Material.FLINT_AND_STEEL),
    VARLIK("varlik", "entity", Material.ARMOR_STAND),
    ITEM_TOPLA("itemtopla", "pickup", Material.HOPPER),
    ITEM_AT("itemat", "drop", Material.DROPPER),
    DAVET("davet", "invite", Material.WRITABLE_BOOK);

    /** Ziyaretçilere (üye olmayanlara) verilebilecek izinler. */
    public static final Set<Izin> ZIYARETCI_IZINLERI;

    static {
        EnumSet<Izin> z = EnumSet.allOf(Izin.class);
        z.remove(DAVET);
        ZIYARETCI_IZINLERI = Collections.unmodifiableSet(z);
    }

    private final String anahtarTr;
    private final String anahtarEn;
    private final Material ikon;

    Izin(String anahtarTr, String anahtarEn, Material ikon) {
        this.anahtarTr = anahtarTr;
        this.anahtarEn = anahtarEn;
        this.ikon = ikon;
    }

    public String anahtar(Dil dil) { return dil == Dil.EN ? anahtarEn : anahtarTr; }
    public Material ikon() { return ikon; }
    public String ad(Dil dil) { return L.t(dil, "izin." + name() + ".ad"); }
    public List<String> aciklama(Dil dil) { return L.l(dil, "izin." + name() + ".aciklama"); }

    /** Türkçe/İngilizce komut anahtarı veya enum adıyla izni bulur. */
    public static Izin bul(String girdi) {
        if (girdi == null) return null;
        String g = normalize(girdi);
        for (Izin i : values()) {
            if (i.anahtarTr.equals(g) || i.anahtarEn.equals(g) || normalize(i.name()).equals(g)) return i;
        }
        return null;
    }

    /** Eski sürümlerdeki izin adlarını yeni izinlere çevirir. */
    public static List<Izin> eskiAddanCevir(String ad) {
        List<Izin> sonuc = new ArrayList<>(2);
        if (ad == null) return sonuc;
        String n = ad.trim().toUpperCase(Locale.ROOT);
        switch (n) {
            case "SANDIK_AC": sonuc.add(SANDIK); break;
            case "BLOGU_AKTIF_ET": sonuc.add(KAPI); sonuc.add(MEKANIZMA); break;
            case "ITEM_KULLAN": sonuc.add(ESYA_KULLAN); sonuc.add(ETKILESIM); break;
            case "DAVET_ET": sonuc.add(DAVET); break;
            default:
                Izin i = bul(n);
                if (i != null) sonuc.add(i);
        }
        return sonuc;
    }

    public static List<String> anahtarlar(Dil dil) {
        List<String> l = new ArrayList<>(values().length);
        for (Izin i : values()) l.add(i.anahtar(dil));
        return l;
    }

    public static String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT)
                .replace("i̇", "i").replace("ı", "i").replace("ş", "s").replace("ğ", "g")
                .replace("ü", "u").replace("ö", "o").replace("ç", "c")
                .replace("_", "").replace("-", "").replace(" ", "");
    }
}
