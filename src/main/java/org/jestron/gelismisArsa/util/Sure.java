package org.jestron.gelismisArsa.util;

import org.jestron.gelismisArsa.dil.Dil;
import org.jestron.gelismisArsa.dil.L;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Süre ayrıştırma ve biçimlendirme. */
public final class Sure {

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
            case "m": case "dk": case "min": case "dakika": return deger * 60L;
            case "h": case "sa": case "saat": case "hour": return deger * 3600L;
            case "d": case "g": case "gun": case "gün": case "day": return deger * 86400L;
            case "w": case "hf": case "hafta": case "week": return deger * 604800L;
            default: return -1;
        }
    }

    /** Milisaniyeyi "3g 4sa" / "3d 4h" biçimine çevirir. */
    public static String yaz(Dil dil, long ms) {
        if (ms <= 0) return L.t(dil, "sure.doldu");
        long toplamDk = ms / 60_000L;
        long gun = toplamDk / 1440;
        long saat = (toplamDk % 1440) / 60;
        long dakika = toplamDk % 60;
        StringBuilder sb = new StringBuilder();
        if (gun > 0) sb.append(gun).append(L.t(dil, "sure.gun")).append(' ');
        if (saat > 0) sb.append(saat).append(L.t(dil, "sure.saat")).append(' ');
        if (gun == 0 && dakika > 0) sb.append(dakika).append(L.t(dil, "sure.dakika"));
        String r = sb.toString().trim();
        return r.isEmpty() ? L.t(dil, "sure.az") : r;
    }

    public static String saniyeYaz(Dil dil, long saniye) {
        return yaz(dil, saniye * 1000L);
    }

    public static String tarih(long epochMs) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(epochMs));
    }
}
