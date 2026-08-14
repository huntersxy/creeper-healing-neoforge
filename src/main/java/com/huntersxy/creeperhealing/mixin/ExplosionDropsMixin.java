package com.huntersxy.creeperhealing.mixin;

import com.huntersxy.creeperhealing.util.ExplosionDropController;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.BiConsumer;

/**
 * Suppresses the item drops of blocks destroyed by explosions that are scheduled to be healed,
 * according to the mod's drop settings.
 *
 * <p>NeoForge's explosion destruction path ({@link BlockBehaviour.BlockStateBase#onExplosionHit})
 * drops loot first and then destroys the block. When the drops of a position are suppressed, the
 * block is still destroyed (and its block entity removed beforehand when the contents will be
 * restored), so the world state matches a regular explosion.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ExplosionDropsMixin {

    private ExplosionDropsMixin() {
    }

    @WrapMethod(method = "onExplosionHit")
    private void creeperhealing$suppressExplosionDrops(Level level, BlockPos pos, Explosion explosion,
                                                       BiConsumer<ItemStack, BlockPos> dropConsumer, Operation<Void> original) {
        if (ExplosionDropController.shouldSuppressDrops(level, pos)) {
            // Destroy the block without dropping anything, mirroring the original flow.
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && explosion.getBlockInteraction() != Explosion.BlockInteraction.TRIGGER_BLOCK) {
                state.onBlockExploded(level, pos, explosion);
            }
            return;
        }
        original.call(level, pos, explosion, dropConsumer);
    }
}
