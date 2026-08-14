package com.huntersxy.creeperhealing.explosions;

import java.util.Locale;

/**
 * The different special modes that customize the way explosions are healed.
 */
public enum ExplosionHealingMode {

    DEFAULT_MODE("default_mode"),
    DAYTIME_HEALING_MODE("daytime_healing_mode"),
    DIFFICULTY_BASED_HEALING_MODE("difficulty_based_healing_mode"),
    BLAST_RESISTANCE_BASED_HEALING_MODE("blast_resistance_based_healing_mode");

    private final String name;

    ExplosionHealingMode(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /** Case-insensitive lookup that accepts both the lowercase name and the enum constant name. */
    public static ExplosionHealingMode getFromName(String name) {
        if (name == null) {
            return DEFAULT_MODE;
        }
        for (ExplosionHealingMode mode : ExplosionHealingMode.values()) {
            if (mode.getName().equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return DEFAULT_MODE;
    }
}
