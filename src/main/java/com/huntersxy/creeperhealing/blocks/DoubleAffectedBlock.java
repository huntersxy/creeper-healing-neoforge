package com.huntersxy.creeperhealing.blocks;

import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.explosions.ExplosionEvent;
import com.huntersxy.creeperhealing.util.FallingBlockSuppressor;
import com.huntersxy.creeperhealing.util.ExplosionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One half of a double block (double plants, beds) scheduled to be healed. Both halves are
 * healed together so that the block is valid again.
 */
public class DoubleAffectedBlock extends SingleAffectedBlock {

    private final BlockPos secondHalfPos;
    private final BlockState secondHalfState;
    @Nullable
    private final CompoundTag secondHalfNbt;

    protected DoubleAffectedBlock(BlockPos firstHalfPos, BlockState firstHalfState, @Nullable CompoundTag firstHalfNbt,
                                  @Nullable BlockPos secondHalfPos, @Nullable BlockState secondHalfState,
                                  @Nullable CompoundTag secondHalfNbt, long affectedBlockTimer, boolean placed) {
        super(firstHalfPos, firstHalfState, firstHalfNbt, affectedBlockTimer, placed);
        this.secondHalfNbt = secondHalfNbt;
        if (secondHalfState == null) {
            if (firstHalfState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                DoubleBlockHalf secondHalf = firstHalfState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                        ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER;
                this.secondHalfState = firstHalfState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, secondHalf);
            } else if (firstHalfState.hasProperty(BlockStateProperties.BED_PART)) {
                BedPart secondBedPart = firstHalfState.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD
                        ? BedPart.FOOT : BedPart.HEAD;
                this.secondHalfState = firstHalfState.setValue(BlockStateProperties.BED_PART, secondBedPart);
            } else {
                this.secondHalfState = null;
            }
        } else {
            this.secondHalfState = secondHalfState;
        }
        if (secondHalfPos == null) {
            this.secondHalfPos = getOtherHalfPos(firstHalfPos, firstHalfState);
        } else {
            this.secondHalfPos = secondHalfPos;
        }
    }

    public BlockState getSecondHalfState() {
        return this.secondHalfState;
    }

    public BlockPos getSecondHalfPos() {
        return this.secondHalfPos;
    }

    @Nullable
    public CompoundTag getSecondHalfNbt() {
        return this.secondHalfNbt;
    }

    @Override
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = super.save(registries);
        tag.putString("type", "double");
        if (this.secondHalfPos != null) {
            tag.putIntArray("second_half_pos", new int[]{this.secondHalfPos.getX(), this.secondHalfPos.getY(), this.secondHalfPos.getZ()});
        }
        if (this.secondHalfState != null) {
            tag.put("second_half_state", BlockState.CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), this.secondHalfState)
                    .result().orElseThrow());
        }
        if (this.secondHalfNbt != null) {
            tag.put("second_half_nbt", this.secondHalfNbt);
        }
        return tag;
    }

    @Override
    protected boolean shouldHealBlock(ServerLevel level) {
        return level.getBlockState(this.getBlockPos()).canBeReplaced()
                && level.getBlockState(this.secondHalfPos).canBeReplaced();
    }

    @Override
    protected boolean shouldForceHeal() {
        return this.getNbt() != null && this.getSecondHalfNbt() != null && CreeperHealingConfig.forceBlocksWithNbtToAlwaysHeal();
    }

    @Override
    protected void tryHealing(ServerLevel level, ExplosionEvent currentExplosionEvent) {
        if (this.secondHalfState == null) {
            super.tryHealing(level, currentExplosionEvent);
            return;
        }

        this.setPlaced();
        BlockState firstHalfState = this.getBlockState();
        BlockPos firstHalfPos = this.getBlockPos();
        BlockState secondHalfState = this.secondHalfState;
        BlockPos secondHalfPos = this.secondHalfPos;
        boolean stateReplaced = false;

        String blockIdentifier = BuiltInRegistries.BLOCK.getKey(firstHalfState.getBlock()).toString();
        String replaceMapValue = CreeperHealingConfig.getReplaceMap().get(blockIdentifier);
        // Hardcode an exception to allow beds to be replaced with other blocks despite them having an Nbt tag.
        if (replaceMapValue != null && (!this.shouldForceHeal() || firstHalfState.is(BlockTags.BEDS))) {
            //? if >=1.21.8 {
            /*Block replacementBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(replaceMapValue)).map(net.minecraft.core.Holder::value).orElse(null);
            *///?} else {
            Block replacementBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(replaceMapValue));
            //?}
            if (replacementBlock == null) {
                return;
            }
            firstHalfState = SingleAffectedBlock.copyMatchingProperties(firstHalfState, replacementBlock.defaultBlockState());
            secondHalfState = SingleAffectedBlock.copyMatchingProperties(secondHalfState, replacementBlock.defaultBlockState());
            stateReplaced = true;
        }

        // Prevent both halves of a double block from being replaced with two of a single regular block
        if (!firstHalfState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && !firstHalfState.hasProperty(BlockStateProperties.BED_PART)) {
            super.tryHealing(level, currentExplosionEvent);
            return;
        }

        if (!this.shouldHealBlock(level)) {
            return;
        }

        ExplosionUtils.pushEntitiesUpwards(level, firstHalfPos, firstHalfState, firstHalfState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF));
        boolean makeFallingBlocksFall = CreeperHealingConfig.makeFallingBlocksFall();
        if (!makeFallingBlocksFall) {
            if (firstHalfState.getBlock() instanceof FallingBlock) {
                FallingBlockSuppressor.suppressFall(level, firstHalfPos);
            }
            if (secondHalfState.getBlock() instanceof FallingBlock) {
                FallingBlockSuppressor.suppressFall(level, secondHalfPos);
            }
        }

        level.setBlock(firstHalfPos, firstHalfState, 3);
        level.setBlock(secondHalfPos, secondHalfState, 3);

        boolean healFirstHalfNbt = this.getNbt() != null && !stateReplaced;
        if (healFirstHalfNbt) {
            BlockEntity blockEntity = BlockEntity.loadStatic(firstHalfPos, firstHalfState, this.getNbt(), level.registryAccess());
            if (blockEntity != null) {
                level.setBlockEntity(blockEntity);
            }
        }
        boolean healSecondHalfNbt = this.secondHalfNbt != null && !stateReplaced;
        if (healSecondHalfNbt) {
            BlockEntity blockEntity = BlockEntity.loadStatic(secondHalfPos, secondHalfState, this.secondHalfNbt, level.registryAccess());
            if (blockEntity != null) {
                level.setBlockEntity(blockEntity);
            }
        }
        ExplosionUtils.playBlockPlacementSoundEffect(level, firstHalfPos, firstHalfState);
        ExplosionUtils.spawnParticles(level, firstHalfPos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoubleAffectedBlock that)) return false;
        return Objects.equals(getBlockPos(), that.getBlockPos())
                && Objects.equals(getBlockState(), that.getBlockState())
                && Objects.equals(this.getSecondHalfPos(), that.getSecondHalfPos())
                && Objects.equals(this.getSecondHalfState(), that.getSecondHalfState());
    }

    @Override
    public String toString() {
        return "DoubleAffectedBlock(firstHalfPos=%s, firstHalfState=%s, firstHalfNbt=%s, secondHalfPos=%s, secondHalfState=%s, secondHalfNbt=%s, timer=%s, placed=%s)"
                .formatted(this.getBlockPos(), this.getBlockState(), this.getNbt(), this.secondHalfPos, this.secondHalfState, this.secondHalfNbt, this.getBlockTimer(), this.isPlaced());
    }

    /** Returns the position of the other half of a double block, or null if the block is not double. */
    @Nullable
    public static BlockPos getOtherHalfPos(BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return getSecondDoubleBlockHalfPos(pos, state);
        } else if (state.hasProperty(BlockStateProperties.BED_PART)) {
            return getSecondBedBlockHalfPos(pos, state);
        }
        return null;
    }

    public static BlockPos getSecondDoubleBlockHalfPos(BlockPos firstHalfPos, BlockState firstHalfState) {
        return firstHalfState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                ? firstHalfPos.below() : firstHalfPos.above();
    }

    public static BlockPos getSecondBedBlockHalfPos(BlockPos firstHalfPos, BlockState firstHalfState) {
        BedPart secondBedPart = firstHalfState.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD
                ? BedPart.FOOT : BedPart.HEAD;
        Direction firstBedPartOrientation = firstHalfState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return switch (secondBedPart) {
            case HEAD -> switch (firstBedPartOrientation) {
                case NORTH -> firstHalfPos.north();
                case SOUTH -> firstHalfPos.south();
                case EAST -> firstHalfPos.east();
                default -> firstHalfPos.west();
            };
            case FOOT -> switch (firstBedPartOrientation) {
                case NORTH -> firstHalfPos.south();
                case SOUTH -> firstHalfPos.north();
                case EAST -> firstHalfPos.west();
                default -> firstHalfPos.east();
            };
        };
    }
}
