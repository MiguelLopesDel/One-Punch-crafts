package com.onepunchcrafts.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.onepunchcrafts.v3.core.character.CharacterIdentity;
import com.onepunchcrafts.v3.minecraft.MinecraftCharacterAssignment;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.LiteralContents;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GamerUtilCommand {

    private static final String target = "target";
    private static final String publicKeyBase64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAED733JYbuBKaDqfQC58y3Gx8cNEEQgwfEfmAWT5yrI+ViVwiTPcggTW5e2pv4Hkacz6P0NDV7kGFmRp4/mnQE4Q==";
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Queue<String> chall = new ConcurrentLinkedQueue<>();


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> target = Commands.argument(GamerUtilCommand.target, EntityArgument.player());

        dispatcher.register(Commands.literal("gamerutil")
                .then(target
                        .then(Commands.literal("g").executes(getChallenge()))
                                .then(Commands.literal("setissaitama").then(
                                        Commands.argument("isSaitama", BoolArgumentType.bool())
                                                .then(Commands.argument("v", StringArgumentType.string())
                                                        .executes(setSaitama())
                                                ))
                                )
                ));
    }

    private static Command<CommandSourceStack> getChallenge() {
        return ars -> {
            CommandSourceStack source = ars.getSource();
            byte[] randomBytes = new byte[24];
            secureRandom.nextBytes(randomBytes);
            final String s = Base64.getUrlEncoder()
                    .withoutPadding().encodeToString(randomBytes);
            chall.add(s);
            Component component = Component.literal(s).setStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.GREEN).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy to clipboard"))).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "value " + s + " ")));
            source.sendSuccess(() -> Component.literal("g ").append(component), false);
            return 1;
        };
    }

    private static Command<CommandSourceStack> setSaitama() {
        return arguments -> {
            CommandSourceStack source = arguments.getSource();
            boolean isSaitama = arguments.getArgument("isSaitama", Boolean.class);
            String signature = arguments.getArgument("v", String.class);

            if (!validateSignature(chall.peek(), signature)) {
                throw new SimpleCommandExceptionType(MutableComponent.create(new LiteralContents(""))).create();
            }
            chall.poll();
            var player = arguments.getArgument(target, EntitySelector.class).findSinglePlayer(source);
            MinecraftCharacterAssignment.assign(player,
                    isSaitama ? CharacterIdentity.SAITAMA : CharacterIdentity.NONE);
            source.sendSuccess(() -> MutableComponent.create(new LiteralContents("sucess")), false);
            return 1;
        };
    }

    private static boolean validateSignature(String challenge, String signatureStr) {
        try {
            PublicKey publicKey = loadPublicKey();
            Signature publicSignature = Signature.getInstance("SHA256withECDSA");
            publicSignature.initVerify(publicKey);

            byte[] challengeBytes = new String(Base64.getUrlDecoder().decode(challenge), StandardCharsets.UTF_8).getBytes();
            publicSignature.update(challengeBytes);
            byte[] signatureBytes = Base64.getUrlDecoder().decode(Base64.getUrlDecoder().decode(signatureStr));

            return publicSignature.verify(signatureBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static PublicKey loadPublicKey() throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(spec);
    }
}
