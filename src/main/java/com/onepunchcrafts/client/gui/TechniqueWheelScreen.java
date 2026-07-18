package com.onepunchcrafts.client.gui;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.PowerSetDefinition;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.client.Keybinding;
import com.onepunchcrafts.client.power.TechniquePresentation;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.SelectTechniqueIntentPacket;
import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.runtime.state.PowerState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/** Hold-to-open radial selector. Releasing the selection key confirms the hovered technique. */
public final class TechniqueWheelScreen extends Screen {
    private static final int SLOT_SIZE = 44;
    private static final int COLOR_NORMAL = 0xF2E9E4D8;
    private static final int COLOR_CURRENT = 0xF2B8DFC0;
    private static final int COLOR_HOVERED = 0xFFF1B75E;
    private static final int COLOR_BORDER = 0xFF090909;

    private final PowerState state;
    private final PowerSetDefinition powerSet;
    private int page;
    private int hovered = -1;
    private boolean confirmed;

    public TechniqueWheelScreen(PowerState state) {
        super(Component.translatable("technique.wheel.title"));
        this.state = state;
        this.powerSet = OnePunchRuntime.REGISTRIES.powerSets.require(state.powerSetId());
        this.page = Mth.clamp(state.abilities().selectedPage(), 0, powerSet.techniquePages().size() - 1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x99000000);
        updateHovered(mouseX, mouseY);

        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Mth.clamp(Math.min(width, height) / 4, 64, 105);
        List<Id> techniques = techniques();
        for (int index = 0; index < techniques.size(); index++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * index / techniques.size();
            int slotX = centerX + (int) Math.round(Math.cos(angle) * radius) - SLOT_SIZE / 2;
            int slotY = centerY + (int) Math.round(Math.sin(angle) * radius) - SLOT_SIZE / 2;
            Id id = techniques.get(index);
            Technique technique = OnePunchRuntime.REGISTRIES.techniques.require(id);
            int color = index == hovered ? COLOR_HOVERED
                    : id.equals(state.abilities().selectedTechnique()) ? COLOR_CURRENT : COLOR_NORMAL;
            graphics.fill(slotX - 2, slotY - 2, slotX + SLOT_SIZE + 2, slotY + SLOT_SIZE + 2, COLOR_BORDER);
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, color);
            Id icon = technique.presentation().icon();
            graphics.blit(new ResourceLocation(icon.namespace(), icon.path()), slotX + 5, slotY + 5,
                    0, 0, 34, 34, 64, 64);
        }

        PowerSetDefinition.TechniquePage currentPage = powerSet.techniquePages().get(page);
        graphics.drawCenteredString(font, Component.translatable(currentPage.titleKey()), centerX,
                centerY - 25, 0xFFFFD27A);
        graphics.drawCenteredString(font, Component.translatable("technique.wheel.page",
                page + 1, powerSet.techniquePages().size()), centerX, 18, 0xFFD7D7D7);

        if (hovered >= 0) renderDetails(graphics,
                OnePunchRuntime.REGISTRIES.techniques.require(techniques.get(hovered)), centerX, centerY - 8);
        graphics.drawCenteredString(font, Component.translatable("technique.wheel.hint"), centerX,
                height - 22, 0xFFB8B8B8);
    }

    private void renderDetails(GuiGraphics graphics, Technique technique, int centerX, int y) {
        int line = y;
        graphics.drawCenteredString(font, TechniquePresentation.name(technique, state), centerX, line, 0xFFFFFFFF);
        line += font.lineHeight + 2;
        var primary = TechniquePresentation.primary(technique);
        if (primary.isPresent()) {
            graphics.drawCenteredString(font, primary.orElseThrow(), centerX, line, 0xFFFFFFFF);
            line += font.lineHeight + 2;
        }
        var active = TechniquePresentation.active(technique);
        if (active.isPresent())
            graphics.drawCenteredString(font, active.orElseThrow(), centerX, line, 0xFFFFFFFF);
    }

    private void updateHovered(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 24) {
            hovered = -1;
            return;
        }
        int count = techniques().size();
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0;
        if (angle < 0) angle += Math.PI * 2.0;
        double slice = Math.PI * 2.0 / count;
        hovered = Math.floorMod((int) Math.floor((angle + slice / 2.0) / slice), count);
    }

    private List<Id> techniques() { return powerSet.techniquePages().get(page).techniques(); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0) return false;
        page = Math.floorMod(page + (delta < 0 ? 1 : -1), powerSet.techniquePages().size());
        hovered = -1;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            confirmAndClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (Keybinding.INSTANCE.CHANGE_SKILL.matches(keyCode, scanCode)) {
            confirmAndClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    public void confirmAndClose() {
        if (confirmed) return;
        confirmed = true;
        if (hovered >= 0) {
            Id selected = techniques().get(hovered);
            state.abilities().selectedPage(page);
            state.abilities().select(selected);
            state.consumeDirty();
            NetworkRegister.sendToServer(new SelectTechniqueIntentPacket(selected));
        }
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
