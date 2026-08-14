package com.huntersxy.creeperhealing;

import com.huntersxy.creeperhealing.commands.CreeperHealingCommands;
import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.handler.ExplosionEventHandler;
import com.huntersxy.creeperhealing.handler.PotionEventHandler;
import com.huntersxy.creeperhealing.handler.WorldEventHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Creeper Healing: a server-side mod that automatically and naturally heals explosions.
 *
 * <p>NeoForge port of creeper-healing by ArkoSammy12
 * (https://github.com/ArkoSammy12/creeper-healing), licensed under LGPL-2.1.
 */
@Mod(CreeperHealing.MOD_ID)
public class CreeperHealing {

    public static final String MOD_ID = "creeperhealing";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreeperHealing(IEventBus modEventBus, ModContainer modContainer) {
        CreeperHealingConfig.init();
        NeoForge.EVENT_BUS.register(new ExplosionEventHandler());
        NeoForge.EVENT_BUS.register(new WorldEventHandler());
        NeoForge.EVENT_BUS.register(new PotionEventHandler());
        NeoForge.EVENT_BUS.register(new CreeperHealingCommands());
        LOGGER.info("I will try my best to heal your explosions :)");
    }
}
