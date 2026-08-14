package com.huntersxy.creeperhealing.blocks;

import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.explosions.ExplosionEvent;
import com.huntersxy.creeperhealing.mixin.FallingBlockMixin;
import com.huntersxy.creeperhealing.util.ExplosionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single (non-double) block scheduled to be healed.
 */
public class SingleAffectedBlock implements AffectedBlock {

    private final BlockPos blockPos;
    private final BlockState blockState;
    @Nullable
    private final CompoundTag nbt;
    private long timer;
    private boolean placed;

    protected SingleAffectedBlock(BlockPos blockPos, BlockState blockState, @Nullable CompoundTag nbt, long timer, boolean placed) {
        this.blockPos = blockPos;
        this.blockState = blockState;
        this.nbt = nbt;
        this.timer = timer;
        this.placed = placed;
    }

    @Override
    public void setTimer(long delay) {
        this.timer = delay;
    }

    @Override
    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    @Override
    public BlockState getBlockState() {
        return this.blockState;
    }

    @Nullable
    public CompoundTag getNbt() {
        return this.nbt;
    }

    @Override
    public final void setPlaced() {
        this.placed = true;
    }

    @Override
    public boolean isPlaced() {
        return this.placed;
    }

    @Override
    public long getBlockTimer() {
        return this.timer;
    }

    @Override
    public void tick(ExplosionEvent explosionEvent, ServerLevel level) {
        this.timer--;
        if (this.timer >= 0) {
            return;
        }
        this.tryHealing(level, explosionEvent);
    }

    @Override
    public boolean canBePlaced(ServerLevel level) {
        if (this.shouldForceHeal()) {
            return true;
        }
        return this.getBlockState().canSurvive(level, this.getBlockPos());
    }

    @Override
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "single");
        tag.putIntArray("pos", new int[]{this.blockPos.getX(), this.blockPos.getY(), this.blockPos.getZ()});
        tag.put("state", BlockState.CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), this.blockState)
                .result().orElseThrow());
        if (this.nbt != null) {
            tag.put("nbt", this.nbt);
        }
        tag.putLong("timer", this.timer);
        tag.putBoolean("placed", this.placed);
        return tag;
    }

    protected void tryHealing(ServerLevel level, ExplosionEvent currentExplosionEvent) {

        this.setPlaced();
        BlockState state = this.getBlockState();
        BlockPos pos = this.getBlockPos();
        boolean stateReplaced = false;

        // Check if the block we are about to try placing is in the replace-map.
        // If it is, switch the state for the corresponding one in the replace-map.
        String blockIdentifier = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String replaceMapValue = CreeperHealingConfig.getReplaceMap().get(blockIdentifier);
        if (replaceMapValue != null && !this.shouldForceHeal()) {
            Block replacementBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(replaceMapValue));
            state = copyMatchingProperties(state, replacementBlock.defaultBlockState());
            stateReplaced = true;
        }

        if (!this.shouldHealBlock(level)) {
            return;
        }

        ExplosionUtils.pushEntitiesUpwards(level, pos, state, false);
        boolean makeFallingBlocksFall = CreeperHealingConfig.makeFallingBlocksFall();
        if (state.getBlock() instanceof FallingBlock && !makeFallingBlocksFall) {
            FallingBlockMixin.suppressFall(level, pos);
        }
        level.setBlock(pos, state, 3);
        this.handleChestBlockIfNeeded(currentExplosionEvent, state, pos, level);
        boolean healNbt = this.nbt != null && !stateReplaced;
        if (healNbt) {
            BlockEntity blockEntity = BlockEntity.loadStatic(pos, state, this.nbt, level.registryAccess());
            if (blockEntity != null) {
                level.setBlockEntity(blockEntity);
            }
        }
        ExplosionUtils.playBlockPlacementSoundEffect(level, pos, state);
        ExplosionUtils.spawnParticles(level, pos);
    }

    protected boolean shouldHealBlock(ServerLevel level) {
        if (this.shouldForceHeal()) {
            return true;
        }
        return level.getBlockState(this.blockPos).canBeReplaced();
    }

    protected boolean shouldForceHeal() {
        return this.nbt != null && CreeperHealingConfig.forceBlocksWithNbtToAlwaysHeal();
    }

    /**
     * When healing one half of a double chest, the other half has to be healed at the same time
     * so that the chest is valid again.
     */
    private void handleChestBlockIfNeeded(ExplosionEvent explosionEvent, BlockState blockState, BlockPos chestPos, ServerLevel level) {
        if (!blockState.is(Blocks.CHEST)) {
            return;
        }
        ChestType chestType = blockState.getValue(ChestBlock.TYPE);
        Direction facing = blockState.getValue(ChestBlock.FACING);
        BlockPos otherHalfPos = switch (chestType) {
            case SINGLE -> null;
            case LEFT -> switch (facing) {
                case NORTH -> chestPos.east();
                case EAST -> chestPos.south();
                case SOUTH -> chestPos.west();
                case WEST -> chestPos.north();
                default -> null;
            };
            case RIGHT -> switch (facing) {
                case NORTH -> chestPos.west();
                case EAST -> chestPos.north();
                case SOUTH -> chestPos.east();
                case WEST -> chestPos.south();
                default -> null;
            };
        };
        if (otherHalfPos == null) {
            return;
        }
        for (AffectedBlock affectedBlock : explosionEvent.getAffectedBlocks()) {
            if (!(affectedBlock instanceof SingleAffectedBlock singleAffectedBlock)) {
                continue;
            }
            if (singleAffectedBlock.isPlaced()) {
                continue;
            }
            BlockState affectedState = singleAffectedBlock.getBlockState();
            BlockPos affectedPosition = singleAffectedBlock.getBlockPos();
            if (!affectedState.is(Blocks.CHEST) || !affectedPosition.equals(otherHalfPos)) {
                continue;
            }
            singleAffectedBlock.tryHealing(level, explosionEvent);
        }
    }

    /** Copies the property values of the source state onto the target state where they exist. */
    public static BlockState copyMatchingProperties(BlockState source, BlockState target) {
        BlockState result = target;
        for (Map.Entry<Property<?>, Comparable<?>> entry : source.getValues().entrySet()) {
            Property<?> property = entry.getKey();
            if (result.hasProperty(property)) {
                result = setValueUnchecked(result, property, entry.getValue());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState setValueUnchecked(BlockState state, Property<T> property, Comparable<?> value) {
        return state.setValue(property, (T) value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SingleAffectedBlock that)) return false;
        return Objects.equals(getBlockPos(), that.getBlockPos())
                && Objects.equals(getBlockState(), that.getBlockState());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBlockPos(), getBlockState());
    }

    @Override
    public String toString() {
        return "SingleAffectedBlock(pos=%s, state=%s, nbt=%s, timer=%s, placed=%s)"
                .formatted(this.blockPos, this.blockState, this.nbt != null ? this.nbt : "null", this.timer, this.placed);
    }
}
