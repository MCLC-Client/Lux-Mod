package com.mclc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.TitleScreen;

@Mixin({ Screen.class, TitleScreen.class })
public abstract class ScreenMixin {

    private static final net.minecraft.util.Identifier CUSTOM_BACKGROUND = net.minecraft.util.Identifier.of("mclc",
            "textures/gui/background.png");

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderCustomBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isMenuScreen = client.world == null
                || (Object) this instanceof net.minecraft.client.gui.screen.world.LevelLoadingScreen
                || (Object) this instanceof net.minecraft.client.gui.screen.DownloadingTerrainScreen
                || (Object) this instanceof net.minecraft.client.gui.screen.ProgressScreen
                || (Object) this instanceof net.minecraft.client.gui.screen.MessageScreen
                || (Object) this instanceof net.minecraft.client.gui.screen.DisconnectedScreen;

        if (isMenuScreen) {
            ci.cancel();

            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // Bind the texture specifically and set GL_LINEAR for HD smooth scaling to
            // avoid pixelation
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, CUSTOM_BACKGROUND);
            com.mojang.blaze3d.systems.RenderSystem.texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
                    org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
            com.mojang.blaze3d.systems.RenderSystem.texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
                    org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);

            // Draw fullscreen custom background
            context.drawTexture(CUSTOM_BACKGROUND, 0, 0, 0, 0, self.width, self.height, self.width, self.height);

            // Draw a slight dark overlay on top to ensure text readability
            context.fill(0, 0, self.width, self.height, 0x44000000);

            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }
}
