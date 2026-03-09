package com.lux.gui;

import com.lux.config.HUDConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ArmorStatusSettingsScreen extends Screen {

    private final Screen parent;
    private HUDConfig.ModuleData armorMod;

    // UI Colors matching the main Lunar settings style
    private static final int CONTAINER_BG = 0xE0101214;
    private static final int TOGGLE_ON = 0xFF4EE2A1;
    private static final int TOGGLE_OFF = 0xFFE25C5C;
    private static final int BTN_BG = 0xFF252830;
    private static final int BTN_HOVER = 0xFF353840;

    public ArmorStatusSettingsScreen(Screen parent) {
        super(Text.literal("Armor Status Settings"));
        this.parent = parent;
        this.armorMod = HUDConfig.getInstance().getModule("Armor Status");
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark translucent overlay, no dirt/blur
        context.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int containerW = 300;
        int containerH = 200;
        int containerX = (this.width - containerW) / 2;
        int containerY = (this.height - containerH) / 2;

        // Main container - rounded rect
        drawRoundedRect(context, containerX, containerY, containerW, containerH, 12, CONTAINER_BG);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, "Armor Status Settings",
                this.width / 2, containerY + 18, 0xFFF0F2F5);

        // Divider
        context.fill(containerX + 20, containerY + 36, containerX + containerW - 20, containerY + 37, 0x44FFFFFF);

        // Label for display mode
        context.drawTextWithShadow(this.textRenderer, "Durability Display:",
                containerX + 24, containerY + 55, 0xFFA0A5B0);

        // Left option button: "Exact (e.g. 320/363)"
        int optBtnW = 110;
        int optBtnH = 28;
        int leftBtnX = containerX + 24;
        int btnY = containerY + 72;

        boolean showPercent = armorMod != null && armorMod.showPercentage;

        boolean hoverLeft = mouseX >= leftBtnX && mouseX <= leftBtnX + optBtnW && mouseY >= btnY
                && mouseY <= btnY + optBtnH;
        int leftCol = !showPercent ? TOGGLE_ON : (hoverLeft ? BTN_HOVER : BTN_BG);
        drawRoundedRect(context, leftBtnX, btnY, optBtnW, optBtnH, 6, leftCol);
        context.drawCenteredTextWithShadow(this.textRenderer, "Exact (320/363)",
                leftBtnX + optBtnW / 2, btnY + (optBtnH - 8) / 2, 0xFFFFFFFF);

        // Right option button: "Percent (%)"
        int rightBtnX = leftBtnX + optBtnW + 14;
        boolean hoverRight = mouseX >= rightBtnX && mouseX <= rightBtnX + optBtnW && mouseY >= btnY
                && mouseY <= btnY + optBtnH;
        int rightCol = showPercent ? TOGGLE_ON : (hoverRight ? BTN_HOVER : BTN_BG);
        drawRoundedRect(context, rightBtnX, btnY, optBtnW, optBtnH, 6, rightCol);
        context.drawCenteredTextWithShadow(this.textRenderer, "Percent (88%)",
                rightBtnX + optBtnW / 2, btnY + (optBtnH - 8) / 2, 0xFFFFFFFF);

        // Preview label
        context.drawTextWithShadow(this.textRenderer, "Preview:",
                containerX + 24, containerY + 115, 0xFFA0A5B0);

        String preview = showPercent ? "87%" : "317/363";
        int previewColor = 0xFF55FF55;
        context.drawTextWithShadow(this.textRenderer, preview,
                containerX + 80, containerY + 115, previewColor);

        // Back button
        int backW = 90;
        int backH = 28;
        int backX = (this.width - backW) / 2;
        int backY = containerY + containerH - backH - 18;

        boolean hoverBack = mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH;
        drawRoundedRect(context, backX, backY, backW, backH, 6, hoverBack ? BTN_HOVER : BTN_BG);
        context.drawCenteredTextWithShadow(this.textRenderer, "Back",
                backX + backW / 2, backY + (backH - 8) / 2, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return super.mouseClicked(mouseX, mouseY, button);

        int containerW = 300;
        int containerH = 200;
        int containerX = (this.width - containerW) / 2;
        int containerY = (this.height - containerH) / 2;

        int optBtnW = 110;
        int optBtnH = 28;
        int leftBtnX = containerX + 24;
        int rightBtnX = leftBtnX + optBtnW + 14;
        int btnY = containerY + 72;

        // Exact button clicked
        if (mouseX >= leftBtnX && mouseX <= leftBtnX + optBtnW && mouseY >= btnY && mouseY <= btnY + optBtnH) {
            if (armorMod != null) {
                armorMod.showPercentage = false;
                HUDConfig.getInstance().save();
            }
            return true;
        }

        // Percent button clicked
        if (mouseX >= rightBtnX && mouseX <= rightBtnX + optBtnW && mouseY >= btnY && mouseY <= btnY + optBtnH) {
            if (armorMod != null) {
                armorMod.showPercentage = true;
                HUDConfig.getInstance().save();
            }
            return true;
        }

        // Back button
        int backW = 90;
        int backH = 28;
        int backX = (this.width - backW) / 2;
        int backY = containerY + containerH - backH - 18;
        if (mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH) {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Rendering utility
    private void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0)
            return;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        for (int row2 = 0; row2 < radius; row2++) {
            for (int col2 = 0; col2 < radius; col2++) {
                double dist = Math.hypot(radius - 0.5 - col2, radius - 0.5 - row2);
                if (dist <= radius) {
                    double alphaMult = dist > radius - 1 ? radius - dist : 1.0;
                    int pixelA = (int) (a * alphaMult);
                    if (pixelA > 0) {
                        int c = (pixelA << 24) | (r << 16) | (g << 8) | b;
                        context.fill(x + col2, y + row2, x + col2 + 1, y + row2 + 1, c);
                        context.fill(x + col2, y + height - 1 - row2, x + col2 + 1, y + height - row2, c);
                        context.fill(x + width - 1 - col2, y + row2, x + width - col2, y + row2 + 1, c);
                        context.fill(x + width - 1 - col2, y + height - 1 - row2, x + width - col2, y + height - row2,
                                c);
                    }
                }
            }
        }
    }
}
