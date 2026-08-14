package com.huntersxy.creeperhealing.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Allows healed falling blocks (sand, gravel, ...) to stay in place until they receive a
 * neighbor update, controlled by the {@code make_falling_blocks_fall} preference.
 *
 * <p>When the mod places a falling block with falling suppressed, the position is recorded here.
 * The scheduled fall tick then sees the suppression and skips the fall. Entries are consumed on
 * read and stale entries are pruned periodically by the explosion manager.
 */
@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin {

    /** level key -> (block pos -> game time when the suppression was recorded) */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> SUPPRESSED = new HashMap<>();

    private FallingBlockMixin() {
    }

    public static void suppressFall(ServerLevel level, BlockPos pos) {
        SUPPRESSED.computeIfAbsent(level.dimension(), key -> new HashMap<>()).put(pos.immutable(), level.getGameTime());
    }

    private static boolean consumeSuppression(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Long> entries = SUPPRESSED.get(level.dimension());
        if (entries == null) {
            return false;
        }
        Long recordedAt = entries.remove(pos.immutable());
        if (entries.isEmpty()) {
            SUPPRESSED.remove(level.dimension());
        }
        return recordedAt != null;
    }

    /** Removes suppressions that were never consumed (block broken before its fall tick, etc.). */
    public static void pruneSuppressions(ServerLevel level) {
        Map<BlockPos, Long> entries = SUPPRESSED.get(level.dimension());
        if (entries == null || entries.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > 100) {
                iterator.remove();
            }
        }
        if (entries.isEmpty()) {
            SUPPRESSED.remove(level.dimension());
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FallingBlock;canFallThrough(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean creeperhealing$preventScheduledFall(boolean original,
                                                        @Local(argsOnly = true) ServerLevel level,
                                                        @Local(argsOnly = true) BlockPos pos) {
        if (consumeSuppression(level, pos)) {
            return false;
        }
        return original;
    }
}
