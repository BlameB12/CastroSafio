from pathlib import Path
import sys

root = Path(sys.argv[1])
move_path = root / 'src/main/java/com/castrosafio/bosscontroller/control/CataclysmBossMovementBridge.java'
action_path = root / 'src/main/java/com/castrosafio/bosscontroller/control/CataclysmBossActionController.java'


def replace_method(src: str, signature: str, replacement: str) -> str:
    start = src.index(signature)
    brace = src.index('{', start)
    depth = 0
    end = None
    for i in range(brace, len(src)):
        ch = src[i]
        if ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        raise RuntimeError('Could not find end of method: ' + signature)
    return src[:start] + replacement + src[end:]


# =============================================================================
# Movement / native Goal-effect bridge
# =============================================================================
s = move_path.read_text()

new_netherite = r'''    private static void tickNetherite(LivingEntity boss, LivingEntity target) {
        int state = getAttackState(boss);
        int tick = getAttackTicks(boss);

        // These projectiles are created inside Cataclysm AI Goals, not aiStep(). NoAI stops the
        // selector, so manually tick ONLY the matching original Goal while the selected ability
        // is active. This keeps Cataclysm's real Lava_Bomb / Flare_Bomb constructors, config,
        // spread and damage instead of duplicating them in Boss Controller.
        if (state == 6) tickRegisteredGoal(boss, "Magmashoot", "MagmaShoot");
        if (state == 10) tickRegisteredGoal(boss, "Flareshoot", "FlareShoot");

        // Native ShoulderCheck navigation is also suppressed by NoAI. Keep collision-authoritative
        // direct movement so the charge actually travels instead of only playing its animation.
        if (state == 8 && tick > 19 && tick < 49) {
            moveTowardOrForward(boss, target, 0.58D, 2.2D, false);
        }
    }
'''
s = replace_method(s, '    private static void tickNetherite(LivingEntity boss, LivingEntity target)', new_netherite)

new_remnant = r'''    private static void tickRemnant(LivingEntity boss, LivingEntity target) {
        int state = getAttackState(boss);
        int tick = getAttackTicks(boss);

        // Monolith/steles are spawned from RemnantMonolithAttackGoal. aiStep only handles the
        // animation-side damage/particles, so without this manual Goal tick the controlled boss
        // can visually attack while spawning no monoliths.
        if (state == 17) tickRegisteredGoal(boss, "RemnantMonolithAttackGoal");

        // Charge state: the original Goal continuously advances the Remnant. Direct movement is
        // required because BossControl intentionally clears horizontal delta velocity each tick.
        if (state == 11 && tick > 0 && tick <= 60) {
            moveTowardOrForward(boss, target, 0.78D, 1.8D, false);
        }
    }
'''
s = replace_method(s, '    private static void tickRemnant(LivingEntity boss, LivingEntity target)', new_remnant)

new_scylla = r'''    private static void tickScylla(LivingEntity boss, LivingEntity target) {
        int state = getAttackState(boss);
        int tick = getAttackTicks(boss);

        // Goal-only entity/projectile abilities. Ticking the registered original Goal restores the
        // real Cataclysm projectiles and entity UUID bookkeeping while keeping global AI disabled.
        if (state == 9) tickRegisteredGoal(boss, "ThunderCloud");
        if (state == 10 || state == 11) tickRegisteredGoal(boss, "SpearThrowGoal");
        if (state == 13) tickRegisteredGoal(boss, "AnchorThrowGoal");
        if (state == 15) tickRegisteredGoal(boss, "Scylla_Flying", "ScyllaFlying");
        if (state == 18) {
            Object goal = findRegisteredGoal(boss, "Scylla_Lightning_Explosion");
            // This Goal normally records the target position in start(). The controller starts
            // attackState directly, so seed the same fields once before its predictive strikes.
            if (goal != null && tick <= 1 && target != null) {
                writeNumericField(goal, "prevX", target.getX());
                writeNumericField(goal, "prevZ", target.getZ());
            }
            tickGoalObject(goal);
        }
        if (state == 25) tickRegisteredGoal(boss, "WhipAndSpearGoal", "WhipAndSpear");
        if (state == 26) tickRegisteredGoal(boss, "SummonSnake", "SummonSnakeGoal");

        // Back-step: preserve the visible dodge because its native horizontal delta is cleared by
        // BossControl after the tick.
        if (state == 6 && tick >= 1 && tick <= 7) {
            double dy = switch (tick) {
                case 1 -> 0.30D;
                case 2 -> 0.22D;
                case 3 -> 0.14D;
                case 4 -> 0.06D;
                case 5 -> -0.02D;
                case 6 -> -0.10D;
                default -> -0.18D;
            };
            moveRelativeToLook(boss, -0.27D, dy);
        }

        // Chain Jump. The original Scylla_Flying Goal performs the large vertical launch at tick 9
        // (and is manually ticked above). Keep only horizontal homing here so we do not add a
        // second fake vertical arc on top of Cataclysm's native 1.7+ jump impulse.
        if (state == 15 && tick >= 9 && tick <= 46) {
            Vec3 horizontal = horizontalToward(boss, target, 0.20D, 2.0D);
            if (horizontal.lengthSqr() > 1.0E-8D) {
                moveDirect(boss, new Vec3(horizontal.x, 0.0D, horizontal.z));
            }
        } else if (state == 16) {
            // Pull/falling section: horizontal velocity from the native Goal would be cleared, so
            // keep homing directly while gravity/vertical movement remains entity-controlled.
            Vec3 horizontal = horizontalToward(boss, target, 0.18D, 1.8D);
            if (horizontal.lengthSqr() > 1.0E-8D) {
                moveDirect(boss, new Vec3(horizontal.x, 0.0D, horizontal.z));
            }
        }

        // Grab Smash closes its own gap; the actual damage/grab behavior remains Cataclysm aiStep.
        if (state == 27 && tick >= 2 && tick <= 12) {
            moveTowardOrForward(boss, target, 0.46D, 2.2D, false);
        }
    }
'''
s = replace_method(s, '    private static void tickScylla(LivingEntity boss, LivingEntity target)', new_scylla)

new_guardian = r'''    private static void tickGuardian(LivingEntity boss, LivingEntity target) {
        int tick = getAnimationTick(boss);
        if (tick < 0) return;

        // Uppercut bullets and Black Hole's Void Vortex are emitted from AI Goals. Execute only
        // those original Goal ticks so the controller gets the exact native entities/effects.
        if (isCurrentAnimation(boss, "GUARDIAN_UPPERCUT_AND_BULLET")) {
            tickRegisteredGoal(boss, "UppercutAndBulletGoal");
        }
        if (isCurrentAnimation(boss, "GUARDIAN_RAGE_UPPERCUT")) {
            tickRegisteredGoal(boss, "RageUppercut", "RageUppercutGoal");
        }
        if (isCurrentAnimation(boss, "GUARDIAN_BLACKHOLE")) {
            tickRegisteredGoal(boss, "VoidVortexGoal");
        }

        if ((isCurrentAnimation(boss, "GUARDIAN_UPPERCUT_AND_BULLET")
                || isCurrentAnimation(boss, "GUARDIAN_RAGE_UPPERCUT"))
                && tick >= 26 && tick <= 31) {
            moveTowardOrForward(boss, target, 0.62D, 1.5D, false);
        }

        if (isCurrentAnimation(boss, "GUARDIAN_HUG_ME") && tick == 20 && target != null) {
            boss.teleportTo(target.getX(), target.getY(), target.getZ());
        }

        if (isCurrentAnimation(boss, "GUARDIAN_AIR_STRIKE1")) {
            if (tick == 40 && target != null) {
                double y = invokeBooleanGetter(boss, "getIsHelmetless", false) ? 8.0D : 4.0D;
                boss.teleportTo(target.getX(), target.getY() + y, target.getZ());
            }
            if (tick > 48 && boss.onGround()) triggerAnimation(boss, "GUARDIAN_AIR_STRIKE2");
        }

        if (isCurrentAnimation(boss, "GUARDIAN_BLACKHOLE") && tick == 37 && target != null
                && boss.distanceToSqr(target) >= 25.0D) {
            triggerAnimation(boss, "GUARDIAN_ROCKETPUNCH");
        }

        if (isCurrentAnimation(boss, "GUARDIAN_ROCKETPUNCH") && tick >= 24 && tick <= 29) {
            moveTowardOrForward(boss, target, 0.88D, 1.1D, false);
        }
    }
'''
s = replace_method(s, '    private static void tickGuardian(LivingEntity boss, LivingEntity target)', new_guardian)

new_harbinger = r'''    private static void tickHarbinger(LivingEntity boss, LivingEntity target) {
        int tick = getAnimationTick(boss);
        if (tick < 0) return;

        // Harbinger stores virtually all of its special projectile creation in Goals. Manual Goal
        // ticking restores the real Death Laser, Howitzer volley and both homing-missile patterns.
        if (isCurrentAnimation(boss, "DEATHLASER_ANIMATION")) {
            tickRegisteredGoal(boss, "DeathLaserGoal");
        } else if (isCurrentAnimation(boss, "LAUNCH_ANIAMATION")
                || isCurrentAnimation(boss, "LAUNCH_ANIMATION")) {
            tickRegisteredGoal(boss, "LaunchGoal");
        } else if (isCurrentAnimation(boss, "MISSILE_FIRE_ANIAMATION")
                || isCurrentAnimation(boss, "MISSILE_FIRE_ANIMATION")) {
            tickRegisteredGoal(boss, "MissileLaunchGoal");
        } else if (isCurrentAnimation(boss, "MISSILE_FIRE_FAST_ANIAMATION")
                || isCurrentAnimation(boss, "MISSILE_FIRE_FAST_ANIMATION")) {
            tickRegisteredGoal(boss, "MissileLaunchGoal2");
        }

        if (!isCurrentAnimation(boss, "CHARGE_ANIMATION")) return;

        // Run the native Goal too (sets charge flags and target-directed velocity), then mirror the
        // horizontal component with direct movement because BossControl clears horizontal delta.
        tickRegisteredGoal(boss, "ChargeGoal");
        if (tick == 24) invokeBooleanSetter(boss, "setIsCharge", true);
        if (tick >= 24 && tick <= 35) {
            if (target != null) {
                Vec3 delta = target.position().subtract(boss.position()).add(0.0D, -2.0D, 0.0D);
                double speed = tick == 24 ? 1.85D : 1.20D;
                moveVectorLimited(boss, delta, speed, 0.8D);
            } else {
                moveRelativeToLook(boss, tick == 24 ? 1.65D : 1.05D, 0.0D);
            }
        }
        if (tick >= 36) invokeBooleanSetter(boss, "setIsCharge", false);
    }
'''
s = replace_method(s, '    private static void tickHarbinger(LivingEntity boss, LivingEntity target)', new_harbinger)

new_leviathan = r'''    private static void tickLeviathan(LivingEntity boss, LivingEntity target) {
        int tick = getAnimationTick(boss);
        if (tick < 0) return;

        // Goal-only native entities/effects: tongue grab, Abyss Blast beams, portal barrage,
        // dimensional rift and mines. The real registered Goals already know exact timing/config.
        if (isCurrentAnimation(boss, "LEVIATHAN_GRAB")) {
            tickRegisteredGoal(boss, "LeviathanGrabAttackGoal");
            if (tick >= 3 && tick <= 45) {
                moveTowardOrForward(boss, target, 0.48D, 7.2D, true);
            }
            if (tick >= 25 && target != null && boss.distanceTo(target) < 8.5F) {
                triggerAnimation(boss, "LEVIATHAN_GRAB_BITE");
            }
        }

        if (isCurrentAnimation(boss, "LEVIATHAN_ABYSS_BLAST")) {
            tickRegisteredGoal(boss, "LeviathanBlastAttackGoal");
        }
        if (isCurrentAnimation(boss, "LEVIATHAN_ABYSS_BLAST_FIRE")) {
            tickRegisteredGoal(boss, "LeviathanBlastFireAttackGoal");
        }
        if (isCurrentAnimation(boss, "LEVIATHAN_ABYSS_BLAST_PORTAL")) {
            tickRegisteredGoal(boss, "LeviathanAbyssBlastPortalAttackGoal");
        }
        if (isCurrentAnimation(boss, "LEVIATHAN_TENTACLE_HOLD_BLAST")) {
            tickRegisteredGoal(boss, "LeviathanTentacleHoldBlastAttackGoal");
        }
        if (isCurrentAnimation(boss, "LEVIATHAN_BREAK_DIMENSION")) {
            tickRegisteredGoal(boss, "LeviathanAbyssDimensionAttackGoal");
        }
        if (isCurrentAnimation(boss, "LEVIATHAN_MINE")) {
            tickRegisteredGoal(boss, "LeviathanMineAttackGoal");
        }

        if (isCurrentAnimation(boss, "LEVIATHAN_RUSH") && tick > 53 && tick < 134) {
            moveTowardOrForward(boss, target, 0.52D, 2.8D, true);
        }
    }
'''
s = replace_method(s, '    private static void tickLeviathan(LivingEntity boss, LivingEntity target)', new_leviathan)

# Registered Goal reflection helpers. These deliberately use reflection so Boss Controller remains
# loadable without Cataclysm and survives package/API differences between Cataclysm builds.
goal_helpers = r'''    // -------------------- Native registered Goal bridge --------------------

    private static Object findRegisteredGoal(LivingEntity boss, String... simpleNames) {
        try {
            Field selectorField = findField(boss.getClass(), "goalSelector");
            if (selectorField == null) return null;
            Object selector = selectorField.get(boss);
            if (selector == null) return null;

            Method available = findMethod(selector.getClass(), "getAvailableGoals", 0);
            if (available == null) return null;
            Object raw = available.invoke(selector);
            if (!(raw instanceof Iterable<?> iterable)) return null;

            for (Object wrapped : iterable) {
                if (wrapped == null) continue;
                Method getGoal = findMethod(wrapped.getClass(), "getGoal", 0);
                Object goal = getGoal == null ? wrapped : getGoal.invoke(wrapped);
                if (goal == null) continue;
                String simple = goal.getClass().getSimpleName();
                for (String wanted : simpleNames) {
                    if (wanted.equals(simple)) return goal;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static boolean tickRegisteredGoal(LivingEntity boss, String... simpleNames) {
        return tickGoalObject(findRegisteredGoal(boss, simpleNames));
    }

    private static boolean tickGoalObject(Object goal) {
        if (goal == null) return false;
        try {
            Method tick = findMethod(goal.getClass(), "tick", 0);
            if (tick == null) return false;
            tick.invoke(goal);
            return true;
        } catch (ReflectiveOperationException exception) {
            CastroSafioBossController.LOGGER.debug("No se pudo ejecutar Goal nativo {}", goal.getClass().getName(), exception);
            return false;
        }
    }

    private static void writeNumericField(Object target, String name, double value) {
        Field field = findField(target.getClass(), name);
        if (field == null) return;
        try {
            Class<?> t = field.getType();
            if (t == double.class || t == Double.class) field.set(target, value);
            else if (t == float.class || t == Float.class) field.set(target, (float) value);
            else if (t == int.class || t == Integer.class) field.set(target, (int) value);
            else if (t == long.class || t == Long.class) field.set(target, (long) value);
        } catch (IllegalAccessException ignored) {
        }
    }

'''
marker = '    // -------------------- Automatic target selection --------------------\n'
if marker not in s:
    raise SystemExit('Automatic target marker not found')
s = s.replace(marker, goal_helpers + marker, 1)
move_path.write_text(s)


# =============================================================================
# Internal-state deadlock prevention and native state-transition adoption
# =============================================================================
s = action_path.read_text()

# Anchor Shot can natively enter Anchor Pull when its spawned anchor grabs something. Include state
# 14 in the tracked sequence so that real Goal transition is adopted instead of treated as a bug.
s = s.replace(
    '                        state("Anchor Shot", 13, 90),',
    '                        sequence("Anchor Shot", new int[]{13, 14}, new int[]{90, 23}),',
    1
)

new_tick_controlled = r'''    public static void tickControlledBoss(ServerPlayer player, LivingEntity boss) {
        ResourceLocation type = typeId(boss);
        if (IGNIS.equals(type)) return;
        BossProfile profile = PROFILES.get(type);
        if (profile == null || !boss.isAlive()) return;

        if (profile.mode() == Mode.INTERNAL) {
            // Cataclysm InternalAnimation bosses can change attackState from aiStep/Goal code even
            // while global AI is disabled. If a previous/interrupted sequence leaves a non-zero
            // state with no tracked run, recover it for ALL internal bosses, not only Maledictus.
            if (!INTERNAL_RUNS.containsKey(player.getUUID()) && getAttackState(boss) != 0) {
                resetInternalBossToIdle(type, boss);
            }
            tickInternalRun(player, boss, type);
        }

        tickAutomaticTransitions(player, boss, type, profile);
    }
'''
s = replace_method(s, '    public static void tickControlledBoss(ServerPlayer player, LivingEntity boss)', new_tick_controlled)

new_tick_run = r'''    private static void tickInternalRun(ServerPlayer player, LivingEntity boss, ResourceLocation type) {
        InternalRun run = INTERNAL_RUNS.get(player.getUUID());
        if (run == null) return;

        int expectedState = run.variant().states()[run.step()];
        int currentState = getAttackState(boss);

        if (currentState != expectedState) {
            // Adopt genuine native transitions (landing stages, Scylla anchor pull, Maledictus
            // success/fall stages, etc.) when the resulting state exists later in this sequence.
            int nativeStep = findRemainingStateStep(run.variant(), currentState, run.step() + 1);
            if (nativeStep >= 0) {
                run = new InternalRun(run.variant(), nativeStep);
                INTERNAL_RUNS.put(player.getUUID(), run);
                expectedState = currentState;
            } else if (currentState == 0) {
                INTERNAL_RUNS.remove(player.getUUID());
                return;
            } else {
                // Unknown/unexpected transition: fail closed to idle. Never let R/click remain
                // blocked forever just because a mod update changed an intermediate state.
                INTERNAL_RUNS.remove(player.getUUID());
                resetInternalBossToIdle(type, boss);
                return;
            }
        }

        int attackTicks = getAttackTicks(boss);

        // Maledictus native grab success bridge retained from v0.2.5.
        if (MALEDICTUS.equals(type) && expectedState == 29 && readBooleanField(boss, "grab", false)) {
            writeBooleanField(boss, "grab", false);
            InternalVariant success = new InternalVariant(new int[]{31, 32, 33}, new int[]{60, 100, 35});
            setAttackState(boss, 31);
            setupState(type, boss, 31);
            INTERNAL_RUNS.put(player.getUUID(), new InternalRun(success, 0));
            return;
        }

        // Ancient Remnant's native charge Goal stops early on a crash. NoAI prevents GoalSelector
        // stop(), so promote state 11 -> 12 immediately when the entity's own collision code sets
        // the crash flag instead of waiting for the full artificial duration.
        if (REMNANT.equals(type) && expectedState == 11
                && (invokeBooleanGetter(boss, "getCrash", false) || readBooleanField(boss, "crash", false))) {
            int next = findRemainingStateStep(run.variant(), 12, run.step() + 1);
            if (next >= 0) {
                cleanupState(type, boss, 11);
                setAttackState(boss, 12);
                setupState(type, boss, 12);
                INTERNAL_RUNS.put(player.getUUID(), new InternalRun(run.variant(), next));
                return;
            }
        }

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
            finishInternalRun(type, boss, expectedState);
            setAttackState(boss, 0);
            INTERNAL_RUNS.remove(player.getUUID());
        }
    }
'''
s = replace_method(s, '    private static void tickInternalRun(ServerPlayer player, LivingEntity boss, ResourceLocation type)', new_tick_run)

extra_helpers = r'''    private static void resetInternalBossToIdle(ResourceLocation type, LivingEntity boss) {
        int state = getAttackState(boss);
        cleanupState(type, boss, state);
        if (MALEDICTUS.equals(type)) {
            resetMaledictusToIdle(boss, true);
            return;
        }
        if (SCYLLA.equals(type)) {
            invokeBooleanSetter(boss, "setEye", false);
            invokeBooleanSetter(boss, "setChainAnchor", false);
            invokeBooleanSetter(boss, "setFlying", false);
        }
        if (REMNANT.equals(type)) {
            invokeBooleanSetter(boss, "setCrash", false);
            writeBooleanField(boss, "crash", false);
        }
        boss.setNoGravity(false);
        setAttackState(boss, 0);
    }

    private static void finishInternalRun(ResourceLocation type, LivingEntity boss, int state) {
        if (MALEDICTUS.equals(type)) {
            if (state == 33) boss.ejectPassengers();
            invokeBooleanSetter(boss, "setFlying", false);
            invokeIntSetter(boss, "setWeapon", 0);
            writeBooleanField(boss, "grab", false);
            boss.setNoGravity(false);
        } else if (SCYLLA.equals(type)) {
            invokeBooleanSetter(boss, "setFlying", false);
        } else if (REMNANT.equals(type)) {
            invokeBooleanSetter(boss, "setCrash", false);
            writeBooleanField(boss, "crash", false);
        }
    }

'''
marker2 = '    private static int findRemainingStateStep(InternalVariant variant, int state, int fromStep) {'
if marker2 not in s:
    raise SystemExit('findRemainingStateStep marker not found')
s = s.replace(marker2, extra_helpers + marker2, 1)

action_path.write_text(s)
print('v0.2.7 Cataclysm-wide native Goal bridge + deadlock recovery applied')
