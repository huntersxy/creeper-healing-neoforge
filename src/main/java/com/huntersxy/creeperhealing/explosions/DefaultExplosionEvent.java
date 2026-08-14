package com.huntersxy.creeperhealing.explosions;

import com.huntersxy.creeperhealing.blocks.AffectedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * The default healing mode: blocks are healed after the configured delays, one at a time.
 */
public class DefaultExplosionEvent extends AbstractExplosionEvent {

    public DefaultExplosionEvent(List<AffectedBlock> affectedBlocks, int radius, BlockPos center) {
        super(affectedBlocks, radius, center);
    }

    public DefaultExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer, int blockCounter, int radius, BlockPos center) {
        super(affectedBlocks, healTimer, blockCounter, radius, center);
    }

    @Override
    protected ExplosionHealingMode getHealingMode() {
        return ExplosionHealingMode.DEFAULT_MODE;
    }

    @Override
    public void setup(ServerLevel level) {
    }
}
