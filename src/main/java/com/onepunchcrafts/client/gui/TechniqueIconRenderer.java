package com.onepunchcrafts.client.gui;

import com.onepunchcrafts.api.Id;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Draws the complete square technique artwork at any GUI size. */
public final class TechniqueIconRenderer {
    public static final int TEXTURE_SIZE = 64;

    private TechniqueIconRenderer() {}

    public static Layout layout(int destinationSize) {
        if (destinationSize <= 0) throw new IllegalArgumentException("Icon size must be positive");
        return new Layout(destinationSize, destinationSize, TEXTURE_SIZE, TEXTURE_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE);
    }

    public static void draw(GuiGraphics graphics, Id icon, int x, int y, int destinationSize) {
        Layout layout = layout(destinationSize);
        graphics.blit(new ResourceLocation(icon.namespace(), icon.path()), x, y,
                layout.destinationWidth(), layout.destinationHeight(), 0, 0,
                layout.sourceWidth(), layout.sourceHeight(), layout.textureWidth(), layout.textureHeight());
    }

    /** Explicit source and destination geometry prevents destination size from becoming a crop rectangle. */
    public record Layout(int destinationWidth, int destinationHeight, int sourceWidth, int sourceHeight,
                         int textureWidth, int textureHeight) {}
}
