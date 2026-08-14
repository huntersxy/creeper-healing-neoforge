package com.huntersxy.creeperhealing.config;

import com.huntersxy.creeperhealing.explosions.ExplosionHealingMode;
import com.huntersxy.creeperhealing.explosions.ExplosionSourceType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration of the mod. Values are stored in a TOML-like file (config/creeper-healing.toml)
 * using the same section/key layout as the original creeper-healing mod, so that existing
 * configuration files can be carried over.
 *
 * <p>The file is read on startup and can be reloaded in-game via {@code /creeper-healing reload_config}.
 * Every setting can also be changed in-game through commands, which persist the change to disk.
 */
public final class CreeperHealingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreeperHealingConfig.class);

    public static final String FILE_NAME = "creeper-healing.toml";

    // ---- Section keys ----
    public static final String DELAYS = "delays";
    public static final String ITEM_DROPS = "explosion_item_drops";
    public static final String SOURCES = "explosion_sources";
    public static final String MODE_SECTION = "explosion_healing_mode";
    public static final String PREFERENCES = "preferences";
    public static final String WHITELIST_SECTION = "whitelist";
    public static final String REPLACE_MAP_SECTION = "replace_map";

    // ---- Setting keys (section.key) ----
    public static final String EXPLOSION_HEAL_DELAY = DELAYS + ".explosion_heal_delay";
    public static final String BLOCK_PLACEMENT_DELAY = DELAYS + ".block_placement_delay";

    public static final String DROP_ITEMS_ON_MOB_EXPLOSIONS = ITEM_DROPS + ".drop_items_on_mob_explosions";
    public static final String DROP_ITEMS_ON_BLOCK_EXPLOSIONS = ITEM_DROPS + ".drop_items_on_block_explosions";
    public static final String DROP_ITEMS_ON_TNT_EXPLOSIONS = ITEM_DROPS + ".drop_items_on_tnt_explosions";
    public static final String DROP_ITEMS_ON_TRIGGERED_EXPLOSIONS = ITEM_DROPS + ".drop_items_on_triggered_explosions";
    public static final String DROP_ITEMS_ON_OTHER_EXPLOSIONS = ITEM_DROPS + ".drop_items_on_other_explosions";
    public static final String DROP_ITEMS_ON_MOB_EXPLOSIONS_BLACKLIST = ITEM_DROPS + ".drop_items_on_mob_explosions_blacklist";

    public static final String HEAL_MOB_EXPLOSIONS = SOURCES + ".heal_mob_explosions";
    public static final String HEAL_BLOCK_EXPLOSIONS = SOURCES + ".heal_block_explosions";
    public static final String HEAL_TNT_EXPLOSIONS = SOURCES + ".heal_tnt_explosions";
    public static final String HEAL_TRIGGERED_EXPLOSIONS = SOURCES + ".heal_triggered_explosions";
    public static final String HEAL_OTHER_EXPLOSIONS = SOURCES + ".heal_other_explosions";
    public static final String HEAL_MOB_EXPLOSIONS_BLACKLIST = SOURCES + ".heal_mob_explosions_blacklist";

    public static final String MODE = MODE_SECTION + ".mode";

    public static final String RESTORE_BLOCK_NBT = PREFERENCES + ".restore_block_nbt";
    public static final String FORCE_BLOCKS_WITH_NBT_TO_ALWAYS_HEAL = PREFERENCES + ".force_blocks_with_nbt_to_always_heal";
    public static final String MAKE_FALLING_BLOCKS_FALL = PREFERENCES + ".make_falling_blocks_fall";
    public static final String BLOCK_PLACEMENT_SOUND_EFFECT = PREFERENCES + ".block_placement_sound_effect";
    public static final String BLOCK_PLACEMENT_PARTICLES = PREFERENCES + ".block_placement_particles";
    public static final String HEAL_ON_HEALING_POTION_SPLASH = PREFERENCES + ".heal_on_healing_potion_splash";
    public static final String HEAL_ON_REGENERATION_POTION_SPLASH = PREFERENCES + ".heal_on_regeneration_potion_splash";
    public static final String ENABLE_WHITELIST = PREFERENCES + ".enable_whitelist";

    public static final String WHITELIST = WHITELIST_SECTION + ".whitelist";

    public static final double DEFAULT_EXPLOSION_HEAL_DELAY = 3.0;
    public static final double DEFAULT_BLOCK_PLACEMENT_DELAY = 1.0;

    /** Minimum accepted value (in seconds) for both delay settings. */
    private static final double MIN_DELAY = 0.05;

    /** All known scalar settings with their defaults, in file order. */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    /** Current raw values: "section.key" -> raw string value. */
    private static final Map<String, String> values = new LinkedHashMap<>();

    /** Replace map: old block id -> new block id. */
    private static final Map<String, String> replaceMap = new LinkedHashMap<>();

    private static Path configPath;

    static {
        DEFAULTS.put(EXPLOSION_HEAL_DELAY, "3.0");
        DEFAULTS.put(BLOCK_PLACEMENT_DELAY, "1.0");
        DEFAULTS.put(DROP_ITEMS_ON_MOB_EXPLOSIONS, "false");
        DEFAULTS.put(DROP_ITEMS_ON_BLOCK_EXPLOSIONS, "true");
        DEFAULTS.put(DROP_ITEMS_ON_TNT_EXPLOSIONS, "true");
        DEFAULTS.put(DROP_ITEMS_ON_TRIGGERED_EXPLOSIONS, "true");
        DEFAULTS.put(DROP_ITEMS_ON_OTHER_EXPLOSIONS, "true");
        DEFAULTS.put(DROP_ITEMS_ON_MOB_EXPLOSIONS_BLACKLIST, "[\"minecraft:placeholder\"]");
        DEFAULTS.put(HEAL_MOB_EXPLOSIONS, "true");
        DEFAULTS.put(HEAL_BLOCK_EXPLOSIONS, "false");
        DEFAULTS.put(HEAL_TNT_EXPLOSIONS, "false");
        DEFAULTS.put(HEAL_TRIGGERED_EXPLOSIONS, "false");
        DEFAULTS.put(HEAL_OTHER_EXPLOSIONS, "false");
        DEFAULTS.put(HEAL_MOB_EXPLOSIONS_BLACKLIST, "[\"minecraft:placeholder\"]");
        DEFAULTS.put(MODE, "default_mode");
        DEFAULTS.put(RESTORE_BLOCK_NBT, "false");
        DEFAULTS.put(FORCE_BLOCKS_WITH_NBT_TO_ALWAYS_HEAL, "false");
        DEFAULTS.put(MAKE_FALLING_BLOCKS_FALL, "true");
        DEFAULTS.put(BLOCK_PLACEMENT_SOUND_EFFECT, "true");
        DEFAULTS.put(BLOCK_PLACEMENT_PARTICLES, "true");
        DEFAULTS.put(HEAL_ON_HEALING_POTION_SPLASH, "true");
        DEFAULTS.put(HEAL_ON_REGENERATION_POTION_SPLASH, "true");
        DEFAULTS.put(ENABLE_WHITELIST, "false");
        DEFAULTS.put(WHITELIST, "[\"minecraft:placeholder\"]");
    }

    private CreeperHealingConfig() {
        throw new AssertionError();
    }

    /** Must be called once, from the mod constructor. */
    public static void init() {
        configPath = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        reload();
    }

    /** (Re-)reads the configuration file from disk, falling back to defaults for missing keys. */
    public static void reload() {
        values.clear();
        values.putAll(DEFAULTS);
        replaceMap.clear();
        replaceMap.put("minecraft:diamond_block", "minecraft:stone");
        if (configPath == null) {
            return;
        }
        if (Files.exists(configPath)) {
            try {
                List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
                String currentSection = "";
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
                        continue;
                    }
                    int equalsIndex = trimmed.indexOf('=');
                    if (equalsIndex < 0) {
                        continue;
                    }
                    String key = unquote(trimmed.substring(0, equalsIndex).trim());
                    String rawValue = unquote(trimmed.substring(equalsIndex + 1).trim());
                    if (REPLACE_MAP_SECTION.equals(currentSection)) {
                        replaceMap.put(key, rawValue);
                    } else if (!key.isEmpty()) {
                        values.put(currentSection + "." + key, rawValue);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read configuration file {}: {}", configPath, e.toString());
            }
        } else {
            save();
        }
        LOGGER.info("Loaded configuration from {}", configPath);
    }

    /** Writes the current configuration to disk, including comments. */
    public static void save() {
        if (configPath == null) {
            return;
        }
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, buildFileContents(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration file {}: {}", configPath, e.toString());
        }
    }

    private static String buildFileContents() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Creeper Healing configuration file.
");
        sb.append("# All settings can also be changed in-game via /creeper-healing commands.
");
        sb.append("# Use /creeper-healing reload_config to re-read this file while the game is running.

");

        sb.append("# Configure the delays related to the healing of explosions.
");
        sb.append("[delays]
");
        appendDouble(sb, EXPLOSION_HEAL_DELAY, DEFAULT_EXPLOSION_HEAL_DELAY, "How much time in seconds should an explosion wait for to begin healing.");
        appendDouble(sb, BLOCK_PLACEMENT_DELAY, DEFAULT_BLOCK_PLACEMENT_DELAY, "The time in seconds that a block takes to heal.");
        sb.append('
');

        sb.append("# Toggle whether certain explosions should drop items. Does not include items stored in container blocks.
");
        sb.append("[explosion_item_drops]
");
        appendBool(sb, DROP_ITEMS_ON_MOB_EXPLOSIONS, "Whether to drop items on explosions caused by mobs such as Creepers.");
        appendBool(sb, DROP_ITEMS_ON_BLOCK_EXPLOSIONS, "Whether to drop items on explosions caused by blocks such as beds or end crystal blocks.");
        appendBool(sb, DROP_ITEMS_ON_TNT_EXPLOSIONS, "Whether to drop items on explosions caused by TNT blocks and TNT minecarts.");
        appendBool(sb, DROP_ITEMS_ON_TRIGGERED_EXPLOSIONS, "Whether to drop items on explosions such as those caused by wind bursts.");
        appendBool(sb, DROP_ITEMS_ON_OTHER_EXPLOSIONS, "Whether to drop items on explosions whose source is not any of the ones provided in this setting category.");
        appendList(sb, DROP_ITEMS_ON_MOB_EXPLOSIONS_BLACKLIST, "Add mob identifiers to this blacklist to prevent explosions caused by the added mobs from dropping items if drop_items_on_mob_explosions is enabled.");
        sb.append('
');

        sb.append("# Configure which explosions are allowed to heal.
");
        sb.append("[explosion_sources]
");
        appendBool(sb, HEAL_MOB_EXPLOSIONS, "Heal explosions caused by mobs such as Creepers.");
        appendBool(sb, HEAL_BLOCK_EXPLOSIONS, "Heal explosions caused by blocks such as beds or end crystal blocks.");
        appendBool(sb, HEAL_TNT_EXPLOSIONS, "Heal explosions caused by TNT blocks and TNT minecarts.");
        appendBool(sb, HEAL_TRIGGERED_EXPLOSIONS, "Heal explosions such as those caused by wind bursts.");
        appendBool(sb, HEAL_OTHER_EXPLOSIONS, "Heal explosions caused by sources which aren't any of the ones provided in this setting category.");
        appendList(sb, HEAL_MOB_EXPLOSIONS_BLACKLIST, "Add mob identifiers to this blacklist to prevent explosions caused by the added mobs from healing if heal_mob_explosions is enabled.");
        sb.append('
');

        sb.append("# Choose between different special modes for explosion healing. Note that certain healing modes will not follow the explosion delay and block delay settings.
");
        sb.append("[explosion_healing_mode]
");
        sb.append("	# Choose any of the following healing modes by copying one of the strings and pasting it into the value of the \"mode\" setting below:
");
        sb.append("	#\"default_mode\", \"daytime_healing_mode\", \"difficulty_based_healing_mode\", \"blast_resistance_based_healing_mode\"
");
        sb.append("	mode = \"").append(getMode().getName()).append("\"

");

        sb.append("# Toggleable settings for extra features.
");
        sb.append("[preferences]
");
        appendBool(sb, RESTORE_BLOCK_NBT, "Whether to restore block nbt data upon healing. This option prevents container blocks like chests from dropping their inventories. Does not apply when the healed block is different from the destroyed block due to a replace map entry.");
        appendBool(sb, FORCE_BLOCKS_WITH_NBT_TO_ALWAYS_HEAL, "Whether to force blocks with nbt data to always heal, even if the replace map specifies a replacement for that block, and regardless of the block that may be occupying that position at the moment of healing.");
        appendBool(sb, MAKE_FALLING_BLOCKS_FALL, "Allows for a falling block, like sand or gravel, to fall when healed. Disabling this option makes the falling block have to receive a neighbor update before falling.");
        appendBool(sb, BLOCK_PLACEMENT_SOUND_EFFECT, "Whether a block placement sound effect should be played when a block is healed.");
        appendBool(sb, BLOCK_PLACEMENT_PARTICLES, "Whether a block placement sound effect should produce some cloud particles.");
        appendBool(sb, HEAL_ON_HEALING_POTION_SPLASH, "Makes explosion heal immediately when a potion of Healing is thrown on them.");
        appendBool(sb, HEAL_ON_REGENERATION_POTION_SPLASH, "Makes explosions begin their healing process when a potion of Regeneration is thrown on them.");
        appendBool(sb, ENABLE_WHITELIST, "Toggle the usage of the whitelist.");
        sb.append('
');

        sb.append("# Use an optional whitelist to customize which blocks are allowed to heal. To add an entry, specify the block's namespace
");
        sb.append("# along with its identifier, separated by a colon and enclosed in double quotes, and add it in-between the square brackets below. Separate each entry with a comma.
");
        sb.append("#Example entries:
");
        sb.append("#whitelist = [\"minecraft:grass\", \"minecraft:stone\", \"minecraft:sand\"]
");
        sb.append("[whitelist]
");
        appendList(sb, WHITELIST, null);
        sb.append('
');

        sb.append("# Add your own replace entries to configure which blocks should be used to heal other blocks. The block on the right will be used to heal the block on the left.
");
        sb.append("#Specify the block's namespace along with the block's name identifier, separated by a colon and enclosed in double quotes.
");
        sb.append("#Example entry:
");
        sb.append("#\"minecraft:gold_block\" = \"minecraft:stone\"
");
        sb.append("#Warning, the same key cannot appear more than once in the replace map!
");
        sb.append("[replace_map]
");
        if (replaceMap.isEmpty()) {
            sb.append("	# (empty)
");
        } else {
            for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
                sb.append("	\"").append(entry.getKey()).append("\" = \"").append(entry.getValue()).append("\"
");
            }
        }
        return sb.toString();
    }

    private static void appendDouble(StringBuilder sb, String key, double defaultValue, String comment) {
        sb.append("	#(Default = ").append(String.format(java.util.Locale.ROOT, "%.1f", defaultValue)).append(") ").append(comment).append('
');
        sb.append("	").append(key.substring(key.indexOf('.') + 1)).append(" = ").append(getDouble(key)).append('
');
    }

    private static void appendBool(StringBuilder sb, String key, String comment) {
        sb.append("	#(Default = ").append(DEFAULTS.get(key)).append(") ").append(comment).append('
');
        sb.append("	").append(key.substring(key.indexOf('.') + 1)).append(" = ").append(getBoolean(key)).append('
');
    }

    private static void appendList(StringBuilder sb, String key, String comment) {
        if (comment != null) {
            sb.append("	#").append(comment).append('
');
        }
        sb.append("	").append(key.substring(key.indexOf('.') + 1)).append(" = [");
        List<String> entries = getStringList(key);
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(entries.get(i)).append('"');
        }
        sb.append("]
");
    }

    // ---- Raw accessors ----

    private static String getRaw(String key) {
        return values.getOrDefault(key, DEFAULTS.getOrDefault(key, ""));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(getRaw(key));
    }

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(getRaw(key));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static List<String> getStringList(String key) {
        List<String> result = new ArrayList<>();
        String raw = getRaw(key).trim();
        if (raw.startsWith("[")) {
            raw = raw.substring(1);
        }
        if (raw.endsWith("]")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        for (String part : raw.split(",")) {
            String entry = unquote(part.trim());
            if (!entry.isEmpty()) {
                result.add(entry);
            }
        }
        return result;
    }

    public static ExplosionHealingMode getMode() {
        return ExplosionHealingMode.getFromName(getRaw(MODE));
    }

    public static Map<String, String> getReplaceMap() {
        return Map.copyOf(replaceMap);
    }

    // ---- Command-facing setters (persist to disk) ----

    public static void setBoolean(String key, boolean value) {
        values.put(key, Boolean.toString(value));
        save();
    }

    public static void setDouble(String key, double value) {
        values.put(key, String.format(java.util.Locale.ROOT, "%.2f", value));
        save();
    }

    public static void setMode(ExplosionHealingMode mode) {
        values.put(MODE, mode.getName());
        save();
    }

    public static boolean addToList(String key, String entry) {
        List<String> list = new ArrayList<>(getStringList(key));
        if (list.contains(entry)) {
            return false;
        }
        list.add(entry);
        values.put(key, toStringList(list));
        save();
        return true;
    }

    public static boolean removeFromList(String key, String entry) {
        List<String> list = new ArrayList<>(getStringList(key));
        if (!list.remove(entry)) {
            return false;
        }
        values.put(key, toStringList(list));
        save();
        return true;
    }

    public static boolean addToReplaceMap(String oldBlock, String newBlock) {
        if (replaceMap.containsKey(oldBlock)) {
            return false;
        }
        replaceMap.put(oldBlock, newBlock);
        save();
        return true;
    }

    public static boolean removeFromReplaceMap(String oldBlock) {
        if (replaceMap.remove(oldBlock) == null) {
            return false;
        }
        save();
        return true;
    }

    private static String toStringList(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(list.get(i)).append('"');
        }
        return sb.append(']').toString();
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith(""") && value.endsWith(""")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    // ---- Derived helpers used by the rest of the mod ----

    /** Heal delay in ticks (20 ticks = 1 second), clamped to a minimum of 20 ticks. */
    public static long getExplosionHealDelayTicks() {
        long rounded = Math.round(Math.max(0, getDouble(EXPLOSION_HEAL_DELAY)) * 20);
        return rounded == 0 ? 20 : rounded;
    }

    /** Block placement delay in ticks, clamped to a minimum of 20 ticks. */
    public static long getBlockPlacementDelayTicks() {
        long rounded = Math.round(Math.max(0, getDouble(BLOCK_PLACEMENT_DELAY)) * 20);
        return rounded == 0 ? 20 : rounded;
    }

    public static boolean healsSource(ExplosionSourceType sourceType, LivingEntity causingMob) {
        return switch (sourceType) {
            case MOB -> {
                if (!getBoolean(HEAL_MOB_EXPLOSIONS)) {
                    yield false;
                }
                if (causingMob == null) {
                    yield true;
                }
                String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(causingMob.getType()).toString();
                yield !getStringList(HEAL_MOB_EXPLOSIONS_BLACKLIST).contains(entityId);
            }
            case BLOCK -> getBoolean(HEAL_BLOCK_EXPLOSIONS);
            case TNT -> getBoolean(HEAL_TNT_EXPLOSIONS);
            case TRIGGERED -> getBoolean(HEAL_TRIGGERED_EXPLOSIONS);
            case OTHER -> getBoolean(HEAL_OTHER_EXPLOSIONS);
        };
    }

    public static boolean dropsItemsFor(ExplosionSourceType sourceType, LivingEntity causingMob) {
        return switch (sourceType) {
            case MOB -> {
                if (!getBoolean(DROP_ITEMS_ON_MOB_EXPLOSIONS)) {
                    yield false;
                }
                if (causingMob == null) {
                    yield true;
                }
                String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(causingMob.getType()).toString();
                yield !getStringList(DROP_ITEMS_ON_MOB_EXPLOSIONS_BLACKLIST).contains(entityId);
            }
            case BLOCK -> getBoolean(DROP_ITEMS_ON_BLOCK_EXPLOSIONS);
            case TNT -> getBoolean(DROP_ITEMS_ON_TNT_EXPLOSIONS);
            case TRIGGERED -> getBoolean(DROP_ITEMS_ON_TRIGGERED_EXPLOSIONS);
            case OTHER -> getBoolean(DROP_ITEMS_ON_OTHER_EXPLOSIONS);
        };
    }

    public static boolean restoreBlockNbt() {
        return getBoolean(RESTORE_BLOCK_NBT);
    }

    public static boolean forceBlocksWithNbtToAlwaysHeal() {
        return getBoolean(FORCE_BLOCKS_WITH_NBT_TO_ALWAYS_HEAL);
    }

    public static boolean makeFallingBlocksFall() {
        return getBoolean(MAKE_FALLING_BLOCKS_FALL);
    }

    public static boolean blockPlacementSoundEffect() {
        return getBoolean(BLOCK_PLACEMENT_SOUND_EFFECT);
    }

    public static boolean blockPlacementParticles() {
        return getBoolean(BLOCK_PLACEMENT_PARTICLES);
    }

    public static boolean healOnHealingPotionSplash() {
        return getBoolean(HEAL_ON_HEALING_POTION_SPLASH);
    }

    public static boolean healOnRegenerationPotionSplash() {
        return getBoolean(HEAL_ON_REGENERATION_POTION_SPLASH);
    }

    public static boolean isWhitelistEnabled() {
        return getBoolean(ENABLE_WHITELIST);
    }

    public static List<String> getWhitelist() {
        return getStringList(WHITELIST);
    }

    public static boolean isValidDelay(double delay) {
        return delay >= MIN_DELAY;
    }
}
