package com.huntersxy.creeperhealing.explosions;

import com.huntersxy.creeperhealing.blocks.AffectedBlock;
import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Base implementation of a scheduled explosion healing. Blocks are healed one at a time,
 * starting from the edge of the explosion and going bottom-to-top, opaque blocks first.
 */
public abstract class AbstractExplosionEvent implements ExplosionEvent {

    private final List<AffectedBlock> affectedBlocks;
    protected long healTimer;
    private int blockCounter;
    protected boolean finished;
    private final int radius;
    private final BlockPos center;

    protected AbstractExplosionEvent(List<AffectedBlock> affectedBlocks, int radius, BlockPos center) {
        this(affectedBlocks, CreeperHealingConfig.getExplosionHealDelayTicks(), 0, radius, center);
    }

    protected AbstractExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer, int blockCounter, int radius, BlockPos center) {
        this.affectedBlocks = affectedBlocks;
        this.healTimer = healTimer;
        this.blockCounter = blockCounter;
        this.radius = radius;
        this.center = center;
    }

    @Override
    public List<AffectedBlock> getAffectedBlocks() {
        return this.affectedBlocks;
    }

    @Override
    public long getHealTimer() {
        return this.healTimer;
    }

    @Override
    public void setHealTimer(long timer) {
        this.healTimer = timer;
    }

    public int getBlockCounter() {
        return this.blockCounter;
    }

    public BlockPos getCenter() {
        return this.center;
    }

    public int getRadius() {
        return this.radius;
    }

    @Override
    public boolean isFinished() {
        return this.finished;
    }

    protected Optional<AffectedBlock> getCurrentAffectedBlock() {
        return this.blockCounter < this.affectedBlocks.size()
                ? Optional.of(this.affectedBlocks.get(this.blockCounter))
                : Optional.empty();
    }

    protected final void incrementCounter() {
        this.blockCounter++;
    }

    /**
     * Called before a block is healed; healing modes may decide that the event is finished
     * (for example, when no lit position remains in daytime healing mode).
     */
    protected void updateFinishedStatus(ServerLevel level) {
    }

    @Override
    public void tick(ServerLevel level) {
        if (this.isFinished()) {
            return;
        }
        this.healTimer--;
        if (this.healTimer >= 0) {
            return;
        }
        Optional<AffectedBlock> optionalAffectedBlock = this.getCurrentAffectedBlock();
        if (optionalAffectedBlock.isEmpty()) {
            this.finished = true;
            return;
        }
        AffectedBlock currentAffectedBlock = optionalAffectedBlock.get();
        if (currentAffectedBlock.isPlaced()) {
            this.incrementCounter();
            return;
        }
        if (!currentAffectedBlock.canBePlaced(level)) {
            this.delayAffectedBlock(currentAffectedBlock, level);
            return;
        }
        this.updateFinishedStatus(level);
        if (this.isFinished()) {
            return;
        }
        currentAffectedBlock.tick(this, level);
        if (currentAffectedBlock.getBlockTimer() < 0) {
            this.incrementCounter();
        }
    }

    /**
     * If the current affected block cannot be placed at this moment, find the next block that is
     * placeable in the list and swap them in the list. This effectively gives the delayed block
     * more chances to be placed until no more placeable blocks are found.
     * Examples include wall torches, vines, lanterns, candles, etc.
     */
    private void delayAffectedBlock(AffectedBlock affectedBlockToDelay, ServerLevel level) {
        int indexOfDelayedBlock = this.affectedBlocks.indexOf(affectedBlockToDelay);
        if (indexOfDelayedBlock < 0) {
            this.incrementCounter();
            affectedBlockToDelay.setPlaced();
            return;
        }
        int indexOfNextPlaceable = this.findNextPlaceableBlockIndex(level);
        if (indexOfNextPlaceable >= 0) {
            Collections.swap(this.affectedBlocks, indexOfDelayedBlock, indexOfNextPlaceable);
        } else {
            this.incrementCounter();
            affectedBlockToDelay.setPlaced();
        }
    }

    private int findNextPlaceableBlockIndex(ServerLevel level) {
        for (int i = this.blockCounter; i < this.affectedBlocks.size(); i++) {
            if (this.affectedBlocks.get(i).canBePlaced(level)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("mode", this.getHealingMode().getName());
        tag.putLong("heal_timer", this.healTimer);
        tag.putInt("block_counter", this.blockCounter);
        tag.putInt("radius", this.radius);
        tag.putIntArray("center", new int[]{this.center.getX(), this.center.getY(), this.center.getZ()});
        ListTag blocks = new ListTag();
        for (AffectedBlock affectedBlock : this.affectedBlocks) {
            blocks.add(affectedBlock.save(registries));
        }
        tag.put("blocks", blocks);
        return tag;
    }

    /** Reconstructs an event from its serialized form. */
    public static ExplosionEvent load(CompoundTag tag, HolderLookup.Provider registries) {
        ExplosionHealingMode mode = ExplosionHealingMode.getFromName(tag.getString("mode"));
        long healTimer = tag.getLong("heal_timer");
        int blockCounter = tag.getInt("block_counter");
        int radius = tag.getInt("radius");
        int[] center = tag.getIntArray("center");
        BlockPos centerPos = center.length == 3 ? new BlockPos(center[0], center[1], center[2]) : BlockPos.ZERO;
        List<AffectedBlock> blocks = new ArrayList<>();
        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (Tag blockTag : blockList) {
            AffectedBlock block = AffectedBlock.load((CompoundTag) blockTag, registries);
            if (block != null) {
                blocks.add(block);
            }
        }
        return switch (mode) {
            case DEFAULT_MODE -> new DefaultExplosionEvent(blocks, healTimer, blockCounter, radius, centerPos);
            case DAYTIME_HEALING_MODE -> new DaytimeExplosionEvent(blocks, healTimer, blockCounter, radius, centerPos);
            case DIFFICULTY_BASED_HEALING_MODE -> new DifficultyBasedExplosionEvent(blocks, healTimer, blockCounter, radius, centerPos);
            case BLAST_RESISTANCE_BASED_HEALING_MODE -> new BlastResistanceBasedExplosionEvent(blocks, healTimer, blockCounter, radius, centerPos);
        };
    }
}
