package com.lux;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class TestDrawContext {
    public void test(DrawContext context) {
        Identifier obsidian = Identifier.of("minecraft", "textures/block/obsidian.png");
        context.drawTexture(obsidian, 0, 0, 0, 0, 16, 16, 16, 16);
    }
}
