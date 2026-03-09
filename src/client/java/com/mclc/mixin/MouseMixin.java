package com.lux.mixin;

import com.lux.LuxModClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1) { // GLFW.GLFW_PRESS
            if (button == 0) {
                LuxModClient.addLeftClick();
            } else if (button == 1) {
                LuxModClient.addRightClick();
            }
        }
    }
}
