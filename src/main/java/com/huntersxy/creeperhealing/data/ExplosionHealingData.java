package com.huntersxy.creeperhealing.data;

import com.huntersxy.creeperhealing.explosions.AbstractExplosionEvent;
import com.huntersxy.creeperhealing.explosions.ExplosionEvent;
import com.huntersxy.creeperhealing.managers.ExplosionManager;
//? if >=1.21.8 {
/*import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
*///?} else {
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
//?}

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
        //? if >=1.21.8 {
        /*ListTag events = tag.getListOrEmpty("events");
        *///?} else {
        ListTag events = tag.getList("events", Tag.TAG_COMPOUND);
        //?}
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

    //? if >=1.21.8 {
    /*public static Codec<ExplosionHealingData> codec(SavedData.Context context, ExplosionManager manager) {
        return CompoundTag.CODEC.flatXmap(
                tag -> {
                    ExplosionHealingData data = new ExplosionHealingData(manager);
                    ListTag events = tag.getListOrEmpty("events");
                    var registries = context.levelOrThrow().registryAccess();
                    for (Tag eventTag : events) {
                        if (!(eventTag instanceof CompoundTag compoundTag)) {
                            continue;
                        }
                        ExplosionEvent event = AbstractExplosionEvent.load(compoundTag, registries);
                        if (event != null && !event.getAffectedBlocks().isEmpty()) {
                            manager.getExplosionEvents().add(event);
                        }
                    }
                    return DataResult.success(data);
                },
                data -> {
                    CompoundTag tag = new CompoundTag();
                    ListTag events = new ListTag();
                    var registries = context.levelOrThrow().registryAccess();
                    for (ExplosionEvent event : data.manager.getExplosionEvents()) {
                        events.add(event.save(registries));
                    }
                    tag.put("events", events);
                    return DataResult.success(tag);
                }
        );
    }

    public static SavedDataType<ExplosionHealingData> type(ServerLevel level, ExplosionManager manager) {
        return new SavedDataType<>(
                DATA_KEY,
                ctx -> new ExplosionHealingData(manager),
                ctx -> codec(ctx, manager),
                DataFixTypes.SAVED_DATA_MAP_DATA
        );
    }
    *///?} else {
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag events = new ListTag();
        for (ExplosionEvent event : this.manager.getExplosionEvents()) {
            events.add(event.save(registries));
        }
        tag.put("events", events);
        return tag;
    }
    //?}
}
