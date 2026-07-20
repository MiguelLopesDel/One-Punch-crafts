package com.onepunchcrafts.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.onepunchcrafts.client.Keybinding;
import com.onepunchcrafts.client.gui.CsrcOptionsScreen;
import com.onepunchcrafts.client.gui.GuiDimension;
import com.onepunchcrafts.client.gui.TechniqueWheelScreen;
import com.onepunchcrafts.client.input.TechniqueSelectorKeyState;
import com.onepunchcrafts.client.power.SaitamaClientSystem;
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
import com.onepunchcrafts.network.packet.ActivateTechniqueIntentPacket;
import com.onepunchcrafts.network.packet.SwapTechniqueIntentPacket;
import com.onepunchcrafts.runtime.state.PowerState;
import com.onepunchcrafts.runtime.OnePunchRuntime;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.onepunchcrafts.OnePunchCrafts.MODID;


@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientTickEventHandler {

    private static final List<Integer> tasks = new ArrayList<>();
    private static final int TECHNIQUE_WHEEL_HOLD_TICKS = 4;
    private static boolean techniqueKeyWasDown;
    private static boolean techniqueWheelOpened;
    private static int techniqueKeyHeldTicks;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) throws Exception {
        TickClientScheduler.tick(event);
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean playerExist = player != null;
        if (event.phase == TickEvent.Phase.END) {
            manageTechniqueSelector(minecraft, player);
            buttonsManager(player, playerExist);
            sendBorosMovementInput(minecraft, player, playerExist);
            if (playerExist) SaitamaClientSystem.tick(player);
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
        if (Keybinding.INSTANCE.OPEN_CSRC_OPTIONS.consumeClick() && playerExist) {
            Minecraft.getInstance().setScreen(new CsrcOptionsScreen(null));
        }
        if (Keybinding.INSTANCE.OPEN_DIMENSIONS_GUI.consumeClick() && playerExist) {
            if (HelpUtility.hasSaitamaPowerSet(player) || HelpUtility.verifyIsSaitamaAndGetCapability(player).isPresent())
                Minecraft.getInstance().setScreen(new GuiDimension(MutableComponent.create(new LiteralContents("Select Dimension"))));
            NetworkRegister.sendToServer(new TeleportPacket());
        }
        if (Keybinding.INSTANCE.USE_DIMENSIONAL_PUNCH.consumeClick() && playerExist
                && (HelpUtility.hasSaitamaPowerSet(player) || HelpUtility.verifyIsSaitamaAndGetCapability(player).isPresent())) {
            Minecraft.getInstance().setScreen(GuiDimension.forDimensionalPunch(
                    MutableComponent.create(new LiteralContents("Dimensional Punch"))));
        }
    }

    private static void manageTechniqueSelector(Minecraft minecraft, LocalPlayer player) {
        if (player == null) {
            resetTechniqueKey();
            return;
        }
        PowerState state = HelpUtility.getSkillData(player).getPowerState();
        if (state.powerSetId().equals(PowerState.NONE)) {
            resetTechniqueKey();
            if (Keybinding.INSTANCE.CHANGE_SKILL.consumeClick()) onKeyChangePressed();
            return;
        }

        while (Keybinding.INSTANCE.CHANGE_SKILL.consumeClick()) { /* edge is represented by isDown below */ }
        boolean down = TechniqueSelectorKeyState.resolve(
                Keybinding.INSTANCE.CHANGE_SKILL.isDown(),
                minecraft.screen instanceof TechniqueWheelScreen,
                isTechniqueKeyPhysicallyDown(minecraft));
        if (down) {
            if (!techniqueKeyWasDown) {
                techniqueKeyHeldTicks = 0;
                techniqueWheelOpened = false;
            } else {
                techniqueKeyHeldTicks++;
            }
            if (!techniqueWheelOpened && techniqueKeyHeldTicks >= TECHNIQUE_WHEEL_HOLD_TICKS
                    && minecraft.screen == null) {
                minecraft.setScreen(new TechniqueWheelScreen(state));
                techniqueWheelOpened = true;
            }
        } else if (techniqueKeyWasDown) {
            if (techniqueWheelOpened) {
                if (minecraft.screen instanceof TechniqueWheelScreen wheel) wheel.releaseSelectionKey();
            } else {
                if (state.abilities().previousTechnique() != null) {
                    OnePunchRuntime.POWERS.swapPrevious(state);
                    state.consumeDirty();
                }
                NetworkRegister.sendToServer(new SwapTechniqueIntentPacket());
            }
            resetTechniqueKey();
        }
        techniqueKeyWasDown = down;
    }

    private static boolean isTechniqueKeyPhysicallyDown(Minecraft minecraft) {
        InputConstants.Key key = Keybinding.INSTANCE.CHANGE_SKILL.getKey();
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        if (key.getType() == InputConstants.Type.KEYSYM)
            return InputConstants.isKeyDown(window, key.getValue());
        if (key.getType() == InputConstants.Type.SCANCODE) {
            for (int keyCode = GLFW.GLFW_KEY_SPACE; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++) {
                if (GLFW.glfwGetKeyScancode(keyCode) == key.getValue())
                    return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            }
            return false;
        }
        return false;
    }

    private static void resetTechniqueKey() {
        techniqueKeyWasDown = false;
        techniqueWheelOpened = false;
        techniqueKeyHeldTicks = 0;
    }

    private static void onKeySpecialSkillPressed() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            PowerState state = HelpUtility.getSkillData(player).getPowerState();
            if (!state.powerSetId().equals(PowerState.NONE)) {
                NetworkRegister.sendToServer(new ActivateTechniqueIntentPacket(state.abilities().selectedTechnique()));
                return;
            }
        }
        NetworkRegister.sendToServer(new SpecialSkillPacket());
    }

    private static void managerAnimation(LocalPlayer player) {
        PowerState state = HelpUtility.getSkillData(player).getPowerState();
        if (!state.powerSetId().equals(PowerState.NONE)
                && (state.abilities().selectedTechnique().equals(com.onepunchcrafts.content.SaitamaContent.WEAK_PUNCH)
                || state.abilities().selectedTechnique().equals(com.onepunchcrafts.content.SaitamaContent.NORMAL_PUNCH))) {
            startAnimation(player, "multiple_punches");
            tasks.add(TickClientScheduler.scheduleFromHere(Duration.of(5, ChronoUnit.SECONDS), () -> stopAnimation(player)));
            return;
        }
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
        if (!data.getPowerState().powerSetId().equals(PowerState.NONE)) return;
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
