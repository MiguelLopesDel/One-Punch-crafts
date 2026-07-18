package com.onepunchcrafts.client.power;

import com.onepunchcrafts.api.Technique;
import com.onepunchcrafts.client.Keybinding;
import com.onepunchcrafts.runtime.state.PowerState;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/** Client Adapter from domain presentation metadata to translated Minecraft text. */
public final class TechniquePresentation {
    private TechniquePresentation() {}

    public static Component name(Technique technique, PowerState state) {
        if (technique.activeAction() instanceof Technique.ActiveAction.Adjust adjust)
            return Component.translatable(technique.presentation().nameKey(),
                    (int) state.attributes().base(adjust.attribute()));
        return Component.translatable(technique.presentation().nameKey());
    }

    public static Optional<Component> primary(Technique technique) {
        return technique.presentation().primaryKey().map(key -> Component.translatable(
                "technique.input.primary", Component.translatable(key)));
    }

    public static Optional<Component> active(Technique technique) {
        if (technique.presentation().activeKey().isEmpty()) return Optional.empty();
        Component action = Component.translatable(technique.presentation().activeKey().orElseThrow());
        if (technique.activeAction() instanceof Technique.ActiveAction.Adjust)
            return Optional.of(Component.translatable("technique.input.adjust", action));
        return Optional.of(Component.translatable("technique.input.active",
                Keybinding.INSTANCE.USE_SPECIAL_SKILL.getTranslatedKeyMessage(), action));
    }

    public static boolean disabledToggle(Technique technique, PowerState state) {
        return technique.activeAction() instanceof Technique.ActiveAction.Toggle toggle
                && !state.tags().contains(toggle.tag());
    }
}
