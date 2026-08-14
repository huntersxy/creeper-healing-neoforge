package com.huntersxy.creeperhealing.handler;

import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.explosions.ExplosionSourceType;
import com.huntersxy.creeperhealing.managers.ExplosionManager;
import com.huntersxy.creeperhealing.managers.ExplosionManagerRegistry;
import com.huntersxy.creeperhealing.util.EmptyLevel;
import com.huntersxy.creeperhealing.util.ExcludedBlocks;
import com.huntersxy.creeperhealing.util.ExplosionContext;
import com.huntersxy.creeperhealing.util.ExplosionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures the blocks destroyed by an explosion while they are still present in the world
 * (via the {@link ExplosionEvent.Detonate} event), schedules their healing, and controls
 * item drops during the explosion (via {@link BlockEvent.DropItemsEvent}).
 *
 * <p>NeoForge fires {@code Detonate} before any block is destroyed and {@code End} after the
 * explosion finished, both synchronously on the server thread. The drop policy recorded at
 * {@code Detonate} therefore applies exactly to the drops of that explosion.
 */
public class ExplosionEventHandler {

    /** Drop policy of the currently-finalizing explosion, per level. */
    private final Map<ServerLevel, DropPolicy> activeDropPolicies = new HashMap<>();

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        Explosion explosion = event.getExplosion();
        ExplosionSourceType sourceType = ExplosionSourceType.classify(explosion);
        LivingEntity causingMob = explosion.getSourceMob();

        if (!CreeperHealingConfig.healsSource(sourceType, causingMob)) {
            return;
        }

        List<BlockPos> affectedPositions = ExplosionUtils.filterPositionsToHeal(
                new ArrayList<>(event.getAffectedBlocks()), serverLevel::getBlockState);
        if (affectedPositions.isEmpty()) {
            return;
        }

        List<BlockPos> indirectlyAffectedPositions = this.findIndirectlyAffectedPositions(serverLevel, affectedPositions);

        Map<BlockPos, ExplosionContext.BlockSnapshot> snapshots = new HashMap<>();
        for (BlockPos pos : affectedPositions) {
            snapshots.put(pos, this.snapshotBlock(serverLevel, pos));
        }
        for (BlockPos pos : indirectlyAffectedPositions) {
            snapshots.put(pos, this.snapshotBlock(serverLevel, pos));
        }

        // Drop policy for the finalizeExplosion phase of this explosion.
        boolean dropItems = CreeperHealingConfig.dropsItemsFor(sourceType, causingMob);
        Set<BlockPos> noItemDropPositions = new HashSet<>();
        Set<BlockPos> restoreContentsPositions = new HashSet<>();
        if (CreeperHealingConfig.restoreBlockNbt()) {
            for (BlockPos pos : affectedPositions) {
                ExplosionContext.BlockSnapshot snapshot = snapshots.get(pos);
                if (snapshot != null && snapshot.state().hasBlockEntity()) {
                    // The block item and its contents will be restored later, so neither should drop.
                    noItemDropPositions.add(pos);
                    restoreContentsPositions.add(pos);
                }
            }
        }
        this.activeDropPolicies.put(serverLevel, new DropPolicy(explosion, affectedPositions, dropItems, noItemDropPositions, restoreContentsPositions));

        int radius = ExplosionUtils.getMaxExplosionRadius(affectedPositions);
        BlockPos center = ExplosionUtils.calculateCenter(affectedPositions);
        ExplosionContext context = new ExplosionContext(affectedPositions, indirectlyAffectedPositions, snapshots,
                sourceType, serverLevel, radius, center);

        ExplosionManager manager = ExplosionManagerRegistry.get(serverLevel);
        if (manager != null) {
            manager.addExplosion(context);
        }
    }

    @SubscribeEvent
    public void onExplosionEnd(ExplosionEvent.End event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        DropPolicy policy = this.activeDropPolicies.get(serverLevel);
        if (policy != null && policy.explosion == event.getExplosion()) {
            this.activeDropPolicies.remove(serverLevel);
        }
    }

    @SubscribeEvent
    public void onBlockDropItems(BlockEvent.DropItemsEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        DropPolicy policy = this.activeDropPolicies.get(serverLevel);
        if (policy == null) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!policy.positions.contains(pos)) {
            return;
        }
        if (!policy.dropItems || policy.noItemDropPositions.contains(pos)) {
            event.setCanceled(true);
        }
        if (policy.restoreContentsPositions.contains(pos)) {
            // Remove the block entity before the destruction loop runs, so that container
            // contents are not scattered (they will be restored with the block).
            serverLevel.removeBlockEntity(pos);
        }
    }

    private ExplosionContext.BlockSnapshot snapshotBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        return new ExplosionContext.BlockSnapshot(state, blockEntity);
    }

    /**
     * Finds blocks that are not directly destroyed by the explosion but will lose their support
     * (for example sand floating above a destroyed block, or a torch on a wall that was blown up).
     * These are also healed so that nothing is left behind.
     */
    private List<BlockPos> findIndirectlyAffectedPositions(ServerLevel level, List<BlockPos> affectedPositions) {
        Set<BlockPos> affected = new HashSet<>(affectedPositions);

        // Only consider block positions with adjacent non-affected positions
        List<BlockPos> edgeAffectedPositions = new ArrayList<>();
        for (BlockPos affectedPosition : affectedPositions) {
            if (level.getBlockState(affectedPosition).isAir()) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = affectedPosition.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.isAir()) {
                    continue;
                }
                if (!affected.contains(neighborPos)) {
                    edgeAffectedPositions.add(affectedPosition);
                    break;
                }
            }
        }

        // Pass in a custom LevelReader that always returns air so that further checks with
        // BlockState#canSurvive are done in what will look like an empty world.
        EmptyLevel emptyLevel = new EmptyLevel(level);
        Set<BlockPos> newPositions = new HashSet<>();
        for (BlockPos filteredPosition : edgeAffectedPositions) {
            this.checkNeighbors(512, filteredPosition, newPositions, emptyLevel, level, affected);
        }
        return ExplosionUtils.filterPositionsToHeal(newPositions, level::getBlockState);
    }

    private void checkNeighbors(int maxCheckDepth, BlockPos currentPosition, Set<BlockPos> newPositions,
                                EmptyLevel emptyLevel, ServerLevel level, Set<BlockPos> affectedPositions) {
        if (maxCheckDepth <= 0) {
            return;
        }
        for (Direction neighborDirection : Direction.values()) {
            BlockPos neighborPos = currentPosition.relative(neighborDirection);
            BlockState neighborState = level.getBlockState(neighborPos);

            // If the block cannot be placed at an empty position also surrounded by air, then we
            // assume the block needs a supporting block to be placed.
            if (neighborState.isAir() || neighborState.canSurvive(emptyLevel, neighborPos) || affectedPositions.contains(neighborPos)) {
                continue;
            }
            if (newPositions.add(neighborPos)) {
                this.checkNeighbors(maxCheckDepth - 1, neighborPos, newPositions, emptyLevel, level, affectedPositions);
            }
        }
    }

    /** Drop policy of the explosion that is currently finalizing. */
    private record DropPolicy(Explosion explosion, Set<BlockPos> positions, boolean dropItems,
                              Set<BlockPos> noItemDropPositions, Set<BlockPos> restoreContentsPositions) {
        DropPolicy(Explosion explosion, List<BlockPos> positions, boolean dropItems,
                   Set<BlockPos> noItemDropPositions, Set<BlockPos> restoreContentsPositions) {
            this(explosion, new HashSet<>(positions), dropItems, noItemDropPositions, restoreContentsPositions);
        }
    }
}
