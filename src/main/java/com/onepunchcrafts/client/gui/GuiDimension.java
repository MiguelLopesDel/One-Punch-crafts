package com.onepunchcrafts.client.gui;

import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.DimensionalPunchPacket;
import com.onepunchcrafts.network.packet.DimensionsPacket;
import com.onepunchcrafts.network.packet.TeleportPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import static com.onepunchcrafts.OnePunchCrafts.IMMERSIVE_PORTALS_MOD;

public class GuiDimension extends Screen {

    private static final int BUTTON_HEIGHT = 30;
    private static final int BUTTON_SPACING = 10;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0x00FFFF;
    private static final int HOVER_COLOR = 0xFF404040;
    private static final int BORDER_COLOR = 0xFF00FFFF;

    private static List<ResourceKey<Level>> dimensions = new ArrayList<>();
    private StringWidget pWidget;
    private boolean dimensionalPunch = false;

    public GuiDimension(Component title) {
        super(title);
        if (dimensions.isEmpty())
            NetworkRegister.sendToServer(new DimensionsPacket());
    }

    /** Picker whose selection triggers the Dimensional Punch instead of a plain portal. */
    public static GuiDimension forDimensionalPunch(Component title) {
        GuiDimension gui = new GuiDimension(title);
        gui.dimensionalPunch = true;
        return gui;
    }

    public static void setDimensions(List<ResourceKey<Level>> dimensions) {
        GuiDimension dimension = (GuiDimension) Minecraft.getInstance().screen;
        GuiDimension.dimensions = dimensions;
        dimension.createDimensionButtons();
    }

    @Override
    protected void init() {
        super.init();
        if (!dimensions.isEmpty()) {
            createDimensionButtons();
        } else {
            int textWidth = font.width("Carregando dimensões...");
            int x = (width - textWidth) / 2;
            int y = height / 2;
            pWidget = new StringWidget(x, y, MutableComponent.create(new LiteralContents("Carregando dimensões...")), font);
            this.addRenderableWidget(pWidget);
        }
    }

    private void createDimensionButtons() {
        this.removeWidget(pWidget);
        if (dimensions.isEmpty()) return;

        int padding = 20;
        int minButtonWidth = 100;
        int buttonHeight = BUTTON_HEIGHT;
        int buttonWidth = minButtonWidth;

        for (ResourceKey<Level> dimension : dimensions) {
            String dimensionName = dimension.location().getPath();
            int textWidth = font.width(dimensionName);
            buttonWidth = Math.max(buttonWidth, textWidth + padding);
        }

        int availableWidth = width - 40;
        int maxColumns = Math.max(1, availableWidth / (buttonWidth + BUTTON_SPACING));
        int columns = Math.min(dimensions.size(), maxColumns);
        int rows = (int) Math.ceil((double) dimensions.size() / columns);

        int totalGridWidth = (buttonWidth * columns) + (BUTTON_SPACING * (columns - 1));
        int totalGridHeight = (buttonHeight * rows) + (BUTTON_SPACING * (rows - 1));

        int startX = (width - totalGridWidth) / 2;
        int startY = (height - totalGridHeight) / 2;

        this.renderables.clear();

        for (int i = 0; i < dimensions.size(); i++) {
            ResourceKey<Level> dimension = dimensions.get(i);
            String dimensionName = dimension.location().getPath();

            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (buttonWidth + BUTTON_SPACING);
            int y = startY + row * (buttonHeight + BUTTON_SPACING);

            ButtonMod button = ButtonMod.builder(MutableComponent.create(new LiteralContents(dimensionName)),
                            (b) -> onDimensionButtonClicked(dimension))
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build();
//            button.setFGColor(TEXT_COLOR);

            this.addRenderableWidget(button);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        for (Renderable renderable : this.renderables) {
            if (renderable instanceof Button button) {
                boolean isHovered = button.isHoveredOrFocused();
                int fillColor = isHovered ? HOVER_COLOR : BACKGROUND_COLOR;

                int x = button.getX();
                int y = button.getY();
                int btnWidth = button.getWidth();
                int btnHeight = button.getHeight();

                graphics.fill(x, y, x + btnWidth, y + btnHeight, fillColor);

                graphics.fill(x, y, x + btnWidth, y + 1, BORDER_COLOR);
                graphics.fill(x, y + btnHeight - 1, x + btnWidth, y + btnHeight, BORDER_COLOR);
                graphics.fill(x, y, x + 1, y + btnHeight, BORDER_COLOR);
                graphics.fill(x + btnWidth - 1, y, x + btnWidth, y + btnHeight, BORDER_COLOR);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawString(font, this.title, width / 2 - font.width(this.title) / 2, 20, TEXT_COLOR);
    }


    private void onDimensionButtonClicked(ResourceKey<Level> dimension) {
        if (dimensionalPunch)
            NetworkRegister.sendToServer(new DimensionalPunchPacket(dimension));
        else
            NetworkRegister.sendToServer(new TeleportPacket(dimension));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}