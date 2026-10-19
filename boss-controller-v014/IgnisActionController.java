package com.castrosafio.bosscontroller.control;

import com.castrosafio.bosscontroller.CastroSafioBossController;
import com.castrosafio.bosscontroller.network.BossActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class IgnisActionController {
    private static final ResourceLocation IGNIS_ID = ResourceLocation.fromNamespaceAndPath("cataclysm", "ignis");
    private static final Map<UUID, Long> BASIC_NEXT_TICK = new HashMap<>();
    private static final Map<UUID, Long> ABILITY_NEXT_TICK = new HashMap<>();

    private IgnisActionController() {}

    public static void handle(ServerPlayer player, int action) {
        LivingEntity boss = BossControlManager.getControlledTarget(player);
        if (boss == null) return;

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        if (!IGNIS_ID.equals(typeId)) {
            player.sendSystemMessage(Component.literal("Esta prueba de ataques es solo para Ignis.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        long now = player.serverLevel().getGameTime();
        if (!isAnimationIdle(boss)) return;

        if (action == BossActionPayload.ACTION_BASIC) {
            if (now < BASIC_NEXT_TICK.getOrDefault(player.getUUID(), 0L)) return;
            String animation = switch (getBossPhase(boss)) {
                case 2 -> "SWING_ATTACK_BERSERK";
                case 1 -> "SWING_ATTACK_SOUL";
                default -> "SWING_ATTACK";
            };
            if (triggerAnimation(boss, animation)) {
                BASIC_NEXT_TICK.put(player.getUUID(), now + 26L);
            } else {
                sendBridgeError(player, animation);
            }
            return;
        }

        if (action == BossActionPayload.ACTION_ABILITY_1) {
            if (now < ABILITY_NEXT_TICK.getOrDefault(player.getUUID(), 0L)) return;
            if (triggerAnimation(boss, "SPIN_ATTACK")) {
                ABILITY_NEXT_TICK.put(player.getUUID(), now + 80L);
            } else {
                sendBridgeError(player, "SPIN_ATTACK");
            }
        }
    }

    public static void clear(UUID playerId) {
        BASIC_NEXT_TICK.remove(playerId);
        ABILITY_NEXT_TICK.remove(playerId);
    }

    public static void clearAll() {
        BASIC_NEXT_TICK.clear();
        ABILITY_NEXT_TICK.clear();
    }

    private static boolean isAnimationIdle(LivingEntity boss) {
        try {
            Method getter = findNoArgMethod(boss.getClass(), "getAnimation");
            if (getter == null) return true;
            Object current = getter.invoke(boss);
            Field noAnimationField = findPublicField(boss.getClass(), "NO_ANIMATION");
            if (noAnimationField == null) return true;
            Object idle = noAnimationField.get(null);
            return current == idle || (current != null && current.equals(idle));
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static int getBossPhase(LivingEntity boss) {
        try {
            Method method = findNoArgMethod(boss.getClass(), "getBossPhase");
            if (method != null) {
                Object result = method.invoke(boss);
                if (result instanceof Number number) return number.intValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0;
    }

    private static boolean triggerAnimation(LivingEntity boss, String fieldName) {
        try {
            Field animationField = findPublicField(boss.getClass(), fieldName);
            if (animationField == null || !Modifier.isStatic(animationField.getModifiers())) return false;
            Object animation = animationField.get(null);
            if (animation == null) return false;

            try {
                Class<?> handlerClass = Class.forName("com.github.L_Ender.lionfishapi.server.animation.AnimationHandler");
                Field instanceField = handlerClass.getField("INSTANCE");
                Object instance = instanceField.get(null);
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

            for (Method method : boss.getClass().getMethods()) {
                if (!method.getName().equals("setAnimation") || method.getParameterCount() != 1) continue;
                if (method.getParameterTypes()[0].isInstance(animation)) {
                    method.invoke(boss, animation);
                    return true;
                }
            }
        } catch (ReflectiveOperationException exception) {
            CastroSafioBossController.LOGGER.warn("No se pudo iniciar animacion {} de Ignis", fieldName, exception);
        }
        return false;
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) return method;
        }
        return null;
    }

    private static Field findPublicField(Class<?> type, String name) {
        try {
            return type.getField(name);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static void sendBridgeError(ServerPlayer player, String animation) {
        player.sendSystemMessage(Component.literal("No pude activar " + animation + " en esta version de Cataclysm.")
                .withStyle(ChatFormatting.RED));
    }
}
