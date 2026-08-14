package com.huntersxy.creeperhealing.handler;

import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.explosions.ExplosionSourceType;
import com.huntersxy.creeperhealing.managers.ExplosionManager;
import com.huntersxy.creeperhealing.managers.ExplosionManagerRegistry;
import com.huntersxy.creeperhealing.util.EmptyLevel;
import com.huntersxy.creeperhealing.util.ExplosionContext;
import com.huntersxy.creeperhealing.util.ExplosionDropController;
import com.huntersxy.creeperhealing.util.ExplosionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures the blocks destroyed by an explosion while they are still present in the world
 * (via the {@link ExplosionEvent.Detonate} event) and schedules their healing.
 *
 * <p>Item drop control for healable explosions is delegated to {@link ExplosionDropController},
 * which is consulted by the {@code ExplosionDropsMixin} during block destruction.
 */
public class ExplosionEventHandler {

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        Explosion explosion = event.getExplosion();
        ExplosionSourceType sourceType = ExplosionSourceType.classify(explosion);
        LivingEntity causingMob = explosion.getIndirectSourceEntity();

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

        // Drop policy for the finalizeExplosion phase of this explosion. The blocks of a healable
        // explosion never drop their items (they are restored shortly after; dropping them too
        // would duplicate every destroyed block), so the drop_items_* settings do not apply here.
        Set<BlockPos> restoreContentsPositions = new HashSet<>();
        if (CreeperHealingConfig.restoreBlockNbt()) {
            for (BlockPos pos : affectedPositions) {
                ExplosionContext.BlockSnapshot snapshot = snapshots.get(pos);
                if (snapshot != null && snapshot.state().hasBlockEntity()) {
                    // The block's contents will be restored later, so they should not drop
                    // (the block entity is removed before the block is destroyed).
                    restoreContentsPositions.add(pos);
                }
            }
        }
        ExplosionDropController.setPolicy(serverLevel, explosion, affectedPositions, indirectlyAffectedPositions,
                restoreContentsPositions);

        int radius = ExplosionUtils.getMaxExplosionRadius(affectedPositions);
        BlockPos center = ExplosionUtils.calculateCenter(affectedPositions);
        ExplosionContext context = new ExplosionContext(affectedPositions, indirectlyAffectedPositions, snapshots,
                sourceType, serverLevel, radius, center);

        ExplosionManager manager = ExplosionManagerRegistry.get(serverLevel);
        if (manager != null) {
            manager.addExplosion(context);
        }
    }

    /**
     * Suppresses the drops of blocks that are broken by a neighbor update instead of directly
     * by the explosion (e.g. torches or rails whose supporting block was destroyed). Without
     * this, their items would drop and the blocks would also be healed back, duplicating them.
     */
    @SubscribeEvent
    public void onBlockDrops(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (ExplosionDropController.shouldSuppressNeighborBreakDrops(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLevelTickEnd(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Any explosion whose Detonate fired before this point has already finished its drops.
        ExplosionDropController.clearForLevel(serverLevel);
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
}
