package com.huntersxy.creeperhealing.commands;

import com.huntersxy.creeperhealing.config.CreeperHealingConfig;
import com.huntersxy.creeperhealing.explosions.ExplosionHealingMode;
import com.huntersxy.creeperhealing.managers.ExplosionManagerRegistry;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The {@code /creeper-healing} command tree. All settings of the mod can be queried and
 * changed in-game; changes are persisted to the configuration file. All commands require
 * operator permission.
 */
public class CreeperHealingCommands {

    private static final int REQUIRED_PERMISSION = 2;

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var root = Commands.literal("creeper-healing")
                .requires(source -> source.hasPermission(REQUIRED_PERMISSION))
                .then(this.reloadCommand())
                .then(this.modeCommand())
                .then(this.numberSetting(CreeperHealingConfig.EXPLOSION_HEAL_DELAY))
                .then(this.numberSetting(CreeperHealingConfig.BLOCK_PLACEMENT_DELAY));

        for (String key : BOOLEAN_SETTINGS) {
            root.then(this.booleanSetting(key));
        }
        for (String key : LIST_SETTINGS) {
            root.then(this.listSetting(key));
        }
        root.then(this.replaceMapCommand());
        event.getDispatcher().register(root);
    }

    private static final List<String> BOOLEAN_SETTINGS = List.of(
            CreeperHealingConfig.DROP_ITEMS_ON_MOB_EXPLOSIONS,
            CreeperHealingConfig.DROP_ITEMS_ON_BLOCK_EXPLOSIONS,
            CreeperHealingConfig.DROP_ITEMS_ON_TNT_EXPLOSIONS,
            CreeperHealingConfig.DROP_ITEMS_ON_TRIGGERED_EXPLOSIONS,
            CreeperHealingConfig.DROP_ITEMS_ON_OTHER_EXPLOSIONS,
            CreeperHealingConfig.HEAL_MOB_EXPLOSIONS,
            CreeperHealingConfig.HEAL_BLOCK_EXPLOSIONS,
            CreeperHealingConfig.HEAL_TNT_EXPLOSIONS,
            CreeperHealingConfig.HEAL_TRIGGERED_EXPLOSIONS,
            CreeperHealingConfig.HEAL_OTHER_EXPLOSIONS,
            CreeperHealingConfig.RESTORE_BLOCK_NBT,
            CreeperHealingConfig.FORCE_BLOCKS_WITH_NBT_TO_ALWAYS_HEAL,
            CreeperHealingConfig.MAKE_FALLING_BLOCKS_FALL,
            CreeperHealingConfig.BLOCK_PLACEMENT_SOUND_EFFECT,
            CreeperHealingConfig.BLOCK_PLACEMENT_PARTICLES,
            CreeperHealingConfig.HEAL_ON_HEALING_POTION_SPLASH,
            CreeperHealingConfig.HEAL_ON_REGENERATION_POTION_SPLASH,
            CreeperHealingConfig.ENABLE_WHITELIST);

    private static final List<String> LIST_SETTINGS = List.of(
            CreeperHealingConfig.DROP_ITEMS_ON_MOB_EXPLOSIONS_BLACKLIST,
            CreeperHealingConfig.HEAL_MOB_EXPLOSIONS_BLACKLIST,
            CreeperHealingConfig.WHITELIST);

    private LiteralArgumentBuilder<CommandSourceStack> reloadCommand() {
        return Commands.literal("reload_config")
                .executes(context -> {
                    CreeperHealingConfig.reload();
                    ExplosionManagerRegistry.all().forEach(manager -> manager.updateAffectedBlocksTimers());
                    sendFeedback(context, "Configuration reloaded from " + CreeperHealingConfig.FILE_NAME);
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSourceStack> modeCommand() {
        return Commands.literal("mode")
                .executes(context -> {
                    sendFeedback(context, "Current mode: " + CreeperHealingConfig.getMode().getName());
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("mode", StringArgumentType.word())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "mode");
                            ExplosionHealingMode mode = ExplosionHealingMode.getFromName(name);
                            if (mode.getName().equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                                CreeperHealingConfig.setMode(mode);
                                sendFeedback(context, "Healing mode set to: " + mode.getName());
                            } else {
                                sendFeedback(context, "Unknown mode: " + name + ". Valid modes: default_mode, daytime_healing_mode, difficulty_based_healing_mode, blast_resistance_based_healing_mode");
                            }
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> booleanSetting(String key) {
        String name = key.substring(key.indexOf('.') + 1);
        return Commands.literal(name)
                .executes(context -> {
                    sendFeedback(context, name + " = " + CreeperHealingConfig.getBoolean(key));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean value = BoolArgumentType.getBool(context, "value");
                            CreeperHealingConfig.setBoolean(key, value);
                            sendFeedback(context, name + " = " + value);
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> numberSetting(String key) {
        String name = key.substring(key.indexOf('.') + 1);
        return Commands.literal(name)
                .executes(context -> {
                    sendFeedback(context, String.format(Locale.ROOT, "%s = %.2f seconds", name, CreeperHealingConfig.getDouble(key)));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.05))
                        .executes(context -> {
                            double value = DoubleArgumentType.getDouble(context, "value");
                            CreeperHealingConfig.setDouble(key, value);
                            ExplosionManagerRegistry.all().forEach(manager -> manager.updateAffectedBlocksTimers());
                            sendFeedback(context, String.format(Locale.ROOT, "%s = %.2f seconds", name, value));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> listSetting(String key) {
        String name = key.substring(key.indexOf('.') + 1);
        return Commands.literal(name)
                .then(Commands.literal("add")
                        .then(Commands.argument("entry", StringArgumentType.word())
                                .executes(context -> {
                                    String entry = StringArgumentType.getString(context, "entry");
                                    if (CreeperHealingConfig.addToList(key, entry)) {
                                        sendFeedback(context, "Added " + entry + " to " + name);
                                    } else {
                                        sendFeedback(context, entry + " is already in " + name);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("remove")
                        .then(Commands.argument("entry", StringArgumentType.word())
                                .executes(context -> {
                                    String entry = StringArgumentType.getString(context, "entry");
                                    if (CreeperHealingConfig.removeFromList(key, entry)) {
                                        sendFeedback(context, "Removed " + entry + " from " + name);
                                    } else {
                                        sendFeedback(context, entry + " is not in " + name);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("list")
                        .executes(context -> {
                            sendFeedback(context, name + ": " + String.join(", ", CreeperHealingConfig.getStringList(key)));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> replaceMapCommand() {
        return Commands.literal("replace_map")
                .then(Commands.literal("add")
                        .then(Commands.argument("old_block", StringArgumentType.word())
                                .then(Commands.argument("new_block", StringArgumentType.word())
                                        .executes(context -> {
                                            String oldBlock = StringArgumentType.getString(context, "old_block");
                                            String newBlock = StringArgumentType.getString(context, "new_block");
                                            if (CreeperHealingConfig.addToReplaceMap(oldBlock, newBlock)) {
                                                sendFeedback(context, oldBlock + " will now be healed as " + newBlock);
                                            } else {
                                                sendFeedback(context, oldBlock + " is already in the replace map");
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("old_block", StringArgumentType.word())
                                .executes(context -> {
                                    String oldBlock = StringArgumentType.getString(context, "old_block");
                                    if (CreeperHealingConfig.removeFromReplaceMap(oldBlock)) {
                                        sendFeedback(context, "Removed " + oldBlock + " from the replace map");
                                    } else {
                                        sendFeedback(context, oldBlock + " is not in the replace map");
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("list")
                        .executes(context -> {
                            StringBuilder sb = new StringBuilder("Replace map:");
                            CreeperHealingConfig.getReplaceMap().forEach((oldBlock, newBlock) -> sb.append(' ').append(oldBlock).append('=').append(newBlock));
                            sendFeedback(context, sb.toString());
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private void sendFeedback(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), true);
    }
}
