package com.onepunchcrafts.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

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

    public final KeyMapping SPECIAL_CHANGE_SKILL = new KeyMapping(
            defaultKeyCode + "special_change_skill",
            KeyConflictContext.IN_GAME, KeyModifier.CONTROL,
            InputConstants.getKey(InputConstants.KEY_R, -1),
            CATEGORY
    );

    public final KeyMapping USE_SPECIAL_SKILL = new KeyMapping(
            defaultKeyCode + "use_special_skill",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_Z, -1),
            CATEGORY
    );
    public final KeyMapping USE_FART = new KeyMapping(
            defaultKeyCode + "use_fart",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_P, -1),
            CATEGORY
    );
    public final KeyMapping USE_TELEPORT = new KeyMapping(
            defaultKeyCode + "use_teleport",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_J, -1),
            CATEGORY
    );
    public final KeyMapping OPEN_DIMENSIONS_GUI = new KeyMapping(
            defaultKeyCode + "use_dimensions_gui",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_O, -1),
            CATEGORY
    );
    public final KeyMapping OPEN_CSRC_OPTIONS = new KeyMapping(
            defaultKeyCode + "open_csrc_options",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_H, -1),
            CATEGORY
    );
    public final KeyMapping USE_DIMENSIONAL_PUNCH = new KeyMapping(
            defaultKeyCode + "use_dimensional_punch",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_K, -1),
            CATEGORY
    );
}