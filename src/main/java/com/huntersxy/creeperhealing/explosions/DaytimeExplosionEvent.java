package com.huntersxy.creeperhealing.explosions;

import com.huntersxy.creeperhealing.blocks.AffectedBlock;
import com.huntersxy.creeperhealing.blocks.SingleAffectedBlock;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.GameRules;

import java.util.List;

/**
 * Daytime healing mode: explosions wait until sunrise to begin healing, and only heal while
 * (and where) there is a light source. The timer is recomputed every tick, so sleeping and
 * {@code /time} commands are handled automatically without needing extra hooks.
 */
public class DaytimeExplosionEvent extends AbstractExplosionEvent {

    public DaytimeExplosionEvent(List<AffectedBlock> affectedBlocks, int radius, BlockPos center) {
        super(affectedBlocks, radius, center);
    }

    public DaytimeExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer, int blockCounter, int radius, BlockPos center) {
        super(affectedBlocks, healTimer, blockCounter, radius, center);
    }

    @Override
    public ExplosionHealingMode getHealingMode() {
        return ExplosionHealingMode.DAYTIME_HEALING_MODE;
    }

    @Override
    public void setup(ServerLevel level) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            return;
        }
        this.healTimer = ticksUntilSunrise(level);
        int daylightBasedBlockPlacementDelay = (int) (13000 / Math.max(this.getAffectedBlocks().size(), 1));
        for (AffectedBlock affectedBlock : this.getAffectedBlocks()) {
            if (!(affectedBlock instanceof SingleAffectedBlock singleAffectedBlock)) {
                continue;
            }
            singleAffectedBlock.setTimer(daylightBasedBlockPlacementDelay);
        }
    }

    @Override
    public void tick(ServerLevel level) {
        if (level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            // Pull the timer down toward the next sunrise as time advances.
            // This handles sleeping and /time commands without extra event hooks.
            long target = ticksUntilSunrise(level);
            if (this.healTimer > target) {
                this.healTimer = target;
            }
        }
        super.tick(level);
    }

    private static long ticksUntilSunrise(ServerLevel level) {
        return SharedConstants.TICKS_PER_GAME_DAY - (level.getDayTime() % SharedConstants.TICKS_PER_GAME_DAY);
    }

    @Override
    public void updateFinishedStatus(ServerLevel level) {
        if (this.getBlockCounter() > 0) {
            return;
        }
        boolean sufficientLight = this.getAffectedBlocks().stream().anyMatch(affectedBlock -> {
            BlockPos pos = affectedBlock.getBlockPos();
            return level.getBrightness(LightLayer.BLOCK, pos) > 0 || level.getBrightness(LightLayer.SKY, pos) > 0;
        });
        if (!sufficientLight) {
            this.finished = true;
        }
    }
}
