package com.lux.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Universal TitleScreenMixin intended for 1.21.1 through 1.21.11.
 * Supports legacy immediate-mode GUI (1.21.1) and modern Layout + Widget GUI
 * (1.21.5+).
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends net.minecraft.client.gui.screen.Screen {

    private static boolean luxIconsPrinted = false;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 1. init() — Hide Realms button, hide 1.21.5+ widgets, shift remaining buttons
    // ────────────────────────────────────────────────────────────────────────────
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;

        // Recursively hunt down widgets specific to 1.21.5+ (LogoWidget,
        // SplashTextWidget, etc)
        // as well as the Realms button
        hideVanillaWidgets(screen.children());

        // Find the Realms button's original Y position to shift everything else
        ButtonWidget realmsBtn = findRealmsButton(screen.children());
        if (realmsBtn == null)
            return;

        int removedY = realmsBtn.getY();
        int shift = realmsBtn.getHeight() + 4;

        // Shift buttons
        shiftButtons(screen.children(), removedY, shift);

        // Pin accessibility/language
        pinOtherWidgets(screen.children(), this.height);
    }

    private void hideVanillaWidgets(Iterable<? extends Element> elements) {
        if (elements == null)
            return;
        for (Element element : elements) {
            String className = element.getClass().getSimpleName();

            // Hide 1.21.5+ specific widgets for Logos and Splash text
            if (className.contains("Logo") || className.contains("Splash")) {
                if (element instanceof ClickableWidget cw) {
                    cw.visible = false;
                    cw.active = false;
                    cw.setWidth(0);
                    cw.setHeight(0);
                }
            }

            // Hide Copyright text if it's a widget
            if (element instanceof ClickableWidget cw && cw.getMessage() != null) {
                if (cw.getMessage().getString().contains("Copyright Mojang AB")) {
                    cw.visible = false;
                    cw.active = false;
                    cw.setWidth(0);
                    cw.setHeight(0);
                }
            }

            // Also hide Realms button directly just to be 100% sure
            if (element instanceof ButtonWidget btn
                    && btn.getMessage().getContent() instanceof TranslatableTextContent t
                    && t.getKey().equals("menu.online")) {
                btn.visible = false;
                btn.active = false;
                btn.setWidth(0);
                btn.setHeight(0);
            }

            if (element instanceof net.minecraft.client.gui.ParentElement parent) {
                hideVanillaWidgets(parent.children());
            }
        }
    }

    private ButtonWidget findRealmsButton(Iterable<? extends Element> elements) {
        if (elements == null)
            return null;
        for (Element element : elements) {
            if (element instanceof ButtonWidget btn
                    && btn.getMessage().getContent() instanceof TranslatableTextContent t
                    && t.getKey().equals("menu.online")) {
                return btn;
            } else if (element instanceof net.minecraft.client.gui.ParentElement parent) {
                ButtonWidget found = findRealmsButton(parent.children());
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private void shiftButtons(Iterable<? extends Element> elements, int removedY, int shift) {
        if (elements == null)
            return;
        for (Element element : elements) {
            if (element instanceof ButtonWidget btn && btn.visible && btn.getY() > removedY) {
                btn.setY(btn.getY() - shift);
            } else if (element instanceof net.minecraft.client.gui.ParentElement parent) {
                shiftButtons(parent.children(), removedY, shift);
            }
        }
    }

    private void pinOtherWidgets(Iterable<? extends Element> elements, int screenHeight) {
        if (elements == null)
            return;
        for (Element element : elements) {
            if (element instanceof ClickableWidget cw && !(element instanceof ButtonWidget) && cw.visible
                    && cw.getY() > screenHeight / 2) {
                cw.setY(screenHeight - 24);
            } else if (element instanceof net.minecraft.client.gui.ParentElement parent) {
                pinOtherWidgets(parent.children(), screenHeight);
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 3. render() overrides for 1.21.1 legacy elements + Drawing the Custom Logo
    // ────────────────────────────────────────────────────────────────────────────

    // Intercept 1.21.1 LogoDrawer calls and NO-OP them to prevent vanilla logo
    // (require=0 avoids 1.21.5 crashes)
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IFI)V"), require = 0)
    private void hideLegacyLogoWithY(net.minecraft.client.gui.LogoDrawer instance, DrawContext context, int screenWidth,
            float alpha, int y) {
        // Ignored
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"), require = 0)
    private void hideLegacyLogo(net.minecraft.client.gui.LogoDrawer instance, DrawContext context, int screenWidth,
            float alpha) {
        // Ignored
    }

    // Intercept 1.21.1 Copyright text drawing and NO-OP it (require=0 avoids 1.21.5
    // crashes)
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"), require = 0)
    private int hideLegacyCopyrightText(DrawContext instance, net.minecraft.client.font.TextRenderer textRenderer,
            Text text, int x, int y, int color) {
        if (text != null && text.getString().contains("Copyright Mojang AB")) {
            return x;
        }
        return instance.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    // At the end of rendering the TitleScreen, render our beautiful Lux Client
    // logo
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!luxIconsPrinted) {
            com.lux.TestIcon.printIcons();
            luxIconsPrinted = true;
        }

        // Use proper rendering blending to avoid black rectangles
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        // Slightly lower Y offset (40) since the background is immersive and buttons
        // are lower
        drawCustomLogo(context, this.width, 40);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Custom Logo Rendering Implementation
    // ────────────────────────────────────────────────────────────────────────────

    private void drawCustomLogo(DrawContext context, int screenWidth, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null)
            return;

        Text titleText = Text.literal("")
                .append(Text.literal("MC").formatted(net.minecraft.util.Formatting.GREEN))
                .append(Text.literal("LC").formatted(net.minecraft.util.Formatting.AQUA))
                .append(Text.literal(" Client").formatted(net.minecraft.util.Formatting.WHITE));

        net.minecraft.util.Identifier obsidian = net.minecraft.util.Identifier.of("minecraft",
                "textures/block/obsidian.png");
        int textWidth = client.textRenderer.getWidth(titleText) * 3;
        int bgWidth = textWidth + 40;
        int bgHeight = 55;
        int startX = (screenWidth - bgWidth) / 2;
        int startY = y - 10;

        int radius = 8;
        int borderSize = 2;

        fillRoundedRect(context, startX, startY, bgWidth, bgHeight, radius, 0xFF000000);
        fillRoundedTiledTexture(context, obsidian, startX + borderSize, startY + borderSize, bgWidth - borderSize * 2,
                bgHeight - borderSize * 2, Math.max(0, radius - borderSize));

        context.getMatrices().push();
        context.getMatrices().scale(3.0f, 3.0f, 3.0f);
        context.drawCenteredTextWithShadow(client.textRenderer, titleText, screenWidth / 2 / 3, y / 3, 0xFFFFFF);
        context.getMatrices().pop();

        String clientVersion = net.minecraft.SharedConstants.getGameVersion().getName();
        context.getMatrices().push();
        context.getMatrices().translate(screenWidth / 2f + 85f, y + 8f, 50f);
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(20.0f));
        context.getMatrices().scale(1.5f, 1.5f, 1.5f);
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
                u = 0;
            }
        }
    }
}
