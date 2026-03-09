package com.lux.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.Color;

public class LuxScreen extends Screen {

    public LuxScreen() {
        super(Text.literal("Lux Mods"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background overlay with modern blur-like darkening
        context.fillGradient(0, 0, this.width, this.height, 0xDD000000, 0xEE000000);

        super.render(context, mouseX, mouseY, delta);

        // Top bar
        fillRoundedRect(context, 10, 10, this.width - 10, 40, 6, 0xAA202020);

        // Lux Logo Title
        context.getMatrices().push();
        context.getMatrices().scale(1.5f, 1.5f, 1.5f);
        context.drawTextWithShadow(this.textRenderer, "Lux", (int) (25 / 1.5f), (int) (18 / 1.5f), 0xFFFFFF);
        context.getMatrices().pop();

        // Top bar buttons
        drawRoundedButton(context, 100, 15, 60, 20, 4, "MODS", true);
        drawRoundedButton(context, 170, 15, 80, 20, 4, "SETTINGS", false);

        drawRoundedButton(context, this.width - 35, 15, 20, 20, 4, "X", false); // Close button placeholder

        // Global Search Bar across the top right
        fillRoundedRect(context, this.width - 200, 15, this.width - 45, 35, 4, 0xAA151515);
        context.drawText(this.textRenderer, "Search...", this.width - 190, 21, 0x777777, false);

        // Left Sidebar for the Default Profile (smaller)
        fillRoundedRect(context, 10, 50, 100, 35, 4, 0xAA252525);
        context.drawText(this.textRenderer, "Default", 20, 64, 0xFFFFFF, false);

        // Responsive Mod Cards Layout
        int cardWidth = 100;
        int cardHeight = 100;
        int gap = 15;

        // Calculate how many cards fit in one row
        int startX = 125; // start after the left sidebar
        int availableWidth = this.width - startX - 20; // 20 px padding on the right
        int columns = availableWidth / (cardWidth + gap);
        if (columns < 1)
            columns = 1; // At least one column

        // Define some dummy mods
        String[][] dummyMods = {
                { "Dummy Mod 1", "LOCKED", "0xFFAAAAAA" },
                { "Dummy Mod 2", "ENABLED", "0xFF50A14F" },
                { "Dummy Mod 3", "DISABLED", "0xFFE24A4A" },
                { "Dummy Mod 4", "DISABLED", "0xFFE24A4A" },
                { "Dummy Mod 5", "ENABLED", "0xFF50A14F" },
                { "Dummy Mod 6", "LOCKED", "0xFFAAAAAA" },
                { "Dummy Mod 7", "ENABLED", "0xFF50A14F" },
                { "Dummy Mod 8", "ENABLED", "0xFF50A14F" },
                { "Dummy Mod 9", "LOCKED", "0xFFAAAAAA" }
        };

        int row = 0;
        int col = 0;
        int startY = 80;

        for (String[] mod : dummyMods) {
            int x = startX + col * (cardWidth + gap);
            int y = startY + row * (cardHeight + gap);

            drawModCard(context, x, y, cardWidth, cardHeight, mod[0], mod[1], parseColor(mod[2]));

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
    }

    private int parseColor(String hex) {
        return Long.decode(hex).intValue();
    }

    private void drawRoundedButton(DrawContext context, int x, int y, int width, int height, int radius, String text,
            boolean selected) {
        drawRoundedButton(context, x, y, width, height, radius, text, selected, 0xFF4A90E2);
    }

    private void drawRoundedButton(DrawContext context, int x, int y, int width, int height, int radius, String text,
            boolean selected, int focusColor) {
        int color = selected ? focusColor : 0x88333333;
        fillRoundedRect(context, x, y, x + width, y + height, radius, color);
        int textWidth = this.textRenderer.getWidth(text);
        context.drawText(this.textRenderer, text, x + (width - textWidth) / 2, y + (height - 8) / 2, 0xFFFFFF, false);
    }

    private void drawModCard(DrawContext context, int x, int y, int cardWidth, int cardHeight, String name,
            String status, int statusColor) {
        // Background with rounded corners
        fillRoundedRect(context, x, y, x + cardWidth, y + cardHeight, 8, 0xAA252525);

        // Icon Area placeholder (a generic centered box at the very top)
        int iconSize = 32;
        int iconX = x + (cardWidth - iconSize) / 2;
        fillRoundedRect(context, iconX, y + 10, iconX + iconSize, y + 10 + iconSize, 6, 0x33FFFFFF);

        // Name (Middle)
        int nameWidth = this.textRenderer.getWidth(name);
        context.drawText(this.textRenderer, name, x + (cardWidth - nameWidth) / 2, y + 50, 0xFFFFFF, false);

        // Options/Settings "Gear" Button placeholder
        int gearX = x + cardWidth - 25;
        int gearY = y + 45;
        fillRoundedRect(context, gearX, gearY, gearX + 16, gearY + 16, 4, 0xAA404040);
        context.drawText(this.textRenderer, "\u2699", gearX + 4, gearY + 4, 0xFFFFFF, false); // Unicode gear symbol

        // Status button at the bottom (large button)
        fillRoundedRect(context, x + 8, y + 75, x + cardWidth - 8, y + 92, 4, statusColor);
        int statusWidth = this.textRenderer.getWidth(status);
        context.drawText(this.textRenderer, status, x + (cardWidth - statusWidth) / 2, y + 79, 0xFFFFFF, false);
    }

    /**
     * Helper to draw a pseudo-rounded rectangle by drawing multiple rects to
     * simulate corner radius.
     */
    private void fillRoundedRect(DrawContext context, int x1, int y1, int x2, int y2, int radius, int color) {
        // Draw the central large cross
        context.fill(x1 + radius, y1, x2 - radius, y2, color);
        context.fill(x1, y1 + radius, x2, y2 - radius, color);

        // Corners (approximated for simplicity)
        // Top-Left
        context.fill(x1 + 1, y1 + 1, x1 + radius, y1 + radius, color);
        // Top-Right
        context.fill(x2 - radius, y1 + 1, x2 - 1, y1 + radius, color);
        // Bottom-Left
        context.fill(x1 + 1, y2 - radius, x1 + radius, y2 - 1, color);
        // Bottom-Right
        context.fill(x2 - radius, y2 - radius, x2 - 1, y2 - 1, color);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
