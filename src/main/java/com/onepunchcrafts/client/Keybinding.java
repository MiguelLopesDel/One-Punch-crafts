package com.onepunchcrafts.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

public class Keybinding {

    public static final Keybinding INSTANCE = new Keybinding();
    private static final String CATEGORY = "key.categories." + MODID;
    private final String defaultKeyCode = "key." + MODID + ".";

    public final KeyMapping CHANGE_SKILL = new KeyMapping(
            defaultKeyCode + "change_skill",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_R, -1),
            CATEGORY
    );

    public final KeyMapping USE_SPECIAL_SKILL = new KeyMapping(
            defaultKeyCode + "use_special_skill",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_Z, -1),
            CATEGORY
    );
}