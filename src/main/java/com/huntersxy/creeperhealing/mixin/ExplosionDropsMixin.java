package com.huntersxy.creeperhealing.mixin;

import com.huntersxy.creeperhealing.util.ExplosionDropController;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Suppresses the item drops of blocks destroyed by explosions that are scheduled to be healed,
 * according to the mod's drop settings. The check happens inside
 * {@link BlockBehaviour.BlockStateBase#onExplosionHit} via {@code canDropFromExplosion}.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ExplosionDropsMixin {

    private ExplosionDropsMixin() {
    }

    @WrapOperation(method = "onExplosionHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canDropFromExplosion:(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;)Z"))
    private boolean creeperhealing$suppressExplosionDrops(BlockGetter level, BlockPos pos, Explosion explosion, Operation<Boolean> original) {
        if (ExplosionDropController.shouldSuppressDrops(level, pos)) {
            return false;
        }
        return original.call(level, pos, explosion);
    }
}
