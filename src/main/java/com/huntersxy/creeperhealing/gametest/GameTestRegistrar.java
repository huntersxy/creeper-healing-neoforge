//? if <1.21.8 {
package com.huntersxy.creeperhealing.gametest;

import com.huntersxy.creeperhealing.CreeperHealing;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = CreeperHealing.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GameTestRegistrar {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(CreeperHealingGameTests.class);
    }
}
//?}
