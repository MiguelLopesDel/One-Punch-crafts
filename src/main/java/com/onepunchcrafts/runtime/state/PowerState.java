package com.onepunchcrafts.runtime.state;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.effect.EffectSpec;
import com.onepunchcrafts.runtime.ability.AbilityBook;

import java.util.Objects;
import java.util.function.Consumer;

/** Aggregate root persisted for one player. */
public final class PowerState {
    public static final Id NONE = Id.parse("onepunchcrafts:none");

    private Id powerSetId = NONE;
    private final AttributeMap attributes = new AttributeMap();
    private final ResourceMap resources = new ResourceMap();
    private final TagSet tags = new TagSet();
    private final EffectContainer effects = new EffectContainer();
    private final AbilityBook abilities = new AbilityBook();
    private final VfxPreferenceMap vfxPreferences = new VfxPreferenceMap();
    private boolean identityDirty;

    public Id powerSetId() { return powerSetId; }
    public void powerSetId(Id value) { if (!Objects.equals(powerSetId, value)) { powerSetId = value; identityDirty = true; } }
    public AttributeMap attributes() { return attributes; }
    public ResourceMap resources() { return resources; }
    public TagSet tags() { return tags; }
    public EffectContainer effects() { return effects; }
    public AbilityBook abilities() { return abilities; }
    public VfxPreferenceMap vfxPreferences() { return vfxPreferences; }

    public void reset() {
        powerSetId(NONE);
        attributes.clear();
        resources.clear();
        tags.clear();
        effects.clear();
        abilities.clear();
    }

    public void tick(long now, Consumer<EffectSpec.Operation> operationSink) {
        resources.tick();
        effects.tick(now, operationSink);
    }

    public DirtyComponents consumeDirty() {
        DirtyComponents dirty = new DirtyComponents(identityDirty, attributes.consumeDirty(), resources.consumeDirty(),
                tags.consumeDirty(), effects.consumeDirty(), abilities.consumeDirty());
        identityDirty = false;
        return dirty;
    }

    public record DirtyComponents(boolean identity, boolean attributes, boolean resources, boolean tags,
                                  boolean effects, boolean abilities) {
        public boolean any() { return identity || attributes || resources || tags || effects || abilities; }
    }
}
