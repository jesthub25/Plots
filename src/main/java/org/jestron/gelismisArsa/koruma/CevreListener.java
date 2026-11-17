package org.jestron.gelismisArsa.koruma;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Vindicator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.jestron.gelismisArsa.Ayarlar;
import org.jestron.gelismisArsa.Yetki;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.veri.ArsaManager;

import java.util.List;
import java.util.function.Supplier;

/**
 * Oyuncu dışı tehlikelere karşı arsa koruması (config.yml → koruma bölümü ile açılıp kapanır).
 * Sık tetiklenen olaylarda (sıvı akışı, huni) önce ucuz kontroller yapılır.
 */
public final class CevreListener implements Listener {

    private final ArsaManager arsalar;
    private final Yetki yetki;
    private final Supplier<Ayarlar> ayar;

    public CevreListener(ArsaManager arsalar, Yetki yetki, Supplier<Ayarlar> ayar) {
        this.arsalar = arsalar;
        this.yetki = yetki;
        this.ayar = ayar;
    }

    // ── Patlama ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void varlikPatlama(EntityExplodeEvent e) {
        if (ayar.get().korumaPatlama) patlamaFiltrele(e.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void blokPatlama(BlockExplodeEvent e) {
        if (ayar.get().korumaPatlama) patlamaFiltrele(e.blockList());
    }

    private void patlamaFiltrele(List<Block> bloklar) {
        bloklar.removeIf(b -> arsalar.bul(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void asiliPatlama(HangingBreakEvent e) {
        if (e.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION && ayar.get().korumaPatlama
                && arsalar.bul(e.getEntity().getLocation()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void asiliMobKirma(HangingBreakByEntityEvent e) {
        Entity kaldiran = e.getRemover();
        if (kaldiran == null || KorumaListener.oyuncuKaynagi(kaldiran) != null) return;
        if (ayar.get().korumaMobTahribat && arsalar.bul(e.getEntity().getLocation()) != null) {
            e.setCancelled(true);
        }
    }

    /** Patlama veya mob hasarından zırh askısı / eşya çerçevesi koruması. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void dekorHasar(EntityDamageByEntityEvent e) {
        Entity kurban = e.getEntity();
        if (!(kurban instanceof ArmorStand) && !(kurban instanceof Hanging)) return;
        if (KorumaListener.oyuncuKaynagi(e.getDamager()) != null) return; // oyuncu kaynaklı → KorumaListener
        EntityDamageEvent.DamageCause neden = e.getCause();
        boolean patlama = neden == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || neden == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
        Ayarlar a = ayar.get();
        if ((patlama ? a.korumaPatlama : a.korumaMobTahribat) && arsalar.bul(kurban.getLocation()) != null) {
            e.setCancelled(true);
        }
    }

    // ── Ateş ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void yanma(BlockBurnEvent e) {
        if (ayar.get().korumaAtes && arsalar.bul(e.getBlock()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void atesYayilma(BlockSpreadEvent e) {
        if (!ayar.get().korumaAtes) return;
        Material kaynak = e.getSource().getType();
        if (!EtkilesimHaritasi.ates(kaynak)) return;
        if (arsalar.bul(e.getBlock()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void tutusma(BlockIgniteEvent e) {
        if (e.getPlayer() != null || !ayar.get().korumaAtes) return;
        BlockIgniteEvent.IgniteCause neden = e.getCause();
        if (neden == BlockIgniteEvent.IgniteCause.SPREAD || neden == BlockIgniteEvent.IgniteCause.LAVA
                || neden == BlockIgniteEvent.IgniteCause.LIGHTNING || neden == BlockIgniteEvent.IgniteCause.FIREBALL
                || neden == BlockIgniteEvent.IgniteCause.EXPLOSION) {
            if (arsalar.bul(e.getBlock()) != null) e.setCancelled(true);
        }
    }

    // ── Sıvı akışı ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void sivi(BlockFromToEvent e) {
        if (!ayar.get().korumaSivi) return;
        Arsa hedef = arsalar.bul(e.getToBlock());
        if (hedef == null) return;
        if (arsalar.bul(e.getBlock()) != hedef) e.setCancelled(true);
    }

    // ── Piston ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void pistonIt(BlockPistonExtendEvent e) {
        if (!ayar.get().korumaPiston) return;
        Block piston = e.getBlock();
        BlockFace yon = e.getDirection();
        Arsa p = arsalar.bul(piston);
        if (arsalar.bul(piston.getRelative(yon)) != p || sinirAsiyor(p, e.getBlocks(), yon)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void pistonCek(BlockPistonRetractEvent e) {
        if (!ayar.get().korumaPiston) return;
        Arsa p = arsalar.bul(e.getBlock());
        if (sinirAsiyor(p, e.getBlocks(), e.getDirection())) e.setCancelled(true);
    }

    private boolean sinirAsiyor(Arsa pistonArsa, List<Block> bloklar, BlockFace yon) {
        for (Block b : bloklar) {
            if (arsalar.bul(b) != pistonArsa || arsalar.bul(b.getRelative(yon)) != pistonArsa) return true;
        }
        return false;
    }

    // ── Dağıtıcı ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void dagitici(BlockDispenseEvent e) {
        if (!ayar.get().korumaDagitici) return;
        Block d = e.getBlock();
        BlockData veri = d.getBlockData();
        if (!(veri instanceof Directional yonlu)) return;
        Arsa hedef = arsalar.bul(d.getRelative(yonlu.getFacing()));
        if (hedef != null && hedef != arsalar.bul(d)) e.setCancelled(true);
    }

    // ── Huni / eşya taşıma (arsadaki depodan dışarı eşya çekme) ─────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void huni(InventoryMoveItemEvent e) {
        if (!ayar.get().korumaHuni) return;
        Location kaynak = e.getSource().getLocation();
        if (kaynak == null) return;
        Arsa k = arsalar.bul(kaynak);
        if (k == null) return;
        Location hedef = e.getDestination().getLocation();
        if (hedef == null || arsalar.bul(hedef) != k) e.setCancelled(true);
    }

    // ── Mob tahribatı ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void blokDegisimi(EntityChangeBlockEvent e) {
        Entity v = e.getEntity();
        if (v instanceof Player || v instanceof FallingBlock) return;
        Arsa arsa = arsalar.bul(e.getBlock());
        if (arsa == null) return;
        if (v instanceof Projectile pr) {
            Player p = KorumaListener.oyuncuKaynagi(pr);
            if (p != null) {
                if (!yetki.izinli(p, arsa, Izin.BLOK_KIR)) e.setCancelled(true);
                return;
            }
            if (ayar.get().korumaMobTahribat) e.setCancelled(true);
            return;
        }
        if (ayar.get().korumaMobTahribat && tahripkar(v)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void kapiKirma(EntityBreakDoorEvent e) {
        if (ayar.get().korumaMobTahribat && arsalar.bul(e.getBlock()) != null) e.setCancelled(true);
    }

    private static boolean tahripkar(Entity v) {
        return v instanceof Enderman || v instanceof Wither || v instanceof WitherSkull || v instanceof Ravager
                || v instanceof Zombie || v instanceof Vindicator || v instanceof Silverfish;
    }

    // ── Ağaç / yapı büyümesi ────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void buyume(StructureGrowEvent e) {
        if (!ayar.get().korumaBuyume) return;
        Arsa kok = arsalar.bul(e.getLocation());
        e.getBlocks().removeIf(bs -> {
            Arsa a = arsalar.bul(bs.getWorld(), bs.getX(), bs.getY(), bs.getZ());
            return a != null && a != kok;
        });
    }
}
