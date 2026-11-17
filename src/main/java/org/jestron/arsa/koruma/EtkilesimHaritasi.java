package org.jestron.arsa.koruma;

import org.bukkit.Material;
import org.jestron.arsa.model.Izin;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/**
 * Hangi bloğa sağ tıklamanın / hangi eşyayı kullanmanın hangi izni gerektirdiği.
 * Eşleşme malzeme ADINA göre açılışta bir kez hesaplanır; böylece 1.20.1'de olmayan
 * yeni bloklar (bakır kapılar, crafter vb.) üst sürümlerde otomatik olarak korunur.
 * Çalışma anında yalnızca EnumMap/EnumSet araması yapılır.
 */
public final class EtkilesimHaritasi {

    private static final EnumMap<Material, Izin> BLOK = new EnumMap<>(Material.class);
    private static final EnumSet<Material> BLOK_DEGISTIREN_ESYA = EnumSet.noneOf(Material.class);
    private static final EnumSet<Material> HAVADA_KISITLI_ESYA = EnumSet.noneOf(Material.class);
    private static final EnumSet<Material> EZILEN = EnumSet.noneOf(Material.class);
    private static final EnumSet<Material> ATES = EnumSet.noneOf(Material.class);

    private static final Set<String> SANDIK = Set.of(
            "CHEST", "TRAPPED_CHEST", "BARREL", "ENDER_CHEST", "HOPPER", "DROPPER", "DISPENSER",
            "FURNACE", "BLAST_FURNACE", "SMOKER", "BREWING_STAND", "CHISELED_BOOKSHELF",
            "DECORATED_POT", "CRAFTER", "SHELF");

    private static final Set<String> MEKANIZMA = Set.of(
            "LEVER", "REPEATER", "COMPARATOR", "DAYLIGHT_DETECTOR", "NOTE_BLOCK", "JUKEBOX",
            "BELL", "TRIPWIRE", "TRIPWIRE_HOOK", "REDSTONE_WIRE", "COPPER_BULB");

    private static final Set<String> ETKILESIM = Set.of(
            "CRAFTING_TABLE", "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL", "ENCHANTING_TABLE",
            "GRINDSTONE", "STONECUTTER", "SMITHING_TABLE", "LOOM", "CARTOGRAPHY_TABLE",
            "FLETCHING_TABLE", "BEACON", "LECTERN", "RESPAWN_ANCHOR", "CAKE", "FLOWER_POT",
            "COMPOSTER", "CAULDRON", "CAMPFIRE", "SOUL_CAMPFIRE", "DRAGON_EGG", "BEEHIVE",
            "BEE_NEST", "SWEET_BERRY_BUSH", "CAVE_VINES", "CAVE_VINES_PLANT", "CANDLE",
            "VAULT", "TRIAL_SPAWNER", "SPAWNER", "CONDUIT", "LODESTONE", "END_PORTAL_FRAME");

    private static final Set<String> ESYA = Set.of(
            "BONE_MEAL", "HONEYCOMB", "FLINT_AND_STEEL", "FIRE_CHARGE", "BRUSH", "SHEARS",
            "GLASS_BOTTLE", "POTION", "INK_SAC", "GLOW_INK_SAC", "ARMOR_STAND", "END_CRYSTAL",
            "ENDER_EYE", "LEAD", "MINECART", "WIND_CHARGE");

    static {
        for (Material m : Material.values()) {
            String n = m.name();
            if (n.startsWith("LEGACY_")) continue;
            if (m.isBlock()) {
                Izin i = blokIzni(n);
                if (i != null) BLOK.put(m, i);
                if (n.equals("FARMLAND") || n.equals("TURTLE_EGG") || n.equals("SNIFFER_EGG")) EZILEN.add(m);
                if (n.equals("FIRE") || n.equals("SOUL_FIRE")) ATES.add(m);
            }
            if (m.isItem() && esyaMi(n)) BLOK_DEGISTIREN_ESYA.add(m);
            if (m.isItem() && n.endsWith("_SPAWN_EGG")) HAVADA_KISITLI_ESYA.add(m);
        }
    }

    private EtkilesimHaritasi() {}

    private static Izin blokIzni(String n) {
        if (n.endsWith("_DOOR") || n.endsWith("_TRAPDOOR") || n.endsWith("_FENCE_GATE")) return Izin.KAPI;
        if (n.endsWith("_BUTTON") || n.endsWith("_PRESSURE_PLATE") || MEKANIZMA.contains(n)) return Izin.MEKANIZMA;
        if (n.endsWith("SHULKER_BOX") || n.endsWith("_SHELF") || SANDIK.contains(n)) return Izin.SANDIK;
        if (n.endsWith("_SIGN")) return Izin.BLOK_KOY; // 1.20+: sağ tık tabelayı düzenler
        if (n.endsWith("_BED") || n.endsWith("CANDLE_CAKE") || n.endsWith("_CANDLE")
                || n.endsWith("_CAULDRON") || n.startsWith("POTTED_") || ETKILESIM.contains(n)) {
            return Izin.ETKILESIM;
        }
        return null;
    }

    private static boolean esyaMi(String n) {
        return n.endsWith("_AXE") || n.endsWith("_HOE") || n.endsWith("_SHOVEL")
                || n.endsWith("_SPAWN_EGG") || n.endsWith("_DYE") || n.endsWith("_BOAT")
                || n.endsWith("_RAFT") || n.endsWith("_MINECART") || ESYA.contains(n);
    }

    /** Sağ tıklanan blok için gereken izin; kısıtlı değilse null. */
    public static Izin blok(Material m) {
        return BLOK.get(m);
    }

    /** Bloğa sağ tıklayarak blok/dünya değiştiren eşya mı? */
    public static boolean blokDegistirenEsya(Material m) {
        return BLOK_DEGISTIREN_ESYA.contains(m);
    }

    /** Havaya sağ tıklayınca da etkisi olan kısıtlı eşya mı (spawn yumurtası)? */
    public static boolean havadaKisitli(Material m) {
        return HAVADA_KISITLI_ESYA.contains(m);
    }

    /** Üzerine basınca bozulan blok (tarla, kaplumbağa yumurtası). */
    public static boolean ezilen(Material m) {
        return EZILEN.contains(m);
    }

    public static boolean ates(Material m) {
        return ATES.contains(m);
    }
}
