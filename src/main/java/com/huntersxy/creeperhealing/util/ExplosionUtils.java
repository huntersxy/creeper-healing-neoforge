package com.huntersxy.creeperhealing.util;

import com.huntersxy.creeperhealing.blocks.AffectedBlock;
import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shared helpers for the healing process.
 */
public final class ExplosionUtils {

    private ExplosionUtils() {
        throw new AssertionError();
    }

    /** Pushes entities standing on a position up so that they are not trapped by the healed block. */
    public static void pushEntitiesUpwards(ServerLevel level, BlockPos pos, BlockState state, boolean isTallBlock) {
        if (!state.isSolid()) {
            return;
        }
        int amountToPush = isTallBlock ? 2 : 1;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), Entity::isAlive)) {
            if (areAboveBlocksFree(level, pos, entity, amountToPush)) {
                entity.teleportTo(entity.getX(), entity.getBlockY() + amountToPush, entity.getZ());
            }
        }
    }

    private static boolean areAboveBlocksFree(ServerLevel level, BlockPos pos, Entity entity, int amountToPush) {
        int entityTop = pos.getY() + (int) Math.ceil(entity.getEyeHeight());
        for (int i = pos.getY(); i < entityTop; i++) {
            BlockPos currentPos = pos.atY(i + amountToPush);
            if (level.getBlockState(currentPos).isSolid()) {
                return false;
            }
        }
        return true;
    }

    /** Filters a collection of positions down to those whose block should be healed. */
    public static List<BlockPos> filterPositionsToHeal(Collection<BlockPos> positions, Function<BlockPos, BlockState> positionToStateMapper) {
        List<BlockPos> affectedPositions = new ArrayList<>();
        boolean whitelistEnabled = CreeperHealingConfig.isWhitelistEnabled();
        List<String> whitelist = CreeperHealingConfig.getWhitelist();
        for (BlockPos affectedPosition : positions) {
            BlockState affectedState = positionToStateMapper.apply(affectedPosition);
            // Hardcoded exception. Place before all logic
            if (ExcludedBlocks.isExcluded(affectedState)) {
                continue;
            }
            boolean stateCannotHeal = affectedState.isAir() || affectedState.is(Blocks.TNT) || affectedState.is(BlockTags.FIRE);
            if (stateCannotHeal) {
                continue;
            }
            String affectedBlockIdentifier = BuiltInRegistries.BLOCK.getKey(affectedState.getBlock()).toString();
            boolean whitelistContainsIdentifier = whitelist.contains(affectedBlockIdentifier);
            if (!whitelistEnabled || whitelistContainsIdentifier) {
                affectedPositions.add(affectedPosition);
            }
        }
        return affectedPositions;
    }

    /**
     * Sorts affected blocks so that healing happens inwards from the edge of the explosion,
     * bottom to top, and opaque blocks before transparent ones.
     */
    public static @NotNull List<AffectedBlock> sortAffectedBlocks(@NotNull List<AffectedBlock> affectedBlocksList, ServerLevel level) {
        List<BlockPos> affectedBlocksAsPositions = affectedBlocksList.stream()
                .map(AffectedBlock::getBlockPos).collect(Collectors.toList());
        int centerX = getCenterXCoordinate(affectedBlocksAsPositions);
        int centerZ = getCenterZCoordinate(affectedBlocksAsPositions);
        Comparator<AffectedBlock> comparator = Comparator
                // Transparent blocks last (opaque blocks first)
                .comparingInt((AffectedBlock affectedBlock) -> affectedBlock.getBlockState().isTransparent() ? 1 : 0)
                // Bottom blocks first
                .thenComparingInt(affectedBlock -> affectedBlock.getBlockPos().getY())
                // Farthest from the center first
                .thenComparingInt(affectedBlock -> (int) -(Math.round(Math.pow(affectedBlock.getBlockPos().getX() - centerX, 2)
                        + Math.pow(affectedBlock.getBlockPos().getZ() - centerZ, 2))));
        List<AffectedBlock> sortedAffectedBlocks = new ArrayList<>(affectedBlocksList);
        sortedAffectedBlocks.sort(comparator);
        return sortedAffectedBlocks;
    }

    public static BlockPos calculateCenter(Collection<BlockPos> affectedPositions) {
        return new BlockPos(
                getCenterXCoordinate(affectedPositions),
                getCenterYCoordinate(affectedPositions),
                getCenterZCoordinate(affectedPositions));
    }

    public static int getCenterXCoordinate(Collection<BlockPos> affectedCoordinates) {
        int maxX = affectedCoordinates.stream().mapToInt(BlockPos::getX).max().orElse(0);
        int minX = affectedCoordinates.stream().mapToInt(BlockPos::getX).min().orElse(0);
        return (maxX + minX) / 2;
    }

    public static int getCenterYCoordinate(Collection<BlockPos> affectedCoordinates) {
        int maxY = affectedCoordinates.stream().mapToInt(BlockPos::getY).max().orElse(0);
        int minY = affectedCoordinates.stream().mapToInt(BlockPos::getY).min().orElse(0);
        return (maxY + minY) / 2;
    }

    public static int getCenterZCoordinate(Collection<BlockPos> affectedCoordinates) {
        int maxZ = affectedCoordinates.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        int minZ = affectedCoordinates.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        return (maxZ + minZ) / 2;
    }

    public static int getMaxExplosionRadius(Collection<BlockPos> affectedCoordinates) {
        int maxX = affectedCoordinates.stream().mapToInt(BlockPos::getX).max().orElse(0);
        int minX = affectedCoordinates.stream().mapToInt(BlockPos::getX).min().orElse(0);
        int maxY = affectedCoordinates.stream().mapToInt(BlockPos::getY).max().orElse(0);
        int minY = affectedCoordinates.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxZ = affectedCoordinates.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        int minZ = affectedCoordinates.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        return Math.max((maxX - minX) / 2, Math.max((maxY - minY) / 2, (maxZ - minZ) / 2));
    }

    public static void playBlockPlacementSoundEffect(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        boolean placementSoundEffectSetting = CreeperHealingConfig.blockPlacementSoundEffect();
        boolean doPlacementSoundEffect = placementSoundEffectSetting && !blockState.isAir();
        if (!doPlacementSoundEffect) {
            return;
        }
        level.playSound(null, blockPos, blockState.getSoundType().getPlaceSound(), SoundSource.BLOCKS,
                blockState.getSoundType().getVolume(), blockState.getSoundType().getPitch());
    }

    public static void spawnParticles(ServerLevel level, BlockPos blockPos) {
        boolean blockPlacementParticlesSetting = CreeperHealingConfig.blockPlacementParticles();
        if (!blockPlacementParticlesSetting) {
            return;
        }
        level.sendParticles(ParticleTypes.CLOUD, blockPos.getX(), blockPos.getY() + 2, blockPos.getZ(),
                1, 0, 1, 0, 0.001);
    }
}
