package com.castrosafio.revive;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@Mod(value = CastroRevive.MOD_ID, dist = Dist.CLIENT)
public final class CastroReviveClient {
    private static final Vec3 X = new Vec3(1, 0, 0);
    private static final Vec3 Y = new Vec3(0, 1, 0);
    private static final Vec3 Z = new Vec3(0, 0, 1);

    private static final int CYAN = 0x16F2FF;
    private static final int GREEN = 0x9CFF2E;
    private static final int YELLOW = 0xFFF23A;
    private static final int WHITE = 0xFFFFFF;

    public CastroReviveClient() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = event.getPoseStack().last().pose();

        List<RitualView> rituals = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand)) continue;
            Component name = stand.getCustomName();
            if (name == null) continue;
            String raw = name.getString();
            if (!raw.startsWith(CastroRevive.FX_PREFIX)) continue;

            String[] parts = raw.split("\\|", 3);
            if (parts.length < 2) continue;
            try {
                long start = Long.parseLong(parts[1]);
                double t = mc.level.getGameTime() - start + partial;
                if (t >= -2.0 && t <= 344.0) {
                    rituals.add(new RitualView(stand.position(), t));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (rituals.isEmpty()) return;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buffer = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Geometry geo = new Geometry(buffer, pose, camera);

        for (RitualView ritual : rituals) {
            drawRitual(geo, ritual.center, ritual.tick);
        }

        MeshData mesh = buffer.build();
        if (mesh != null) {
            RenderType.lightning().draw(mesh);
        }
    }

    private static void drawRitual(Geometry g, Vec3 c, double t) {
        // Phase 1: small luminous seed rising over the altar.
        if (t >= 0 && t < 55) {
            double p = smooth(t / 55.0);
            Vec3 orb = c.add(0, 1.6 + p * 4.3, 0);
            double pulse = 0.18 + 0.05 * Math.sin(t * 0.25);
            glowOrb(g, orb, pulse, 1.0);
            glowRing(g, orb.add(0, -0.12, 0), 0.32 + p * 0.38, X, Z, 0.06, 0.7, t * 0.03);
        }

        // Phase 2: three clean continuous rings in the sky. No vanilla particles.
        if (t >= 32 && t < 132) {
            double p = smooth((t - 32) / 90.0);
            double fade = envelope(t, 32, 52, 116, 132);
            Vec3 sky = c.add(0, 5.45 + p * 1.1, 0);
            double r = 0.8 + p * 2.4;

            Basis b1 = tiltedBasis(0.10 + Math.sin(t * 0.018) * 0.18, t * 0.012);
            Basis b2 = tiltedBasis(0.50 + Math.sin(t * 0.021) * 0.16, -t * 0.009 + 1.9);
            Basis b3 = tiltedBasis(-0.43 + Math.cos(t * 0.017) * 0.15, t * 0.010 + 0.7);

            glowRing(g, sky, r, b1.u, b1.v, 0.095, fade, t * 0.012);
            glowRing(g, sky.add(0, 0.03, 0), r + 0.28, b2.u, b2.v, 0.085, fade * 0.94, t * 0.014 + 1.4);
            glowRing(g, sky.add(0, -0.03, 0), r + 0.52, b3.u, b3.v, 0.075, fade * 0.90, -t * 0.011 + 2.2);
            glowOrb(g, sky, 0.16 + p * 0.05, fade);
        }

        // Phase 3: the large geometric diamond/square visible behind the floating rings.
        if (t >= 92 && t < 175) {
            double p = smooth((t - 92) / 70.0);
            double fade = envelope(t, 92, 110, 157, 175);
            double size = 1.5 + p * 5.8;
            Vec3 center = c.add(0, 6.0, 0);
            glowingSquare(g, center, size, t * 0.0045, 0.09, fade);
            glowingSquare(g, center.add(0, -0.08, 0), size * 0.73, -t * 0.006 + Math.PI / 4.0, 0.07, fade * 0.65);
        }

        // Phase 4: custom star-like geometry and a broad aura, matching the reference without using particles.
        if (t >= 125 && t < 285) {
            double fade = envelope(t, 125, 155, 260, 285);
            drawStars(g, c, t, fade);
        }

        // Phase 5: huge ground rings / translucent energy field.
        if (t >= 150 && t < 315) {
            double p = smooth((t - 150) / 145.0);
            double fade = envelope(t, 150, 172, 292, 315);
            double radius = 2.2 + p * 9.4;
            Vec3 floor = c.add(0, 0.12, 0);

            glowRing(g, floor, radius, X, Z, 0.13, fade * 0.75, t * 0.004);
            glowRing(g, floor.add(0, 0.06, 0), radius * 0.72, X, Z, 0.12, fade * 0.9, -t * 0.005 + 1.3);
            glowRing(g, floor.add(0, 0.12, 0), radius * 0.43, X, Z, 0.11, fade, t * 0.007 + 2.0);

            // Soft disc-like glow made from wide continuous bands rather than particle clouds.
            for (int i = 1; i <= 8; i++) {
                double rr = radius * i / 8.0;
                annulus(g, floor.add(0, -0.015, 0), rr, X, Z, radius / 8.0 + 0.06,
                        CYAN, GREEN, (int) (8 * fade), 96, t * 0.002 + i * 0.35);
            }
        }

        // Phase 6: smooth orbital cage around the altar.
        if (t >= 175 && t < 325) {
            double p = smooth((t - 175) / 125.0);
            double fade = envelope(t, 175, 200, 305, 325);
            double radius = 2.0 + p * 5.4;
            Vec3 orbCenter = c.add(0, 2.2, 0);

            Basis a = tiltedBasis(1.15, t * 0.010);
            Basis b = tiltedBasis(-0.75, -t * 0.013 + 1.6);
            Basis d = tiltedBasis(0.35, t * 0.016 + 3.2);
            glowRing(g, orbCenter, radius, a.u, a.v, 0.07, fade * 0.8, t * 0.011);
            glowRing(g, orbCenter, radius * 0.92, b.u, b.v, 0.065, fade * 0.85, -t * 0.013);
            glowRing(g, orbCenter, radius * 0.78, d.u, d.v, 0.06, fade * 0.75, t * 0.016 + 2.0);
        }

        // Phase 7: contraction around the altar before the player reappears.
        if (t >= 238 && t < 326) {
            double p = smooth((t - 238) / 88.0);
            double fade = envelope(t, 238, 250, 318, 326);
            double radius = 4.8 * (1.0 - p) + 0.65;
            Vec3 mid = c.add(0, 1.8, 0);
            for (int i = 0; i < 4; i++) {
                Basis basis = tiltedBasis(-1.0 + i * 0.62, t * (0.012 + i * 0.002) + i * 1.1);
                glowRing(g, mid, radius + i * 0.08, basis.u, basis.v, 0.055, fade, t * 0.018 + i);
            }
        }

        // Final smooth flash: geometry only, not ParticleTypes.FLASH/TOTEM/etc.
        if (t >= 308 && t < 340) {
            double p = clamp((t - 308) / 32.0, 0, 1);
            double fade = 1.0 - p;
            Vec3 flash = c.add(0, 1.4, 0);
            glowOrb(g, flash, 0.45 + p * 2.4, fade);
            glowRing(g, c.add(0, 0.15, 0), 1.0 + p * 9.0, X, Z, 0.16 + p * 0.12, fade, t * 0.008);
        }
    }

    private static void glowRing(Geometry g, Vec3 center, double radius, Vec3 u, Vec3 v,
                                 double width, double alpha, double phase) {
        if (alpha <= 0.001 || radius <= 0.01) return;
        annulus(g, center, radius, u, v, width * 5.0, CYAN, GREEN, (int) (20 * alpha), 96, phase);
        annulus(g, center, radius, u, v, width * 2.4, CYAN, YELLOW, (int) (58 * alpha), 96, phase + 0.02);
        annulus(g, center, radius, u, v, width, CYAN, GREEN, (int) (220 * alpha), 128, phase + 0.04);
    }

    private static void glowOrb(Geometry g, Vec3 center, double radius, double alpha) {
        if (alpha <= 0.001) return;
        Basis bx = new Basis(Y, Z);
        Basis by = new Basis(X, Z);
        Basis bz = new Basis(X, Y);
        double pulse = radius * (1.0 + Math.sin(center.y * 1.71) * 0.02);
        annulus(g, center, pulse * 0.55, bx.u, bx.v, pulse * 1.25, CYAN, GREEN, (int) (50 * alpha), 48, 0);
        annulus(g, center, pulse * 0.55, by.u, by.v, pulse * 1.15, GREEN, YELLOW, (int) (46 * alpha), 48, 0.7);
        annulus(g, center, pulse * 0.55, bz.u, bz.v, pulse * 0.95, WHITE, CYAN, (int) (80 * alpha), 48, 1.4);
    }

    private static void glowingSquare(Geometry g, Vec3 center, double size, double rotation, double width, double alpha) {
        Vec3[] pts = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double a = rotation + Math.PI / 4.0 + i * Math.PI / 2.0;
            pts[i] = center.add(Math.cos(a) * size, 0, Math.sin(a) * size);
        }
        for (int i = 0; i < 4; i++) {
            Vec3 a = pts[i];
            Vec3 b = pts[(i + 1) % 4];
            beamXZ(g, a, b, width * 5.0, CYAN, 18, alpha);
            beamXZ(g, a, b, width * 2.3, GREEN, 60, alpha);
            beamXZ(g, a, b, width, i % 2 == 0 ? CYAN : YELLOW, 220, alpha);
        }
    }

    private static void beamXZ(Geometry g, Vec3 a, Vec3 b, double width, int rgb, int baseAlpha, double alphaScale) {
        Vec3 dir = b.subtract(a);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        if (side.lengthSqr() < 1.0e-8) return;
        side = side.normalize().scale(width * 0.5);
        int aa = (int) clamp(baseAlpha * alphaScale, 0, 255);
        g.quad(a.add(side), a.subtract(side), b.subtract(side), b.add(side), rgb, rgb, aa, aa);
    }

    private static void annulus(Geometry g, Vec3 center, double radius, Vec3 u, Vec3 v, double width,
                                int c1, int c2, int alpha, int segments, double phase) {
        if (alpha <= 0) return;
        Vec3 uu = u.normalize();
        Vec3 vv = v.normalize();
        double half = Math.max(0.002, width * 0.5);
        double outer = radius + half;
        double inner = Math.max(0.001, radius - half);

        for (int i = 0; i < segments; i++) {
            double a0 = phase + Math.PI * 2.0 * i / segments;
            double a1 = phase + Math.PI * 2.0 * (i + 1) / segments;
            Vec3 o0 = circlePoint(center, uu, vv, outer, a0);
            Vec3 i0 = circlePoint(center, uu, vv, inner, a0);
            Vec3 i1 = circlePoint(center, uu, vv, inner, a1);
            Vec3 o1 = circlePoint(center, uu, vv, outer, a1);

            int ca = blendColor(c1, c2, (Math.sin(a0 * 1.45 + phase) + 1.0) * 0.5);
            int cb = blendColor(c1, c2, (Math.sin(a1 * 1.45 + phase) + 1.0) * 0.5);
            g.quad(o0, i0, i1, o1, ca, cb, alpha, alpha);
            g.quad(o1, i1, i0, o0, cb, ca, alpha, alpha); // two-sided
        }
    }

    private static Vec3 circlePoint(Vec3 center, Vec3 u, Vec3 v, double radius, double a) {
        return center.add(u.scale(Math.cos(a) * radius)).add(v.scale(Math.sin(a) * radius));
    }

    private static void drawStars(Geometry g, Vec3 center, double t, double fade) {
        int count = 74;
        for (int i = 0; i < count; i++) {
            double h1 = hash(i * 12.9898 + 78.233);
            double h2 = hash(i * 31.416 + 17.17);
            double h3 = hash(i * 7.77 + 4.21);
            double angle = h1 * Math.PI * 2.0;
            double radius = 2.0 + h2 * 19.0;
            double y = 0.8 + h3 * 9.0;
            Vec3 p = center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            double twinkle = 0.45 + 0.55 * Math.sin(t * 0.08 + i * 2.17);
            double appear = smooth(clamp((t - 125 - i * 0.7) / 36.0, 0, 1));
            double a = fade * appear * clamp(twinkle, 0.05, 1.0);
            if (a < 0.04) continue;
            double size = 0.025 + hash(i * 4.11) * 0.075;
            starBillboard(g, p, size, i % 3 == 0 ? YELLOW : CYAN, a);
        }
    }

    private static void starBillboard(Geometry g, Vec3 pos, double size, int rgb, double alpha) {
        Vec3 look = g.camera.subtract(pos);
        if (look.lengthSqr() < 1.0e-8) return;
        look = look.normalize();
        Vec3 right = look.cross(Y);
        if (right.lengthSqr() < 1.0e-8) right = X;
        else right = right.normalize();
        Vec3 up = right.cross(look).normalize();

        Vec3 r = right.scale(size * 2.1);
        Vec3 u = up.scale(size * 0.35);
        Vec3 r2 = right.scale(size * 0.35);
        Vec3 u2 = up.scale(size * 2.1);
        int aa = (int) (alpha * 210);
        g.quad(pos.add(r).add(u), pos.add(r).subtract(u), pos.subtract(r).subtract(u), pos.subtract(r).add(u), rgb, WHITE, aa, aa);
        g.quad(pos.add(r2).add(u2), pos.add(r2).subtract(u2), pos.subtract(r2).subtract(u2), pos.subtract(r2).add(u2), rgb, WHITE, aa, aa);
    }

    private static Basis tiltedBasis(double tilt, double yaw) {
        // Starts from the XZ plane, then tilts around X and rotates around Y.
        Vec3 u = rotateY(X, yaw);
        Vec3 v = rotateY(rotateX(Z, tilt), yaw);
        return new Basis(u.normalize(), v.normalize());
    }

    private static Vec3 rotateX(Vec3 p, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3(p.x, p.y * c - p.z * s, p.y * s + p.z * c);
    }

    private static Vec3 rotateY(Vec3 p, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c);
    }

    private static double envelope(double t, double inStart, double inEnd, double outStart, double outEnd) {
        double in = smooth(clamp((t - inStart) / Math.max(0.001, inEnd - inStart), 0, 1));
        double out = 1.0 - smooth(clamp((t - outStart) / Math.max(0.001, outEnd - outStart), 0, 1));
        return clamp(in * out, 0, 1);
    }

    private static double smooth(double x) {
        x = clamp(x, 0, 1);
        return x * x * (3.0 - 2.0 * x);
    }

    private static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }

    private static double hash(double n) {
        double s = Math.sin(n) * 43758.5453123;
        return s - Math.floor(s);
    }

    private static int blendColor(int a, int b, double t) {
        t = clamp(t, 0, 1);
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private record Basis(Vec3 u, Vec3 v) {}
    private record RitualView(Vec3 center, double tick) {}

    private static final class Geometry {
        private final BufferBuilder buffer;
        private final Matrix4f pose;
        private final Vec3 camera;

        private Geometry(BufferBuilder buffer, Matrix4f pose, Vec3 camera) {
            this.buffer = buffer;
            this.pose = pose;
            this.camera = camera;
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgbA, int rgbB, int alphaA, int alphaB) {
            vertex(a, rgbA, alphaA);
            vertex(b, rgbA, alphaA);
            vertex(c, rgbB, alphaB);
            vertex(d, rgbB, alphaB);
        }

        private void vertex(Vec3 p, int rgb, int alpha) {
            Vec3 q = p.subtract(camera);
            int r = (rgb >> 16) & 255;
            int gg = (rgb >> 8) & 255;
            int b = rgb & 255;
            buffer.addVertex(pose, (float) q.x, (float) q.y, (float) q.z).setColor(r, gg, b, Math.max(0, Math.min(255, alpha)));
        }
    }
}
