package com.onepunchcrafts.client.gui;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.presentation.VfxProfile;
import com.onepunchcrafts.client.power.TechniquePresentation;
import com.onepunchcrafts.content.SaitamaContent;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.SetVfxPreferenceIntentPacket;
import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.runtime.state.PowerState;
import com.onepunchcrafts.util.HelpUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Per-Technique A/B selector. The server stores the performer's choice. */
public final class SaitamaVfxOptionsScreen extends Screen {
    private static final int PAGE_SIZE = 8;
    private final Screen parent;
    private int page;

    public SaitamaVfxOptionsScreen(Screen parent) {
        super(Component.translatable("gui.onepunchcrafts.saitama_vfx.title"));
        this.parent = parent;
    }

    @Override protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets();
        PowerState state = state();
        int start = page * PAGE_SIZE;
        List<Id> visible = SaitamaContent.VFX_TECHNIQUES.subList(start,
                Math.min(start + PAGE_SIZE, SaitamaContent.VFX_TECHNIQUES.size()));
        int left = width / 2 - 206;
        int top = height / 2 - 76;
        for (int i = 0; i < visible.size(); i++) {
            Id techniqueId = visible.get(i);
            int x = left + (i % 2) * 212;
            int y = top + (i / 2) * 25;
            Component name = TechniquePresentation.name(
                    OnePunchRuntime.REGISTRIES.techniques.require(techniqueId), state);
            addRenderableWidget(CycleButton.<VfxProfile>builder(profile -> Component.translatable(
                            "gui.onepunchcrafts.vfx.profile." + profile.name().toLowerCase()))
                    .withValues(VfxProfile.values())
                    .withInitialValue(state.vfxPreferences().get(techniqueId))
                    .create(x, y, 200, 20, name, (button, profile) -> {
                        state.vfxPreferences().set(techniqueId, profile);
                        NetworkRegister.sendToServer(new SetVfxPreferenceIntentPacket(techniqueId, profile));
                    }));
        }
        int pages = (SaitamaContent.VFX_TECHNIQUES.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.floorMod(page - 1, pages); rebuild();
        }).bounds(width / 2 - 100, height - 53, 45, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.onepunchcrafts.vfx.page", page + 1, pages),
                button -> {}).bounds(width / 2 - 50, height - 53, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page = (page + 1) % pages; rebuild();
        }).bounds(width / 2 + 55, height - 53, 45, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private PowerState state() {
        if (minecraft == null || minecraft.player == null) throw new IllegalStateException("No local player");
        return HelpUtility.getSkillData(minecraft.player).getPowerState();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.onepunchcrafts.saitama_vfx.hint"),
                width / 2, 34, 0xFFBBBBBB);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
