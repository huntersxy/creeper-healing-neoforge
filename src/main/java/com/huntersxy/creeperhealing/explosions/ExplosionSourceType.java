package com.huntersxy.creeperhealing.explosions;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.EntityType;

/**
 * Classification of the source of an explosion, mirroring the source types
 * used by the original creeper-healing mod.
 */
public enum ExplosionSourceType {

    MOB,
    BLOCK,
    TNT,
    TRIGGERED,
    OTHER;

    /**
     * Classifies the source of the given explosion based on the entity that caused it.
     */
    public static ExplosionSourceType classify(Explosion explosion) {
        Entity direct = explosion.getDirectSourceEntity();
        if (direct instanceof PrimedTnt || direct instanceof MinecartTNT) {
            return TNT;
        }
        if (direct instanceof Player) {
            // Bed / respawn anchor explosions are attributed to the player.
            return BLOCK;
        }
        if (direct instanceof EndCrystal) {
            return BLOCK;
        }
        if (direct instanceof LivingEntity) {
            return MOB;
        }
        if (direct instanceof Projectile) {
            EntityType<?> type = direct.getType();
            if (type == EntityType.WIND_CHARGE || type == EntityType.BREEZE_WIND_CHARGE) {
                return TRIGGERED;
            }
            // Fireballs and wither skulls count as mob explosions.
            return MOB;
        }
        return OTHER;
    }
}
