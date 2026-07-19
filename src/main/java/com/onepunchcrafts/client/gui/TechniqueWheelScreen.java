package com.onepunchcrafts.client.gui;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.PowerSetDefinition;
import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.client.Keybinding;
import com.onepunchcrafts.client.input.TechniqueWheelReleasePolicy;
import com.onepunchcrafts.client.power.TechniquePresentation;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.ActivateTechniqueIntentPacket;
import com.onepunchcrafts.network.packet.AdjustTechniqueIntentPacket;
import com.onepunchcrafts.network.packet.SelectTechniqueIntentPacket;
import com.onepunchcrafts.network.packet.SetTechniqueValueIntentPacket;
import com.onepunchcrafts.runtime.OnePunchRuntime;
import com.onepunchcrafts.runtime.state.PowerState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/** Hold-to-open radial selector. Releasing the selection key confirms the hovered technique. */
public final class TechniqueWheelScreen extends Screen {
    private static final int SLOT_SIZE = 52;
    private static final int ICON_SIZE = 48;
    private static final int COLOR_NORMAL = 0xF2E9E4D8;
    private static final int COLOR_CURRENT = 0xF2B8DFC0;
    private static final int COLOR_HOVERED = 0xFFF1B75E;
    private static final int COLOR_BORDER = 0xFF090909;
    private static final int COLOR_ENABLED = 0xFF4BAA68;
    private static final int COLOR_DISABLED = 0xFFB94B4B;
    private static final int CONTROL_WIDTH = 104;
    private static final int CONTROL_HEIGHT = 17;
    private static final int DETAILS_Y_OFFSET = -20;

    private final PowerState state;
    private final PowerSetDefinition powerSet;
    private int page;
    private int hovered = -1;
    private boolean confirmed;
    private boolean waitingForClick;
    private boolean pointerMoved;
    private double openingMouseX = Double.NaN;
    private double openingMouseY = Double.NaN;

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
        int radius = Mth.clamp(Math.min(width, height) / 4, 72, 112);
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
            TechniqueIconRenderer.draw(graphics, technique.presentation().icon(),
                    slotX + 2, slotY + 2, ICON_SIZE);
            renderSlotState(graphics, technique, slotX, slotY);
        }

        PowerSetDefinition.TechniquePage currentPage = powerSet.techniquePages().get(page);
        graphics.drawCenteredString(font, Component.translatable(currentPage.titleKey()), centerX,
                centerY - 40, 0xFFFFD27A);
        graphics.drawCenteredString(font, Component.translatable("technique.wheel.page",
                page + 1, powerSet.techniquePages().size()), centerX, 18, 0xFFD7D7D7);

        if (hovered >= 0) renderDetails(graphics,
                OnePunchRuntime.REGISTRIES.techniques.require(techniques.get(hovered)),
                centerX, centerY + DETAILS_Y_OFFSET);
        String hint = waitingForClick ? "technique.wheel.touchpad_hint" : "technique.wheel.hint";
        graphics.drawCenteredString(font, Component.translatable(hint), centerX,
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
        if (technique.activeAction() instanceof Technique.ActiveAction.Toggle toggle) {
            renderToggleControl(graphics, centerX, line + 1, state.tags().contains(toggle.tag()));
            return;
        }
        if (technique.activeAction() instanceof Technique.ActiveAction.Adjust adjust) {
            renderAdjustControl(graphics, centerX, line + 1, adjust);
            return;
        }
        var active = TechniquePresentation.active(technique);
        if (active.isPresent()) graphics.drawCenteredString(font, active.orElseThrow(), centerX, line, 0xFFFFFFFF);
    }

    private void renderSlotState(GuiGraphics graphics, Technique technique, int slotX, int slotY) {
        if (technique.activeAction() instanceof Technique.ActiveAction.Toggle toggle) {
            int color = state.tags().contains(toggle.tag()) ? COLOR_ENABLED : COLOR_DISABLED;
            graphics.fill(slotX + 3, slotY + SLOT_SIZE - 7, slotX + SLOT_SIZE - 3, slotY + SLOT_SIZE - 3, color);
        } else if (technique.activeAction() instanceof Technique.ActiveAction.Adjust adjust) {
            double ratio = adjustmentRatio(adjust);
            int left = slotX + 3;
            int right = slotX + SLOT_SIZE - 3;
            graphics.fill(left, slotY + SLOT_SIZE - 7, right, slotY + SLOT_SIZE - 3, 0xCC171717);
            graphics.fill(left, slotY + SLOT_SIZE - 7,
                    left + (int) Math.round((right - left) * ratio), slotY + SLOT_SIZE - 3, COLOR_HOVERED);
        }
    }

    private void renderToggleControl(GuiGraphics graphics, int centerX, int y, boolean enabled) {
        Bounds bounds = controlBounds(centerX, y);
        graphics.fill(bounds.left() - 1, bounds.top() - 1, bounds.right() + 1, bounds.bottom() + 1, COLOR_BORDER);
        graphics.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(),
                enabled ? COLOR_ENABLED : COLOR_DISABLED);
        graphics.drawCenteredString(font, Component.translatable(enabled
                ? "technique.wheel.enabled" : "technique.wheel.disabled"), centerX, y + 4, 0xFFFFFFFF);
    }

    private void renderAdjustControl(GuiGraphics graphics, int centerX, int y, Technique.ActiveAction.Adjust adjust) {
        Bounds bounds = controlBounds(centerX, y);
        int buttonWidth = 19;
        graphics.fill(bounds.left() - 1, bounds.top() - 1, bounds.right() + 1, bounds.bottom() + 1, COLOR_BORDER);
        graphics.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), 0xDD242424);
        graphics.fill(bounds.left(), bounds.top(), bounds.left() + buttonWidth, bounds.bottom(), 0xFFE0A84F);
        graphics.fill(bounds.right() - buttonWidth, bounds.top(), bounds.right(), bounds.bottom(), 0xFFE0A84F);
        graphics.drawCenteredString(font, "−", bounds.left() + buttonWidth / 2, y + 4, 0xFF101010);
        graphics.drawCenteredString(font, "+", bounds.right() - buttonWidth / 2, y + 4, 0xFF101010);

        int trackLeft = bounds.left() + buttonWidth + 4;
        int trackRight = bounds.right() - buttonWidth - 4;
        int trackY = y + CONTROL_HEIGHT / 2;
        graphics.fill(trackLeft, trackY - 2, trackRight, trackY + 2, 0xFF5A5A5A);
        int marker = trackLeft + (int) Math.round((trackRight - trackLeft) * adjustmentRatio(adjust));
        graphics.fill(marker - 2, trackY - 4, marker + 2, trackY + 4, 0xFFFFD27A);
    }

    private double adjustmentRatio(Technique.ActiveAction.Adjust adjust) {
        if (adjust.maximum() == adjust.minimum()) return 1.0;
        return Mth.clamp((state.attributes().base(adjust.attribute()) - adjust.minimum())
                / (adjust.maximum() - adjust.minimum()), 0.0, 1.0);
    }

    private void updateHovered(double mouseX, double mouseY) {
        if (Double.isNaN(openingMouseX)) {
            openingMouseX = mouseX;
            openingMouseY = mouseY;
        } else if (Math.hypot(mouseX - openingMouseX, mouseY - openingMouseY) >= 4.0) {
            pointerMoved = true;
        }
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        double distance = Math.sqrt(dx * dx + dy * dy);
        // Once a control Technique is focused, keep that focus while the pointer
        // travels inward from its radial slot to the central control.
        Technique focused = hoveredTechnique();
        if (focused != null && hasDirectControl(focused) && distance < 64) return;
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
        Technique hoveredTechnique = hoveredTechnique();
        if (hoveredTechnique != null
                && hoveredTechnique.activeAction() instanceof Technique.ActiveAction.Adjust) {
            adjust(hoveredTechnique, delta > 0 ? 1 : -1);
            return true;
        }
        page = Math.floorMod(page + (delta < 0 ? 1 : -1), powerSet.techniquePages().size());
        hovered = -1;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (handleControl(mouseX, mouseY)) return true;
            confirmAndClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean handleControl(double mouseX, double mouseY) {
        Technique technique = hoveredTechnique();
        if (technique == null) return false;
        int centerX = width / 2;
        int controlY = height / 2 + DETAILS_Y_OFFSET + font.lineHeight + 3;
        Bounds bounds = controlBounds(centerX, controlY);
        if (!bounds.contains(mouseX, mouseY)) return false;

        if (technique.activeAction() instanceof Technique.ActiveAction.Toggle toggle) {
            if (state.tags().contains(toggle.tag())) state.tags().remove(toggle.tag());
            else state.tags().add(toggle.tag());
            state.consumeDirty();
            NetworkRegister.sendToServer(new ActivateTechniqueIntentPacket(technique.id()));
            return true;
        }
        if (technique.activeAction() instanceof Technique.ActiveAction.Adjust) {
            int buttonWidth = 19;
            if (mouseX < bounds.left() + buttonWidth) adjust(technique, -1);
            else if (mouseX >= bounds.right() - buttonWidth) adjust(technique, 1);
            else setAdjustmentFromSlider(technique, mouseX, bounds, buttonWidth);
            return true;
        }
        return false;
    }

    private void adjust(Technique technique, int direction) {
        OnePunchRuntime.POWERS.adjust(state, technique.id(), direction);
        state.consumeDirty();
        NetworkRegister.sendToServer(new AdjustTechniqueIntentPacket(technique.id(), direction));
    }

    private void setAdjustmentFromSlider(Technique technique, double mouseX, Bounds bounds, int buttonWidth) {
        Technique.ActiveAction.Adjust adjustable = (Technique.ActiveAction.Adjust) technique.activeAction();
        double trackLeft = bounds.left() + buttonWidth + 4.0;
        double trackRight = bounds.right() - buttonWidth - 4.0;
        double ratio = Mth.clamp((mouseX - trackLeft) / (trackRight - trackLeft), 0.0, 1.0);
        double requested = adjustable.minimum() + ratio * (adjustable.maximum() - adjustable.minimum());
        double applied = OnePunchRuntime.POWERS.setAdjustment(state, technique.id(), requested);
        state.consumeDirty();
        NetworkRegister.sendToServer(new SetTechniqueValueIntentPacket(technique.id(), applied));
    }

    private static boolean hasDirectControl(Technique technique) {
        return technique.activeAction() instanceof Technique.ActiveAction.Toggle
                || technique.activeAction() instanceof Technique.ActiveAction.Adjust;
    }

    private Technique hoveredTechnique() {
        if (hovered < 0 || hovered >= techniques().size()) return null;
        return OnePunchRuntime.REGISTRIES.techniques.require(techniques().get(hovered));
    }

    private static Bounds controlBounds(int centerX, int y) {
        return new Bounds(centerX - CONTROL_WIDTH / 2, y,
                centerX + CONTROL_WIDTH / 2, y + CONTROL_HEIGHT);
    }

    private record Bounds(int left, int top, int right, int bottom) {
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (Keybinding.INSTANCE.CHANGE_SKILL.matches(keyCode, scanCode)) {
            releaseSelectionKey();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    public void releaseSelectionKey() {
        if (confirmed) return;
        switch (TechniqueWheelReleasePolicy.resolve(pointerMoved, hovered >= 0)) {
            case CONFIRM -> confirmAndClose();
            case CANCEL -> onClose();
            case KEEP_OPEN -> waitingForClick = true;
        }
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
