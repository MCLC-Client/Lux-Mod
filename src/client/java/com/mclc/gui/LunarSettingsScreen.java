package com.mclc.gui;

import com.mclc.config.HUDConfig;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LunarSettingsScreen extends Screen {

    // === Modern Clean UI Colors ===
    private static final int BG_GRADIENT_TOP = 0xA6080A0C; // Darker gradient with 65% alpha
    private static final int BG_GRADIENT_BOT = 0xA6040506; // Darker gradient with 65% alpha

    private static final int CONTAINER_BG = 0xA60D0F12; // 65% alpha (166/255) glassmorphism center
    private static final int CARD_BG = 0xFF181A1E;

    private static final int ENABLED_MINT = 0xFF4EE2A1;
    private static final int DISABLED_RED = 0xFFE25C5C;

    private static final int OPTIONS_GEAR_BG = 0xFF2A2C31;
    private static final int OPTIONS_GEAR_HOVER = 0xFF3E4149;
    private static final Identifier GEAR_TEXTURE = Identifier.of("mclc", "textures/gui/gear.png");
    private static final Identifier SETTINGS_SPRITE = Identifier.ofVanilla("icon/settings");

    // Map module names to their specific icon textures
    private Identifier getModuleIcon(String moduleName) {
        String cleanName = moduleName.toLowerCase().replace(" ", "_");
        return Identifier.of("mclc", "textures/gui/" + cleanName + ".png");
    }

    private static final int TEXT_TITLE = 0xFFF0F2F5;
    private static final int TEXT_SUB = 0xFFA0A5B0;

    private final List<ModData> mods = new ArrayList<>();

    // Animation & Scrolling State
    private double targetScrollOffset = 0;
    private double currentScrollOffset = 0;
    private long lastRenderTime = 0;
    private long initTime = 0;

    // Scrollbar state
    private boolean isDraggingScrollbar = false;

    public LunarSettingsScreen() {
        super(Text.literal("Modern Settings"));

        HUDConfig config = HUDConfig.getInstance();
        for (Map.Entry<String, HUDConfig.ModuleData> entry : config.getModules().entrySet()) {
            mods.add(new ModData(entry.getValue().name, entry.getValue().enabled));
        }

        // Dummy padding if less than 15 for visual testing purposes
        for (int i = mods.size() + 1; i <= 15; i++) {
            mods.add(new ModData("Module " + i, false));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.initTime = System.currentTimeMillis();
        this.lastRenderTime = System.currentTimeMillis();
        this.targetScrollOffset = 0;
        this.currentScrollOffset = 0;
    }

    // Dynamic layout vars
    private int containerWidth;
    private int containerHeight;
    private int containerX;
    private int containerY;
    private int contentHeight;
    private int visibleHeight;
    private int maxScroll;

    private void updateLayout() {
        // Main GUI: Full-Screen (100%)
        this.containerX = 0;
        this.containerY = 0;
        this.containerWidth = this.width;
        this.containerHeight = this.height;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateLayout();

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRenderTime) / 1000f;
        this.lastRenderTime = currentTime;

        // Entrance Animation
        float rawAnim = Math.min(1.0f, (currentTime - initTime) / 450f);
        float openAnim = (float) (1.0 - Math.pow(1.0 - rawAnim, 4));

        // Ensure scroll within bounds
        if (targetScrollOffset > maxScroll)
            targetScrollOffset = maxScroll;
        if (targetScrollOffset < 0)
            targetScrollOffset = 0;

        // Scroll Interpolation
        this.currentScrollOffset += (this.targetScrollOffset - this.currentScrollOffset) * 15f * deltaTime;
        if (Math.abs(currentScrollOffset - targetScrollOffset) < 0.5) {
            currentScrollOffset = targetScrollOffset;
        }

        // Global BG
        int topColor = (int) ((BG_GRADIENT_TOP >>> 24) * openAnim) << 24 | (BG_GRADIENT_TOP & 0xFFFFFF);
        int botColor = (int) ((BG_GRADIENT_BOT >>> 24) * openAnim) << 24 | (BG_GRADIENT_BOT & 0xFFFFFF);
        context.fillGradient(0, 0, this.width, this.height, topColor, botColor);

        super.render(context, mouseX, mouseY, delta);

        float animOffset = (1.0f - openAnim) * 40f;

        context.getMatrices().push();
        context.getMatrices().translate(0, animOffset, 0);

        // Full Screen BG Frame
        int containerAlpha = (int) (153 * openAnim); // 60% opacity (153/255)
        drawSmoothRoundedRect(context, containerX, containerY, containerWidth, containerHeight, 20,
                (containerAlpha << 24) | 0x0D0F12);

        // Headline
        context.getMatrices().push();
        context.getMatrices().translate(containerX + 30, containerY + 25, 0);
        float scaleTitle = 1.6f;
        context.getMatrices().scale(scaleTitle, scaleTitle, 1.0f);
        context.drawTextWithShadow(this.textRenderer, "Modules", 0, 0, applyAlpha(TEXT_TITLE, openAnim));
        context.getMatrices().pop();

        context.drawTextWithShadow(this.textRenderer, "Manage your active modifications", containerX + 31,
                containerY + 50, applyAlpha(TEXT_SUB, openAnim));

        // Auto-Grid (~1/6th of screen width, forcing max 100px height per user request)
        int desiredCardWidth = Math.max(90, Math.min(this.width / 6, 120)); // roughly 1/6, max 120px wide
        int maxInternalWidth = containerWidth - 100; // 50px padding on each side for the grid

        if (maxScroll > 0) {
            maxInternalWidth -= 20; // make room for scrollbar
        }

        int actualCardWidth = Math.min(desiredCardWidth, maxInternalWidth);
        int gap = (int) (actualCardWidth * 0.15); // Smaller gap for smaller cards

        int startX = containerX + 50;
        int startY = containerY + 100; // start below title

        int columns = maxInternalWidth / (actualCardWidth + gap);
        if (columns < 1)
            columns = 1;

        int totalGridWidth = (columns * actualCardWidth) + ((columns - 1) * gap);
        int gridXOffset = (maxInternalWidth - totalGridWidth) / 2;

        startX += gridXOffset;

        int row = 0;
        int col = 0;

        // Force smaller cards: ~80-100px height. Keep width proportionate.
        int cardHeight = Math.max(80, Math.min(100, (int) (actualCardWidth * 0.85)));

        // Calculate dynamic scroll lengths
        int totalRows = (int) Math.ceil((double) mods.size() / columns);
        this.contentHeight = totalRows * (cardHeight + gap) - gap;
        this.visibleHeight = containerHeight - 110; // below title

        // Critical Fix: maxScroll Calculation.
        // It should precisely represent how far we can scroll to reach the bottom row.
        this.maxScroll = Math.max(0, contentHeight - visibleHeight);

        // Enforce bounds immediately
        if (this.targetScrollOffset > maxScroll) {
            this.targetScrollOffset = maxScroll;
        }
        if (this.targetScrollOffset < 0) {
            this.targetScrollOffset = 0;
        }

        // Smooth Interpolation using Minecraft MathHelper
        this.currentScrollOffset = MathHelper.lerp(deltaTime * 15f, (float) this.currentScrollOffset,
                (float) this.targetScrollOffset);
        if (Math.abs(currentScrollOffset - targetScrollOffset) < 0.5) {
            currentScrollOffset = targetScrollOffset;
        }

        // Clipping: only render inside the valid area!
        // We crop slightly inside the container to avoid edge overlap
        int clipY1 = startY - 10;
        int clipY2 = containerY + containerHeight - 15;

        context.enableScissor(containerX, clipY1, containerX + containerWidth, clipY2);

        for (ModData mod : mods) {
            int cx = startX + col * (actualCardWidth + gap);
            int cy = startY + row * (cardHeight + gap) - (int) currentScrollOffset; // Correct Y position using offset

            // Render optimization & clipping
            if (cy + cardHeight > clipY1 && cy < clipY2) {
                int trueMouseY = (int) (mouseY - animOffset);
                boolean hoveringCard = mouseX >= cx && mouseX <= cx + actualCardWidth && trueMouseY >= cy
                        && trueMouseY <= cy + cardHeight;

                if (hoveringCard) {
                    mod.hoverAnim = Math.min(1f, mod.hoverAnim + deltaTime * 12f);
                } else {
                    mod.hoverAnim = Math.max(0f, mod.hoverAnim - deltaTime * 12f);
                }

                drawModCard(context, cx, cy, actualCardWidth, cardHeight, mouseX, trueMouseY, mod, openAnim);
            }

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
        context.disableScissor();

        // Draw Scrollbar (Modern thin handle)
        if (maxScroll > 0) {
            int scrollX = containerX + containerWidth - 15;
            int scrollY = clipY1;
            int scrollH = clipY2 - clipY1;

            // Bar background
            drawSmoothRoundedRect(context, scrollX, scrollY, 4, scrollH, 2, applyAlpha(0x22FFFFFF, openAnim));

            // Handle
            float viewPercent = (float) visibleHeight / contentHeight;
            int handleHeight = Math.max(30, (int) (scrollH * viewPercent));
            float scrollPercent = (float) currentScrollOffset / maxScroll;
            int handleY = scrollY + (int) ((scrollH - handleHeight) * scrollPercent);

            int handleColor = isDraggingScrollbar ? 0x99FFFFFF : 0x66FFFFFF;
            drawSmoothRoundedRect(context, scrollX, handleY, 4, handleHeight, 2, applyAlpha(handleColor, openAnim));

            // Custom scroll drag logic
            if (isDraggingScrollbar) {
                int trueMouseY = (int) (mouseY - animOffset);
                // Math to set targetScrollOffset
                float newScrollPercent = (float) (trueMouseY - scrollY - handleHeight / 2) / (scrollH - handleHeight);
                newScrollPercent = Math.max(0, Math.min(1, newScrollPercent));
                targetScrollOffset = maxScroll * newScrollPercent;
            }
        }

        context.getMatrices().pop();
    }

    private void drawModCard(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY,
            ModData mod, float globalAlpha) {
        float scale = 1.0f + (0.02f * mod.hoverAnim);

        int centerX = x + width / 2;
        int centerY = y + height / 2;

        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-centerX, -centerY, 0);

        int shadowColor = applyAlpha(0x228899AA, globalAlpha * Math.max(0.4f, mod.hoverAnim));
        if (mod.hoverAnim > 0) {
            drawShadowRect(context, x, y, width, height, 8, 4, shadowColor);
        } else {
            drawShadowRect(context, x, y, width, height, 8, 3, applyAlpha(0x11000000, globalAlpha));
        }

        int baseBg = CARD_BG;
        if (mod.hoverAnim > 0) {
            baseBg = brighten(CARD_BG, 1.0f + (0.1f * mod.hoverAnim));
        }
        drawSmoothRoundedRect(context, x, y, width, height, 8, applyAlpha(baseBg, globalAlpha));

        // Icon Box (Perfectly Centered)
        int iconSize = (int) (width * 0.40); // slightly larger relative size for better look
        int iconX = x + (width - iconSize) / 2;
        int iconY = y + (int) (height * 0.12);
        drawSmoothRoundedRect(context, iconX, iconY, iconSize, iconSize, 8, applyAlpha(0x11FFFFFF, globalAlpha));

        // Draw Custom Icon or Fallback "?"
        Identifier moduleIcon = getModuleIcon(mod.name);
        boolean hasIcon = this.client != null && this.client.getResourceManager().getResource(moduleIcon).isPresent();

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, globalAlpha);

        if (hasIcon) {
            int iconPad = (int) (iconSize * 0.15f);
            context.drawTexture(moduleIcon, iconX + iconPad, iconY + iconPad, iconSize - iconPad * 2,
                    iconSize - iconPad * 2, 0.0f, 0.0f, 128, 128, 128, 128);
        } else {
            // Centered Question Mark Fallback
            int textW = this.textRenderer.getWidth("?");
            context.getMatrices().push();
            float qScale = iconSize / 24f; // Scale "?" based on box size
            context.getMatrices().translate(iconX + iconSize / 2f - (textW * qScale) / 2f,
                    iconY + iconSize / 2f - (8 * qScale) / 2f, 0);
            context.getMatrices().scale(qScale, qScale, 1.0f);
            context.drawText(this.textRenderer, "?", 0, 0, applyAlpha(0x88FFFFFF, globalAlpha), false);
            context.getMatrices().pop();
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        context.getMatrices().push();
        int nameWidth = this.textRenderer.getWidth(mod.name);
        int textY = y + (int) (height * 0.50); // Move text slightly up to avoid cutoffs
        if (nameWidth > width - 10) {
            float textScale = (float) (width - 10) / nameWidth;
            context.getMatrices().translate(x + 5, textY, 0);
            context.getMatrices().scale(textScale, textScale, 1.0f);
            context.drawTextWithShadow(this.textRenderer, mod.name, 0, 0, applyAlpha(TEXT_TITLE, globalAlpha));
        } else {
            context.drawTextWithShadow(this.textRenderer, mod.name, x + (width - nameWidth) / 2, textY,
                    applyAlpha(TEXT_TITLE, globalAlpha));
        }
        context.getMatrices().pop();

        int gearSize = Math.max(16, (int) (width * 0.16));
        int gearX = x + width - gearSize - 6;
        int gearY = y + 6;
        boolean hoveringGear = mouseX >= gearX && mouseX <= gearX + gearSize && mouseY >= gearY
                && mouseY <= gearY + gearSize;
        int gearColor = hoveringGear ? OPTIONS_GEAR_HOVER : OPTIONS_GEAR_BG;
        drawSmoothRoundedRect(context, gearX, gearY, gearSize, gearSize, 4, applyAlpha(gearColor, globalAlpha));

        // Draw gear texture icon instead of text glyph - pixel-perfect, no font issues
        context.getMatrices().push();
        float gearIconPad = gearSize * 0.15f;
        int gearIconX = (int) (gearX + gearIconPad);
        int gearIconY = (int) (gearY + gearIconPad);
        int gearIconSize = (int) (gearSize - gearIconPad * 2);
        // Apply hover rotation via matrix transform
        if (mod.hoverAnim > 0) {
            float pivotX = gearX + gearSize * 0.5f;
            float pivotY = gearY + gearSize * 0.5f;
            float rotAngle = mod.hoverAnim * 15f; // up to 15 degrees rotation on full hover
            context.getMatrices().translate(pivotX, pivotY, 0);
            context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rotAngle));
            context.getMatrices().translate(-pivotX, -pivotY, 0);
        }

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, globalAlpha);

        boolean hasCustomGear = false;
        if (this.client != null && this.client.getResourceManager().getResource(GEAR_TEXTURE).isPresent()) {
            RenderSystem.enableBlend();
            hasCustomGear = true;
        }

        if (hasCustomGear) {
            context.drawTexture(GEAR_TEXTURE, gearIconX, gearIconY, gearIconSize, gearIconSize, 0.0f, 0.0f, 128, 128,
                    128, 128);
        } else {
            // Robust Fallback to text "+" if the PNG is missing or not loaded yet
            float fallbackScale = gearIconSize / 10f;
            context.getMatrices().push();
            context.getMatrices().translate(
                    gearX + gearSize / 2f - (this.textRenderer.getWidth("+") * fallbackScale) / 2f,
                    gearY + gearSize / 2f - (8 * fallbackScale) / 2f, 0);
            context.getMatrices().scale(fallbackScale, fallbackScale, 1.0f);
            context.drawTextWithShadow(this.textRenderer, "+", 0, 0, applyAlpha(0xFFFFFFFF, globalAlpha));
            context.getMatrices().pop();
        }

        RenderSystem.disableBlend();
        context.getMatrices().pop();

        int btnHeight = Math.max(18, (int) (height * 0.18));
        int btnWidth = width - 16; // less padding on sides for smaller cards
        int btnX = x + 8;
        int btnY = y + height - btnHeight - 8;

        boolean hoveringStatus = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY
                && mouseY <= btnY + btnHeight;
        int statusColor = mod.enabled ? ENABLED_MINT : DISABLED_RED;
        int finalStatusColor = hoveringStatus ? brighten(statusColor, 1.15f) : statusColor;

        drawSmoothRoundedRect(context, btnX, btnY, btnWidth, btnHeight, 5, applyAlpha(finalStatusColor, globalAlpha));

        String statusText = mod.enabled ? "Active" : "Disabled";
        int statusTextW = this.textRenderer.getWidth(statusText);

        context.getMatrices().push();
        float btnTScale = Math.min(1.0f, (float) (btnWidth - 6) / statusTextW);
        context.getMatrices().translate(btnX + btnWidth / 2f - (statusTextW * btnTScale) / 2f,
                btnY + btnHeight / 2f - (8 * btnTScale) / 2f, 0);
        context.getMatrices().scale(btnTScale, btnTScale, 1.0f);
        context.drawText(this.textRenderer, statusText, 0, 0, applyAlpha(0xFF101215, globalAlpha), false);
        context.getMatrices().pop();

        context.getMatrices().pop();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Correct directional scrolling.
        // verticalAmount is positive when scrolling up, negative down.
        // We invert it so sliding the wheel down moves the offset UP (content goes up).
        this.targetScrollOffset -= verticalAmount * 45; // 45px per tick is a clean amount

        // Enforce bounds immediately upon scroll tick to avoid dragging past zero
        this.targetScrollOffset = Math.max(0, Math.min(maxScroll, this.targetScrollOffset));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float rawAnim = Math.min(1.0f, (System.currentTimeMillis() - initTime) / 450f);
            float openAnim = (float) (1.0 - Math.pow(1.0 - rawAnim, 4));
            float animOffset = (1.0f - openAnim) * 40f;
            int trueMouseY = (int) (mouseY - animOffset);

            int clipY1 = containerY + 80 - 10;
            int clipY2 = containerY + containerHeight - 15;

            if (maxScroll > 0) {
                int scrollX = containerX + containerWidth - 15;
                if (mouseX >= scrollX - 5 && mouseX <= scrollX + 15 && trueMouseY >= clipY1 && trueMouseY <= clipY2) {
                    isDraggingScrollbar = true;
                    return true;
                }
            }

            if (trueMouseY < clipY1 || trueMouseY > clipY2) {
                return super.mouseClicked(mouseX, mouseY, button); // out of bounds
            }

            // Reverse-calculate grid to find clicks
            int desiredCardWidth = Math.max(90, Math.min(this.width / 6, 120));
            int maxInternalWidth = containerWidth - 100;
            if (maxScroll > 0)
                maxInternalWidth -= 20;
            int actualCardWidth = Math.min(desiredCardWidth, maxInternalWidth);
            int gap = (int) (actualCardWidth * 0.15);
            int columns = Math.max(1, maxInternalWidth / (actualCardWidth + gap));
            int totalGridWidth = (columns * actualCardWidth) + ((columns - 1) * gap);
            int gridXOffset = (maxInternalWidth - totalGridWidth) / 2;
            int startX = containerX + 50 + gridXOffset;
            int startY = containerY + 100;
            int cardHeight = Math.max(80, Math.min(100, (int) (actualCardWidth * 0.85)));

            int row = 0;
            int col = 0;

            for (ModData mod : mods) {
                int cx = startX + col * (actualCardWidth + gap);
                int cy = startY + row * (cardHeight + gap) - (int) currentScrollOffset;

                int btnHeight = Math.max(18, (int) (cardHeight * 0.18));
                int btnWidth = actualCardWidth - 16;
                int btnX = cx + 8;
                int btnY = cy + cardHeight - btnHeight - 8;

                int gearSize = Math.max(16, (int) (actualCardWidth * 0.16));
                int gearX = cx + actualCardWidth - gearSize - 6;
                int gearY = cy + 6;

                if (mouseX >= btnX && mouseX <= btnX + btnWidth && trueMouseY >= btnY
                        && trueMouseY <= btnY + btnHeight) {
                    mod.enabled = !mod.enabled;

                    // Sync with HUDConfig if it's a real config module
                    HUDConfig config = HUDConfig.getInstance();
                    HUDConfig.ModuleData data = config.getModule(mod.name);
                    if (data != null) {
                        data.enabled = mod.enabled;
                        config.save();
                    }
                    return true;
                }

                if (mouseX >= gearX && mouseX <= gearX + gearSize && trueMouseY >= gearY
                        && trueMouseY <= gearY + gearSize) {
                    // Open dedicated settings screen per module
                    if (this.client != null) {
                        if (mod.name.equals("Armor Status")) {
                            this.client.setScreen(new ArmorStatusSettingsScreen(this));
                        } else if (mod.name.equals("CPS") || mod.name.equals("Ping")
                                || mod.name.equals("Potion Effects")) {
                            HUDConfig config = HUDConfig.getInstance();
                            HUDConfig.ModuleData data = config.getModule(mod.name);
                            if (data != null) {
                                data.mode = (data.mode + 1) % 2; // Quick swap mode
                                config.save();
                            }
                        }
                    }
                    return true;
                }

                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // === Modern Rendering Utilities ===

    private int applyAlpha(int color, float alphaMult) {
        int a = (int) (((color >> 24) & 0xFF) * alphaMult);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private int brighten(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawSmoothRoundedRect(DrawContext context, int x, int y, int width, int height, int radius,
            int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0)
            return;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        for (int row = 0; row < radius; row++) {
            for (int col = 0; col < radius; col++) {
                double dist = Math.hypot(radius - 0.5 - col, radius - 0.5 - row);
                if (dist <= radius) {
                    double alphaMult = 1.0;
                    if (dist > radius - 1)
                        alphaMult = radius - dist;
                    int pixelA = (int) (a * alphaMult);
                    if (pixelA > 0) {
                        int c = (pixelA << 24) | (r << 16) | (g << 8) | b;
                        context.fill(x + col, y + row, x + col + 1, y + row + 1, c);
                        context.fill(x + col, y + height - 1 - row, x + col + 1, y + height - row, c);
                        context.fill(x + width - 1 - col, y + row, x + width - col, y + row + 1, c);
                        context.fill(x + width - 1 - col, y + height - 1 - row, x + width - col, y + height - row, c);
                    }
                }
            }
        }
    }

    private void drawShadowRect(DrawContext context, int x, int y, int width, int height, int radius, int spread,
            int hexColor) {
        int a = (hexColor >> 24) & 0xFF;
        int r = (hexColor >> 16) & 0xFF;
        int g = (hexColor >> 8) & 0xFF;
        int b = hexColor & 0xFF;

        for (int i = 1; i <= spread; i++) {
            int currentAlpha = (int) (a * (1.0 - (double) i / spread));
            if (currentAlpha > 0) {
                drawSmoothRoundedRect(context, x - i, y - i, width + i * 2, height + i * 2, radius + i,
                        (currentAlpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    private static class ModData {
        String name;
        boolean enabled;
        float hoverAnim = 0f;

        ModData(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }
    }
}
