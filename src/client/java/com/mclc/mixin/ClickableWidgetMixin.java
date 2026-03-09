package com.lux.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.widget.PressableWidget.class)
public abstract class ClickableWidgetMixin {

    @Shadow
    public abstract void drawMessage(DrawContext context, TextRenderer textRenderer, int color);

    private float luxHoverState = 0f;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    protected void onRenderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null)
            return;
        TextRenderer textRenderer = client.textRenderer;

        ClickableWidget self = (ClickableWidget) (Object) this;

        boolean hovered = self.isHovered() || self.isFocused();
        if (hovered && self.active) {
            this.luxHoverState = MathHelper.clamp(this.luxHoverState + 0.15f * delta, 0f, 1f);
        } else {
            this.luxHoverState = MathHelper.clamp(this.luxHoverState - 0.15f * delta, 0f, 1f);
        }

        int x = self.getX();
        int y = self.getY();
        int width = self.getWidth();
        int height = self.getHeight();

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Modern Glassmorphic Background Color
        int bgColor = 0xAA000000;
        if (!self.active) {
            bgColor = 0x88000000;
        } else if (hovered) {
            // Very slight brightening on hover in the background
            bgColor = 0xBB111111;
        }

        int radius = 4;

        // Draw basic rounded rect background
        fillRoundedRect(context, x, y, width, height, radius, bgColor);

        // Draw border (changes color on hover!)
        int borderColor = 0x55FFFFFF;
        if (self.active) {
            if (this.luxHoverState > 0.01f) {
                // Approximate lerp using alpha and color mixing logic manually
                // We want to fade from white(ish) to Cyan (0x00FFCC)
                int r = (int) (0xFF * (1 - this.luxHoverState) + 0x00 * this.luxHoverState);
                int g = (int) (0xFF * (1 - this.luxHoverState) + 0xFF * this.luxHoverState);
                int b = (int) (0xFF * (1 - this.luxHoverState) + 0xCC * this.luxHoverState);
                int a = (int) (0x55 * (1 - this.luxHoverState) + 0xCC * this.luxHoverState);
                borderColor = (a << 24) | (r << 16) | (g << 8) | b;
            }
            drawRoundedBorder(context, x, y, width, height, radius, borderColor);
        } else {
            drawRoundedBorder(context, x, y, width, height, radius, 0x44FFFFFF);
        }

        // Draw Text
        int textColor = self.active ? 0xFFFFFF : 0xA0A0A0;

        // Very subtle text shift when hovered
        if (self.active && this.luxHoverState > 0.5f) {
            textColor = 0x55FFCC; // Text turns subtle cyan when hovered
        }

        this.drawMessage(context, textRenderer, textColor);

        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private void fillRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        context.fill(x + radius, y, x + width - radius, y + height, color); // vertical center
        context.fill(x, y + radius, x + radius, y + height - radius, color); // left side
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color); // right side

        // Basic corners
        context.fill(x + 1, y + 1, x + radius, y + radius, color); // top left
        context.fill(x + width - radius, y + 1, x + width - 1, y + radius, color); // top right
        context.fill(x + 1, y + height - radius, x + radius, y + height - 1, color); // bottom left
        context.fill(x + width - radius, y + height - radius, x + width - 1, y + height - 1, color); // bottom right

        // Cleanup 1px corner bleed
        int cornerBg = 0x00000000;
        context.fill(x, y, x + 1, y + 1, cornerBg);
        context.fill(x + width - 1, y, x + width, y + 1, cornerBg);
        context.fill(x, y + height - 1, x + 1, y + height, cornerBg);
        context.fill(x + width - 1, y + height - 1, x + width, y + height, cornerBg);
    }

    private void drawRoundedBorder(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        // Horizontal
        context.fill(x + radius, y, x + width - radius, y + 1, color); // Top
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, color); // Bottom
        // Vertical
        context.fill(x, y + radius, x + 1, y + height - radius, color); // Left
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, color); // Right

        // Corners
        context.fill(x + 1, y + 1, x + 2, y + 2, color); // TL
        context.fill(x + width - 2, y + 1, x + width - 1, y + 2, color); // TR
        context.fill(x + 1, y + height - 2, x + 2, y + height - 1, color); // BL
        context.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, color); // BR
    }
}
