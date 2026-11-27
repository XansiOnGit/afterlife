package net.xansidev.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.xansidev.Afterlife;
import net.xansidev.AfterlifeConfig;
import net.xansidev.network.LivesPayload;

/** Draws the life heart between the health and hunger bars, above the XP level number. */
public final class HeartHud {
    private HeartHud() {}

    /** XP level number top is 35px above the screen bottom; keep the icon's bottom 3px above it. */
    private static final int BOTTOM_GAP = 38;

    private static final Identifier L5 = sprite("afterlife_5"); // animated
    private static final Identifier L3 = sprite("afterlife_3");
    private static final Identifier L2 = sprite("afterlife_2");
    private static final Identifier L1 = sprite("afterlife_1");
    private static final Identifier L0 = sprite("afterlife_0"); // 0 and below

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(Afterlife.MOD_ID, "hud/" + name);
    }

    static Identifier spriteFor(int lives) {
        if (lives >= 5) return L5;
        if (lives >= 3) return L3;
        if (lives == 2) return L2;
        if (lives == 1) return L1;
        return L0;
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.FOOD_BAR,
            Identifier.fromNamespaceAndPath(Afterlife.MOD_ID, "life_heart"),
            HeartHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        AfterlifeConfig cfg = AfterlifeConfig.get();
        if (!cfg.heartHudEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.player.isSpectator() || mc.player.isCreative()) return;

        int lives = AfterlifeClient.lives();
        if (lives == LivesPayload.NO_SCORE) return;

        int size = cfg.heartHudSize;
        int x = graphics.guiWidth() / 2 - size / 2 + cfg.heartHudOffsetX;
        int y = graphics.guiHeight() - BOTTOM_GAP - size + cfg.heartHudOffsetY;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteFor(lives), x, y, size, size);
    }
}