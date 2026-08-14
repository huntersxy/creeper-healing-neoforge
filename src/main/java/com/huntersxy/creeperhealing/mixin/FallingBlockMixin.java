package com.huntersxy.creeperhealing.mixin;

import com.huntersxy.creeperhealing.util.FallingBlockSuppressor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Allows healed falling blocks (sand, gravel, ...) to stay in place until they receive a
 * neighbor update, controlled by the {@code make_falling_blocks_fall} preference.
 * The suppression registry itself lives in {@link FallingBlockSuppressor}.
 */
@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin {

    private FallingBlockMixin() {
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FallingBlock;isFree(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean creeperhealing$preventScheduledFall(boolean original,
                                                        @Local(argsOnly = true) ServerLevel level,
                                                        @Local(argsOnly = true) BlockPos pos) {
        if (FallingBlockSuppressor.consumeSuppression(level, pos)) {
            return false;
        }
        return original;
    }
}
