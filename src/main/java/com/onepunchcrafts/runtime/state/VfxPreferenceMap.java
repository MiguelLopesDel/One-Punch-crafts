package com.onepunchcrafts.runtime.state;

import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.api.presentation.VfxProfile;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persisted presentation preferences. Gameplay never branches on these values. */
public final class VfxPreferenceMap {
    private final Map<Id, VfxProfile> values = new LinkedHashMap<>();

    public VfxProfile get(Id technique) { return values.getOrDefault(technique, VfxProfile.NEW); }
    public void set(Id technique, VfxProfile profile) { values.put(technique, profile); }
    public Map<Id, VfxProfile> values() { return Map.copyOf(values); }
    public void clear() { values.clear(); }
}
