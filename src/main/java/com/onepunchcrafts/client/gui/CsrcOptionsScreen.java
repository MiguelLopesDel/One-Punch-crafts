package com.onepunchcrafts.client.gui;

import com.onepunchcrafts.client.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Live presentation options for Saitama and the Collapsing Star Roaring Cannon. Every widget writes the
 * client config immediately, so changes apply in real time (even mid-attack);
 * the file is persisted when the screen closes. Opened with the VFX options
 * key (default H) or through the mod list config button.
 */
public class CsrcOptionsScreen extends Screen {

    private final @Nullable Screen parent;

    public CsrcOptionsScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.onepunchcrafts.vfx_options.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 6 + 4;

        addRenderableWidget(CycleButton.<ClientConfig.SaitamaVfxProfile>builder(profile ->
                        Component.translatable("gui.onepunchcrafts.vfx_options.saitama_profile."
                                + profile.name().toLowerCase()))
                .withValues(ClientConfig.SaitamaVfxProfile.values())
                .withInitialValue(ClientConfig.SAITAMA_VFX_PROFILE.get())
                .create(x, y, 200, 20,
                        Component.translatable("gui.onepunchcrafts.vfx_options.saitama_profile"),
                        (button, value) -> ClientConfig.SAITAMA_VFX_PROFILE.set(value)));
        y += 28;

        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.CSRC_CASTER_BEAM_VIEW.get())
                .create(x, y, 200, 20,
                        Component.translatable("gui.onepunchcrafts.csrc_options.caster_view"),
                        (button, value) -> ClientConfig.CSRC_CASTER_BEAM_VIEW.set(value)));
        y += 24;

        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.CSRC_CINEMATIC_CAMERA.get())
                .create(x, y, 200, 20,
                        Component.translatable("gui.onepunchcrafts.csrc_options.cinematic_camera"),
                        (button, value) -> ClientConfig.CSRC_CINEMATIC_CAMERA.set(value)));
        y += 24;

        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.CSRC_REDUCED_FLASHES.get())
                .create(x, y, 200, 20,
                        Component.translatable("gui.onepunchcrafts.csrc_options.reduced_flashes"),
                        (button, value) -> ClientConfig.CSRC_REDUCED_FLASHES.set(value)));
        y += 24;

        addRenderableWidget(CycleButton.<ClientConfig.CsrcVoice>builder(voice ->
                        Component.translatable("gui.onepunchcrafts.csrc_options.voice." + voice.name().toLowerCase()))
                .withValues(ClientConfig.CsrcVoice.values())
                .withInitialValue(ClientConfig.CSRC_VOICE.get())
                .create(x, y, 200, 20,
                        Component.translatable("gui.onepunchcrafts.csrc_options.voice"),
                        (button, value) -> ClientConfig.CSRC_VOICE.set(value)));
        y += 24;

        addRenderableWidget(new VolumeSlider(x, y,
                "gui.onepunchcrafts.csrc_options.voice_volume", ClientConfig.CSRC_VOICE_VOLUME));
        y += 24;

        addRenderableWidget(new VolumeSlider(x, y,
                "gui.onepunchcrafts.csrc_options.music_volume", ClientConfig.CSRC_MUSIC_VOLUME));
        y += 28;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x, height - 28, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, title, width / 2, height / 6 - 12, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        ClientConfig.SPEC.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        // Changes apply live; keep the world running so the effect can be
        // watched while the sliders move.
        return false;
    }

    /** 0–200% slider bound directly to a config value (applies live). */
    private static class VolumeSlider extends AbstractSliderButton {
        private final String labelKey;
        private final ForgeConfigSpec.DoubleValue config;

        VolumeSlider(int x, int y, String labelKey, ForgeConfigSpec.DoubleValue config) {
            super(x, y, 200, 20, Component.empty(), Mth.clamp(config.get() / 2.0, 0.0, 1.0));
            this.labelKey = labelKey;
            this.config = config;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(labelKey, (int) Math.round(value * 200.0)));
        }

        @Override
        protected void applyValue() {
            config.set(value * 2.0);
        }
    }
}
