package com.huntersxy.creeperhealing.handler;

import com.huntersxy.creeperhealing.data.ExplosionHealingData;
import com.huntersxy.creeperhealing.managers.ExplosionManager;
import com.huntersxy.creeperhealing.managers.ExplosionManagerRegistry;
import net.minecraft.server.level.ServerLevel;
//? if >=1.21.8 {
/*import net.minecraft.world.level.saveddata.SavedDataType;
*///?} else {
import net.minecraft.world.level.saveddata.SavedData;
//?}
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Lifecycle of the per-level explosion managers: they are created when a level loads
 * (restoring scheduled healings from disk) and ticked every level tick.
 */
public class WorldEventHandler {

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ExplosionManager manager = new ExplosionManager(serverLevel);
        ExplosionManagerRegistry.register(serverLevel, manager);
        //? if >=1.21.8 {
        /*SavedDataType<ExplosionHealingData> type = ExplosionHealingData.type(serverLevel, manager);
        ExplosionHealingData data = serverLevel.getDataStorage().computeIfAbsent(type);
        *///?} else {
        SavedData.Factory<ExplosionHealingData> factory = new SavedData.Factory<>(
                () -> ExplosionHealingData.create(manager),
                (tag, provider) -> ExplosionHealingData.load(tag, serverLevel, manager),
                net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_MAP_DATA);
        ExplosionHealingData data = serverLevel.getDataStorage().computeIfAbsent(factory, ExplosionHealingData.DATA_KEY);
        //?}
        manager.setDirtyCallback(data::setDirty);
        manager.updateAffectedBlocksTimers();
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ExplosionManagerRegistry.unregister(serverLevel);
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ExplosionManager manager = ExplosionManagerRegistry.get(serverLevel);
        if (manager != null) {
            manager.tick();
        }
    }
}
