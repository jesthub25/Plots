package org.jestron.gelismisArsa.entegrasyon;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jestron.gelismisArsa.model.Arsa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WorldEdit / FastAsyncWorldEdit köprüsü. Yalnızca WorldEdit yüklüyse oluşturulur.
 *  - Seçimden arsa oluşturma
 *  - Arsa şablonu (schematic) kaydetme / geri yükleme (kira bitince sıfırlama)
 */
public final class WorldEditKopru {

    private final Logger log;

    public WorldEditKopru(Logger log) {
        this.log = log;
    }

    /** @return {minX, minY, minZ, maxX, maxY, maxZ} veya seçim yoksa null */
    public int[] secim(Player p) {
        try {
            LocalSession s = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(p));
            Region r = s.getSelection(BukkitAdapter.adapt(p.getWorld()));
            BlockVector3 min = r.getMinimumPoint();
            BlockVector3 max = r.getMaximumPoint();
            return new int[]{min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                    max.getBlockX(), max.getBlockY(), max.getBlockZ()};
        } catch (IncompleteRegionException e) {
            return null;
        } catch (Throwable t) {
            log.log(Level.WARNING, "WorldEdit seçimi okunamadı.", t);
            return null;
        }
    }

    public boolean sablonKaydet(Arsa arsa, File dosya) {
        org.bukkit.World bw = Bukkit.getWorld(arsa.dunya());
        if (bw == null) return false;
        World w = BukkitAdapter.adapt(bw);
        CuboidRegion bolge = bolge(arsa, w);
        BlockArrayClipboard pano = new BlockArrayClipboard(bolge);
        try (EditSession es = WorldEdit.getInstance().newEditSession(w)) {
            ForwardExtentCopy kopya = new ForwardExtentCopy(es, bolge, pano, bolge.getMinimumPoint());
            kopya.setCopyingEntities(false);
            Operations.complete(kopya);
        } catch (Throwable t) {
            log.log(Level.WARNING, "#" + arsa.id() + " arsasının şablonu kopyalanamadı.", t);
            return false;
        }
        ClipboardFormat bicim = ClipboardFormats.findByAlias("sponge");
        if (bicim == null) bicim = ClipboardFormats.findByAlias("schem");
        if (bicim == null) {
            log.warning("Schematic biçimi bulunamadı, şablon kaydedilemedi.");
            return false;
        }
        File klasor = dosya.getParentFile();
        if (klasor != null && !klasor.exists() && !klasor.mkdirs()) return false;
        try (ClipboardWriter yazici = bicim.getWriter(new FileOutputStream(dosya))) {
            yazici.write(pano);
            return true;
        } catch (Throwable t) {
            log.log(Level.WARNING, "#" + arsa.id() + " arsasının şablonu yazılamadı.", t);
            return false;
        }
    }

    /** Şablonu arsaya geri yapıştırır (hava blokları dahil, böylece tamamen eski hale döner). */
    public boolean sablonYukle(Arsa arsa, File dosya) {
        org.bukkit.World bw = Bukkit.getWorld(arsa.dunya());
        if (bw == null || !dosya.exists()) return false;
        ClipboardFormat bicim = ClipboardFormats.findByFile(dosya);
        if (bicim == null) {
            log.warning("#" + arsa.id() + " şablon dosyası tanınamadı: " + dosya.getName());
            return false;
        }
        Clipboard pano;
        try (ClipboardReader okuyucu = bicim.getReader(new FileInputStream(dosya))) {
            pano = okuyucu.read();
        } catch (Throwable t) {
            log.log(Level.WARNING, "#" + arsa.id() + " şablonu okunamadı.", t);
            return false;
        }
        World w = BukkitAdapter.adapt(bw);
        try (EditSession es = WorldEdit.getInstance().newEditSession(w)) {
            Operation op = new ClipboardHolder(pano).createPaste(es)
                    .to(BlockVector3.at(arsa.minX(), arsa.minY(), arsa.minZ()))
                    .ignoreAirBlocks(false)
                    .build();
            Operations.complete(op);
            return true;
        } catch (Throwable t) {
            log.log(Level.WARNING, "#" + arsa.id() + " şablonu yapıştırılamadı.", t);
            return false;
        }
    }

    /** Şablon yoksa eski davranış: arsanın tamamını havaya çevirir. */
    public boolean havayaCevir(Arsa arsa) {
        org.bukkit.World bw = Bukkit.getWorld(arsa.dunya());
        if (bw == null || BlockTypes.AIR == null) return false;
        World w = BukkitAdapter.adapt(bw);
        try (EditSession es = WorldEdit.getInstance().newEditSession(w)) {
            es.setBlocks((Region) bolge(arsa, w), BlockTypes.AIR.getDefaultState());
            return true;
        } catch (Throwable t) {
            log.log(Level.WARNING, "#" + arsa.id() + " arsası temizlenemedi.", t);
            return false;
        }
    }

    private static CuboidRegion bolge(Arsa a, World w) {
        return new CuboidRegion(w,
                BlockVector3.at(a.minX(), a.minY(), a.minZ()),
                BlockVector3.at(a.maxX(), a.maxY(), a.maxZ()));
    }
}
