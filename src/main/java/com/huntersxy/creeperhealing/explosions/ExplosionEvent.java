package com.huntersxy.creeperhealing.explosions;

import com.huntersxy.creeperhealing.blocks.AffectedBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * A scheduled healing of the blocks destroyed by a single explosion.
 */
public interface ExplosionEvent {

    List<AffectedBlock> getAffectedBlocks();

    long getHealTimer();

    void setHealTimer(long timer);

    boolean isFinished();

    ExplosionHealingMode getHealingMode();

    /** Advances this event by one tick in the given level. */
    void tick(ServerLevel level);

    /** Applies mode-specific setup to the timers. */
    void setup(ServerLevel level);

    /** Serializes this event to NBT so that it can be resumed after a restart. */
    CompoundTag save(HolderLookup.Provider registries);
}
