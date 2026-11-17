package org.jestron.gelismisArsa.veri;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jestron.gelismisArsa.model.Arsa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Arsaların kaydı ve hızlı konum araması.
 * Arama: dünya → sınır kutusu kontrolü → chunk anahtarı (long) → küçük dizi.
 * Chunk yüklemez, Location nesnesi oluşturmaz.
 */
public final class ArsaManager {

    private final TreeMap<Integer, Arsa> idIle = new TreeMap<>();
    private final Map<String, DunyaIndeksi> indeks = new HashMap<>();
    private int sonrakiId = 1;

    // ── Arama ───────────────────────────────────────────────────────────────

    public Arsa bul(World dunya, int x, int y, int z) {
        if (dunya == null) return null;
        DunyaIndeksi d = indeks.get(dunya.getName());
        if (d == null || x < d.minX || x > d.maxX || z < d.minZ || z > d.maxZ) return null;
        Arsa[] adaylar = d.chunkler.get(anahtar(x >> 4, z >> 4));
        if (adaylar == null) return null;
        for (Arsa a : adaylar) {
            if (a.icinde(x, y, z)) return a;
        }
        return null;
    }

    public Arsa bul(Location konum) {
        if (konum == null) return null;
        return bul(konum.getWorld(), konum.getBlockX(), konum.getBlockY(), konum.getBlockZ());
    }

    public Arsa bul(Block blok) {
        if (blok == null) return null;
        return bul(blok.getWorld(), blok.getX(), blok.getY(), blok.getZ());
    }

    public Arsa getir(int id) {
        return idIle.get(id);
    }

    /** Arsanın hâlâ kayıtlı olup olmadığını (silinmemiş) kontrol eder. */
    public boolean kayitli(Arsa arsa) {
        return arsa != null && idIle.get(arsa.id()) == arsa;
    }

    public Collection<Arsa> tumu() {
        return Collections.unmodifiableCollection(idIle.values());
    }

    public List<Arsa> sahipArsalari(UUID sahip) {
        List<Arsa> l = new ArrayList<>(2);
        for (Arsa a : idIle.values()) {
            if (a.sahipMi(sahip)) l.add(a);
        }
        return l;
    }

    public Arsa cakisan(String dunya, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (Arsa a : idIle.values()) {
            if (a.kesisir(dunya, x1, y1, z1, x2, y2, z2)) return a;
        }
        return null;
    }

    // ── Değişiklik ──────────────────────────────────────────────────────────

    public Arsa olustur(String dunya, int x1, int y1, int z1, int x2, int y2, int z2) {
        Arsa a = new Arsa(sonrakiId++, dunya, x1, y1, z1, x2, y2, z2);
        idIle.put(a.id(), a);
        indeksiYenile();
        return a;
    }

    public boolean sil(Arsa arsa) {
        if (!kayitli(arsa)) return false;
        idIle.remove(arsa.id());
        indeksiYenile();
        return true;
    }

    /** Depodan yükleme: mevcut kayıtları değiştirir. */
    public void yukle(Collection<Arsa> arsalar, int sonrakiId) {
        idIle.clear();
        int enBuyuk = 0;
        for (Arsa a : arsalar) {
            idIle.put(a.id(), a);
            enBuyuk = Math.max(enBuyuk, a.id());
        }
        this.sonrakiId = Math.max(sonrakiId, enBuyuk + 1);
        indeksiYenile();
    }

    public int sonrakiId() {
        return sonrakiId;
    }

    // ── İndeks ──────────────────────────────────────────────────────────────

    private void indeksiYenile() {
        Map<String, Map<Long, List<Arsa>>> gecici = new HashMap<>();
        Map<String, DunyaIndeksi> yeni = new HashMap<>();
        for (Arsa a : idIle.values()) {
            DunyaIndeksi d = yeni.computeIfAbsent(a.dunya(), k -> new DunyaIndeksi());
            d.minX = Math.min(d.minX, a.minX());
            d.maxX = Math.max(d.maxX, a.maxX());
            d.minZ = Math.min(d.minZ, a.minZ());
            d.maxZ = Math.max(d.maxZ, a.maxZ());
            Map<Long, List<Arsa>> chunkler = gecici.computeIfAbsent(a.dunya(), k -> new HashMap<>());
            for (int cx = a.minX() >> 4; cx <= a.maxX() >> 4; cx++) {
                for (int cz = a.minZ() >> 4; cz <= a.maxZ() >> 4; cz++) {
                    chunkler.computeIfAbsent(anahtar(cx, cz), k -> new ArrayList<>(1)).add(a);
                }
            }
        }
        for (Map.Entry<String, Map<Long, List<Arsa>>> e : gecici.entrySet()) {
            DunyaIndeksi d = yeni.get(e.getKey());
            for (Map.Entry<Long, List<Arsa>> c : e.getValue().entrySet()) {
                d.chunkler.put(c.getKey(), c.getValue().toArray(new Arsa[0]));
            }
        }
        indeks.clear();
        indeks.putAll(yeni);
    }

    private static long anahtar(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static final class DunyaIndeksi {
        final HashMap<Long, Arsa[]> chunkler = new HashMap<>();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
    }
}
