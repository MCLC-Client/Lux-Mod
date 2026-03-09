package com.lux.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoDrawer.class)
public class LogoDrawerMixin {
    private static boolean luxIconsPrinted = false;

    @Inject(method = "draw(Lnet/minecraft/client/gui/DrawContext;IFI)V", at = @At("HEAD"), cancellable = true)
    public void onDrawWithY(DrawContext context, int screenWidth, float alpha, int y, CallbackInfo ci) {
        ci.cancel();
        drawCustomLogo(context, screenWidth, y);
    }

    @Inject(method = "draw(Lnet/minecraft/client/gui/DrawContext;IF)V", at = @At("HEAD"), cancellable = true)
    public void onDraw(DrawContext context, int screenWidth, float alpha, CallbackInfo ci) {
        if (!luxIconsPrinted) {
            com.lux.TestIcon.printIcons();
            luxIconsPrinted = true;
        }
        ci.cancel();
        drawCustomLogo(context, screenWidth, 30);
    }

    private void drawCustomLogo(DrawContext context, int screenWidth, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null)
            return;

        // Draw Lux Client
        Text titleText = Text.literal("")
                .append(Text.literal("MC").formatted(net.minecraft.util.Formatting.GREEN))
                .append(Text.literal("LC").formatted(net.minecraft.util.Formatting.AQUA))
                .append(Text.literal(" Client").formatted(net.minecraft.util.Formatting.WHITE));

        // Draw Obsidian Background
        net.minecraft.util.Identifier obsidian = net.minecraft.util.Identifier.of("minecraft",
                "textures/block/obsidian.png");
        int textWidth = client.textRenderer.getWidth(titleText) * 3;
        int bgWidth = textWidth + 40;
        int bgHeight = 55;
        int startX = (screenWidth - bgWidth) / 2;
        int startY = y - 10;

        int radius = 8;
        int borderSize = 2; // 2px border requested by user

        // 1. Draw the Black Border (Outer rounded rect)
        fillRoundedRect(context, startX, startY, bgWidth, bgHeight, radius, 0xFF000000);

        // 2. Draw the Tiled Obsidian Background (Inner rounded rect)
        // Texture tiled row-by-row to perfectly mask into the rounded corners
        fillRoundedTiledTexture(context, obsidian, startX + borderSize, startY + borderSize,
                bgWidth - borderSize * 2, bgHeight - borderSize * 2, Math.max(0, radius - borderSize));

        // Draw the main Title
        context.getMatrices().push();
        context.getMatrices().scale(3.0f, 3.0f, 3.0f);
        context.drawCenteredTextWithShadow(client.textRenderer, titleText, screenWidth / 2 / 3, y / 3,
                0xFFFFFF);
        context.getMatrices().pop();

        // Draw Version (Minecraft version replacement), slanted over the title
        String clientVersion = net.minecraft.SharedConstants.getGameVersion().getName();
        context.getMatrices().push();
        // Move to the position over the 't' in the "Client" text
        // Keep Z-axis at 50f to ensure it renders *on top* of the Client text
        context.getMatrices().translate(screenWidth / 2f + 85f, y + 8f, 50f);
        // Rotate 20 degrees clockwise to slant from top-left to bottom-right
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(20.0f));
        // Scale the text
        context.getMatrices().scale(1.5f, 1.5f, 1.5f);
        // Draw centered at the new Pivot (0, 0)
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(clientVersion), 0, 0, 0xFFFF00);
        context.getMatrices().pop();
    }

    private void fillRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        for (int i = 0; i < height; i++) {
            int dx = 0;
            if (i < radius) {
                float dy = radius - i;
                dx = (int) Math.round(radius - Math.sqrt(radius * radius - dy * dy));
            } else if (i >= height - radius) {
                float dy = i - (height - radius) + 1;
                dx = (int) Math.round(radius - Math.sqrt(radius * radius - dy * dy));
            }
            context.fill(x + dx, y + i, x + width - dx, y + i + 1, color);
        }
    }

    private void fillRoundedTiledTexture(DrawContext context, net.minecraft.util.Identifier texture, int x, int y,
            int width, int height, int radius) {
        for (int i = 0; i < height; i++) {
            int dx = 0;
            if (i < radius) {
                float dy = radius - i;
                dx = (int) Math.round(radius - Math.sqrt(radius * radius - dy * dy));
            } else if (i >= height - radius) {
                float dy = i - (height - radius) + 1;
                dx = (int) Math.round(radius - Math.sqrt(radius * radius - dy * dy));
            }

            int rowX = x + dx;
            int rowW = width - 2 * dx;
            int rowY = y + i;

            // Texture UV offsetting to keep it properly tiled
            int v = (rowY - y) % 16;
            if (v < 0)
                v += 16;

            int currentX = rowX;
            int remainingW = rowW;
            int u = (rowX - x) % 16;
            if (u < 0)
                u += 16;

            while (remainingW > 0) {
                int drawW = Math.min(16 - u, remainingW);
                context.drawTexture(texture, currentX, rowY, u, v, drawW, 1, 16, 16);

                currentX += drawW;
                remainingW -= drawW;
                u = 0; // The next segment will start cleanly at u=0
            }
        }
    }
}
