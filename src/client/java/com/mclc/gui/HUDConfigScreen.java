package com.lux.gui;

import com.lux.config.HUDConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.Map;

public class HUDConfigScreen extends Screen {

    private final HUDConfig config = HUDConfig.getInstance();
    private HUDConfig.ModuleData draggingModule = null;

    // Drag offset
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HUDConfigScreen() {
        super(Text.literal("HUD Layout Editor"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Override dies, um den Standard-Minecraft-Blur/Dirt-Hintergrund zu verhindern!
        // Wir wollen nur das Spiel kristallklar im Hintergrund sehen.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark translucent background
        context.fill(0, 0, this.width, this.height, 0x88000000);

        // Draw instructions
        context.drawCenteredTextWithShadow(this.textRenderer, "Drag to move modules. Close when done.", this.width / 2,
                20, 0xFFAAAAAA);

        // Render Mods Button in Center
        int btnWidth = 140;
        int btnHeight = 35;
        int btnX = (this.width - btnWidth) / 2;
        int btnY = (this.height - btnHeight) / 2;

        boolean hoveringBtn = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY
                && mouseY <= btnY + btnHeight;
        int btnColor = hoveringBtn ? 0xFF353840 : 0xFF252830;

        drawSmoothRoundedRect(context, btnX, btnY, btnWidth, btnHeight, 8, btnColor);
        context.drawCenteredTextWithShadow(this.textRenderer, "Mods", this.width / 2, btnY + (btnHeight - 8) / 2,
                0xFFFFFFFF);

        // Render Reset Button (Top Right)
        int resetWidth = 60;
        int resetHeight = 20;
        int resetX = this.width - resetWidth - 15;
        int resetY = 15;

        boolean hoveringReset = mouseX >= resetX && mouseX <= resetX + resetWidth && mouseY >= resetY
                && mouseY <= resetY + resetHeight;
        int resetColor = hoveringReset ? 0xFFE04545 : 0xAA252830; // Slight red hover
        drawSmoothRoundedRect(context, resetX, resetY, resetWidth, resetHeight, 4, resetColor);
        context.drawTextWithShadow(this.textRenderer, "Reset",
                resetX + (resetWidth - this.textRenderer.getWidth("Reset")) / 2, resetY + (resetHeight - 8) / 2,
                0xFFFFFFFF);

        // Render Active Modules
        for (Map.Entry<String, HUDConfig.ModuleData> entry : config.getModules().entrySet()) {
            HUDConfig.ModuleData mod = entry.getValue();
            if (mod.enabled) {
                int modX = mod.x;
                int modY = mod.y;
                int modWidth;
                int modHeight;

                // Dynamic sizing based on mode and module
                if (mod.name.equals("Armor Status")) {
                    modWidth = 60; // Enough for icon + "300/300" text
                    modHeight = 18 * 4; // 4 armor slots
                } else if (mod.name.equals("CPS") || mod.name.equals("Ping") || mod.name.equals("Potion Effects")) {
                    String displayText = "[" + mod.name + " (Mode: " + mod.mode + ")]";
                    modWidth = this.textRenderer.getWidth(displayText) + 10;
                    modHeight = 16;
                } else {
                    String displayText = "[" + mod.name + "]";
                    modWidth = this.textRenderer.getWidth(displayText) + 10;
                    modHeight = 16;
                }

                // Active dragging outline & snapping visuals
                if (draggingModule == mod) {
                    drawDashedOutline(context, modX - 2, modY - 2, modWidth + 4, modHeight + 4, 0xFF00FF00);
                } else {
                    boolean hoveringMod = mouseX >= modX && mouseX <= modX + modWidth && mouseY >= modY
                            && mouseY <= modY + modHeight;
                    if (hoveringMod) {
                        drawSmoothRoundedRect(context, modX, modY, modWidth, modHeight, 4, 0x55FFFFFF);
                    }
                }

                if (mod.name.equals("Armor Status")) {
                    context.fill(modX, modY, modX + modWidth, modY + modHeight, 0xAA000000);
                    // Draw Player Armor or Dummy Armor
                    Iterable<ItemStack> armorItems = null;
                    if (this.client != null && this.client.player != null) {
                        armorItems = this.client.player.getArmorItems();
                    }

                    ItemStack[] armorArray = new ItemStack[4];
                    boolean hasAnyArmor = false;

                    if (armorItems != null) {
                        int index = 3; // Armor items are returned from boots to helmet, render helmet at top
                        for (ItemStack stack : armorItems) {
                            armorArray[index--] = stack;
                            if (stack != null && !stack.isEmpty())
                                hasAnyArmor = true;
                        }
                    }

                    if (!hasAnyArmor) {
                        // Dummy rendering so the user can see what they are dragging
                        armorArray[3] = new ItemStack(Items.DIAMOND_HELMET);
                        armorArray[2] = new ItemStack(Items.DIAMOND_CHESTPLATE);
                        armorArray[1] = new ItemStack(Items.DIAMOND_LEGGINGS);
                        armorArray[0] = new ItemStack(Items.DIAMOND_BOOTS);
                    }

                    int currentY = modY;
                    for (int i = 3; i >= 0; i--) {
                        ItemStack stack = armorArray[i];
                        if (stack != null && !stack.isEmpty()) {
                            context.drawItem(stack, modX + 2, currentY + 1);
                            context.drawTextWithShadow(this.textRenderer, "100%", modX + 22, currentY + 5, 0xFFFFFFFF);
                        }
                        currentY += 18;
                    }
                } else if (mod.name.equals("CPS") || mod.name.equals("Ping") || mod.name.equals("Potion Effects")) {
                    String displayText = "[" + mod.name + " (Mode: " + mod.mode + ")]";
                    context.fill(modX, modY, modX + modWidth, modY + modHeight, 0xAA000000);
                    context.drawTextWithShadow(this.textRenderer, displayText, modX + 5, modY + 4, 0xFF55FFFF);
                } else {
                    // Standard Rendering
                    String displayText = "[" + mod.name + "]";
                    context.fill(modX, modY, modX + modWidth, modY + modHeight, 0xAA000000);
                    context.drawTextWithShadow(this.textRenderer, displayText, modX + 5, modY + 4, 0xFFFFFFFF);
                }
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check central 'Mods' button
            int btnWidth = 140;
            int btnHeight = 35;
            int btnX = (this.width - btnWidth) / 2;
            int btnY = (this.height - btnHeight) / 2;
            if (mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight) {
                if (this.client != null) {
                    this.client.setScreen(new LunarSettingsScreen());
                }
                return true;
            }

            // Check Reset Button
            int resetWidth = 60;
            int resetHeight = 20;
            int resetX = this.width - resetWidth - 15;
            int resetY = 15;
            if (mouseX >= resetX && mouseX <= resetX + resetWidth && mouseY >= resetY
                    && mouseY <= resetY + resetHeight) {
                config.resetDefaults();
                return true;
            }

            // Check if clicking a module (reverse order to pick top-most if overlapping)
            HUDConfig.ModuleData clickedMod = null;
            for (Map.Entry<String, HUDConfig.ModuleData> entry : config.getModules().entrySet()) {
                HUDConfig.ModuleData mod = entry.getValue();
                if (mod.enabled) {
                    int modWidth;
                    int modHeight;
                    if (mod.name.equals("Armor Status")) {
                        modWidth = 60;
                        modHeight = 18 * 4;
                    } else if (mod.name.equals("CPS") || mod.name.equals("Ping") || mod.name.equals("Potion Effects")) {
                        String displayText = "[" + mod.name + " (Mode: " + mod.mode + ")]";
                        modWidth = this.textRenderer.getWidth(displayText) + 10;
                        modHeight = 16;
                    } else {
                        String displayText = "[" + mod.name + "]";
                        modWidth = this.textRenderer.getWidth(displayText) + 10;
                        modHeight = 16;
                    }

                    if (mouseX >= mod.x && mouseX <= mod.x + modWidth && mouseY >= mod.y
                            && mouseY <= mod.y + modHeight) {
                        clickedMod = mod;
                        // Calculate drag offset
                        dragOffsetX = (int) (mouseX - mod.x);
                        dragOffsetY = (int) (mouseY - mod.y);
                        break; // Stop at first found (could improve with Z-index sorting later)
                    }
                }
            }
            if (clickedMod != null) {
                this.draggingModule = clickedMod;
                return true;
            }
        } else if (button == 1) { // Right click
            HUDConfig.ModuleData clickedMod = null;
            for (Map.Entry<String, HUDConfig.ModuleData> entry : config.getModules().entrySet()) {
                HUDConfig.ModuleData mod = entry.getValue();
                if (mod.enabled) {
                    int modWidth;
                    int modHeight;
                    if (mod.name.equals("Armor Status")) {
                        modWidth = 60;
                        modHeight = 18 * 4;
                    } else if (mod.name.equals("CPS") || mod.name.equals("Ping") || mod.name.equals("Potion Effects")) {
                        modWidth = this.textRenderer.getWidth("[" + mod.name + " (Mode: " + mod.mode + ")]") + 10;
                        modHeight = 16;
                    } else {
                        modWidth = this.textRenderer.getWidth("[" + mod.name + "]") + 10;
                        modHeight = 16;
                    }
                    if (mouseX >= mod.x && mouseX <= mod.x + modWidth && mouseY >= mod.y
                            && mouseY <= mod.y + modHeight) {
                        clickedMod = mod;
                        break; // Stop at first found (could improve with Z-index sorting later)
                    }
                }
            }
            if (clickedMod != null) {
                // Toggle mode logic based on the requested modules
                if (clickedMod.name.equals("CPS")) {
                    clickedMod.mode = (clickedMod.mode + 1) % 2; // 0 = Text (Left | Right), 1 = Minimal (12 | 14)
                } else if (clickedMod.name.equals("Ping")) {
                    clickedMod.mode = (clickedMod.mode + 1) % 2; // 0 = Text (25 ms), 1 = Icon + Bar
                } else if (clickedMod.name.equals("Potion Effects")) {
                    clickedMod.mode = (clickedMod.mode + 1) % 2; // 0 = Text Time (03:45), 1 = Icon + Bar
                }
                config.save();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingModule != null && button == 0) {
            int newX = (int) mouseX - dragOffsetX;
            int newY = (int) mouseY - dragOffsetY;

            // Snap to Grid (nearest 2 pixels to keep it clean)
            newX = Math.round(newX / 2.0f) * 2;
            newY = Math.round(newY / 2.0f) * 2;

            int modWidth;
            int modHeight;
            if (draggingModule.name.equals("Armor Status")) {
                modWidth = 60;
                modHeight = 18 * 4;
            } else {
                modWidth = this.textRenderer.getWidth("[" + draggingModule.name + "]") + 10;
                modHeight = 16;
            }

            // Mods Button Block Zone Collision Logic
            int btnWidth = 140;
            int btnHeight = 35;
            int btnX = (this.width - btnWidth) / 2;
            int btnY = (this.height - btnHeight) / 2;

            boolean intersectingBtn = (newX < btnX + btnWidth && newX + modWidth > btnX &&
                    newY < btnY + btnHeight && newY + modHeight > btnY);

            if (intersectingBtn) {
                // Ignore the drag attempt or bounce against the edge.
                // We'll simply block the position update entirely to simulate an invisible
                // wall.
                return true;
            }

            // Ensure bounds
            newX = Math.max(0, Math.min(newX, this.width - modWidth));
            newY = Math.max(0, Math.min(newY, this.height - modHeight));

            this.draggingModule.x = newX;
            this.draggingModule.y = newY;

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingModule != null && button == 0) {
            this.draggingModule = null; // stop drag
            config.save(); // save positions on release
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Modern Rendering Utilities
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

    private void drawDashedOutline(DrawContext context, int x, int y, int width, int height, int color) {
        int dashLength = 4;
        // Top & Bottom
        for (int i = 0; i < width; i += dashLength * 2) {
            context.fill(x + i, y, Math.min(x + i + dashLength, x + width), y + 1, color);
            context.fill(x + i, y + height, Math.min(x + i + dashLength, x + width), y + height - 1, color);
        }
        // Left & Right
        for (int i = 0; i < height; i += dashLength * 2) {
            context.fill(x, y + i, x + 1, Math.min(y + i + dashLength, y + height), color);
            context.fill(x + width - 1, y + i, x + width, Math.min(y + i + dashLength, y + height), color);
        }
    }
}
