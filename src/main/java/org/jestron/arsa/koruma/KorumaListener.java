package org.jestron.arsa.koruma;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.jestron.arsa.Yetki;
import org.jestron.arsa.gui.Menu;
import org.jestron.arsa.model.Arsa;
import org.jestron.arsa.model.Izin;
import org.jestron.arsa.util.Mesaj;
import org.jestron.arsa.veri.ArsaManager;

/**
 * Oyuncu kaynaklı tüm arsa korumaları.
 *
 * Önemli noktalar:
 *  - PlayerInteractEvent HER İKİ EL için kontrol edilir. Ana el reddedilince sunucu
 *    işlemi yan elle tekrar dener; yan el atlanırsa kapı/sandık yine açılır (eski açık buydu).
 *  - Basınç plakası / tarla ezme (PHYSICAL) da kontrol edilir.
 *  - Reddedilen etkileşim HIGHEST önceliğinde tekrar uygulanır; başka bir eklenti
 *    iptali geri alsa bile koruma bozulmaz.
 *  - Sandık açılışı ayrıca InventoryOpenEvent ile ikinci kez denetlenir (güvenlik ağı).
 */
public final class KorumaListener implements Listener {

    private final ArsaManager arsalar;
    private final Yetki yetki;
    private PlayerInteractEvent sonReddedilen;

    public KorumaListener(ArsaManager arsalar, Yetki yetki) {
        this.arsalar = arsalar;
        this.yetki = yetki;
    }

    private boolean izinli(Player p, Arsa a, Izin izin) {
        return a == null || yetki.izinli(p, a, izin);
    }

    // ── Blok kırma / koyma ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void blokKir(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!izinli(p, arsalar.bul(e.getBlock()), Izin.BLOK_KIR)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.BLOK_KIR);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void blokKoy(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!izinli(p, arsalar.bul(e.getBlockPlaced()), Izin.BLOK_KOY)) {
            e.setCancelled(true);
            e.setBuild(false);
            Mesaj.reddedildi(p, Izin.BLOK_KOY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void tabela(SignChangeEvent e) {
        Player p = e.getPlayer();
        if (!izinli(p, arsalar.bul(e.getBlock()), Izin.BLOK_KOY)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.BLOK_KOY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void tutusturma(BlockIgniteEvent e) {
        Player p = e.getPlayer();
        if (p == null) return;
        if (!izinli(p, arsalar.bul(e.getBlock()), Izin.ESYA_KULLAN)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.ESYA_KULLAN);
        }
    }

    // ── Kova ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void kovaBosalt(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        Arsa hedef = arsalar.bul(e.getBlock());
        Arsa tiklanan = arsalar.bul(e.getBlockClicked());
        if (!izinli(p, hedef, Izin.BLOK_KOY) || !izinli(p, tiklanan, Izin.BLOK_KOY)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.BLOK_KOY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void kovaDoldur(PlayerBucketFillEvent e) {
        Player p = e.getPlayer();
        Arsa hedef = arsalar.bul(e.getBlock());
        Arsa tiklanan = arsalar.bul(e.getBlockClicked());
        if (!izinli(p, hedef, Izin.BLOK_KIR) || !izinli(p, tiklanan, Izin.BLOK_KIR)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.BLOK_KIR);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void kovaVarlik(PlayerBucketEntityEvent e) {
        Player p = e.getPlayer();
        if (!izinli(p, arsalar.bul(e.getEntity().getLocation()), Izin.VARLIK)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.VARLIK);
        }
    }

    // ── Blok etkileşimi (sandık, kapı, düğme, istasyon, eşya) ───────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void etkilesim(PlayerInteractEvent e) {
        Action a = e.getAction();
        Player p = e.getPlayer();

        if (a == Action.RIGHT_CLICK_AIR || a == Action.LEFT_CLICK_AIR) {
            // Havaya sağ tıkla etkili olan kısıtlı eşyalar (ör. spawn yumurtası)
            ItemStack el = e.getItem();
            if (a == Action.RIGHT_CLICK_AIR && el != null && EtkilesimHaritasi.havadaKisitli(el.getType())) {
                Arsa arsa = arsalar.bul(p.getLocation());
                if (!izinli(p, arsa, Izin.ESYA_KULLAN)) {
                    e.setUseItemInHand(Event.Result.DENY);
                    Mesaj.reddedildi(p, Izin.ESYA_KULLAN);
                }
            }
            return;
        }

        Block b = e.getClickedBlock();
        if (b == null) return;

        if (a == Action.PHYSICAL) {
            Arsa arsa = arsalar.bul(b);
            if (arsa == null) return;
            Material m = b.getType();
            Izin gerek = EtkilesimHaritasi.ezilen(m) ? Izin.BLOK_KIR
                    : (EtkilesimHaritasi.blok(m) == Izin.MEKANIZMA ? Izin.MEKANIZMA : null);
            if (gerek != null && !izinli(p, arsa, gerek)) {
                reddet(e);
                Mesaj.sessizRed(p, gerek);
            }
            return;
        }

        if (a == Action.LEFT_CLICK_BLOCK) {
            Arsa arsa = arsalar.bul(b);
            Material m = b.getType();
            if (arsa != null && (m == Material.NOTE_BLOCK || m == Material.DRAGON_EGG)) {
                Izin gerek = m == Material.NOTE_BLOCK ? Izin.MEKANIZMA : Izin.ETKILESIM;
                if (!izinli(p, arsa, gerek)) {
                    reddet(e);
                    Mesaj.reddedildi(p, gerek);
                    return;
                }
            }
            // Ateş söndürme
            Block on = b.getRelative(e.getBlockFace());
            if (EtkilesimHaritasi.ates(on.getType())) {
                Arsa atesArsa = arsalar.bul(on);
                if (!izinli(p, atesArsa, Izin.BLOK_KIR)) {
                    reddet(e);
                    Mesaj.reddedildi(p, Izin.BLOK_KIR);
                }
            }
            return;
        }

        // RIGHT_CLICK_BLOCK
        Arsa arsa = arsalar.bul(b);
        if (arsa != null) {
            Izin gerek = EtkilesimHaritasi.blok(b.getType());
            if (gerek != null && blokKullanilir(p) && !izinli(p, arsa, gerek)) {
                reddet(e);
                Mesaj.reddedildi(p, gerek);
                return;
            }
        }

        ItemStack el = e.getItem();
        if (el != null && EtkilesimHaritasi.blokDegistirenEsya(el.getType())) {
            Arsa komsu = arsalar.bul(b.getRelative(e.getBlockFace()));
            if (!izinli(p, arsa, Izin.ESYA_KULLAN) || !izinli(p, komsu, Izin.ESYA_KULLAN)) {
                // Blok etkileşimi (izinliyse kapı vb.) serbest kalır, sadece eşya kullanımı engellenir
                e.setUseItemInHand(Event.Result.DENY);
                if (EtkilesimHaritasi.blok(b.getType()) == null) {
                    reddet(e);
                }
                Mesaj.reddedildi(p, Izin.ESYA_KULLAN);
            }
        }
    }

    /** Başka eklentiler iptali kaldırsa bile reddi kesinleştirir. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void etkilesimKesinlestir(PlayerInteractEvent e) {
        if (e == sonReddedilen) {
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            sonReddedilen = null;
        }
    }

    private void reddet(PlayerInteractEvent e) {
        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
        sonReddedilen = e;
    }

    /**
     * Vanilla: oyuncu eğilirken elinde (ana veya yan) eşya varsa bloğun kendisi kullanılmaz,
     * eşya kullanılır. Bu durumda blok izni değil eşya/koyma izinleri geçerlidir.
     */
    private static boolean blokKullanilir(Player p) {
        if (!p.isSneaking()) return true;
        return bos(p.getInventory().getItemInMainHand()) && bos(p.getInventory().getItemInOffHand());
    }

    private static boolean bos(ItemStack i) {
        return i == null || i.getType().isAir();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void kursuKitap(PlayerTakeLecternBookEvent e) {
        Player p = e.getPlayer();
        if (!izinli(p, arsalar.bul(e.getLectern().getLocation()), Izin.SANDIK)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.SANDIK);
        }
    }

    /** Hangi yolla açılırsa açılsın arsa içindeki depolar için ikinci kontrol. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void envanterAc(InventoryOpenEvent e) {
        HumanEntity he = e.getPlayer();
        if (!(he instanceof Player p)) return;
        Inventory inv = e.getInventory();
        String tip = inv.getType().name();
        if (tip.equals("PLAYER") || tip.equals("CRAFTING") || tip.equals("CREATIVE")
                || tip.equals("LECTERN") || tip.equals("WORKBENCH")) return;
        InventoryHolder h = inv.getHolder();
        if (h == null || h instanceof Menu) return;
        Location konum;
        Izin gerek = Izin.SANDIK;
        if (h instanceof BlockInventoryHolder bh) {
            konum = bh.getBlock().getLocation();
        } else if (h instanceof DoubleChest dc) {
            konum = dc.getLocation();
        } else if (h instanceof Entity en && !(h instanceof HumanEntity)) {
            konum = en.getLocation();
            if (h instanceof AbstractVillager) gerek = Izin.VARLIK;
        } else {
            return;
        }
        if (!izinli(p, arsalar.bul(konum), gerek)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, gerek);
        }
    }

    // ── Varlıklar ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void varlikEtkilesim(PlayerInteractEntityEvent e) {
        varlikKontrol(e.getPlayer(), e.getRightClicked(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void varlikEtkilesimNokta(PlayerInteractAtEntityEvent e) {
        varlikKontrol(e.getPlayer(), e.getRightClicked(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void zirhAskisi(PlayerArmorStandManipulateEvent e) {
        varlikKontrol(e.getPlayer(), e.getRightClicked(), e);
    }

    private void varlikKontrol(Player p, Entity hedef, PlayerInteractEntityEvent e) {
        if (hedef instanceof Player) return;
        if (hedef instanceof Tameable t) {
            AnimalTamer sahip = t.getOwner();
            if (sahip != null && sahip.getUniqueId().equals(p.getUniqueId())) return;
        }
        if (!izinli(p, arsalar.bul(hedef.getLocation()), Izin.VARLIK)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.VARLIK);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void varlikHasar(EntityDamageByEntityEvent e) {
        Entity kurban = e.getEntity();
        if (kurban instanceof Player || dusman(kurban)) return;
        Player p = oyuncuKaynagi(e.getDamager());
        if (p == null) return;
        if (kurban instanceof Tameable t) {
            AnimalTamer sahip = t.getOwner();
            if (sahip != null && sahip.getUniqueId().equals(p.getUniqueId())) return;
        }
        if (!izinli(p, arsalar.bul(kurban.getLocation()), Izin.VARLIK)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.VARLIK);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void aracHasar(VehicleDamageEvent e) {
        Player p = oyuncuKaynagi(e.getAttacker());
        if (p != null && !izinli(p, arsalar.bul(e.getVehicle().getLocation()), Izin.VARLIK)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.VARLIK);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void aracYikim(VehicleDestroyEvent e) {
        Player p = oyuncuKaynagi(e.getAttacker());
        if (p != null && !izinli(p, arsalar.bul(e.getVehicle().getLocation()), Izin.VARLIK)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.VARLIK);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void asiliKoy(HangingPlaceEvent e) {
        Player p = e.getPlayer();
        if (p != null && !izinli(p, arsalar.bul(e.getEntity().getLocation()), Izin.BLOK_KOY)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.BLOK_KOY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void asiliKir(HangingBreakByEntityEvent e) {
        Player p = oyuncuKaynagi(e.getRemover());
        if (p != null && !izinli(p, arsalar.bul(e.getEntity().getLocation()), Izin.VARLIK)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.VARLIK);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void varlikYerlestir(EntityPlaceEvent e) {
        Player p = e.getPlayer();
        if (p != null && !izinli(p, arsalar.bul(e.getEntity().getLocation()), Izin.BLOK_KOY)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.BLOK_KOY);
        }
    }

    /** Ok vb. ile düğme / basınç plakası tetikleme. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void mermiEtkilesim(EntityInteractEvent e) {
        if (!(e.getEntity() instanceof Projectile pr)) return;
        if (!(pr.getShooter() instanceof Player p)) return;
        if (!izinli(p, arsalar.bul(e.getBlock()), Izin.MEKANIZMA)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void olta(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
        Entity yakalanan = e.getCaught();
        if (yakalanan == null || yakalanan instanceof Player) return;
        Izin gerek = yakalanan instanceof Item ? Izin.ITEM_TOPLA : Izin.VARLIK;
        Player p = e.getPlayer();
        if (!izinli(p, arsalar.bul(yakalanan.getLocation()), gerek)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, gerek);
        }
    }

    // ── Yerden eşya alma / atma ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void esyaAl(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!izinli(p, arsalar.bul(e.getItem().getLocation()), Izin.ITEM_TOPLA)) {
            e.setCancelled(true);
            Mesaj.sessizRed(p, Izin.ITEM_TOPLA);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void esyaAt(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (!izinli(p, arsalar.bul(p.getLocation()), Izin.ITEM_AT)) {
            e.setCancelled(true);
            Mesaj.reddedildi(p, Izin.ITEM_AT);
        }
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    static Player oyuncuKaynagi(Entity kaynak) {
        if (kaynak instanceof Player p) return p;
        if (kaynak instanceof Projectile pr) {
            ProjectileSource s = pr.getShooter();
            if (s instanceof Player p) return p;
        }
        return null;
    }

    /** Oyuncuların kendini savunabilmesi için düşman yaratıklara vurmak her zaman serbesttir. */
    private static boolean dusman(Entity e) {
        return e instanceof Monster || e instanceof Slime || e instanceof Ghast || e instanceof Phantom
                || e instanceof Shulker || e instanceof EnderDragon || e instanceof Hoglin;
    }
}
