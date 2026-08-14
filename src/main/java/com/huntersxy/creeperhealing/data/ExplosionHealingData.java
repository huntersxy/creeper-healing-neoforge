package com.huntersxy.creeperhealing.data;

import com.huntersxy.creeperhealing.explosions.AbstractExplosionEvent;
import com.huntersxy.creeperhealing.explosions.ExplosionEvent;
import com.huntersxy.creeperhealing.managers.ExplosionManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-level {@link SavedData} that keeps scheduled explosion healings across server restarts.
 */
public class ExplosionHealingData extends SavedData {

    public static final String DATA_KEY = "creeperhealing_events";

    private final ExplosionManager manager;

    private ExplosionHealingData(ExplosionManager manager) {
        this.manager = manager;
    }

    public static ExplosionHealingData create(ExplosionManager manager) {
        return new ExplosionHealingData(manager);
    }

    public static ExplosionHealingData load(CompoundTag tag, ServerLevel level, ExplosionManager manager) {
        ExplosionHealingData data = new ExplosionHealingData(manager);
        ListTag events = tag.getList("events", Tag.TAG_COMPOUND);
        for (Tag eventTag : events) {
            if (!(eventTag instanceof CompoundTag compoundTag)) {
                continue;
            }
            ExplosionEvent event = AbstractExplosionEvent.load(compoundTag, level.registryAccess());
            if (event != null && !event.getAffectedBlocks().isEmpty()) {
                manager.getExplosionEvents().add(event);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag events = new ListTag();
        for (ExplosionEvent event : this.manager.getExplosionEvents()) {
            events.add(event.save(registries));
        }
        tag.put("events", events);
        return tag;
    }
}
