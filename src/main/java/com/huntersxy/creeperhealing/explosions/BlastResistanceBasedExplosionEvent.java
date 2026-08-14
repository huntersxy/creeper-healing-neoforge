package com.huntersxy.creeperhealing.explosions;

import com.huntersxy.creeperhealing.blocks.AffectedBlock;
import com.huntersxy.creeperhealing.blocks.SingleAffectedBlock;
import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Blast-resistance-based healing mode: blocks with a higher blast resistance take longer to
 * heal, and their delays receive a randomized offset, causing blocks to heal in bursts.
 */
public class BlastResistanceBasedExplosionEvent extends AbstractExplosionEvent {

    public BlastResistanceBasedExplosionEvent(List<AffectedBlock> affectedBlocks, int radius, BlockPos center) {
        super(affectedBlocks, radius, center);
    }

    public BlastResistanceBasedExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer, int blockCounter, int radius, BlockPos center) {
        super(affectedBlocks, healTimer, blockCounter, radius, center);
    }

    @Override
    protected ExplosionHealingMode getHealingMode() {
        return ExplosionHealingMode.BLAST_RESISTANCE_BASED_HEALING_MODE;
    }

    @Override
    public void setup(ServerLevel level) {
        RandomSource random = level.getRandom();
        for (AffectedBlock affectedBlock : this.getAffectedBlocks()) {
            if (!(affectedBlock instanceof SingleAffectedBlock singleAffectedBlock)) {
                continue;
            }
            double randomOffset = random.nextBetween(-2, 2);
            float blastResistance = singleAffectedBlock.getBlockState().getBlock().getExplosionResistance();
            double blastResistanceMultiplier = Math.min(blastResistance, 9);
            int offset = (int) (Mth.lerp(blastResistanceMultiplier / 9, -2, 2) + randomOffset);
            long finalOffset = Math.max(1, CreeperHealingConfig.getBlockPlacementDelayTicks() + (offset * 20L));
            singleAffectedBlock.setTimer(finalOffset);
        }
    }
}
