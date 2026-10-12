package com.castrosafio.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class CastroMenuScreen extends Screen {
    // v1.4: the GUI is composed from independent textured elements, never a full-screen screenshot.
    private static final int REF_W = 1672;
    private static final int REF_H = 941;

    private static final ResourceLocation OUTER_FRAME = tex("outer_frame.png");
    private static final ResourceLocation MAIN_PANEL_FRAME = tex("main_panel_frame.png");
    private static final ResourceLocation QUESTION = tex("question.png");
    private static final ResourceLocation SCROLLBAR = tex("scrollbar.png");

    private static final ResourceLocation HEADER_DIFFICULTY = tex("header_difficulty.png");
    private static final ResourceLocation HEADER_CRAFTS = tex("header_crafts.png");
    private static final ResourceLocation HEADER_MISSIONS = tex("header_missions.png");
    private static final ResourceLocation HEADER_MOBS = tex("header_mobs.png");

    private static final ResourceLocation TAB_DIFFICULTY_NORMAL = tex("tab_difficulty_normal.png");
    private static final ResourceLocation TAB_DIFFICULTY_SELECTED = tex("tab_difficulty_selected.png");
    private static final ResourceLocation TAB_CRAFTS_NORMAL = tex("tab_crafts_normal.png");
    private static final ResourceLocation TAB_CRAFTS_SELECTED = tex("tab_crafts_selected.png");
    private static final ResourceLocation TAB_MISSIONS_NORMAL = tex("tab_missions_normal.png");
    private static final ResourceLocation TAB_MISSIONS_SELECTED = tex("tab_missions_selected.png");
    private static final ResourceLocation TAB_MOBS_NORMAL = tex("tab_mobs_normal.png");
    private static final ResourceLocation TAB_MOBS_SELECTED = tex("tab_mobs_selected.png");

    private Tab selected = Tab.MISSIONS;

    public CastroMenuScreen() {
        super(Component.literal("CastroSafio"));
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CastroSafioMenu.MOD_ID,
                "textures/gui/components/" + name
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF000000);

        PixelBounds b = pixelBounds();
        float poseScale = (float) (b.physicalScale / b.guiScale);

        graphics.pose().pushPose();
        graphics.pose().scale(poseScale, poseScale, 1.0F);

        final int ox = b.textureSpaceX;
        final int oy = b.textureSpaceY;

        // Base areas are drawn by the mod; the resource pack only supplies the visual elements.
        graphics.fill(ox, oy, ox + REF_W, oy + REF_H, 0xFF00050A);
        graphics.fill(ox + 42, oy + 48, ox + 286, oy + 889, 0xFF061019);
        graphics.fill(ox + 278, oy + 60, ox + 1590, oy + 859, 0xFF041019);

        drawSharp(graphics, OUTER_FRAME, ox, oy, 1672, 941);
        drawSharp(graphics, MAIN_PANEL_FRAME, ox + 278, oy + 60, 1312, 799);

        drawTabs(graphics, ox, oy);
        drawSharp(graphics, selected.header, ox + 282, oy + 60, 1305, 123);

        drawMissionGrid(graphics, ox, oy);
        drawSharp(graphics, SCROLLBAR, ox + 1566, oy + 184, 25, 639);

        graphics.pose().popPose();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTabs(GuiGraphics g, int ox, int oy) {
        drawSharp(g,
                selected == Tab.DIFFICULTY ? TAB_DIFFICULTY_SELECTED : TAB_DIFFICULTY_NORMAL,
                ox + 42, oy + 48, 244, 199);
        drawSharp(g,
                selected == Tab.CRAFTS ? TAB_CRAFTS_SELECTED : TAB_CRAFTS_NORMAL,
                ox + 42, oy + 247, 244, 213);
        drawSharp(g,
                selected == Tab.MISSIONS ? TAB_MISSIONS_SELECTED : TAB_MISSIONS_NORMAL,
                ox + 42, oy + 460, 244, 222);
        drawSharp(g,
                selected == Tab.MOBS ? TAB_MOBS_SELECTED : TAB_MOBS_NORMAL,
                ox + 42, oy + 682, 244, 207);
    }

    private void drawMissionGrid(GuiGraphics g, int ox, int oy) {
        final int gridX = ox + 294;
        final int gridY = oy + 187;
        final int cellW = 98;
        final int cellH = 102;
        final int cols = 13;
        final int rows = 6;

        // Grid/background is generated cleanly by code so the pack is not one giant screenshot.
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = gridX + col * cellW;
                int y = gridY + row * cellH;
                g.fill(x, y, x + cellW, y + cellH, 0xFF0D2A3C);
                g.fill(x + 2, y + 2, x + cellW - 2, y + cellH - 2, 0xFF031019);

                int index = row * cols + col;
                if (index < 68) {
                    drawSharp(g, QUESTION, x + 9, y + 7, 79, 87);
                }
            }
        }
    }

    private void drawSharp(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        Minecraft.getInstance().getTextureManager().getTexture(texture).setFilter(false, false);
        g.blit(texture, x, y, w, h, 0, 0, w, h, w, h);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            PixelBounds b = pixelBounds();

            if (mouseX >= b.logicalX && mouseX < b.logicalX + b.logicalW
                    && mouseY >= b.logicalY && mouseY < b.logicalY + b.logicalH) {

                double refX = (mouseX - b.logicalX) * REF_W / b.logicalW;
                double refY = (mouseY - b.logicalY) * REF_H / b.logicalH;

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

        double physicalScale = Math.min(1.0D,
                Math.min(framebufferW / (double) REF_W, framebufferH / (double) REF_H));

        double physicalW = REF_W * physicalScale;
        double physicalH = REF_H * physicalScale;
        double physicalX = (framebufferW - physicalW) / 2.0D;
        double physicalY = (framebufferH - physicalH) / 2.0D;

        int textureSpaceX = (int) Math.round(physicalX / physicalScale);
        int textureSpaceY = (int) Math.round(physicalY / physicalScale);

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
        DIFFICULTY(HEADER_DIFFICULTY),
        CRAFTS(HEADER_CRAFTS),
        MISSIONS(HEADER_MISSIONS),
        MOBS(HEADER_MOBS);

        private final ResourceLocation header;

        Tab(ResourceLocation header) {
            this.header = header;
        }
    }
}
