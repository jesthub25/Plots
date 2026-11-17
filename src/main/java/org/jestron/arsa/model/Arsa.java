package org.jestron.arsa.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Koordinat tabanlı tek bir arsa. Kira bilgisi arsanın kendisinde tutulur.
 * Tüm erişimler ana thread üzerinden yapılır.
 */
public final class Arsa {

    private final int id;
    private final String dunya;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    private UUID sahip;
    private long kiraBitis;      // epoch ms, 0 = kiralık değil
    private double saniyeUcreti; // kiralama anındaki saniye ücreti (iade hesabı için)

    private final LinkedHashMap<UUID, ArsaUye> uyeler = new LinkedHashMap<>();
    private final EnumSet<Izin> ziyaretci = EnumSet.noneOf(Izin.class);

    public Arsa(int id, String dunya, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.id = id;
        this.dunya = dunya;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    // ── Konum ───────────────────────────────────────────────────────────────

    public boolean icinde(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean kesisir(String digerDunya, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (!dunya.equals(digerDunya)) return false;
        int aMinX = Math.min(x1, x2), aMaxX = Math.max(x1, x2);
        int aMinY = Math.min(y1, y2), aMaxY = Math.max(y1, y2);
        int aMinZ = Math.min(z1, z2), aMaxZ = Math.max(z1, z2);
        return aMinX <= maxX && aMaxX >= minX
                && aMinY <= maxY && aMaxY >= minY
                && aMinZ <= maxZ && aMaxZ >= minZ;
    }

    public int genislikX() { return maxX - minX + 1; }
    public int genislikZ() { return maxZ - minZ + 1; }
    public int yukseklik() { return maxY - minY + 1; }

    public String konumYazisi() {
        return minX + ", " + minY + ", " + minZ + " → " + maxX + ", " + maxY + ", " + maxZ;
    }

    // ── Sahiplik / kira ─────────────────────────────────────────────────────

    public boolean sahibiVar() { return sahip != null; }

    public boolean sahipMi(UUID uuid) { return sahip != null && sahip.equals(uuid); }

    public boolean kiralik() { return sahip != null && kiraBitis > 0; }

    public long kalanMs() {
        return kiraBitis <= 0 ? 0 : Math.max(0, kiraBitis - System.currentTimeMillis());
    }

    public void sahipAyarla(UUID sahip) { this.sahip = sahip; }

    public void kiraAyarla(long bitis, double saniyeUcreti) {
        this.kiraBitis = bitis;
        this.saniyeUcreti = saniyeUcreti;
    }

    public void kiraBitisAyarla(long bitis) { this.kiraBitis = bitis; }

    /** Arsayı tamamen sahipsiz ve temiz hale getirir. */
    public void sahipsizYap() {
        sahip = null;
        kiraBitis = 0;
        saniyeUcreti = 0;
        uyeler.clear();
        ziyaretci.clear();
    }

    // ── Üyeler ──────────────────────────────────────────────────────────────

    public ArsaUye uye(UUID uuid) { return uyeler.get(uuid); }

    public boolean uyeMi(UUID uuid) { return uyeler.containsKey(uuid); }

    public ArsaUye uyeEkle(UUID uuid, Rutbe rutbe) {
        ArsaUye mevcut = uyeler.get(uuid);
        if (mevcut != null) return mevcut;
        ArsaUye yeni = new ArsaUye(rutbe);
        uyeler.put(uuid, yeni);
        return yeni;
    }

    public void uyeKoy(UUID uuid, ArsaUye uye) { uyeler.put(uuid, uye); }

    public boolean uyeCikar(UUID uuid) { return uyeler.remove(uuid) != null; }

    public Map<UUID, ArsaUye> uyeler() { return Collections.unmodifiableMap(uyeler); }

    /** Üyenin etkin izni: MOD → her şey; özel ayar → o; yoksa rütbe varsayılanı veya ziyaretçi izni. */
    public boolean uyeIzni(ArsaUye uye, Izin izin) {
        if (uye.rutbe() == Rutbe.MOD) return true;
        Boolean ozel = uye.ozel(izin);
        if (ozel != null) return ozel;
        return uye.rutbe().varsayilanIzin(izin) || ziyaretci.contains(izin);
    }

    // ── Ziyaretçiler ────────────────────────────────────────────────────────

    public boolean ziyaretciIzni(Izin izin) { return ziyaretci.contains(izin); }

    public void ziyaretciAyarla(Izin izin, boolean deger) {
        if (!Izin.ZIYARETCI_IZINLERI.contains(izin)) return;
        if (deger) ziyaretci.add(izin); else ziyaretci.remove(izin);
    }

    public Set<Izin> ziyaretciIzinleri() { return Collections.unmodifiableSet(ziyaretci); }

    // ── Getter ──────────────────────────────────────────────────────────────

    public int id() { return id; }
    public String dunya() { return dunya; }
    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxY() { return maxY; }
    public int maxZ() { return maxZ; }
    public UUID sahip() { return sahip; }
    public long kiraBitis() { return kiraBitis; }
    public double saniyeUcreti() { return saniyeUcreti; }
}
