package com.huntersxy.creeperhealing.handler;

import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.managers.ExplosionManager;
import com.huntersxy.creeperhealing.managers.ExplosionManagerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.PotionEvent;

/**
 * Makes explosions heal faster when a splash potion of Healing or Regeneration is thrown on them.
 */
public class PotionEventHandler {

    @SubscribeEvent
    public void onPotionSplash(PotionEvent.PotionSplashEvent event) {
        ThrownPotion potion = event.getEntity();
        if (!potion.getItem().is(Items.SPLASH_POTION)) {
            return;
        }
        Level level = potion.level();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        HitResult hitResult = event.getHitResult();
        if (hitResult == null) {
            return;
        }
        BlockPos potionHitPosition = switch (hitResult.getType()) {
            case BLOCK -> ((BlockHitResult) hitResult).getBlockPos().relative(((BlockHitResult) hitResult).getDirection());
            case ENTITY -> ((EntityHitResult) hitResult).getEntity().blockPosition();
            case MISS -> null;
        };
        if (potionHitPosition == null) {
            return;
        }
        PotionContents potionContents = potion.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        boolean hasInstantHealth = potionContents.getEffects().stream()
                .anyMatch(instance -> instance.getEffect().is(MobEffects.HEALING));
        boolean hasRegeneration = potionContents.getEffects().stream()
                .anyMatch(instance -> instance.getEffect().is(MobEffects.REGENERATION));
        boolean healOnHealingPotion = CreeperHealingConfig.healOnHealingPotionSplash();
        boolean healOnRegenerationPotion = CreeperHealingConfig.healOnRegenerationPotionSplash();
        if (hasInstantHealth && healOnHealingPotion) {
            ExplosionManager manager = ExplosionManagerRegistry.get(serverLevel);
            if (manager != null) {
                manager.onPotionSplash(potionHitPosition, true, false);
            }
        } else if (hasRegeneration && healOnRegenerationPotion) {
            ExplosionManager manager = ExplosionManagerRegistry.get(serverLevel);
            if (manager != null) {
                manager.onPotionSplash(potionHitPosition, false, true);
            }
        }
    }
}
