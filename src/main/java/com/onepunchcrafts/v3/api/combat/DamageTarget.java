package com.onepunchcrafts.v3.api.combat;

import com.onepunchcrafts.v3.api.Id;

/** Adapter-owned target view; core combat never sees a Minecraft Entity. */
public interface DamageTarget {
    String stableId();
    double health();
    boolean alive();
    boolean hasTag(Id tag);
}
