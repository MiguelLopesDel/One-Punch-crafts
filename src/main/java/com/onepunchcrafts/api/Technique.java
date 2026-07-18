package com.onepunchcrafts.api;

import java.util.Objects;
import java.util.Optional;

/**
 * A selectable combat/control technique. Selection routes two independent
 * inputs: the primary attack and the explicit activation key.
 */
public record Technique(
        Id id,
        Optional<Id> primaryAbility,
        ActiveAction activeAction,
        Presentation presentation
) {
    public Technique {
        Objects.requireNonNull(id);
        primaryAbility = Objects.requireNonNull(primaryAbility);
        activeAction = Objects.requireNonNull(activeAction);
        presentation = Objects.requireNonNull(presentation);
    }

    public static Technique combat(Id id, String nameKey, Id ability,
                                   String primaryKey, String activeKey) {
        return new Technique(id, Optional.of(ability), new ActiveAction.Cast(ability),
                new Presentation(nameKey, Optional.of(primaryKey), Optional.of(activeKey), defaultIcon(id)));
    }

    public static Technique primary(Id id, String nameKey, Id ability, String primaryKey) {
        return new Technique(id, Optional.of(ability), new ActiveAction.None(),
                new Presentation(nameKey, Optional.of(primaryKey), Optional.empty(), defaultIcon(id)));
    }

    public static Technique cast(Id id, String nameKey, Id ability, String activeKey) {
        return new Technique(id, Optional.empty(), new ActiveAction.Cast(ability),
                new Presentation(nameKey, Optional.empty(), Optional.of(activeKey), defaultIcon(id)));
    }

    public static Technique toggle(Id id, String nameKey, Id tag, String activeKey) {
        return new Technique(id, Optional.empty(), new ActiveAction.Toggle(tag),
                new Presentation(nameKey, Optional.empty(), Optional.of(activeKey), defaultIcon(id)));
    }

    public static Technique adjustable(Id id, String nameKey, Id attribute, double minimum,
                                       double maximum, double step, String adjustKey) {
        return new Technique(id, Optional.empty(), new ActiveAction.Adjust(attribute, minimum, maximum, step),
                new Presentation(nameKey, Optional.empty(), Optional.of(adjustKey), defaultIcon(id)));
    }

    private static Id defaultIcon(Id id) {
        int separator = id.path().lastIndexOf('/');
        String name = separator < 0 ? id.path() : id.path().substring(separator + 1);
        return new Id(id.namespace(), "textures/gui/techniques/" + name + ".png");
    }

    public sealed interface ActiveAction permits ActiveAction.Cast, ActiveAction.Toggle,
            ActiveAction.Adjust, ActiveAction.None {
        record Cast(Id ability) implements ActiveAction { public Cast { Objects.requireNonNull(ability); } }
        record Toggle(Id tag) implements ActiveAction { public Toggle { Objects.requireNonNull(tag); } }
        record Adjust(Id attribute, double minimum, double maximum, double step) implements ActiveAction {
            public Adjust {
                Objects.requireNonNull(attribute);
                if (maximum < minimum || step <= 0) throw new IllegalArgumentException("Invalid adjustment range");
            }
        }
        record None() implements ActiveAction {}
    }

    public record Presentation(String nameKey, Optional<String> primaryKey, Optional<String> activeKey, Id icon) {
        public Presentation {
            Objects.requireNonNull(nameKey);
            primaryKey = Objects.requireNonNull(primaryKey);
            activeKey = Objects.requireNonNull(activeKey);
            Objects.requireNonNull(icon);
        }
    }
}
