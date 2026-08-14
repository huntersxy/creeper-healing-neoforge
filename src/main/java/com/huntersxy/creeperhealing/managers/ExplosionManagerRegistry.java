package com.huntersxy.creeperhealing.managers;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the {@link ExplosionManager} of every loaded server level.
 */
public final class ExplosionManagerRegistry {

    private static final Map<ServerLevel, ExplosionManager> MANAGERS = new HashMap<>();

    private ExplosionManagerRegistry() {
        throw new AssertionError();
    }

    @Nullable
    public static ExplosionManager get(ServerLevel level) {
        return MANAGERS.get(level);
    }

    public static void register(ServerLevel level, ExplosionManager manager) {
        MANAGERS.put(level, manager);
    }

    public static void unregister(ServerLevel level) {
        MANAGERS.remove(level);
    }

    public static Collection<ExplosionManager> all() {
        return MANAGERS.values();
    }
}
