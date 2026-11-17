package org.jestron.arsa.dil;

import java.util.Locale;

/** Desteklenen diller. /arsa → TR, /plot → EN. */
public enum Dil {
    TR("tr", "arsa"),
    EN("en", "plot");

    private final String kod;
    private final String komut;

    Dil(String kod, String komut) {
        this.kod = kod;
        this.komut = komut;
    }

    public String kod() { return kod; }

    /** Bu dilin ana komutu (yardım ve ipucu metinlerinde kullanılır). */
    public String komut() { return komut; }

    public static Dil bul(String s) {
        if (s == null) return null;
        String k = s.trim().toLowerCase(Locale.ROOT);
        if (k.startsWith("tr")) return TR;
        if (k.startsWith("en")) return EN;
        return null;
    }

    /** Komut adına göre dil: "plot" → EN, diğerleri → TR. */
    public static Dil komuttan(String komutAdi) {
        return komutAdi != null && komutAdi.equalsIgnoreCase(EN.komut) ? EN : TR;
    }
}
