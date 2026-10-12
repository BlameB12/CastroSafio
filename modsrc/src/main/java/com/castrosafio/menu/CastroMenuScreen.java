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

        Bounds b = bounds();
        graphics.blit(selected.texture, b.x, b.y, b.w, b.h,
                0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Bounds b = bounds();
            if (mouseX >= b.x && mouseX < b.x + b.w && mouseY >= b.y && mouseY < b.y + b.h) {
                double refX = (mouseX - b.x) * TEX_W / b.w;
                double refY = (mouseY - b.y) * TEX_H / b.h;

                // Zona de las cuatro pestañas del lateral izquierdo, medida sobre la textura 1672x941.
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

    private Bounds bounds() {
        double textureAspect = TEX_W / (double) TEX_H;
        double screenAspect = width / (double) height;

        int drawW;
        int drawH;
        if (screenAspect > textureAspect) {
            drawH = height;
            drawW = (int) Math.round(drawH * textureAspect);
        } else {
            drawW = width;
            drawH = (int) Math.round(drawW / textureAspect);
        }

        int drawX = (width - drawW) / 2;
        int drawY = (height - drawH) / 2;
        return new Bounds(drawX, drawY, drawW, drawH);
    }

    private record Bounds(int x, int y, int w, int h) {}

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
