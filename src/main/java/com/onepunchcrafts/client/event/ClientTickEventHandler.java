package com.onepunchcrafts.client.event;

import com.onepunchcrafts.client.Keybinding;
import com.onepunchcrafts.client.gui.GuiDimension;
import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.skills.boros.BorosPack;
import com.onepunchcrafts.common.skills.saitama.ExtremeSpeed;
import com.onepunchcrafts.common.skills.saitama.NormalPunch;
import com.onepunchcrafts.common.skills.saitama.WeakPunch;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.AnimationPacket;
import com.onepunchcrafts.network.packet.BorosMovementInputPacket;
import com.onepunchcrafts.network.packet.SeriousFartPacket;
import com.onepunchcrafts.network.packet.SpecialSkillPacket;
import com.onepunchcrafts.network.packet.TeleportPacket;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickClientScheduler;
import com.onepunchcrafts.util.TickScheduler;
import com.onepunchcrafts.util.TickTask;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.onepunchcrafts.OnePunchCrafts.MODID;


@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientTickEventHandler {

    private static final List<Integer> tasks = new ArrayList<>();

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) throws Exception {
        TickClientScheduler.tick(event);
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean playerExist = player != null;
        buttonsManager(player, playerExist);
        if (event.phase == TickEvent.Phase.END) {
            sendBorosMovementInput(minecraft, player, playerExist);
        }
    }

    private static void sendBorosMovementInput(Minecraft minecraft, LocalPlayer player, boolean playerExist) {
        if (!playerExist || !(HelpUtility.getSkillData(player).getSkillPack() instanceof BorosPack)) return;
        if (minecraft.screen != null || minecraft.isPaused()) return;

        NetworkRegister.sendToServer(new BorosMovementInputPacket(
                minecraft.options.keyUp.isDown(),
                minecraft.options.keyDown.isDown(),
                minecraft.options.keyLeft.isDown(),
                minecraft.options.keyRight.isDown(),
                minecraft.options.keyJump.isDown(),
                minecraft.options.keyShift.isDown(),
                minecraft.options.keySprint.isDown()
        ));
    }

    private static void buttonsManager(LocalPlayer player, boolean playerExist) {
        if (Keybinding.INSTANCE.CHANGE_SKILL.consumeClick() && playerExist) {
            onKeyChangePressed();
        }
        if (Keybinding.INSTANCE.SPECIAL_CHANGE_SKILL.consumeClick() && playerExist) {
            onKeySpecialChangePressed();
        }
        if (Keybinding.INSTANCE.USE_SPECIAL_SKILL.consumeClick() && playerExist) {
            managerAnimation(player);
            onKeySpecialSkillPressed();
        }
        if (Keybinding.INSTANCE.USE_FART.isDown() && playerExist) {
            NetworkRegister.sendToServer(new SeriousFartPacket());
        }
        if (Keybinding.INSTANCE.USE_TELEPORT.consumeClick() && playerExist) {
            NetworkRegister.sendToServer(new TeleportPacket());
        }
        if (Keybinding.INSTANCE.OPEN_DIMENSIONS_GUI.consumeClick() && playerExist) {
            HelpUtility.verifyIsSaitamaAndGetCapability(player).ifPresent(cap ->
                    Minecraft.getInstance().setScreen(new GuiDimension(MutableComponent.create(new LiteralContents("Select Dimension"))))
            );
            NetworkRegister.sendToServer(new TeleportPacket());
        }
    }

    private static void onKeySpecialSkillPressed() {
        NetworkRegister.sendToServer(new SpecialSkillPacket());
    }

    private static void managerAnimation(LocalPlayer player) {
        HelpUtility.getSaitamaPack(player).ifPresent(one -> {
            if (one.getCurrentSkill() instanceof WeakPunch || one.getCurrentSkill() instanceof NormalPunch) {
                startAnimation(player, "multiple_punches");
                tasks.add(TickClientScheduler.scheduleFromHere(Duration.of(5, ChronoUnit.SECONDS), () -> stopAnimation(player)));
            }
        });
    }

    private static void stopAnimation(LocalPlayer player) {
        HelpUtility.getOneCraftAnimationLayer(player).ifPresent(animation -> animation.setAnimation(null));
    }

    private static void startAnimation(LocalPlayer player, String idAnimation) {
        HelpUtility.getOneCraftAnimationLayer(player).ifPresent(animation -> {
            KeyframeAnimationPlayer animation1 = new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation(MODID, idAnimation))).setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL).setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, false, false));
            if (!tasks.isEmpty())
                TickClientScheduler.cancelTask(tasks.get(0));
            tasks.clear();
            animation.setAnimation(animation1);
        });
    }

    private static void onKeySpecialChangePressed() {
        LocalPlayer player = Minecraft.getInstance().player;
        OnePunchPlayer data = HelpUtility.getSkillData(player);
        data.decideCurrentGroup(player);
        HelpUtility.syncDataWithServer(data);
    }

    private static void onKeyChangePressed() {
        LocalPlayer player = Minecraft.getInstance().player;
        OnePunchPlayer data = HelpUtility.getSkillData(player);
        data.decideCurrentSkill(player);
        HelpUtility.syncDataWithServer(data);
    }
}
