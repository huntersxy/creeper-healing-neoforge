//? if >=1.21.8 {
/*package com.huntersxy.creeperhealing.gametest;

import com.huntersxy.creeperhealing.CreeperHealing;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = CreeperHealing.MOD_ID)
public class GameTestRegistrarModern {

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.TEST_FUNCTION)) {
            return;
        }
        event.register(Registries.TEST_FUNCTION, id("test_mod_loads"), () -> CreeperHealingGameTestsModern::testModLoads);
        event.register(Registries.TEST_FUNCTION, id("test_tnt_healing"), () -> CreeperHealingGameTestsModern::testTntHealing);
    }

    @SubscribeEvent
    public static void registerGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> env = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf());
        registerTest(event, env, "test_mod_loads", 100);
        registerTest(event, env, "test_tnt_healing", 400);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("creeperhealing", path);
    }

    private static void registerTest(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> env, String name, int maxTicks) {
        ResourceKey<Consumer<GameTestHelper>> functionKey = ResourceKey.create(Registries.TEST_FUNCTION, id(name));
        event.registerTest(
                id(name),
                new FunctionGameTestInstance(
                        functionKey,
                        new TestData<>(env, id("empty"), maxTicks, 0, true)));
    }
}
*///?}
