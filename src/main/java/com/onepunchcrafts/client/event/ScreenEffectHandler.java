package com.onepunchcrafts.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ScreenEffectHandler {
    private static float shakeIntensity = 0;
    private static int shakeTicks = 0;
    private static float targetFovMultiplier = 1.0f;
    private static float currentFovMultiplier = 1.0f;
    private static final Random random = new Random();

    public static void addEffect(float intensity, int duration, float fov) {
        shakeIntensity = intensity;
        shakeTicks = duration;
        targetFovMultiplier = fov;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (shakeTicks > 0) {
                shakeTicks--;
            } else {
                shakeIntensity = 0;
            }

            // Suaviza a transição do FOV
            currentFovMultiplier = Mth.lerp(0.1f, currentFovMultiplier, targetFovMultiplier);
            
            // Se o FOV alvo não for 1.0 e o tempo acabou, volta ao normal
            if (shakeTicks == 0 && targetFovMultiplier != 1.0f) {
                targetFovMultiplier = 1.0f;
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (currentFovMultiplier != 1.0f) {
            event.setFOV(event.getFOV() * currentFovMultiplier);
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks > 0 && shakeIntensity > 0) {
            float currentIntensity = shakeIntensity * ((float) shakeTicks / 10.0f); // Decai com o tempo
            if (currentIntensity > shakeIntensity) currentIntensity = shakeIntensity;

            event.setPitch(event.getPitch() + (random.nextFloat() - 0.5f) * currentIntensity);
            event.setYaw(event.getYaw() + (random.nextFloat() - 0.5f) * currentIntensity);
            event.setRoll(event.getRoll() + (random.nextFloat() - 0.5f) * currentIntensity);
        }
    }
}
