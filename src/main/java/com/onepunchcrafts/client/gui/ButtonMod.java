package com.onepunchcrafts.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class ButtonMod extends Button {

    protected ButtonMod(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, CreateNarration pCreateNarration) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pCreateNarration);
    }

    protected ButtonMod(Button.Builder builder) {
        super(builder);
    }


    public static Builder builder(@NotNull Component pMessage, @NotNull OnPress pOnPress) {
        return new Builder(pMessage, pOnPress);
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();
        int backgroundColor = 0x80000000;
        pGuiGraphics.fill(x, y, x + width, y + height, backgroundColor);
        int textColor = getFGColor();
        String buttonText = this.getMessage().getString();
        int textWidth = minecraft.font.width(buttonText);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - minecraft.font.lineHeight) / 2;
        pGuiGraphics.drawString(minecraft.font, buttonText, textX, textY, textColor | (Mth.ceil(this.alpha * 255.0F) << 24));
        RenderSystem.disableBlend();
    }

    public static class Builder extends Button.Builder {
        public Builder(Component message, OnPress onPress) {
            super(message, onPress);
        }

        @Override
        public Builder pos(int pX, int pY) {
            super.pos(pX, pY);
            return this;
        }

        @Override
        public Builder width(int pWidth) {
            super.width(pWidth);
            return this;
        }

        @Override
        public Builder size(int pWidth, int pHeight) {
            super.size(pWidth, pHeight);
            return this;
        }

        @Override
        public Builder bounds(int pX, int pY, int pWidth, int pHeight) {
            super.bounds(pX, pY, pWidth, pHeight);
            return this;
        }

        @Override
        public @NotNull ButtonMod build() {
            return new ButtonMod(this);
        }
    }
}
