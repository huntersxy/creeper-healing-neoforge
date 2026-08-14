package com.huntersxy.creeperhealing.util;

import com.huntersxy.creeperhealing.explosions.ExplosionSourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of an explosion, captured while the destroyed blocks are still in the world.
 */
public record ExplosionContext(
        List<BlockPos> affectedPositions,
        List<BlockPos> indirectlyAffectedPositions,
        Map<BlockPos, BlockSnapshot> snapshots,
        ExplosionSourceType sourceType,
        ServerLevel level,
        int radius,
        BlockPos center
) {

    /** The state (and optional block entity) of a block at the moment of the explosion. */
    public record BlockSnapshot(BlockState state, @Nullable BlockEntity blockEntity) {
    }
}
