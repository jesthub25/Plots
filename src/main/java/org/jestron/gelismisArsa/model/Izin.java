package org.jestron.gelismisArsa.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Arsa üzerindeki her bir ayrı işlem izni.
 * Sıralama GUI'deki sıralamadır.
 */
public enum Izin {

    BLOK_KOY("blokkoy", "Blok Koyma", Material.BRICKS,
            "Blok koyabilir, tabela düzenleyebilir,",
            "kova ile sıvı dökebilir, dekor asabilir."),
    BLOK_KIR("blokkir", "Blok Kırma", Material.CRACKED_STONE_BRICKS,
            "Blok kırabilir, kova ile sıvı alabilir,",
            "tarla ezebilir, ateş söndürebilir."),
    SANDIK("sandik", "Sandık / Depo Açma", Material.CHEST,
            "Sandık, varil, shulker, fırın, huni,",
            "dağıtıcı, demleme standı vb. açabilir."),
    KAPI("kapi", "Kapı / Tuzak Kapı / Çit Kapısı", Material.OAK_DOOR,
            "Her tür kapı, tuzak kapı ve çit",
            "kapısını açıp kapatabilir."),
    MEKANIZMA("mekanizma", "Düğme / Şalter / Plaka", Material.LEVER,
            "Düğme, şalter, basınç plakası, tel,",
            "tekrarlayıcı, nota bloğu, zil vb."),
    ETKILESIM("etkilesim", "İstasyon / Blok Etkileşimi", Material.CRAFTING_TABLE,
            "Çalışma masası, örs, büyü masası,",
            "yatak, saksı, kazan, kamp ateşi vb."),
    ESYA_KULLAN("esyakullan", "Eşya Kullanma", Material.FLINT_AND_STEEL,
            "Çakmak, kemik tozu, balta/çapa/kürek,",
            "spawn yumurtası, boya, fırça vb."),
    VARLIK("varlik", "Hayvan / Varlık Etkileşimi", Material.ARMOR_STAND,
            "Hayvan, köylü, zırh askısı, eşya",
            "çerçevesi, tablo ve araçlar."),
    ITEM_TOPLA("itemtopla", "Yerden Eşya Alma", Material.HOPPER,
            "Arsa içinde yere düşmüş",
            "eşyaları toplayabilir."),
    ITEM_AT("itemat", "Yere Eşya Atma", Material.DROPPER,
            "Arsa içindeyken yere",
            "eşya atabilir."),
    DAVET("davet", "Üye Davet Etme", Material.WRITABLE_BOOK,
            "Arsaya yeni üye davet edebilir.",
            "Davet edilen kişi Üye rütbesiyle katılır.");

    /** Ziyaretçilere (üye olmayanlara) verilebilecek izinler. */
    public static final Set<Izin> ZIYARETCI_IZINLERI;

    static {
        EnumSet<Izin> z = EnumSet.allOf(Izin.class);
        z.remove(DAVET);
        ZIYARETCI_IZINLERI = Collections.unmodifiableSet(z);
    }

    private final String anahtar;
    private final String ad;
    private final Material ikon;
    private final List<String> aciklama;

    Izin(String anahtar, String ad, Material ikon, String... aciklama) {
        this.anahtar = anahtar;
        this.ad = ad;
        this.ikon = ikon;
        this.aciklama = List.of(aciklama);
    }

    public String anahtar() { return anahtar; }
    public String ad() { return ad; }
    public Material ikon() { return ikon; }
    public List<String> aciklama() { return aciklama; }

    /** Komut anahtarı, enum adı veya eski sürüm adıyla izni bulur. */
    public static Izin bul(String girdi) {
        if (girdi == null) return null;
        String g = normalize(girdi);
        for (Izin i : values()) {
            if (i.anahtar.equals(g) || normalize(i.name()).equals(g)) return i;
        }
        return null;
    }

    /**
     * Eski sürümlerdeki izin adlarını yeni izinlere çevirir.
     * Bilinmeyen adlar için boş liste döner.
     */
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

    public static List<String> anahtarlar() {
        List<String> l = new ArrayList<>(values().length);
        for (Izin i : values()) l.add(i.anahtar);
        return l;
    }

    static String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT)
                .replace("i̇", "i").replace("ı", "i").replace("ş", "s").replace("ğ", "g")
                .replace("ü", "u").replace("ö", "o").replace("ç", "c")
                .replace("_", "").replace("-", "").replace(" ", "");
    }
}
