package com.huntersxy.creeperhealing.managers;

import com.huntersxy.creeperhealing.blocks.AffectedBlock;
import com.huntersxy.creeperhealing.blocks.DoubleAffectedBlock;
import com.huntersxy.creeperhealing.blocks.SingleAffectedBlock;
import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.explosions.AbstractExplosionEvent;
import com.huntersxy.creeperhealing.explosions.BlastResistanceBasedExplosionEvent;
import com.huntersxy.creeperhealing.explosions.DaytimeExplosionEvent;
import com.huntersxy.creeperhealing.explosions.DefaultExplosionEvent;
import com.huntersxy.creeperhealing.explosions.DifficultyBasedExplosionEvent;
import com.huntersxy.creeperhealing.explosions.ExplosionEvent;
import com.huntersxy.creeperhealing.explosions.ExplosionHealingMode;
import com.huntersxy.creeperhealing.util.FallingBlockSuppressor;
import com.huntersxy.creeperhealing.util.ExplosionContext;
import com.huntersxy.creeperhealing.util.ExplosionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Per-level manager of scheduled explosion healings. Ticked once per level tick.
 */
public class ExplosionManager {

    private final ServerLevel level;
    private final List<ExplosionEvent> explosionEvents = new ArrayList<>();
    @Nullable
    private Runnable dirtyCallback;

    public ExplosionManager(ServerLevel level) {
        this.level = level;
    }

    /** Wired to the per-level {@link com.huntersxy.creeperhealing.data.ExplosionHealingData} so changes get persisted. */
    public void setDirtyCallback(Runnable dirtyCallback) {
        this.dirtyCallback = dirtyCallback;
    }

    private void markDirty() {
        if (this.dirtyCallback != null) {
            this.dirtyCallback.run();
        }
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public List<ExplosionEvent> getExplosionEvents() {
        return this.explosionEvents;
    }

    public void tick() {
        if (this.explosionEvents.isEmpty()) {
            return;
        }
        FallingBlockSuppressor.pruneSuppressions(this.level);
        for (ExplosionEvent explosionEvent : this.explosionEvents) {
            explosionEvent.tick(this.level);
        }
        if (this.explosionEvents.removeIf(ExplosionEvent::isFinished)) {
            this.markDirty();
        }
    }

    /**
     * Schedules the healing of the blocks destroyed by an explosion.
     */
    public void addExplosion(ExplosionContext context) {
        List<AffectedBlock> affectedBlocks = this.createAffectedBlocks(context);
        if (affectedBlocks.isEmpty()) {
            return;
        }
        AbstractExplosionEvent explosionEvent = this.createExplosionEvent(affectedBlocks, context.radius(), context.center());
        if (explosionEvent == null) {
            return;
        }
        this.markDirty();
        Set<ExplosionEvent> collidingExplosions = this.getCollidingExplosions(explosionEvent, context.affectedPositions());
        if (collidingExplosions.isEmpty()) {
            this.explosionEvents.add(explosionEvent);
        } else {
            this.explosionEvents.removeIf(collidingExplosions::contains);
            List<AffectedBlock> combinedAffectedBlocks = new ArrayList<>();
            collidingExplosions.forEach(collidingExplosion -> combinedAffectedBlocks.addAll(collidingExplosion.getAffectedBlocks()));
            combinedAffectedBlocks.addAll(affectedBlocks);
            AbstractExplosionEvent combinedEvent = this.createExplosionEvent(combinedAffectedBlocks,
                    explosionEvent.getHealTimer(), CreeperHealingConfig.getBlockPlacementDelayTicks(), 0);
            if (combinedEvent != null) {
                this.explosionEvents.add(combinedEvent);
            } else {
                this.explosionEvents.add(explosionEvent);
            }
        }
    }

    /** Updates the timers of all pending blocks to the configured block placement delay. */
    public void updateAffectedBlocksTimers() {
        for (ExplosionEvent explosionEvent : this.explosionEvents) {
            if (!(explosionEvent instanceof AbstractExplosionEvent abstractExplosionEvent)) {
                continue;
            }
            List<AffectedBlock> affectedBlocks = explosionEvent.getAffectedBlocks();
            for (int i = abstractExplosionEvent.getBlockCounter() + 1; i < affectedBlocks.size(); i++) {
                AffectedBlock currentAffectedBlock = affectedBlocks.get(i);
                if (currentAffectedBlock instanceof SingleAffectedBlock singleAffectedBlock) {
                    singleAffectedBlock.setTimer(CreeperHealingConfig.getBlockPlacementDelayTicks());
                }
            }
        }
    }

    /** Called when a splash potion of Healing or Regeneration hits an explosion's area. */
    public void onPotionSplash(BlockPos potionHitPosition, boolean instantHeal, boolean regeneration) {
        for (ExplosionEvent explosionEvent : this.explosionEvents) {
            boolean potionHitExplosion = explosionEvent.getAffectedBlocks().stream()
                    .anyMatch(affectedBlock -> affectedBlock.getBlockPos().equals(potionHitPosition));
            if (!potionHitExplosion || !(explosionEvent instanceof AbstractExplosionEvent abstractExplosionEvent)) {
                continue;
            }
            abstractExplosionEvent.setHealTimer(1);
            if (instantHeal) {
                explosionEvent.getAffectedBlocks().forEach(affectedBlock -> {
                    if (affectedBlock instanceof SingleAffectedBlock singleAffectedBlock) {
                        singleAffectedBlock.setTimer(1);
                    }
                });
            }
        }
    }

    // ---- internal helpers ----

    private List<AffectedBlock> createAffectedBlocks(ExplosionContext context) {
        List<AffectedBlock> affectedBlocks = new ArrayList<>();
        for (BlockPos pos : context.affectedPositions()) {
            if (affectedBlocks.stream().anyMatch(affectedBlock ->
                    affectedBlock.getBlockPos().equals(pos)
                            || (affectedBlock instanceof DoubleAffectedBlock doubleAffectedBlock
                            && doubleAffectedBlock.getSecondHalfPos().equals(pos)))) {
                continue;
            }
            ExplosionContext.BlockSnapshot snapshot = context.snapshots().get(pos);
            if (snapshot == null) {
                continue;
            }
            BlockPos otherHalfPos = DoubleAffectedBlock.getOtherHalfPos(pos, snapshot.state());
            if (otherHalfPos == null) {
                affectedBlocks.add(AffectedBlock.single(pos, snapshot.state(), snapshot.blockEntity(), this.level));
            } else {
                ExplosionContext.BlockSnapshot otherHalfSnapshot = context.snapshots().get(otherHalfPos);
                if (otherHalfSnapshot != null) {
                    affectedBlocks.add(AffectedBlock.doubleBlock(pos, snapshot.state(), snapshot.blockEntity(),
                            otherHalfPos, otherHalfSnapshot.state(), otherHalfSnapshot.blockEntity(), this.level));
                } else {
                    affectedBlocks.add(AffectedBlock.doubleBlock(pos, snapshot.state(), snapshot.blockEntity(),
                            otherHalfPos, null, null, this.level));
                }
            }
        }
        return ExplosionUtils.sortAffectedBlocks(affectedBlocks, this.level);
    }

    @Nullable
    private AbstractExplosionEvent createExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer, long blockHealDelay, int blockCounter) {
        if (affectedBlocks.isEmpty()) {
            return null;
        }
        List<BlockPos> affectedPositions = affectedBlocks.stream().map(AffectedBlock::getBlockPos).toList();
        BlockPos center = ExplosionUtils.calculateCenter(affectedPositions);
        int radius = ExplosionUtils.getMaxExplosionRadius(affectedPositions);
        List<AffectedBlock> sortedAffectedBlocks = ExplosionUtils.sortAffectedBlocks(affectedBlocks, this.level);
        sortedAffectedBlocks.forEach(affectedBlock -> {
            if (affectedBlock instanceof SingleAffectedBlock singleAffectedBlock) {
                singleAffectedBlock.setTimer(blockHealDelay);
            }
        });
        AbstractExplosionEvent explosionEvent = this.createExplosionEvent(sortedAffectedBlocks, healTimer, blockCounter, radius, center);
        if (explosionEvent != null) {
            explosionEvent.setup(this.level);
        }
        return explosionEvent;
    }

    @Nullable
    private AbstractExplosionEvent createExplosionEvent(List<AffectedBlock> affectedBlocks, int radius, BlockPos center) {
        ExplosionHealingMode healingMode = CreeperHealingConfig.getMode();
        AbstractExplosionEvent explosionEvent = switch (healingMode) {
            case DEFAULT_MODE -> new DefaultExplosionEvent(affectedBlocks, radius, center);
            case DAYTIME_HEALING_MODE -> new DaytimeExplosionEvent(affectedBlocks, radius, center);
            case DIFFICULTY_BASED_HEALING_MODE -> new DifficultyBasedExplosionEvent(affectedBlocks, radius, center);
            case BLAST_RESISTANCE_BASED_HEALING_MODE -> new BlastResistanceBasedExplosionEvent(affectedBlocks, radius, center);
        };
        explosionEvent.setup(this.level);
        return explosionEvent;
    }

    @Nullable
    private AbstractExplosionEvent createExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer, int blockCounter, int radius, BlockPos center) {
        ExplosionHealingMode healingMode = CreeperHealingConfig.getMode();
        return switch (healingMode) {
            case DEFAULT_MODE -> new DefaultExplosionEvent(affectedBlocks, healTimer, blockCounter, radius, center);
            case DAYTIME_HEALING_MODE -> new DaytimeExplosionEvent(affectedBlocks, healTimer, blockCounter, radius, center);
            case DIFFICULTY_BASED_HEALING_MODE -> new DifficultyBasedExplosionEvent(affectedBlocks, healTimer, blockCounter, radius, center);
            case BLAST_RESISTANCE_BASED_HEALING_MODE -> new BlastResistanceBasedExplosionEvent(affectedBlocks, healTimer, blockCounter, radius, center);
        };
    }

    /**
     * An explosion collides with another if the square of the distance between their centers is
     * less than or equal to the sum of their radii. Only explosions that have not started healing
     * yet are considered.
     */
    private Set<ExplosionEvent> getCollidingExplosions(ExplosionEvent newExplosionEvent, List<BlockPos> affectedPositions) {
        Set<ExplosionEvent> collidingExplosions = new LinkedHashSet<>();
        BlockPos newExplosionCenter;
        int newExplosionRadius;
        if (newExplosionEvent instanceof AbstractExplosionEvent abstractExplosionEvent) {
            newExplosionCenter = abstractExplosionEvent.getCenter();
            newExplosionRadius = abstractExplosionEvent.getRadius();
        } else {
            newExplosionRadius = ExplosionUtils.getMaxExplosionRadius(affectedPositions);
            newExplosionCenter = ExplosionUtils.calculateCenter(affectedPositions);
        }
        for (ExplosionEvent explosionEvent : this.explosionEvents) {
            boolean hasStartedHealing = explosionEvent.getHealTimer() <= 0;
            if (hasStartedHealing) {
                continue;
            }
            BlockPos currentExplosionCenter;
            int currentExplosionRadius;
            if (explosionEvent instanceof AbstractExplosionEvent abstractExplosionEvent) {
                currentExplosionCenter = abstractExplosionEvent.getCenter();
                currentExplosionRadius = abstractExplosionEvent.getRadius();
            } else {
                List<BlockPos> currentAffectedPositions = explosionEvent.getAffectedBlocks().stream().map(AffectedBlock::getBlockPos).toList();
                currentExplosionRadius = ExplosionUtils.getMaxExplosionRadius(currentAffectedPositions);
                currentExplosionCenter = ExplosionUtils.calculateCenter(currentAffectedPositions);
            }
            int combinedRadius = newExplosionRadius + currentExplosionRadius;
            double distanceBetweenCenters = Math.floor(Math.sqrt(newExplosionCenter.distSqr(currentExplosionCenter)));
            if (distanceBetweenCenters <= combinedRadius) {
                collidingExplosions.add(explosionEvent);
            }
        }
        return collidingExplosions;
    }
}
