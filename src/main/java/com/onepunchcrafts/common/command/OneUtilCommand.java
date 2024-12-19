package com.onepunchcrafts.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.skills.SaitamaPack;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;
import static com.onepunchcrafts.OnePunchCrafts.WITHOUT_PACK;
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
                ));
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
            source.sendSuccess(() -> MutableComponent.create(new LiteralContents("sucess")), false);
            return 1;
        };
    }
}