package com.onepunchcrafts.client;

import com.onepunchcrafts.client.gui.CsrcOptionsScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Client-only extension points. Kept in its own class (and only reached
 * through DistExecutor) so no client class ever leaks into a signature that a
 * dedicated server could try to resolve.
 */
public final class ClientModExtensions {

    private ClientModExtensions() {}

    /** Adds the "Config" button in the mod list, opening the CSRC options screen. */
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new CsrcOptionsScreen(parent)));
    }
}
