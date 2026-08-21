//? if >=1.21.8 {
/*package com.huntersxy.creeperhealing.gametest;

import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.managers.ExplosionManagerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/^*
 * Modern GameTests for >=1.21.8 (new TestEnvironment/TestInstance registry).
 * Mirrors BlockDynamics's BlockDynamicsGameTestsModern.
 ^/
public class CreeperHealingGameTestsModern {

    public static void testModLoads(GameTestHelper helper) {
        helper.succeed();
    }

    public static void testTntHealing(GameTestHelper helper) {
        CreeperHealingConfig.setDouble(CreeperHealingConfig.EXPLOSION_HEAL_DELAY, 0.05);
        CreeperHealingConfig.setDouble(CreeperHealingConfig.BLOCK_PLACEMENT_DELAY, 0.05);
        CreeperHealingConfig.setBoolean(CreeperHealingConfig.HEAL_MOB_EXPLOSIONS, true);
        CreeperHealingConfig.setBoolean(CreeperHealingConfig.HEAL_TNT_EXPLOSIONS, true);
        CreeperHealingConfig.setBoolean(CreeperHealingConfig.HEAL_OTHER_EXPLOSIONS, true);

        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
            }
        }
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 2, 2));

        var level = helper.getLevel();
        var creeper = helper.spawn(EntityType.CREEPER, new BlockPos(2, 3, 2));
        BlockPos explosionAbs = helper.absolutePos(new BlockPos(2, 3, 2));
        double ex = explosionAbs.getX() + 0.5;
        double ey = explosionAbs.getY() + 0.5;
        double ez = explosionAbs.getZ() + 0.5;

        level.explode(creeper, ex, ey, ez, 4.0F, Level.ExplosionInteraction.BLOCK);

        helper.runAfterDelay(2, () -> {
            var manager = ExplosionManagerRegistry.get(level);
            int count = manager != null ? manager.getExplosionEvents().size() : -1;
            System.out.println("[GameTest] After explode, manager events: " + count);
        });

        helper.succeedWhen(() -> {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 2, z));
                }
            }
        });
    }
}
*///?}
