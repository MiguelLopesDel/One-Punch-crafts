package com.onepunchcrafts.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TechniqueIconRendererTest {
    @Test
    void smallerHudIconStillSamplesTheCompleteTexture() {
        var layout = TechniqueIconRenderer.layout(28);

        assertEquals(28, layout.destinationWidth());
        assertEquals(TechniqueIconRenderer.TEXTURE_SIZE, layout.sourceWidth());
        assertEquals(TechniqueIconRenderer.TEXTURE_SIZE, layout.sourceHeight());
    }

    @Test
    void wheelIconStillSamplesTheCompleteTexture() {
        var layout = TechniqueIconRenderer.layout(48);

        assertEquals(48, layout.destinationWidth());
        assertEquals(TechniqueIconRenderer.TEXTURE_SIZE, layout.sourceWidth());
        assertEquals(TechniqueIconRenderer.TEXTURE_SIZE, layout.textureWidth());
    }
}
