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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cataclysm-wide controller bridge.
 *
 * Ignis intentionally remains delegated to IgnisActionController so the proven v0.1.10
 * behavior is not changed. The other Cataclysm bosses are driven through their own native
 * animation API (Lionfish bosses) or native InternalAnimation attackState API.
 */
public final class CataclysmBossActionController {
    private static final ResourceLocation IGNIS = id("ignis");
    private static final ResourceLocation NETHERITE = id("netherite_monstrosity");
    private static final ResourceLocation GUARDIAN = id("ender_guardian");
    private static final ResourceLocation HARBINGER = id("the_harbinger");
    private static final ResourceLocation REMNANT = id("ancient_remnant");
    private static final ResourceLocation LEVIATHAN = id("the_leviathan");
    private static final ResourceLocation SCYLLA = id("scylla");
    private static final ResourceLocation MALEDICTUS = id("maledictus");

    private static final Map<UUID, Integer> SELECTED = new HashMap<>();
    private static final Map<UUID, InternalRun> INTERNAL_RUNS = new HashMap<>();
    private static final Map<ResourceLocation, BossProfile> PROFILES = new HashMap<>();

    static {
        // Netherite Monstrosity - InternalAnimation states.
        register(NETHERITE, new BossProfile("Netherite Monstrosity", Mode.INTERNAL,
                state("Smash", 3, 58),
                new Ability[]{
                        state("Smash", 3, 58),
                        state("Magma Shot", 6, 44),
                        state("Drain", 7, 60),
                        state("Shoulder Check", 8, 70),
                        state("Overpower", 9, 75),
                        state("Flare Shot", 10, 60)
                }));

        // Ender Guardian - native Lionfish animations.
        register(GUARDIAN, new BossProfile("Ender Guardian", Mode.ANIMATION,
                anim("Basic Swing", "GUARDIAN_RIGHT_ATTACK", "GUARDIAN_LEFT_ATTACK"),
                new Ability[]{
                        anim("Strong Swing", "GUARDIAN_RIGHT_STRONG_ATTACK", "GUARDIAN_LEFT_STRONG_ATTACK"),
                        anim("Burst", "GUARDIAN_BURST_ATTACK"),
                        anim("Uppercut + Bullet", "GUARDIAN_UPPERCUT_AND_BULLET"),
                        anim("Stomp", "GUARDIAN_STOMP"),
                        anim("Mass Destruction", "GUARDIAN_MASS_DESTRUCTION"),
                        anim("Rage Stomp", "GUARDIAN_RAGE_STOMP"),
                        anim("Rage Uppercut", "GUARDIAN_RAGE_UPPERCUT"),
                        anim("Hug Me", "GUARDIAN_HUG_ME"),
                        anim("Air Strike", "GUARDIAN_AIR_STRIKE1", "GUARDIAN_AIR_STRIKE2"),
                        anim("Swing", "GUARDIAN_LEFT_SWING", "GUARDIAN_RIGHT_SWING"),
                        anim("Black Hole", "GUARDIAN_BLACKHOLE"),
                        anim("Rocket Punch", "GUARDIAN_ROCKETPUNCH")
                }));

        // The Harbinger - native Lionfish animations. Death/stun lifecycle animations are excluded.
        register(HARBINGER, new BossProfile("The Harbinger", Mode.ANIMATION,
                anim("Charge", "CHARGE_ANIMATION"),
                new Ability[]{
                        anim("Death Laser", "DEATHLASER_ANIMATION"),
                        anim("Charge", "CHARGE_ANIMATION"),
                        anim("Launch", "LAUNCH_ANIAMATION", "LAUNCH_ANIMATION"),
                        anim("Missile Fire", "MISSILE_FIRE_ANIAMATION", "MISSILE_FIRE_ANIMATION"),
                        anim("Fast Missile Fire", "MISSILE_FIRE_FAST_ANIAMATION", "MISSILE_FIRE_FAST_ANIMATION")
                }));

        // Ancient Remnant - InternalAnimation states. Equivalent left/right stomp variants are grouped.
        register(REMNANT, new BossProfile("Ancient Remnant", Mode.INTERNAL,
                state("Bite", 4, 70),
                new Ability[]{
                        state("Bite", 4, 70),
                        state("Three Tail Hit", 5, 90),
                        state("Sandstorm Roar", 6, 60),
                        randomState("Stomp", new int[]{8, 9}, new int[]{50, 50}),
                        randomState("Double Stomp", new int[]{13, 14}, new int[]{66, 66}),
                        state("Ground Tail", 15, 110),
                        state("Tail Swing", 16, 58),
                        state("Monolith", 17, 70),
                        sequence("Charge", new int[]{10, 11, 12}, new int[]{70, 60, 120})
                }));

        // The Leviathan - native Lionfish animations. Phase/death/stun lifecycle animations excluded.
        register(LEVIATHAN, new BossProfile("The Leviathan", Mode.ANIMATION,
                anim("Bite", "LEVIATHAN_BITE"),
                new Ability[]{
                        anim("Bite", "LEVIATHAN_BITE"),
                        anim("Grab", "LEVIATHAN_GRAB"),
                        anim("Grab Bite", "LEVIATHAN_GRAB_BITE"),
                        anim("Abyss Blast", "LEVIATHAN_ABYSS_BLAST"),
                        anim("Abyss Blast Fire", "LEVIATHAN_ABYSS_BLAST_FIRE"),
                        anim("Rush", "LEVIATHAN_RUSH"),
                        anim("Portal Blast", "LEVIATHAN_ABYSS_BLAST_PORTAL"),
                        anim("Tentacle Strike",
                                "LEVIATHAN_TENTACLE_STRIKE_UPPER_R", "LEVIATHAN_TENTACLE_STRIKE_LOWER_R",
                                "LEVIATHAN_TENTACLE_STRIKE_UPPER_L", "LEVIATHAN_TENTACLE_STRIKE_LOWER_L"),
                        anim("Tentacle Hold", "LEVIATHAN_TENTACLE_HOLD"),
                        anim("Tentacle Hold Blast", "LEVIATHAN_TENTACLE_HOLD_BLAST"),
                        anim("Tail Whips", "LEVIATHAN_TAIL_WHIPS"),
                        anim("Break Dimension", "LEVIATHAN_BREAK_DIMENSION"),
                        anim("Abyss Mine", "LEVIATHAN_MINE")
                }));

        // Scylla - InternalAnimation states. Spawn/death/enchant lifecycle states are automatic/excluded.
        register(SCYLLA, new BossProfile("Scylla", Mode.INTERNAL,
                randomState("Cross Swing", new int[]{1, 2}, new int[]{55, 55}),
                new Ability[]{
                        randomState("Cross Swing", new int[]{1, 2}, new int[]{55, 55}),
                        state("Double Swing", 3, 70),
                        state("Swing Smash", 4, 70),
                        state("Smash", 5, 44),
                        state("Back Step", 6, 16),
                        state("Spin", 7, 87),
                        state("Wave", 8, 65),
                        state("Lightning Explosion", 9, 150),
                        randomState("Spear", new int[]{10, 11}, new int[]{60, 60}),
                        state("Anchor Shot", 13, 90),
                        state("Anchor Pull", 14, 23),
                        sequence("Chain Jump", new int[]{15, 16, 17}, new int[]{70, 44, 23}),
                        state("Anchor Explosion", 18, 85),
                        state("Parry", 19, 15),
                        state("Electric Whip", 20, 50),
                        state("Whip + Spear", 25, 130),
                        state("Summon Snake", 26, 75),
                        state("Grab Smash", 27, 35)
                }));

        // Maledictus - InternalAnimation states. Multi-stage attacks are sequenced because NoAI stops Goals.
        register(MALEDICTUS, new BossProfile("Maledictus", Mode.INTERNAL,
                state("Swing", 1, 44),
                new Ability[]{
                        state("Swing", 1, 44),
                        state("Shoot", 2, 45),
                        sequence("Flying Shoot", new int[]{3, 4, 5}, new int[]{68, 100, 27}),
                        state("Mass Effect", 7, 66),
                        sequence("Flying Smash", new int[]{8, 9}, new int[]{100, 27}),
                        sequenceVariants("Back Step Rush",
                                new int[][]{{10, 11}, {10, 12}},
                                new int[][]{{15, 65}, {15, 55}}),
                        randomState("Dash", new int[]{13, 15, 17}, new int[]{55, 55, 58}),
                        state("Spin Slashes", 18, 68),
                        sequence("Combo", new int[]{19, 21}, new int[]{27, 45}),
                        randomState("Uppercut", new int[]{22, 23}, new int[]{60, 60}),
                        sequence("Flying Halberd Smash", new int[]{24, 25}, new int[]{100, 75}),
                        state("Radagon", 26, 73),
                        state("Halberd Swing", 27, 50),
                        sequence("Grab", new int[]{28, 29, 30}, new int[]{26, 15, 30})
                }));
    }

    private CataclysmBossActionController() {}

    public static void onControlStarted(ServerPlayer player, LivingEntity boss) {
        ResourceLocation type = typeId(boss);
        if (IGNIS.equals(type)) return; // Ignis is deliberately frozen in its proven controller.
        BossProfile profile = PROFILES.get(type);
        if (profile == null) return;

        SELECTED.put(player.getUUID(), 0);
        INTERNAL_RUNS.remove(player.getUUID());
        wakeBossForControl(type, boss);
        showSelected(player, boss, profile);
    }

    public static void handle(ServerPlayer player, int action) {
        LivingEntity boss = BossControlManager.getControlledTarget(player);
        if (boss == null) return;

        ResourceLocation type = typeId(boss);
        if (IGNIS.equals(type)) {
            IgnisActionController.handle(player, action);
            return;
        }

        BossProfile profile = PROFILES.get(type);
        if (profile == null) {
            player.displayClientMessage(Component.literal("Este jefe todavía no tiene adaptador de habilidades.")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        if (action == BossActionPayload.ACTION_SELECT_PREV) {
            cycle(player, boss, profile, -1);
            return;
        }
        if (action == BossActionPayload.ACTION_SELECT_NEXT) {
            cycle(player, boss, profile, 1);
            return;
        }

        if (!isIdle(player, boss, profile)) return;

        if (action == BossActionPayload.ACTION_BASIC) {
            execute(player, boss, profile, profile.basic());
            return;
        }

        if (action == BossActionPayload.ACTION_EXECUTE_SELECTED) {
            int selected = normalizedSelection(player.getUUID(), profile);
            execute(player, boss, profile, profile.abilities()[selected]);
        }
    }

    public static void tickControlledBoss(ServerPlayer player, LivingEntity boss) {
        ResourceLocation type = typeId(boss);
        if (IGNIS.equals(type)) return;
        BossProfile profile = PROFILES.get(type);
        if (profile == null || !boss.isAlive()) return;

        if (profile.mode() == Mode.INTERNAL) {
            tickInternalRun(player, boss, type);
        }

        tickAutomaticTransitions(player, boss, type, profile);
    }

    public static void clear(UUID playerId) {
        SELECTED.remove(playerId);
        INTERNAL_RUNS.remove(playerId);
    }

    public static void clearAll() {
        SELECTED.clear();
        INTERNAL_RUNS.clear();
    }

    public static boolean supports(LivingEntity boss) {
        ResourceLocation type = typeId(boss);
        return IGNIS.equals(type) || PROFILES.containsKey(type);
    }

    private static void tickAutomaticTransitions(ServerPlayer player, LivingEntity boss,
                                                 ResourceLocation type, BossProfile profile) {
        if (!isIdle(player, boss, profile)) return;

        float max = boss.getMaxHealth();
        if (max <= 0.0F) return;
        float ratio = boss.getHealth() / max;

        if (NETHERITE.equals(type)) {
            if (ratio <= 0.40F && !invokeBooleanGetter(boss, "getIsBerserk", false)) {
                invokeBooleanSetter(boss, "setIsBerserk", true);
                startInternalRun(player, boss, new InternalVariant(new int[]{4}, new int[]{54}));
                phaseMessage(player, "NETHERITE MONSTROSITY → BERSERK");
            }
            return;
        }

        if (REMNANT.equals(type)) {
            if (ratio <= 0.50F && !invokeBooleanGetter(boss, "getIsPower", false)) {
                startInternalRun(player, boss, new InternalVariant(new int[]{7}, new int[]{60}));
                phaseMessage(player, "ANCIENT REMNANT → FASE PODER");
            }
            return;
        }

        if (SCYLLA.equals(type)) {
            int phase = invokeIntGetter(boss, "isPhase", 0);
            if (phase < 1 && ratio <= (2.0F / 3.0F)) {
                startInternalRun(player, boss, new InternalVariant(new int[]{21}, new int[]{55}));
                phaseMessage(player, "SCYLLA → FASE 2");
            } else if (phase < 2 && ratio <= (1.0F / 3.0F)) {
                startInternalRun(player, boss, new InternalVariant(new int[]{22}, new int[]{68}));
                phaseMessage(player, "SCYLLA → FASE 3");
            }
            return;
        }

        if (LEVIATHAN.equals(type)) {
            if (ratio <= 0.50F && !invokeBooleanGetter(boss, "getMeltDown", false)) {
                if (triggerAnimation(boss, "LEVIATHAN_PHASE2")) {
                    phaseMessage(player, "LEVIATHAN → FASE 2");
                }
            }
            return;
        }

        if (GUARDIAN.equals(type)) {
            boolean helmetless = invokeBooleanGetter(boss, "getIsHelmetless", false);
            boolean usedMass = invokeBooleanGetter(boss, "getUsedMassDestruction", false);
            if (helmetless && !usedMass && triggerAnimation(boss, "GUARDIAN_MASS_DESTRUCTION")) {
                phaseMessage(player, "ENDER GUARDIAN → MASS DESTRUCTION");
            }
        }
    }

    private static void tickInternalRun(ServerPlayer player, LivingEntity boss, ResourceLocation type) {
        InternalRun run = INTERNAL_RUNS.get(player.getUUID());
        if (run == null) return;

        int expectedState = run.variant().states()[run.step()];
        int currentState = getAttackState(boss);
        if (currentState != expectedState) {
            INTERNAL_RUNS.remove(player.getUUID());
            return;
        }

        int attackTicks = getAttackTicks(boss);
        int duration = run.variant().durations()[run.step()];
        if (attackTicks < duration) return;

        cleanupState(type, boss, expectedState);

        int nextStep = run.step() + 1;
        if (nextStep < run.variant().states().length) {
            int nextState = run.variant().states()[nextStep];
            setAttackState(boss, nextState);
            setupState(type, boss, nextState);
            INTERNAL_RUNS.put(player.getUUID(), new InternalRun(run.variant(), nextStep));
        } else {
            setAttackState(boss, 0);
            INTERNAL_RUNS.remove(player.getUUID());
        }
    }

    private static void execute(ServerPlayer player, LivingEntity boss, BossProfile profile, Ability ability) {
        if (profile.mode() == Mode.ANIMATION) {
            String chosen = chooseExistingAnimation(boss, ability.animationFields());
            if (chosen != null && triggerAnimation(boss, chosen)) {
                actionMessage(player, profile.name(), ability.label());
            } else {
                player.displayClientMessage(Component.literal(profile.name() + ": no se encontró la animación de " + ability.label())
                        .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        InternalVariant[] variants = ability.internalVariants();
        if (variants == null || variants.length == 0) return;
        InternalVariant variant = variants[boss.getRandom().nextInt(variants.length)];
        startInternalRun(player, boss, variant);
        setupState(typeId(boss), boss, variant.states()[0]);
        actionMessage(player, profile.name(), ability.label());
    }

    private static void startInternalRun(ServerPlayer player, LivingEntity boss, InternalVariant variant) {
        if (variant.states().length == 0) return;
        setAttackState(boss, variant.states()[0]);
        setupState(typeId(boss), boss, variant.states()[0]);
        INTERNAL_RUNS.put(player.getUUID(), new InternalRun(variant, 0));
    }

    private static boolean isIdle(ServerPlayer player, LivingEntity boss, BossProfile profile) {
        if (profile.mode() == Mode.ANIMATION) return isAnimationIdle(boss);
        return getAttackState(boss) == 0 && !INTERNAL_RUNS.containsKey(player.getUUID());
    }

    private static void wakeBossForControl(ResourceLocation type, LivingEntity boss) {
        if (NETHERITE.equals(type)) {
            invokeBooleanSetter(boss, "setIsAwaken", true);
        } else if (REMNANT.equals(type)) {
            invokeBooleanSetter(boss, "setNecklace", true);
        } else if (SCYLLA.equals(type)) {
            invokeBooleanSetter(boss, "setAct", true);
        }
    }

    private static void setupState(ResourceLocation type, LivingEntity boss, int state) {
        if (SCYLLA.equals(type)) {
            if (state == 7) invokeBooleanSetter(boss, "setChainAnchor", true);
            if (state == 8) invokeBooleanSetter(boss, "setEye", true);
            if (state == 20) {
                invokeBooleanSetter(boss, "setEye", true);
                invokeBooleanSetter(boss, "setChainAnchor", true);
            }
        }
        if (MALEDICTUS.equals(type)) {
            if (state == 24 || state == 25 || state == 26 || state == 27) invokeIntSetter(boss, "setWeapon", 2);
        }
    }

    private static void cleanupState(ResourceLocation type, LivingEntity boss, int state) {
        if (SCYLLA.equals(type)) {
            if (state == 7) invokeBooleanSetter(boss, "setChainAnchor", false);
            if (state == 8) invokeBooleanSetter(boss, "setEye", false);
            if (state == 20) {
                invokeBooleanSetter(boss, "setEye", false);
                invokeBooleanSetter(boss, "setChainAnchor", false);
            }
        }
        if (MALEDICTUS.equals(type)) {
            if (state == 24 || state == 25 || state == 26 || state == 27) invokeIntSetter(boss, "setWeapon", 0);
        }
    }

    private static void cycle(ServerPlayer player, LivingEntity boss, BossProfile profile, int direction) {
        int selected = normalizedSelection(player.getUUID(), profile);
        SELECTED.put(player.getUUID(), Math.floorMod(selected + direction, profile.abilities().length));
        showSelected(player, boss, profile);
    }

    private static int normalizedSelection(UUID playerId, BossProfile profile) {
        return Math.floorMod(SELECTED.getOrDefault(playerId, 0), profile.abilities().length);
    }

    private static void showSelected(ServerPlayer player, LivingEntity boss, BossProfile profile) {
        int selected = normalizedSelection(player.getUUID(), profile);
        Ability ability = profile.abilities()[selected];
        player.displayClientMessage(
                Component.literal(profile.name() + "  [" + (selected + 1) + "/" + profile.abilities().length + "]  ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(ability.label()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("  •  Z/X cambiar · R usar").withStyle(ChatFormatting.GRAY)),
                true
        );
    }

    private static void actionMessage(ServerPlayer player, String boss, String action) {
        player.displayClientMessage(Component.literal(boss + ": " + action).withStyle(ChatFormatting.GOLD), true);
    }

    private static void phaseMessage(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.AQUA), true);
    }

    private static String chooseExistingAnimation(LivingEntity boss, String[] names) {
        if (names == null || names.length == 0) return null;
        List<String> existing = new ArrayList<>();
        for (String name : names) {
            Field field = findField(boss.getClass(), name);
            if (field != null && Modifier.isStatic(field.getModifiers())) existing.add(name);
        }
        if (existing.isEmpty()) return null;
        return existing.get(boss.getRandom().nextInt(existing.size()));
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
            CastroSafioBossController.LOGGER.warn("No se pudo iniciar {} en {}", fieldName, boss.getType(), exception);
        }
        return false;
    }

    private static int getAttackState(LivingEntity boss) {
        return invokeIntGetter(boss, "getAttackState", 0);
    }

    private static void setAttackState(LivingEntity boss, int state) {
        invokeIntSetter(boss, "setAttackState", state);
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

    private static void invokeIntSetter(Object target, String name, int value) {
        try {
            Method method = findPrimitiveMethod(target.getClass(), name, int.class);
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

    private static ResourceLocation typeId(LivingEntity boss) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cataclysm", path);
    }

    private static void register(ResourceLocation id, BossProfile profile) {
        PROFILES.put(id, profile);
    }

    private static Ability anim(String label, String... fields) {
        return new Ability(label, fields, null);
    }

    private static Ability state(String label, int state, int duration) {
        return new Ability(label, null, new InternalVariant[]{new InternalVariant(new int[]{state}, new int[]{duration})});
    }

    private static Ability randomState(String label, int[] states, int[] durations) {
        InternalVariant[] variants = new InternalVariant[states.length];
        for (int i = 0; i < states.length; i++) {
            variants[i] = new InternalVariant(new int[]{states[i]}, new int[]{durations[i]});
        }
        return new Ability(label, null, variants);
    }

    private static Ability sequence(String label, int[] states, int[] durations) {
        return new Ability(label, null, new InternalVariant[]{new InternalVariant(states, durations)});
    }

    private static Ability sequenceVariants(String label, int[][] states, int[][] durations) {
        InternalVariant[] variants = new InternalVariant[states.length];
        for (int i = 0; i < states.length; i++) variants[i] = new InternalVariant(states[i], durations[i]);
        return new Ability(label, null, variants);
    }

    private enum Mode { ANIMATION, INTERNAL }

    private record Ability(String label, String[] animationFields, InternalVariant[] internalVariants) {}
    private record InternalVariant(int[] states, int[] durations) {}
    private record InternalRun(InternalVariant variant, int step) {}
    private record BossProfile(String name, Mode mode, Ability basic, Ability[] abilities) {}
}
