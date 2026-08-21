package com.huntersxy.creeperhealing.handler;

import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.managers.ExplosionManager;
import com.huntersxy.creeperhealing.managers.ExplosionManagerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
//? if >=1.21.8 {
/*import net.minecraft.world.entity.projectile.ThrownSplashPotion;
*///?} else {
import net.minecraft.world.entity.projectile.ThrownPotion;
//?}
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * Makes explosions heal faster when a splash potion of Healing or Regeneration is thrown on them.
 */
public class PotionEventHandler {

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        //? if >=1.21.8 {
        /*if (!(projectile instanceof ThrownSplashPotion potion)) {
            return;
        }
        *///?} else {
        if (!(projectile instanceof ThrownPotion potion)) {
            return;
        }
        //?}
        if (!potion.getItem().is(Items.SPLASH_POTION)) {
            return;
        }
        Level level = potion.level();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        HitResult hitResult = event.getRayTraceResult();
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
        boolean hasInstantHealth = false;
        boolean hasRegeneration = false;
        for (MobEffectInstance effectInstance : potionContents.getAllEffects()) {
            //? if >=1.21.8 {
            /*if (effectInstance.getEffect().is(MobEffects.INSTANT_HEALTH)) {
            *///?} else {
            if (effectInstance.getEffect().is(MobEffects.HEAL)) {
            //?}
                hasInstantHealth = true;
            }
            if (effectInstance.getEffect().is(MobEffects.REGENERATION)) {
                hasRegeneration = true;
            }
        }
        boolean healOnHealingPotion = CreeperHealingConfig.healOnHealingPotionSplash();
        boolean healOnRegenerationPotion = CreeperHealingConfig.healOnRegenerationPotionSplash();
        ExplosionManager manager = ExplosionManagerRegistry.get(serverLevel);
        if (manager == null) {
            return;
        }
        if (hasInstantHealth && healOnHealingPotion) {
            manager.onPotionSplash(potionHitPosition, true, false);
        } else if (hasRegeneration && healOnRegenerationPotion) {
            manager.onPotionSplash(potionHitPosition, false, true);
        }
    }
}
