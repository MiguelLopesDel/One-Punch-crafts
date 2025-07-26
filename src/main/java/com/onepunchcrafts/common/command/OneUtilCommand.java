package com.onepunchcrafts.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.LevelSyncPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.onepunchcrafts.OnePunchCrafts.*;
import static com.onepunchcrafts.util.HelpUtility.removeSaitamaEffectsSet;

public class OneUtilCommand {

    private static final String target = "target";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        Predicate<CommandSourceStack> require = cont -> cont.hasPermission(2);

        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> target = Commands.argument(OneUtilCommand.target, EntityArgument.player());

        dispatcher.register(Commands.literal("oneutil").requires(require)
                .then(target
                        .then(Commands.literal("setissaitama").then(
                                Commands.argument("isSaitama", BoolArgumentType.bool()).executes(setSaitama())
                        ))
                )
                .then(Commands.literal("extremejump")
                        .then(Commands.literal("set")
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg(1, 500))
                                        .then(Commands.argument("y", DoubleArgumentType.doubleArg(1, 500))
                                                .then(Commands.argument("z", DoubleArgumentType.doubleArg(1, 500))
                                                        .executes(setStrength())
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("get").executes(getStrength()))
                        .then(Commands.literal("reset").executes(resetStrength()))

                )
        );
    }

    private static Command<CommandSourceStack> getStrength() {
        return arguments -> {
            CommandSourceStack source = arguments.getSource();
            source.getLevel().getCapability(WORLD_RULES_CAPABILITY).ifPresent(cap ->
                    source.sendSuccess(() -> MutableComponent.create(new PlainTextContents.LiteralContents("limits are " +
                            cap.getMaxStrength().stream().map(String::valueOf).collect(Collectors.joining(" ")))
                    ), false)
            );
            return 1;
        };
    }

    private static Command<CommandSourceStack> resetStrength() {
        return arguments -> {
            CommandSourceStack source = arguments.getSource();
            source.getLevel().getCapability(WORLD_RULES_CAPABILITY).ifPresent(cap -> {
                        cap.resetMaxStrength();
                        NetworkRegister.sendToAllClients(new LevelSyncPacket((CompoundTag) cap.writeNBT()));
                    }
            );
            source.sendSuccess(() -> MutableComponent.create(new PlainTextContents.LiteralContents("sucess")), false);
            return 1;
        };
    }

    private static Command<CommandSourceStack> setStrength() {
        return arguments -> {
            CommandSourceStack source = arguments.getSource();
            Double x = arguments.getArgument("x", Double.class);
            Double y = arguments.getArgument("y", Double.class);
            Double z = arguments.getArgument("z", Double.class);
            source.getLevel().getCapability(WORLD_RULES_CAPABILITY).ifPresent(cap -> {
                        cap.changeStrength(Arrays.asList(x, y, z));
                        NetworkRegister.sendToAllClients(new LevelSyncPacket((CompoundTag) cap.writeNBT()));
                    }
            );
            source.sendSuccess(() -> MutableComponent.create(new PlainTextContents.LiteralContents("sucess")), false);
            return 1;
        };
    }

    private static Command<CommandSourceStack> setSaitama() {
        return arguments -> {
            CommandSourceStack source = arguments.getSource();
            boolean isSaitama = arguments.getArgument("isSaitama", Boolean.class);
            ServerPlayer player = arguments.getArgument(target, EntitySelector.class).findSinglePlayer(source);
            SkillPack skillPack = isSaitama ? new SaitamaPack() : WITHOUT_PACK;
            OnePunchPlayer cap = player.getCapability(ONE_PLAYER_CAPABILITY).orElse(new OnePunchPlayer(skillPack));
            cap.setSkillPack(skillPack);
            if (!isSaitama) {
                HelpUtility.setAttributesToDefault(player);
                removeSaitamaEffectsSet(player);
            }
            HelpUtility.syncWithPlayer(player, cap);
            source.sendSuccess(() -> MutableComponent.create(new PlainTextContents.LiteralContents("sucess")), false);
            return 1;
        };
    }
}