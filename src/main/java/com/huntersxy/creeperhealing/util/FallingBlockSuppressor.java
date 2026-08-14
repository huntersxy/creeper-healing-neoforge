package com.huntersxy.creeperhealing.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Registry of healed falling-block positions whose fall has been suppressed
 * (controlled by the {@code make_falling_blocks_fall} preference).
 *
 * <p>When the mod places a falling block with falling suppressed, the position is recorded here.
 * The scheduled fall tick then sees the suppression (via {@link com.huntersxy.creeperhealing.mixin.FallingBlockMixin})
 * and skips the fall. Entries are consumed on read and stale entries are pruned periodically.
 */
public final class FallingBlockSuppressor {

    /** level key -> (block pos -> game time when the suppression was recorded) */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> SUPPRESSED = new HashMap<>();

    private FallingBlockSuppressor() {
        throw new AssertionError();
    }

    public static void suppressFall(ServerLevel level, BlockPos pos) {
        SUPPRESSED.computeIfAbsent(level.dimension(), key -> new HashMap<>()).put(pos.immutable(), level.getGameTime());
    }

    /** Returns whether a fall was suppressed at this position, consuming the suppression. */
    public static boolean consumeSuppression(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Long> entries = SUPPRESSED.get(level.dimension());
        if (entries == null) {
            return false;
        }
        Long recordedAt = entries.remove(pos.immutable());
        if (entries.isEmpty()) {
            SUPPRESSED.remove(level.dimension());
        }
        return recordedAt != null;
    }

    /** Removes suppressions that were never consumed (block broken before its fall tick, etc.). */
    public static void pruneSuppressions(ServerLevel level) {
        Map<BlockPos, Long> entries = SUPPRESSED.get(level.dimension());
        if (entries == null || entries.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > 100) {
                iterator.remove();
            }
        }
        if (entries.isEmpty()) {
            SUPPRESSED.remove(level.dimension());
        }
    }
}
