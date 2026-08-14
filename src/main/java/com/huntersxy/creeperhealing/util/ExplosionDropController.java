package com.huntersxy.creeperhealing.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls whether the blocks destroyed by a healable explosion should drop their items.
 *
 * <p>NeoForge fires {@code ExplosionEvent.Detonate} before any block is destroyed and the drops
 * (via {@code BlockState#onExplosionHit}) happen synchronously right afterwards, within the same
 * tick. A policy recorded at {@code Detonate} therefore covers the drops of that explosion.
 *
 * <p>The blocks of a healable explosion never drop their items: they are restored shortly after,
 * and dropping them as well would duplicate every destroyed block. The {@code drop_items_*}
 * settings therefore have no effect on healable explosions (they only describe vanilla behavior
 * for sources whose healing is disabled).
 *
 * <p>Policies are kept per level as a list: several explosions can finalize in the same tick
 * (for example a creeper chain where a second creeper explodes while the first is still
 * finalizing), and the drops of every one of them must be checked against the union of all
 * currently active policies. The list is cleared at the end of every level tick, after all
 * synchronous explosion drops have happened.
 */
public final class ExplosionDropController {

    private static final Map<ServerLevel, List<DropPolicy>> POLICIES = new ConcurrentHashMap<>();

    private ExplosionDropController() {
        throw new AssertionError();
    }

    public static void setPolicy(ServerLevel level, Explosion explosion, List<BlockPos> positions,
                                 List<BlockPos> indirectlyAffectedPositions,
                                 Set<BlockPos> restoreContentsPositions) {
        POLICIES.computeIfAbsent(level, key -> new ArrayList<>())
                .add(new DropPolicy(explosion, positions, indirectlyAffectedPositions, restoreContentsPositions));
    }

    /**
     * Returns whether the drops of a block broken by a neighbor update (not directly by the
     * explosion) should be suppressed. Blocks whose support was destroyed (torches, rails,
     * levers, ...) are broken by the game's neighbor-update logic and would otherwise drop their
     * items, effectively duplicating them since the blocks are healed back.
     */
    public static boolean shouldSuppressNeighborBreakDrops(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        List<DropPolicy> policies = POLICIES.get(serverLevel);
        if (policies == null || policies.isEmpty()) {
            return false;
        }
        return policies.stream()
                .anyMatch(policy -> policy.positions.contains(pos) || policy.indirectPositions.contains(pos));
    }

    /** Removes all drop policies of a level (called at the end of every level tick). */
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
        List<DropPolicy> policies = POLICIES.get(serverLevel);
        if (policies == null || policies.isEmpty()) {
            return false;
        }
        boolean suppress = false;
        for (DropPolicy policy : policies) {
            if (!policy.positions.contains(pos)) {
                continue;
            }
            suppress = true;
            if (policy.restoreContentsPositions.contains(pos)) {
                // Remove the block entity before the block is destroyed, so that container
                // contents are not scattered (they will be restored with the block).
                serverLevel.removeBlockEntity(pos);
            }
        }
        return suppress;
    }

    /** Drop policy of one healable explosion that is currently finalizing. */
    private record DropPolicy(Explosion explosion, Set<BlockPos> positions, Set<BlockPos> indirectPositions,
                              Set<BlockPos> restoreContentsPositions) {
        private DropPolicy(Explosion explosion, List<BlockPos> positions, List<BlockPos> indirectlyAffectedPositions,
                           Set<BlockPos> restoreContentsPositions) {
            this(explosion, new HashSet<>(positions), new HashSet<>(indirectlyAffectedPositions), restoreContentsPositions);
        }
    }
}
