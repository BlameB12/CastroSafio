package com.castrosafio.bosscontroller.control;

import com.castrosafio.bosscontroller.CastroSafioBossController;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Restores movement that Cataclysm normally performs inside AI Goals.
 * BossControl disables AI while possessed, so those Goal ticks never run even though the
 * native entity animation/damage tick still does. This bridge mirrors only the missing
 * movement/targeting part and leaves native Cataclysm damage, particles and effects intact.
 *
 * Ignis is deliberately excluded: its proven movement bridge lives in IgnisActionController.
 */
public final class CataclysmBossMovementBridge {
    private static final ResourceLocation IGNIS = id("ignis");
    private static final ResourceLocation NETHERITE = id("netherite_monstrosity");
    private static final ResourceLocation GUARDIAN = id("ender_guardian");
    private static final ResourceLocation HARBINGER = id("the_harbinger");
    private static final ResourceLocation REMNANT = id("ancient_remnant");
    private static final ResourceLocation LEVIATHAN = id("the_leviathan");
    private static final ResourceLocation SCYLLA = id("scylla");
    private static final ResourceLocation MALEDICTUS = id("maledictus");

    private CataclysmBossMovementBridge() {}

    public static void tick(ServerPlayer controller, LivingEntity boss) {
        if (boss == null || !boss.isAlive()) return;
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        if (IGNIS.equals(type)) return;
        if (!isSupported(type)) return;

        LivingEntity target = findAutoTarget(controller, boss, 72.0D);
        if (target != null && boss instanceof Mob mob && isAttacking(type, boss)) {
            // BossControlManager clears target before this call. Restore one only while an attack
            // is active so native Cataclysm aiStep code that reads getTarget() can still work.
            mob.setTarget(target);
        }

        if (NETHERITE.equals(type)) tickNetherite(boss, target);
        else if (REMNANT.equals(type)) tickRemnant(boss, target);
        else if (SCYLLA.equals(type)) tickScylla(boss, target);
        else if (MALEDICTUS.equals(type)) tickMaledictus(boss, target);
        else if (GUARDIAN.equals(type)) tickGuardian(boss, target);
        else if (HARBINGER.equals(type)) tickHarbinger(boss, target);
        else if (LEVIATHAN.equals(type)) tickLeviathan(boss, target);
    }

    private static boolean isSupported(ResourceLocation type) {
        return NETHERITE.equals(type) || GUARDIAN.equals(type) || HARBINGER.equals(type)
                || REMNANT.equals(type) || LEVIATHAN.equals(type) || SCYLLA.equals(type)
                || MALEDICTUS.equals(type);
    }

    private static boolean isAttacking(ResourceLocation type, LivingEntity boss) {
        if (NETHERITE.equals(type) || REMNANT.equals(type) || SCYLLA.equals(type) || MALEDICTUS.equals(type)) {
            return getAttackState(boss) != 0;
        }
        return !isAnimationIdle(boss);
    }

    // -------------------- InternalAnimation bosses --------------------

    private static void tickNetherite(LivingEntity boss, LivingEntity target) {
        int state = getAttackState(boss);
        int tick = getAttackTicks(boss);
        // Native ShoulderCheck Goal calls FowardMoveController.forward while ticks 20..48.
        if (state == 8 && tick > 19 && tick < 49) {
            moveTowardOrForward(boss, target, 0.58D, 2.2D, false);
        }
    }

    private static void tickRemnant(LivingEntity boss, LivingEntity target) {
        int state = getAttackState(boss);
        int tick = getAttackTicks(boss);
        // Charge state: the original Goal continuously accelerates forward while grounded.
        if (state == 11 && tick > 0 && tick <= 60) {
            moveTowardOrForward(boss, target, 0.78D, 1.8D, false);
        }
    }

    private static void tickScylla(LivingEntity boss, LivingEntity target) {
        int state = getAttackState(boss);
        int tick = getAttackTicks(boss);

        // Back-step: original Goal starts with a backwards/side jump. Recreate an arc directly
        // because horizontal delta movement gets cleared by BossControl.
        if (state == 6 && tick >= 1 && tick <= 7) {
            double dy = switch (tick) {
                case 1 -> 0.24D;
                case 2 -> 0.18D;
                case 3 -> 0.12D;
                case 4 -> 0.05D;
                case 5 -> -0.03D;
                case 6 -> -0.10D;
                default -> -0.16D;
            };
            moveRelativeToLook(boss, -0.25D, dy);
        }

        // Chain jump state 15. Native Scylla launches vertically at tick 9; with NoAI that Goal
        // never runs, so make the full jump arc and drift toward the selected target automatically.
        if (state == 15 && tick >= 9 && tick <= 36) {
            double t = tick - 9;
            double dy = 0.72D - t * 0.055D;
            Vec3 horizontal = horizontalToward(boss, target, 0.18D, 2.0D);
            moveDirect(boss, new Vec3(horizontal.x, dy, horizontal.z));
        } else if (state == 16) {
            // Falling part of Chain Jump: home in gently so the slam lands on the target.
            Vec3 horizontal = horizontalToward(boss, target, 0.16D, 1.8D);
            moveDirect(boss, new Vec3(horizontal.x, -0.48D, horizontal.z));
        }

        // Grab Smash normally expects the target to already be close. For controlled Scylla,
        // close the gap automatically before the grab hit window.
        if (state == 27 && tick >= 2 && tick <= 12) {
            moveTowardOrForward(boss, target, 0.46D, 2.2D, false);
        }
    }

    private static void tickMaledictus(LivingEntity boss, LivingEntity target) {
        int state = getAttackState(boss);
        int tick = getAttackTicks(boss);

        // Backstep used before rush.
        if (state == 10 && tick >= 1 && tick <= 7) {
            double dy = switch (tick) {
                case 1 -> 0.28D;
                case 2 -> 0.21D;
                case 3 -> 0.14D;
                case 4 -> 0.06D;
                case 5 -> -0.02D;
                case 6 -> -0.10D;
                default -> -0.18D;
            };
            moveRelativeToLook(boss, -0.30D, dy);
        }

        // Rush variants: native MaledictusChargeState applies its impulse at these exact windows.
        if ((state == 11 || state == 12) && tick >= 24 && tick <= 29) {
            moveTowardOrForward(boss, target, 0.92D, 1.3D, false);
        }
        if ((state == 15 || state == 16 || state == 17) && tick >= 16 && tick <= 21) {
            moveTowardOrForward(boss, target, 0.92D, 1.3D, false);
        }
        // Standalone dash state included by Cataclysm's attack chain.
        if (state == 13 && tick >= 18 && tick <= 23) {
            moveTowardOrForward(boss, target, 0.92D, 1.3D, false);
        }

        // Native backsteps at the end of two dash variants.
        if (state == 15 && tick >= 27 && tick <= 32) moveRelativeToLook(boss, -0.30D, 0.05D);
        if (state == 17 && tick >= 30 && tick <= 35) moveRelativeToLook(boss, -0.30D, 0.05D);

        // Spin Slashes contains two small lunges.
        if (state == 18 && ((tick >= 15 && tick <= 18) || (tick >= 29 && tick <= 32))) {
            moveTowardOrForward(boss, target, 0.58D, 1.8D, false);
        }

        // Combo first/second steps have native forward pushes on tick 8.
        if ((state == 19 || state == 21) && tick >= 8 && tick <= 11) {
            moveTowardOrForward(boss, target, state == 21 ? 0.48D : 0.43D, 1.5D, false);
        }

        // Flying smashes: launch, arc, then automatically dive at the target.
        if (state == 8 || state == 24) {
            int strike = state == 8 ? 56 : 51;
            if (tick >= 25 && tick < strike) {
                double t = tick - 25;
                double dy = 0.68D - t * 0.060D;
                Vec3 horizontal = horizontalToward(boss, target, 0.10D, 2.0D);
                moveDirect(boss, new Vec3(horizontal.x, dy, horizontal.z));
                invokeBooleanSetter(boss, "setFlying", true);
            } else if (tick >= strike) {
                invokeBooleanSetter(boss, "setFlying", false);
                if (target != null) {
                    Vec3 delta = target.position().subtract(boss.position()).add(0.0D, -1.0D, 0.0D);
                    moveVectorLimited(boss, delta, 0.82D, 0.6D);
                } else {
                    moveDirect(boss, new Vec3(0.0D, -0.58D, 0.0D));
                }
            }
        }

        // User-requested grab assist: Maledictus now closes the distance by itself before
        // the original grab hit window instead of requiring WASD.
        if (state == 28 && tick >= 2 && tick <= 15) {
            moveTowardOrForward(boss, target, 0.56D, 2.0D, false);
        }
    }

    // -------------------- Lionfish animation bosses --------------------

    private static void tickGuardian(LivingEntity boss, LivingEntity target) {
        int tick = getAnimationTick(boss);
        if (tick < 0) return;

        if ((isCurrentAnimation(boss, "GUARDIAN_UPPERCUT_AND_BULLET")
                || isCurrentAnimation(boss, "GUARDIAN_RAGE_UPPERCUT"))
                && tick >= 26 && tick <= 31) {
            moveTowardOrForward(boss, target, 0.62D, 1.5D, false);
        }

        // Hug Me predicts/teleports onto the target in Cataclysm. Recreate the closing movement.
        if (isCurrentAnimation(boss, "GUARDIAN_HUG_ME") && tick == 20 && target != null) {
            boss.teleportTo(target.getX(), target.getY(), target.getZ());
        }

        // Teleport strike: warp above the target, fall naturally, then chain the landing strike.
        if (isCurrentAnimation(boss, "GUARDIAN_AIR_STRIKE1")) {
            if (tick == 40 && target != null) {
                double y = invokeBooleanGetter(boss, "getIsHelmetless", false) ? 8.0D : 4.0D;
                boss.teleportTo(target.getX(), target.getY() + y, target.getZ());
            }
            if (tick > 48 && boss.onGround()) triggerAnimation(boss, "GUARDIAN_AIR_STRIKE2");
        }

        // Black Hole can naturally chain into Rocket Punch when the target remains distant.
        if (isCurrentAnimation(boss, "GUARDIAN_BLACKHOLE") && tick == 37 && target != null
                && boss.distanceToSqr(target) >= 25.0D) {
            triggerAnimation(boss, "GUARDIAN_ROCKETPUNCH");
        }

        if (isCurrentAnimation(boss, "GUARDIAN_ROCKETPUNCH") && tick >= 24 && tick <= 29) {
            moveTowardOrForward(boss, target, 0.88D, 1.1D, false);
        }
    }

    private static void tickHarbinger(LivingEntity boss, LivingEntity target) {
        if (!isCurrentAnimation(boss, "CHARGE_ANIMATION")) return;
        int tick = getAnimationTick(boss);
        if (tick == 24) invokeBooleanSetter(boss, "setIsCharge", true);
        if (tick >= 24 && tick <= 35) {
            // Original attack launches in full 3D. Keep a strong automatic homing charge while
            // collision remains server-authoritative.
            if (target != null) moveVectorLimited(boss, target.getEyePosition().subtract(boss.position()), 1.15D, 1.0D);
            else moveRelativeToLook(boss, 1.0D, 0.10D);
        }
        if (tick >= 36) invokeBooleanSetter(boss, "setIsCharge", false);
    }

    private static void tickLeviathan(LivingEntity boss, LivingEntity target) {
        int tick = getAnimationTick(boss);
        if (tick < 0) return;

        // Grab normally assumes the target is already near the tongue. Controlled Leviathan now
        // swims/rushes into grab range automatically, then chains to the native Grab Bite.
        if (isCurrentAnimation(boss, "LEVIATHAN_GRAB")) {
            if (tick >= 3 && tick <= 45) {
                moveTowardOrForward(boss, target, 0.48D, 7.2D, true);
            }
            if (tick >= 25 && target != null && boss.distanceTo(target) < 8.5F) {
                triggerAnimation(boss, "LEVIATHAN_GRAB_BITE");
            }
        }

        // Native Rush uses aquatic/navigation movement during this window. Direct movement keeps
        // the rush alive with NoAI in both water and air.
        if (isCurrentAnimation(boss, "LEVIATHAN_RUSH") && tick > 53 && tick < 134) {
            moveTowardOrForward(boss, target, 0.52D, 2.8D, true);
        }
    }

    // -------------------- Automatic target selection --------------------

    private static LivingEntity findAutoTarget(ServerPlayer controller, LivingEntity boss, double radius) {
        AABB area = boss.getBoundingBox().inflate(radius);
        List<LivingEntity> candidates = boss.level().getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (!entity.isAlive() || entity == boss || entity == controller) return false;
            if (boss.isAlliedTo(entity)) return false;
            if (entity instanceof Player player && player.getAbilities().invulnerable) return false;
            return true;
        });
        if (candidates.isEmpty()) return null;

        double yaw = Math.toRadians(boss.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 delta = candidate.position().subtract(boss.position());
            double distSq = delta.lengthSqr();
            Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
            double dot = horizontal.lengthSqr() < 1.0E-6D ? 1.0D : forward.dot(horizontal.normalize());
            double anglePenalty = dot >= 0.20D ? (1.25D - 0.55D * dot) : 2.8D;
            double playerBonus = candidate instanceof Player ? 0.55D : 1.0D;
            double score = distSq * anglePenalty * playerBonus;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    // -------------------- Direct movement helpers --------------------

    private static void moveTowardOrForward(LivingEntity boss, LivingEntity target, double speed,
                                            double stopDistance, boolean threeD) {
        if (target == null) {
            moveRelativeToLook(boss, speed, 0.0D);
            return;
        }
        Vec3 delta = target.position().subtract(boss.position());
        if (!threeD) delta = new Vec3(delta.x, 0.0D, delta.z);
        moveVectorLimited(boss, delta, speed, stopDistance);
    }

    private static Vec3 horizontalToward(LivingEntity boss, LivingEntity target, double speed, double stopDistance) {
        if (target == null) {
            double yaw = Math.toRadians(boss.getYRot());
            return new Vec3(-Math.sin(yaw) * speed, 0.0D, Math.cos(yaw) * speed);
        }
        Vec3 delta = new Vec3(target.getX() - boss.getX(), 0.0D, target.getZ() - boss.getZ());
        double distance = delta.length();
        if (distance <= stopDistance || distance < 1.0E-6D) return Vec3.ZERO;
        return delta.normalize().scale(Math.min(speed, distance - stopDistance));
    }

    private static void moveVectorLimited(LivingEntity boss, Vec3 delta, double speed, double stopDistance) {
        double distance = delta.length();
        if (distance <= stopDistance || distance < 1.0E-6D) return;
        double step = Math.min(speed, Math.max(0.0D, distance - stopDistance));
        moveDirect(boss, delta.normalize().scale(step));
    }

    private static void moveRelativeToLook(LivingEntity boss, double forward, double y) {
        double yaw = Math.toRadians(boss.getYRot());
        moveDirect(boss, new Vec3(-Math.sin(yaw) * forward, y, Math.cos(yaw) * forward));
    }

    private static void moveDirect(LivingEntity boss, Vec3 movement) {
        if (movement.lengthSqr() < 1.0E-8D) return;
        boss.move(MoverType.SELF, movement);
        boss.hasImpulse = true;
    }

    // -------------------- Reflection bridge --------------------

    private static int getAttackState(LivingEntity boss) {
        return invokeIntGetter(boss, "getAttackState", 0);
    }

    private static int getAttackTicks(LivingEntity boss) {
        Field field = findField(boss.getClass(), "attackTicks");
        if (field == null) return 0;
        try {
            Object value = field.get(boss);
            return value instanceof Number number ? number.intValue() : 0;
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static int getAnimationTick(LivingEntity boss) {
        try {
            Method method = findMethod(boss.getClass(), "getAnimationTick", 0);
            if (method == null) return -1;
            Object result = method.invoke(boss);
            return result instanceof Number number ? number.intValue() : -1;
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }

    private static boolean isAnimationIdle(LivingEntity boss) {
        try {
            Method getter = findMethod(boss.getClass(), "getAnimation", 0);
            Field idleField = findField(boss.getClass(), "NO_ANIMATION");
            if (getter == null || idleField == null) return true;
            Object current = getter.invoke(boss);
            Object idle = idleField.get(null);
            return current == idle || (current != null && current.equals(idle));
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static boolean isCurrentAnimation(LivingEntity boss, String fieldName) {
        try {
            Method getter = findMethod(boss.getClass(), "getAnimation", 0);
            Field field = findField(boss.getClass(), fieldName);
            if (getter == null || field == null || !Modifier.isStatic(field.getModifiers())) return false;
            Object current = getter.invoke(boss);
            Object wanted = field.get(null);
            return current == wanted || (current != null && current.equals(wanted));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean triggerAnimation(LivingEntity boss, String fieldName) {
        try {
            Field animationField = findField(boss.getClass(), fieldName);
            if (animationField == null || !Modifier.isStatic(animationField.getModifiers())) return false;
            Object animation = animationField.get(null);
            if (animation == null) return false;

            try {
                Class<?> handlerClass = Class.forName("com.github.L_Ender.lionfishapi.server.animation.AnimationHandler");
                Object instance = handlerClass.getField("INSTANCE").get(null);
                for (Method method : handlerClass.getMethods()) {
                    if (!method.getName().equals("sendAnimationMessage") || method.getParameterCount() != 2) continue;
                    Class<?>[] params = method.getParameterTypes();
                    if (params[0].isInstance(boss) && params[1].isInstance(animation)) {
                        method.invoke(instance, boss, animation);
                        return true;
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }

            Method setter = findCompatibleMethod(boss.getClass(), "setAnimation", animation);
            if (setter != null) {
                setter.invoke(boss, animation);
                return true;
            }
        } catch (ReflectiveOperationException exception) {
            CastroSafioBossController.LOGGER.warn("No se pudo encadenar {} en {}", fieldName, boss.getType(), exception);
        }
        return false;
    }

    private static int invokeIntGetter(Object target, String name, int fallback) {
        try {
            Method method = findMethod(target.getClass(), name, 0);
            if (method == null) return fallback;
            Object result = method.invoke(target);
            return result instanceof Number number ? number.intValue() : fallback;
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private static boolean invokeBooleanGetter(Object target, String name, boolean fallback) {
        try {
            Method method = findMethod(target.getClass(), name, 0);
            if (method == null) return fallback;
            Object result = method.invoke(target);
            return result instanceof Boolean value ? value : fallback;
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private static void invokeBooleanSetter(Object target, String name, boolean value) {
        try {
            Method method = findPrimitiveMethod(target.getClass(), name, boolean.class);
            if (method != null) method.invoke(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    private static Method findPrimitiveMethod(Class<?> type, String name, Class<?> parameter) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameter);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Method findCompatibleMethod(Class<?> type, String name, Object arg) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != 1) continue;
                if (arg == null || method.getParameterTypes()[0].isInstance(arg)) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cataclysm", path);
    }
}
