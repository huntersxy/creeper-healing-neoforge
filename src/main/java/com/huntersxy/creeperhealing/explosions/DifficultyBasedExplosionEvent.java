package com.huntersxy.creeperhealing.explosions;

import com.huntersxy.creeperhealing.blocks.AffectedBlock;
import com.huntersxy.creeperhealing.blocks.SingleAffectedBlock;
import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;

import java.util.List;

/**
 * Difficulty-based healing mode: the healing is sped up or slowed down depending on
 * the difficulty of the world.
 */
public class DifficultyBasedExplosionEvent extends AbstractExplosionEvent {

    public DifficultyBasedExplosionEvent(List<AffectedBlock> affectedBlocks, int radius, BlockPos center) {
        super(affectedBlocks, radius, center);
    }

    public DifficultyBasedExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer, int blockCounter, int radius, BlockPos center) {
        super(affectedBlocks, healTimer, blockCounter, radius, center);
    }

    @Override
    public ExplosionHealingMode getHealingMode() {
        return ExplosionHealingMode.DIFFICULTY_BASED_HEALING_MODE;
    }

    @Override
    public void setup(ServerLevel level) {
        final int difficultyMultiplier = switch (level.getDifficulty()) {
            case PEACEFUL -> -2;
            case EASY -> -1;
            case NORMAL -> 1;
            case HARD -> 2;
        };
        long newBlockTimer = Math.max(1, CreeperHealingConfig.getBlockPlacementDelayTicks() + (difficultyMultiplier * 20L));
        long newExplosionTimer = Math.max(1, CreeperHealingConfig.getExplosionHealDelayTicks() + (difficultyMultiplier * 20L));
        this.healTimer = newExplosionTimer;
        for (AffectedBlock affectedBlock : this.getAffectedBlocks()) {
            if (!(affectedBlock instanceof SingleAffectedBlock singleAffectedBlock)) {
                continue;
            }
            singleAffectedBlock.setTimer(newBlockTimer);
        }
    }
}
