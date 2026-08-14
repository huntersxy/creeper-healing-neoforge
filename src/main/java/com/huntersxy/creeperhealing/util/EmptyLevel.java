package com.huntersxy.creeperhealing.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A {@link LevelReader} that reports every position as air. Used when calling
 * {@link BlockState#canSurvive(LevelReader, BlockPos)} to check whether a block needs a
 * supporting block, guaranteeing that all positions read from this instance are empty.
 */
public class EmptyLevel implements LevelReader {

    private final Level level;

    public EmptyLevel(Level level) {
        this.level = level;
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        return new EmptyLevelChunk(this.level, new ChunkPos(chunkX, chunkZ),
                this.level.getBiome(new BlockPos(chunkX * 16, 0, chunkZ * 16)));
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        return this.level.getHeight(heightmapType, x, z);
    }

    @Override
    public int getSkyDarken() {
        return 15;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.level.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return this.level.getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public int getSeaLevel() {
        return this.level.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return this.level.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.level.enabledFeatures();
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        return 0.0F;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable net.minecraft.world.entity.Entity entity, AABB box) {
        return List.of();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getMaxLocalRawBrightness(BlockPos pos) {
        return 15;
    }

    @Override
    public int getMaxLocalRawBrightness(BlockPos pos, int amount) {
        return 15;
    }
}
