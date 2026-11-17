package org.jestron.gelismisArsa.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Süre ayrıştırma ve biçimlendirme. */
public final class Sure {

    private static final Locale TR = Locale.forLanguageTag("tr-TR");

    private Sure() {}

    /**
     * Süre metnini saniyeye çevirir.
     * Birimler: m/dk = dakika, h/sa = saat, d/g = gün, w/hf = hafta.
     * Birim yazılmazsa {@code varsayilanBirim} kullanılır ("m", "h", "d", "w").
     *
     * @return saniye, geçersizse -1
     */
    public static long saniyeyeCevir(String girdi, String varsayilanBirim) {
        if (girdi == null) return -1;
        String s = girdi.trim().toLowerCase(Locale.ROOT);
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        if (i == 0 || i > 6) return -1;
        long deger = Long.parseLong(s.substring(0, i));
        if (deger <= 0) return -1;
        String birim = s.substring(i).trim();
        if (birim.isEmpty()) birim = varsayilanBirim;
        switch (birim) {
            case "m": case "dk": case "dakika": return deger * 60L;
            case "h": case "sa": case "saat": return deger * 3600L;
            case "d": case "g": case "gun": case "gün": return deger * 86400L;
            case "w": case "hf": case "hafta": return deger * 604800L;
            default: return -1;
        }
    }

    /** Milisaniyeyi "3g 4sa" / "5sa 12dk" biçimine çevirir. */
    public static String yaz(long ms) {
        if (ms <= 0) return "süre doldu";
        long toplamDk = ms / 60_000L;
        long gun = toplamDk / 1440;
        long saat = (toplamDk % 1440) / 60;
        long dakika = toplamDk % 60;
        StringBuilder sb = new StringBuilder();
        if (gun > 0) sb.append(gun).append("g ");
        if (saat > 0) sb.append(saat).append("sa ");
        if (gun == 0 && dakika > 0) sb.append(dakika).append("dk");
        String r = sb.toString().trim();
        return r.isEmpty() ? "1dk'dan az" : r;
    }

    /** Saniyeyi okunur süreye çevirir (komut mesajları için). */
    public static String saniyeYaz(long saniye) {
        return yaz(saniye * 1000L);
    }

    public static String tarih(long epochMs) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", TR).format(new Date(epochMs));
    }
}
