package com.huntersxy.creeperhealing.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controls whether the blocks destroyed by a healable explosion should drop their items.
 *
 * <p>NeoForge fires {@code ExplosionEvent.Detonate} before any block is destroyed and the drops
 * (via {@code BlockState#onExplosionHit}) happen synchronously right afterwards, within the same
 * tick. The policy recorded at {@code Detonate} therefore applies exactly to the drops of that
 * explosion. Policies are cleared when the next explosion starts and as a safety net at the end
 * of every level tick.
 */
public final class ExplosionDropController {

    private static final Map<ServerLevel, DropPolicy> POLICIES = new HashMap<>();

    private ExplosionDropController() {
        throw new AssertionError();
    }

    public static void setPolicy(ServerLevel level, Explosion explosion, List<BlockPos> positions,
                                 List<BlockPos> indirectlyAffectedPositions,
                                 boolean dropItems, Set<BlockPos> noItemDropPositions,
                                 Set<BlockPos> restoreContentsPositions) {
        POLICIES.put(level, new DropPolicy(explosion, positions, indirectlyAffectedPositions, dropItems, noItemDropPositions, restoreContentsPositions));
    }

    /**
     * Returns whether the drops of a block broken by a neighbor update (not directly by the
     * explosion) should be suppressed. Blocks whose support was destroyed (torches, rails, ...)
     * are broken by the game's neighbor-update logic and would otherwise drop their items,
     * effectively duplicating them since the blocks are healed back.
     */
    public static boolean shouldSuppressNeighborBreakDrops(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        DropPolicy policy = POLICIES.get(serverLevel);
        if (policy == null || policy.dropItems) {
            return false;
        }
        return policy.positions.contains(pos) || policy.indirectPositions.contains(pos);
    }

    public static void clearForLevel(ServerLevel level) {
        POLICIES.remove(level);
    }

    /**
     * Returns whether the drops of the block at the given position should be suppressed, and if so
     * removes its block entity when the contents will be restored later.
     */
    public static boolean shouldSuppressDrops(BlockGetter level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        DropPolicy policy = POLICIES.get(serverLevel);
        if (policy == null || !policy.positions.contains(pos)) {
            return false;
        }
        if (!policy.dropItems || policy.noItemDropPositions.contains(pos)) {
            if (policy.restoreContentsPositions.contains(pos)) {
                // Remove the block entity before the block is destroyed, so that container
                // contents are not scattered (they will be restored with the block).
                serverLevel.removeBlockEntity(pos);
            }
            return true;
        }
        return false;
    }

    /** Drop policy of the explosion that is currently finalizing. */
    private record DropPolicy(Explosion explosion, Set<BlockPos> positions, Set<BlockPos> indirectPositions,
                              boolean dropItems, Set<BlockPos> noItemDropPositions, Set<BlockPos> restoreContentsPositions) {
        private DropPolicy(Explosion explosion, List<BlockPos> positions, List<BlockPos> indirectlyAffectedPositions,
                           boolean dropItems, Set<BlockPos> noItemDropPositions, Set<BlockPos> restoreContentsPositions) {
            this(explosion, new HashSet<>(positions), new HashSet<>(indirectlyAffectedPositions),
                    dropItems, noItemDropPositions, restoreContentsPositions);
        }
    }
}
