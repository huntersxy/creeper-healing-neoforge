package com.huntersxy.creeperhealing.blocks;

import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.explosions.ExplosionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A block that was destroyed by an explosion and is scheduled to be healed.
 */
public interface AffectedBlock {

    BlockPos getBlockPos();

    BlockState getBlockState();

    long getBlockTimer();

    void setTimer(long delay);

    /** Advances this affected block by one tick; heals it once its timer has elapsed. */
    void tick(ExplosionEvent currentExplosion, ServerLevel level);

    void setPlaced();

    boolean isPlaced();

    /** Whether the block can currently be placed at its position. */
    boolean canBePlaced(ServerLevel level);

    CompoundTag save(HolderLookup.Provider registries);

    /** Creates a single-block affected block, capturing NBT data if configured. */
    static AffectedBlock single(BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ServerLevel level) {
        long blockPlacementDelay = CreeperHealingConfig.getBlockPlacementDelayTicks();
        boolean restoreBlockNbt = CreeperHealingConfig.restoreBlockNbt();
        CompoundTag nbt = blockEntity != null && restoreBlockNbt
                ? blockEntity.saveWithFullMetadata(level.registryAccess())
                : null;
        return new SingleAffectedBlock(pos, state, nbt, blockPlacementDelay, false);
    }

    /**
     * Creates an affected block for one half of a double block (double plants, beds).
     * If the other half was not destroyed, a matching state for it is derived.
     */
    static AffectedBlock doubleBlock(BlockPos firstHalfPos, BlockState firstHalfState, @Nullable BlockEntity firstHalfBlockEntity,
                                     @Nullable BlockPos secondHalfPos, @Nullable BlockState secondHalfState,
                                     @Nullable BlockEntity secondHalfBlockEntity, ServerLevel level) {
        long blockPlacementDelay = CreeperHealingConfig.getBlockPlacementDelayTicks();
        boolean restoreBlockNbt = CreeperHealingConfig.restoreBlockNbt();
        CompoundTag firstHalfNbt = firstHalfBlockEntity != null && restoreBlockNbt
                ? firstHalfBlockEntity.saveWithFullMetadata(level.registryAccess())
                : null;
        CompoundTag secondHalfNbt = secondHalfBlockEntity != null && restoreBlockNbt
                ? secondHalfBlockEntity.saveWithFullMetadata(level.registryAccess())
                : null;
        return new DoubleAffectedBlock(firstHalfPos, firstHalfState, firstHalfNbt, secondHalfPos, secondHalfState, secondHalfNbt, blockPlacementDelay, false);
    }

    /** Reconstructs an affected block from its serialized form; returns null on failure. */
    @Nullable
    static AffectedBlock load(CompoundTag tag, HolderLookup.Provider registries) {
        boolean isDouble = "double".equals(tag.getString("type"));
        int[] posArr = tag.getIntArray("pos");
        BlockPos pos = new BlockPos(posArr[0], posArr[1], posArr[2]);
        BlockState state = BlockState.CODEC.decode(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("state"))
                .result().map(pair -> pair.getFirst()).orElse(null);
        if (state == null) {
            return null;
        }
        CompoundTag nbt = tag.contains("nbt", net.minecraft.nbt.Tag.TAG_COMPOUND) ? tag.getCompound("nbt") : null;
        long timer = tag.getLong("timer");
        boolean placed = tag.getBoolean("placed");
        if (!isDouble) {
            return new SingleAffectedBlock(pos, state, nbt, timer, placed);
        }
        BlockPos secondHalfPos = null;
        if (tag.contains("second_half_pos")) {
            int[] halfArr = tag.getIntArray("second_half_pos");
            secondHalfPos = new BlockPos(halfArr[0], halfArr[1], halfArr[2]);
        }
        BlockState secondHalfState = null;
        if (tag.contains("second_half_state")) {
            secondHalfState = BlockState.CODEC.decode(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("second_half_state"))
                    .result().map(pair -> pair.getFirst()).orElse(null);
        }
        CompoundTag secondHalfNbt = tag.contains("second_half_nbt", net.minecraft.nbt.Tag.TAG_COMPOUND) ? tag.getCompound("second_half_nbt") : null;
        return new DoubleAffectedBlock(pos, state, nbt, secondHalfPos, secondHalfState, secondHalfNbt, timer, placed);
    }
}
