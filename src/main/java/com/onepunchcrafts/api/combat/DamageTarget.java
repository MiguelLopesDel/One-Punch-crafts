package com.onepunchcrafts.api.combat;

import com.onepunchcrafts.api.Id;

/** Adapter-owned target view; core combat never sees a Minecraft Entity. */
public interface DamageTarget {
    String stableId();
    double health();
    boolean alive();
    boolean hasTag(Id tag);
}
