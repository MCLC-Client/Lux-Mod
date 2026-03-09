package com.lux.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private ResourceReload reload;
    @Shadow
    private long reloadStartTime;
    @Shadow
    private long reloadCompleteTime;
    @Shadow
    private boolean reloading;

    private Consumer<Optional<Throwable>> lux$exceptionHandler;

    private static final Identifier LOGO = Identifier.of("lux", "textures/gui/icon.png");
    private float smoothedProgress = 0.0f;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initAdjustments(MinecraftClient client, ResourceReload reload,
            Consumer<Optional<Throwable>> exceptionHandler, boolean reloading, CallbackInfo ci) {
        this.lux$exceptionHandler = exceptionHandler;
        // Prevent red background flashing, use very dark violet
        client.getFramebuffer().setClearColor(0.039f, 0.016f, 0.071f, 1.0f); // #0a0412
        client.getFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);

        // Register texture manually to ensure it's loaded before the first frame
        client.getTextureManager().registerTexture(LOGO, new net.minecraft.client.texture.ResourceTexture(LOGO));
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderCustomOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Cancel the original Mojang render method to natively suppress their drawing
        ci.cancel();

        if (this.client == null || this.reload == null) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        long time = Util.getMeasuringTimeMs();

        // 1. Restore critical vanilla time tracking state machine
        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = time;
        }

        float fadeOut = this.reloadCompleteTime > -1L ? (float) (time - this.reloadCompleteTime) / 1000.0F : -1.0F;
        float fadeIn = this.reloadStartTime > -1L ? (float) (time - this.reloadStartTime) / 1000.0F : -1.0F;

        // Custom lerped progress tracking
        float rawProgress = this.reload.getProgress();
        if (rawProgress >= 0.95f)
            rawProgress = 1.0f;
        this.smoothedProgress = MathHelper.lerp(0.15f, this.smoothedProgress, rawProgress);
        if (this.smoothedProgress >= 0.99f)
            this.smoothedProgress = 1.0f;

        // 2. CRITICAL: Replicate vanilla's completion and hand-off to TitleScreen
        if (fadeOut >= 1.0F) {
            this.client.setOverlay(null);
        }

        // Vanilla exception handling and TitleScreen init trigger
        if (this.reloadCompleteTime == -1L && this.reload.isComplete() && (!this.reloading || fadeIn >= 2.0F)) {
            try {
                this.reload.throwException();
                if (this.lux$exceptionHandler != null) {
                    this.lux$exceptionHandler.accept(Optional.empty());
                }
            } catch (Throwable var23) {
                if (this.lux$exceptionHandler != null) {
                    this.lux$exceptionHandler.accept(Optional.of(var23));
                }
            }
            this.reloadCompleteTime = time;

            // This is the CRITICAL missing piece from previous versions!
            // Without initializing the TitleScreen, the next screen hangs or renders
            // missing buttons.
            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(this.client, context.getScaledWindowWidth(),
                        context.getScaledWindowHeight());
            }
        }

        // Fast fade-out transition logic mapped to 500ms
        float alpha = 1.0F;
        if (fadeIn >= 0.0F && fadeOut < 0.0F) {
            alpha = MathHelper.clamp(fadeIn / 0.5f, 0.0F, 1.0F);
        } else if (fadeOut >= 0.0F) {
            alpha = 1.0F - MathHelper.clamp(fadeOut / 0.5F, 0.0F, 1.0F);
        }

        // If we are fading out, render the main menu screen underneath so we can see it
        // Do this BEFORE the purple overlay covers it
        if (fadeOut >= 0.0f && this.client.currentScreen != null) {
            this.client.currentScreen.render(context, mouseX, mouseY, delta);
        }

        if (alpha > 0.0f) {
            RenderSystem.enableBlend();

            // Background: Dark Violet to Black Gradient
            int bgAlpha = (int) (alpha * 255);
            if (bgAlpha > 0) {
                int bgTop = (bgAlpha << 24) | 0x0A0412;
                int bgBot = (bgAlpha << 24) | 0x000000;
                context.fillGradient(0, 0, width, height, bgTop, bgBot);
            }

            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

            // Draw Logo safely using the registered NativeImage
            RenderSystem.setShaderTexture(0, LOGO);
            int logoWidth = 120;
            int logoHeight = 120;

            context.getMatrices().push();
            context.getMatrices().translate(width / 2.0, height / 2.0 - 20, 0);
            context.drawTexture(LOGO, -logoWidth / 2, -logoHeight / 2, 0, 0, logoWidth, logoHeight, logoWidth,
                    logoHeight);
            context.getMatrices().pop();

            // Thin Progress Bar
            int barWidth = 240;
            int barHeight = 4;
            int barX = (width - barWidth) / 2;
            int barY = height / 2 + 70;

            // (Removed text rendering to prevent un-loaded font missing character
            // rectangles)

            // Alpha applied colors for the violet theme
            int brightAlpha = (int) (alpha * 255);
            int bgColor = 0xFF140A21; // Dark track
            int progressColor = 0xFF9D50BB; // Requested light violet
            int glowColor = 0xFF9D50BB; // Glow same color as requested

            int finalBgColor = (bgColor & 0x00FFFFFF) | (brightAlpha << 24);
            int finalProgressColor = (progressColor & 0x00FFFFFF) | (brightAlpha << 24);
            int finalGlowColor = (glowColor & 0x00FFFFFF) | ((int) (alpha * 80) << 24); // Softer glow transparency

            // Empty Track
            drawRoundedRect(context, barX, barY, barWidth, barHeight, finalBgColor);

            int currentFill = (int) (barWidth * this.smoothedProgress);
            if (currentFill > 0) {
                // Soft glowing drop drop-shadow
                drawRoundedRect(context, barX - 2, barY - 2, currentFill + 4, barHeight + 4, finalGlowColor);
                // Progress Fill
                drawRoundedRect(context, barX, barY, currentFill, barHeight, finalProgressColor);
            }

            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0)
            return;
        int radius = height / 2;
        if (width < radius * 2) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x + 1, y, x + radius, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - radius, y, x + width - 1, y + height, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
