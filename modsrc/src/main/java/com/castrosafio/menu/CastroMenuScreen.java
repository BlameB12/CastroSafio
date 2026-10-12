package com.castrosafio.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class CastroMenuScreen extends Screen {
    private static final int TEX_W = 1672;
    private static final int TEX_H = 941;

    private static final ResourceLocation TEX_DIFFICULTY = ResourceLocation.fromNamespaceAndPath(
            CastroSafioMenu.MOD_ID, "textures/gui/menu_difficulty.png");
    private static final ResourceLocation TEX_CRAFTS = ResourceLocation.fromNamespaceAndPath(
            CastroSafioMenu.MOD_ID, "textures/gui/menu_crafts.png");
    private static final ResourceLocation TEX_MISSIONS = ResourceLocation.fromNamespaceAndPath(
            CastroSafioMenu.MOD_ID, "textures/gui/menu_missions.png");
    private static final ResourceLocation TEX_MOBS = ResourceLocation.fromNamespaceAndPath(
            CastroSafioMenu.MOD_ID, "textures/gui/menu_mobs.png");

    private Tab selected = Tab.MISSIONS;

    public CastroMenuScreen() {
        super(Component.literal("CastroSafio"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF000000);

        Minecraft mc = Minecraft.getInstance();

        // Pixel-art must not use bilinear filtering.
        mc.getTextureManager().getTexture(selected.texture).setFilter(false, false);

        PixelBounds b = pixelBounds();

        /*
         * Minecraft draws Screens in GUI-scaled coordinates. On GUI scale 2/3/4,
         * drawing a 1672x941 texture directly to screen.width x screen.height first
         * downsamples the image and then scales the GUI framebuffer back up. That is
         * why the whole menu looked blurred even with NEAREST filtering enabled.
         *
         * Here we cancel Minecraft's GUI scale with the pose transform and render
         * using the real framebuffer pixel dimensions. This keeps the supplied PNG
         * effectively pixel-for-pixel on screen and only applies a tiny fit scale if
         * the physical window is smaller than the source image.
         */
        float poseScale = (float) (b.physicalScale / b.guiScale);
        graphics.pose().pushPose();
        graphics.pose().scale(poseScale, poseScale, 1.0F);
        graphics.blit(selected.texture,
                b.textureSpaceX, b.textureSpaceY,
                TEX_W, TEX_H,
                0, 0,
                TEX_W, TEX_H,
                TEX_W, TEX_H);
        graphics.pose().popPose();

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            PixelBounds b = pixelBounds();

            if (mouseX >= b.logicalX && mouseX < b.logicalX + b.logicalW
                    && mouseY >= b.logicalY && mouseY < b.logicalY + b.logicalH) {

                double refX = (mouseX - b.logicalX) * TEX_W / b.logicalW;
                double refY = (mouseY - b.logicalY) * TEX_H / b.logicalH;

                if (refX >= 42 && refX <= 286) {
                    if (refY >= 48 && refY < 247) {
                        selected = Tab.DIFFICULTY;
                        return true;
                    }
                    if (refY >= 247 && refY < 460) {
                        selected = Tab.CRAFTS;
                        return true;
                    }
                    if (refY >= 460 && refY < 682) {
                        selected = Tab.MISSIONS;
                        return true;
                    }
                    if (refY >= 682 && refY < 889) {
                        selected = Tab.MOBS;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_G) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private PixelBounds pixelBounds() {
        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();
        int framebufferW = mc.getWindow().getWidth();
        int framebufferH = mc.getWindow().getHeight();

        // Never enlarge the source image. If needed, shrink just enough to fit.
        double physicalScale = Math.min(1.0D,
                Math.min(framebufferW / (double) TEX_W, framebufferH / (double) TEX_H));

        double physicalW = TEX_W * physicalScale;
        double physicalH = TEX_H * physicalScale;
        double physicalX = (framebufferW - physicalW) / 2.0D;
        double physicalY = (framebufferH - physicalH) / 2.0D;

        // Coordinates used before the pose scale is applied.
        int textureSpaceX = (int) Math.round(physicalX / physicalScale);
        int textureSpaceY = (int) Math.round(physicalY / physicalScale);

        // Mouse coordinates are still in normal GUI logical coordinates.
        double logicalX = physicalX / guiScale;
        double logicalY = physicalY / guiScale;
        double logicalW = physicalW / guiScale;
        double logicalH = physicalH / guiScale;

        return new PixelBounds(
                guiScale,
                physicalScale,
                textureSpaceX,
                textureSpaceY,
                logicalX,
                logicalY,
                logicalW,
                logicalH
        );
    }

    private record PixelBounds(
            double guiScale,
            double physicalScale,
            int textureSpaceX,
            int textureSpaceY,
            double logicalX,
            double logicalY,
            double logicalW,
            double logicalH
    ) {}

    private enum Tab {
        DIFFICULTY(TEX_DIFFICULTY),
        CRAFTS(TEX_CRAFTS),
        MISSIONS(TEX_MISSIONS),
        MOBS(TEX_MOBS);

        private final ResourceLocation texture;

        Tab(ResourceLocation texture) {
            this.texture = texture;
        }
    }
}
