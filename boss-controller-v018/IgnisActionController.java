package com.castrosafio.bosscontroller.control;

import com.castrosafio.bosscontroller.CastroSafioBossController;
import com.castrosafio.bosscontroller.network.BossActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Optional Cataclysm bridge. Reflection keeps Cataclysm optional at compile time while allowing
 * controlled Ignis to run the boss' own native Lionfish/Cataclysm animations and damage logic.
 */
public final class IgnisActionController {
    private static final ResourceLocation IGNIS_ID = ResourceLocation.fromNamespaceAndPath("cataclysm", "ignis");
    private static final Map<UUID, Integer> SELECTED = new HashMap<>();
    private static final Map<UUID, Double> AIR_VERTICAL_SPEED = new HashMap<>();

    /**
     * Phase-aware ability catalogue. Each slot resolves to the native animation field for
     * phase 1 / phase 2 / phase 3. Identical entries mean the ability has no phase variant.
     */
    private static final Ability[] ABILITIES = new Ability[]{
            new Ability("Spin Attack", "SPIN_ATTACK", "SPIN_ATTACK", "SPIN_ATTACK"),
            new Ability("Horizontal Swing", "HORIZONTAL_SWING_ATTACK", "HORIZONTAL_SWING_ATTACK_SOUL", "HORIZONTAL_SWING_ATTACK_SOUL"),
            new Ability("Shield Smash", "SHIELD_SMASH_ATTACK", "SHIELD_SMASH_ATTACK", "SHIELD_SMASH_ATTACK"),
            new Ability("Poke", "POKE_RANDOM", "POKE_RANDOM", "POKE_RANDOM"),
            new Ability("Magic Attack", "MAGIC_ATTACK", "MAGIC_ATTACK", "MAGIC_ATTACK"),
            new Ability("Smash In Air", "SMASH_IN_AIR", "SMASH_IN_AIR", "SMASH_IN_AIR"),
            new Ability("Smash", "SMASH", "SMASH", "SMASH"),
            new Ability("Body Check", "BODY_RANDOM", "BODY_RANDOM_SOUL", "BODY_RANDOM_SOUL"),
            new Ability("Counter", "COUNTER", "COUNTER", "COUNTER"),
            new Ability("Strike", "STRIKE", "STRIKE", "STRIKE"),
            new Ability("Combo 1", "COMBO1", "COMBO1", "COMBO1"),
            new Ability("Combo 2", "COMBO2", "COMBO2", "COMBO2"),
            new Ability("Break The Shield", "BREAK_THE_SHIELD", "BREAK_THE_SHIELD", "BREAK_THE_SHIELD"),
            new Ability("Swing Uppercut", "SWING_UPPERCUT", "SWING_UPPERCUT", "SWING_UPPERCUT"),
            new Ability("Swing Upperslash", "SWING_UPPERSLASH", "SWING_UPPERSLASH", "SWING_UPPERSLASH"),
            new Ability("Earth Shudders", "EARTH_SHUDDERS_ATTACK", "EARTH_SHUDDERS_ATTACK", "EARTH_SHUDDERS_ATTACK"),
            new Ability("Small Horizontal Swing", "HORIZONTAL_SMALL_SWING_ATTACK", "HORIZONTAL_SMALL_SWING_ATTACK", "HORIZONTAL_SMALL_SWING_ATTACK"),
            new Ability("Small Horizontal Swing ALT", "HORIZONTAL_SMALL_SWING_ALT_ATTACK2", "HORIZONTAL_SMALL_SWING_ALT_ATTACK2", "HORIZONTAL_SMALL_SWING_ALT_ATTACK2"),
            new Ability("Reinforced Smash In Air", "REINFORCED_SMASH_IN_AIR", "REINFORCED_SMASH_IN_AIR_SOUL", "REINFORCED_SMASH_IN_AIR_SOUL"),
            new Ability("Reinforced Smash", "REINFORCED_SMASH", "REINFORCED_SMASH_SOUL", "REINFORCED_SMASH_SOUL"),
            new Ability("Shield Break Counter", "SHIELD_BREAK_COUNTER", "SHIELD_BREAK_COUNTER", "SHIELD_BREAK_COUNTER"),
            new Ability("Shield Break Strike", "SHIELD_BREAK_STRIKE", "SHIELD_BREAK_STRIKE", "SHIELD_BREAK_STRIKE"),
            new Ability("Ultimate", "ULTIMATE_ATTACK", "ULTIMATE_ATTACK", "ULTIMATE_ATTACK")
    };

    private IgnisActionController() {}

    public static void onControlStarted(ServerPlayer player, LivingEntity boss) {
        if (!isIgnis(boss)) return;
        SELECTED.put(player.getUUID(), 0);
        showSelected(player, boss);
    }

    public static void handle(ServerPlayer player, int action) {
        LivingEntity boss = BossControlManager.getControlledTarget(player);
        if (boss == null) return;

        if (!isIgnis(boss)) {
            player.displayClientMessage(Component.literal("Las habilidades de esta beta están configuradas para Ignis.")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        if (action == BossActionPayload.ACTION_SELECT_PREV) {
            cycle(player, boss, -1);
            return;
        }
        if (action == BossActionPayload.ACTION_SELECT_NEXT) {
            cycle(player, boss, 1);
            return;
        }

        if (!isAnimationIdle(boss)) return;

        if (action == BossActionPayload.ACTION_BASIC) {
            String animation = switch (getBossPhase(boss)) {
                case 2 -> "SWING_ATTACK_BERSERK";
                case 1 -> "SWING_ATTACK_SOUL";
                default -> "SWING_ATTACK";
            };
            triggerOrError(player, boss, "Básico", animation);
            return;
        }

        if (action == BossActionPayload.ACTION_EXECUTE_SELECTED) {
            int selected = normalizedSelection(player.getUUID());
            Ability ability = ABILITIES[selected];
            String animation = resolveAbilityAnimation(boss, ability);
            triggerOrError(player, boss, ability.label(), animation);
        }
    }

    public static void tickControlledIgnis(ServerPlayer player, LivingEntity boss) {
        if (!isIgnis(boss) || !boss.isAlive()) return;

        tickNativeAirSmashMovement(player, boss);

        if (!isAnimationIdle(boss)) return;

        int phase = getBossPhase(boss);
        float health = boss.getHealth();
        float max = boss.getMaxHealth();
        if (max <= 0.0F) return;

        if (phase <= 0 && health <= max * (2.0F / 3.0F)) {
            if (triggerAnimation(boss, "PHASE_2")) {
                player.displayClientMessage(Component.literal("IGNIS → FASE 2 AUTOMÁTICA")
                        .withStyle(ChatFormatting.AQUA), true);
            }
        } else if (phase == 1 && health <= max * (1.0F / 3.0F)) {
            if (triggerAnimation(boss, "PHASE_3")) {
                player.displayClientMessage(Component.literal("IGNIS → FASE 3 AUTOMÁTICA")
                        .withStyle(ChatFormatting.RED), true);
            }
        }
    }

    private static void tickNativeAirSmashMovement(ServerPlayer player, LivingEntity boss) {
        int animationTick = getAnimationTick(boss);
        if (animationTick < 0) {
            AIR_VERTICAL_SPEED.remove(player.getUUID());
            return;
        }

        boolean normalAir = isCurrentAnimation(boss, "SMASH_IN_AIR");
        boolean reinforcedSoul = isCurrentAnimation(boss, "REINFORCED_SMASH_IN_AIR_SOUL");
        boolean reinforcedNormal = isCurrentAnimation(boss, "REINFORCED_SMASH_IN_AIR");
        boolean reinforcedAir = reinforcedNormal || reinforcedSoul;

        if (!normalAir && !reinforcedAir) {
            AIR_VERTICAL_SPEED.remove(player.getUUID());
            return;
        }

        int launchTick = normalAir ? 19 : 84;
        double launchSpeed = normalAir ? 1.4D : 1.8D;

        if (animationTick == launchTick && !AIR_VERTICAL_SPEED.containsKey(player.getUUID())) {
            AIR_VERTICAL_SPEED.put(player.getUUID(), launchSpeed);
        }

        Double vertical = AIR_VERTICAL_SPEED.get(player.getUUID());
        if (vertical == null || animationTick < launchTick) return;

        double beforeY = boss.getY();
        boss.move(MoverType.SELF, new Vec3(0.0D, vertical, 0.0D));
        boss.hasImpulse = true;
        boss.setDeltaMovement(0.0D, 0.0D, 0.0D);

        boolean descending = vertical < 0.0D;
        boolean landed = descending && (boss.onGround() || Math.abs(boss.getY() - beforeY) < 1.0E-5D);
        if (landed) {
            AIR_VERTICAL_SPEED.remove(player.getUUID());
            triggerAnimation(boss, normalAir ? "SMASH"
                    : (reinforcedSoul ? "REINFORCED_SMASH_SOUL" : "REINFORCED_SMASH"));
            return;
        }

        double next = (vertical - 0.08D) * 0.98D;
        AIR_VERTICAL_SPEED.put(player.getUUID(), next);
    }

    private static int getAnimationTick(LivingEntity boss) {
        try {
            Method getter = findNoArgMethod(boss.getClass(), "getAnimationTick");
            if (getter == null) return -1;
            Object value = getter.invoke(boss);
            return value instanceof Number number ? number.intValue() : -1;
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }

    private static boolean isCurrentAnimation(LivingEntity boss, String fieldName) {
        try {
            Method getter = findNoArgMethod(boss.getClass(), "getAnimation");
            Field animationField = findPublicField(boss.getClass(), fieldName);
            if (getter == null || animationField == null || !Modifier.isStatic(animationField.getModifiers())) return false;
            Object current = getter.invoke(boss);
            Object expected = animationField.get(null);
            return current == expected || (current != null && current.equals(expected));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static void clear(UUID playerId) {
        SELECTED.remove(playerId);
        AIR_VERTICAL_SPEED.remove(playerId);
    }

    public static void clearAll() {
        SELECTED.clear();
        AIR_VERTICAL_SPEED.clear();
    }

    private static void cycle(ServerPlayer player, LivingEntity boss, int direction) {
        int selected = normalizedSelection(player.getUUID());
        selected = Math.floorMod(selected + direction, ABILITIES.length);
        SELECTED.put(player.getUUID(), selected);
        showSelected(player, boss);
    }

    private static int normalizedSelection(UUID playerId) {
        int value = SELECTED.getOrDefault(playerId, 0);
        return Math.floorMod(value, ABILITIES.length);
    }

    private static void showSelected(ServerPlayer player, LivingEntity boss) {
        int selected = normalizedSelection(player.getUUID());
        Ability ability = ABILITIES[selected];
        int phase = Math.max(0, Math.min(2, getBossPhase(boss)));
        String actual = previewAbilityAnimation(ability, phase);
        player.displayClientMessage(
                Component.literal("Ignis  [" + (selected + 1) + "/" + ABILITIES.length + "]  ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(ability.label()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("  •  " + actual + "  •  Z/X cambiar · R usar")
                                .withStyle(ChatFormatting.GRAY)),
                true
        );
    }

    private static String resolveAbilityAnimation(LivingEntity boss, Ability ability) {
        int phase = getBossPhase(boss);
        String animation = ability.animationForPhase(phase);

        if ("POKE_RANDOM".equals(animation)) {
            return switch (boss.getRandom().nextInt(3)) {
                case 1 -> "POKE_ATTACK2";
                case 2 -> "POKE_ATTACK3";
                default -> "POKE_ATTACK";
            };
        }

        if ("BODY_RANDOM".equals(animation)) {
            return "BODY_CHECK_ATTACK" + (boss.getRandom().nextInt(4) + 1);
        }

        if ("BODY_RANDOM_SOUL".equals(animation)) {
            return "BODY_CHECK_ATTACK_SOUL" + (boss.getRandom().nextInt(4) + 1);
        }

        return animation;
    }

    private static String previewAbilityAnimation(Ability ability, int phase) {
        String animation = ability.animationForPhase(phase);
        return switch (animation) {
            case "POKE_RANDOM" -> "POKE_ATTACK 1/2/3 (auto)";
            case "BODY_RANDOM" -> "BODY_CHECK 1/2/3/4 (auto)";
            case "BODY_RANDOM_SOUL" -> "BODY_CHECK_SOUL 1/2/3/4 (auto)";
            default -> animation;
        };
    }

    private static void triggerOrError(ServerPlayer player, LivingEntity boss, String label, String animation) {
        if (triggerAnimation(boss, animation)) {
            player.displayClientMessage(Component.literal("Ignis: " + label + "  [" + animation + "]")
                    .withStyle(ChatFormatting.GOLD), true);
        } else {
            sendBridgeError(player, animation);
        }
    }

    private static boolean isIgnis(LivingEntity boss) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        return IGNIS_ID.equals(typeId);
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
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Field findPublicField(Class<?> type, String name) {
        try {
            return type.getField(name);
        } catch (NoSuchFieldException ignored) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignoredAgain) {
                }
            }
            return null;
        }
    }

    private static void sendBridgeError(ServerPlayer player, String animation) {
        player.displayClientMessage(
                Component.literal("No se pudo activar " + animation + " con esta versión de Cataclysm.")
                        .withStyle(ChatFormatting.RED), true
        );
    }

    private record Ability(String label, String phase0, String phase1, String phase2) {
        private String animationForPhase(int phase) {
            return switch (phase) {
                case 2 -> phase2;
                case 1 -> phase1;
                default -> phase0;
            };
        }
    }
}
