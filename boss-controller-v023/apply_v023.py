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
        if ch == '{': depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        raise RuntimeError('Could not find end of method: ' + signature)
    return src[:start] + replacement + src[end:]

# -------- Maledictus movement: use native timing, never multi-tick homing for dash impulses --------
s = move_path.read_text()
new_maledictus = r'''    private static void tickMaledictus(LivingEntity boss, LivingEntity target) {
        int state = getAttackState(boss);
        int tick = getAttackTicks(boss);

        // IMPORTANT: Cataclysm's Maledictus movement Goals use ONE impulse at an exact tick.
        // The old controller repeated homing movement over several ticks, which stacked impulses,
        // caused sharp turns and made the boss look/feel broken. Mirror the native timing instead.

        // Back Step (state 10): native Goal applies the jump once when the state starts.
        if (state == 10 && tick == 1) {
            moveRelativeToLook(boss, -1.70D, 0.40D);
        }

        // Charge / dash chains. Native charge impulse is exactly at tick 24 or 16.
        if ((state == 11 || state == 12 || state == 13 || state == 14) && tick == 24) {
            maledictusChargeImpulse(boss, target);
        }
        if ((state == 15 || state == 16 || state == 17) && tick == 16) {
            maledictusChargeImpulse(boss, target);
        }

        // Native backsteps happen once at these exact ticks.
        if (state == 11 && tick == 33) maledictusBackstep(boss);
        if (state == 13 && tick == 34) maledictusBackstep(boss);
        if (state == 15 && tick == 27) maledictusBackstep(boss);
        if (state == 17 && tick == 30) maledictusBackstep(boss);

        // Spin Slashes has exactly two charge impulses, at ticks 15 and 29.
        if (state == 18 && (tick == 15 || tick == 29)) {
            double distance = target == null ? 6.5D : boss.distanceTo(target);
            double r = Math.max(2.5D, Math.min(6.5D, distance));
            moveTowardOrForward(boss, target, 0.35D * r, 1.35D, false);
        }

        // Combo pushes only once at tick 8 of each corresponding state.
        if (state == 19 && tick == 8) {
            moveTowardOrForward(boss, target, 1.35D, 1.25D, false);
        }
        if (state == 21 && tick == 8) {
            moveTowardOrForward(boss, target, 1.50D, 1.25D, false);
        }

        // Flying Smash / Flying Halberd Smash.
        // The native Goal launches vertically at tick 25, stays in flying/no-gravity mode,
        // then dives at tick 56 (normal) or 51 (halberd). Keep a stable controlled arc instead
        // of the previous parabola that could reverse or stack with controller movement.
        if (state == 8 || state == 24) {
            int strike = state == 8 ? 56 : 51;
            if (tick == 25) invokeBooleanSetter(boss, "setFlying", true);

            if (tick >= 25 && tick < strike) {
                int airborneTick = tick - 25;
                double dy = Math.max(0.16D, 0.48D - airborneTick * 0.012D);
                Vec3 horizontal = horizontalToward(boss, target, 0.055D, 2.0D);
                moveDirect(boss, new Vec3(horizontal.x, dy, horizontal.z));
            } else if (tick >= strike) {
                invokeBooleanSetter(boss, "setFlying", false);
                if (!boss.onGround()) {
                    Vec3 horizontal = horizontalToward(boss, target, 0.14D, 1.4D);
                    moveDirect(boss, new Vec3(horizontal.x, -0.68D, horizontal.z));
                }
            }
        }

        // REAL grab movement is state 29 (Grab Loop), not state 28 (Grab Start).
        // Close the gap by itself while the native hitbox decides whether the grab connected.
        if (state == 29 && tick >= 1 && tick <= 15) {
            moveTowardOrForward(boss, target, 0.72D, 1.35D, false);
        }

        // Successful grab sequence: native Goal launches Maledictus + passenger at tick 19,
        // then drops back down. NoAI removes that Goal, so restore just the missing movement.
        if (state == 31) {
            if (tick == 19) invokeBooleanSetter(boss, "setFlying", true);
            if (tick >= 19 && tick < 30) {
                moveDirect(boss, new Vec3(0.0D, 0.52D, 0.0D));
            } else if (tick >= 30) {
                invokeBooleanSetter(boss, "setFlying", false);
                if (!boss.onGround()) moveDirect(boss, new Vec3(0.0D, -0.55D, 0.0D));
            }
        } else if (state == 32 && !boss.onGround()) {
            moveDirect(boss, new Vec3(0.0D, -0.58D, 0.0D));
        }
    }

    private static void maledictusChargeImpulse(LivingEntity boss, LivingEntity target) {
        if (target == null) {
            moveRelativeToLook(boss, 3.0D, 0.0D);
            return;
        }
        double distance = boss.distanceTo(target);
        double r = Math.max(0.0D, Math.min(7.0D, distance));
        // Original Goal uses 0.9 * clamped distance. Cap the direct displacement slightly so
        // server collision remains stable while preserving the strong native dash feel.
        double step = Math.min(4.25D, 0.9D * r);
        moveTowardOrForward(boss, target, step, 1.25D, false);
    }

    private static void maledictusBackstep(LivingEntity boss) {
        moveRelativeToLook(boss, -1.70D, 0.40D);
    }
'''
s = replace_method(s,
    '    private static void tickMaledictus(LivingEntity boss, LivingEntity target)',
    new_maledictus)
move_path.write_text(s)

# -------- Maledictus action chains: follow the real health-dependent state paths --------
s = action_path.read_text()
old_profile = '''                        sequenceVariants("Back Step Rush",
                                new int[][]{{10, 11}, {10, 12}},
                                new int[][]{{15, 65}, {15, 55}}),
                        randomState("Dash", new int[]{13, 15, 17}, new int[]{55, 55, 58}),'''
new_profile = '''                        sequenceVariants("Back Step Rush",
                                new int[][]{{10, 11}, {10, 12, 15}, {10, 12, 16, 17}},
                                new int[][]{{15, 65}, {15, 55, 55}, {15, 55, 40, 58}}),
                        sequenceVariants("Dash",
                                new int[][]{{13}, {14, 15}, {14, 16, 17}},
                                new int[][]{{65}, {55, 55}, {55, 40, 58}}),'''
if old_profile not in s:
    raise SystemExit('Maledictus profile block not found')
s = s.replace(old_profile, new_profile, 1)

needle = '''        InternalVariant[] variants = ability.internalVariants();
        if (variants == null || variants.length == 0) return;'''
insert = '''        if (MALEDICTUS.equals(typeId(boss))) {
            InternalVariant nativeVariant = chooseMaledictusNativeVariant(boss, ability.label());
            if (nativeVariant != null) {
                startInternalRun(player, boss, nativeVariant);
                setupState(typeId(boss), boss, nativeVariant.states()[0]);
                actionMessage(player, profile.name(), ability.label());
                return;
            }
        }

        InternalVariant[] variants = ability.internalVariants();
        if (variants == null || variants.length == 0) return;'''
if needle not in s:
    raise SystemExit('execute insertion point not found')
s = s.replace(needle, insert, 1)

helper = r'''    private static InternalVariant chooseMaledictusNativeVariant(LivingEntity boss, String label) {
        float ratio = boss.getMaxHealth() <= 0.0F ? 1.0F : boss.getHealth() / boss.getMaxHealth();

        if ("Back Step Rush".equals(label)) {
            // Native path: 10 -> 11 above half health;
            // 10 -> 12 -> 15 below half; 10 -> 12 -> 16 -> 17 below quarter.
            if (ratio <= 0.25F) return new InternalVariant(new int[]{10, 12, 16, 17}, new int[]{15, 55, 40, 58});
            if (ratio <= 0.50F) return new InternalVariant(new int[]{10, 12, 15}, new int[]{15, 55, 55});
            return new InternalVariant(new int[]{10, 11}, new int[]{15, 65});
        }

        if ("Dash".equals(label)) {
            // Native direct dash starts in 13 above half health and in 14 below half.
            // State 14 then chains through 15, or 16 -> 17 at quarter health.
            if (ratio <= 0.25F) return new InternalVariant(new int[]{14, 16, 17}, new int[]{55, 40, 58});
            if (ratio <= 0.50F) return new InternalVariant(new int[]{14, 15}, new int[]{55, 55});
            return new InternalVariant(new int[]{13}, new int[]{65});
        }

        return null;
    }

'''
marker = '    private static void startInternalRun(ServerPlayer player, LivingEntity boss, InternalVariant variant) {'
if marker not in s:
    raise SystemExit('startInternalRun marker not found')
s = s.replace(marker, helper + marker, 1)

# Grab success is data-driven by Maledictus' native hit code (private boolean grab).
# If it connected during state 29, follow the native 31 -> 32 -> 33 success chain instead
# of forcing the generic failure state 30.
needle = '''        int attackTicks = getAttackTicks(boss);
        int duration = run.variant().durations()[run.step()];'''
insert = '''        int attackTicks = getAttackTicks(boss);

        if (MALEDICTUS.equals(type) && currentState == 29 && readBooleanField(boss, "grab", false)) {
            writeBooleanField(boss, "grab", false);
            InternalVariant success = new InternalVariant(new int[]{31, 32, 33}, new int[]{60, 100, 35});
            setAttackState(boss, 31);
            setupState(type, boss, 31);
            INTERNAL_RUNS.put(player.getUUID(), new InternalRun(success, 0));
            return;
        }

        int duration = run.variant().durations()[run.step()];'''
if needle not in s:
    raise SystemExit('tickInternalRun insertion point not found')
s = s.replace(needle, insert, 1)

old_setup = '''        if (MALEDICTUS.equals(type)) {
            if (state == 24 || state == 25 || state == 26 || state == 27) invokeIntSetter(boss, "setWeapon", 2);
        }'''
new_setup = '''        if (MALEDICTUS.equals(type)) {
            if (state == 2 || state == 3 || state == 4) {
                invokeIntSetter(boss, "setWeapon", 1);
            } else if ((state >= 11 && state <= 17) || (state >= 24 && state <= 27)) {
                invokeIntSetter(boss, "setWeapon", 2);
            } else if (state >= 28 && state <= 33) {
                invokeIntSetter(boss, "setWeapon", 3);
            }
        }'''
if old_setup not in s:
    raise SystemExit('setupState Maledictus block not found')
s = s.replace(old_setup, new_setup, 1)

old_cleanup = '''        if (MALEDICTUS.equals(type)) {
            if (state == 24 || state == 25 || state == 26 || state == 27) invokeIntSetter(boss, "setWeapon", 0);
        }'''
new_cleanup = '''        if (MALEDICTUS.equals(type)) {
            if (state == 2 || state == 3 || state == 4
                    || (state >= 11 && state <= 17)
                    || (state >= 24 && state <= 33)) {
                invokeIntSetter(boss, "setWeapon", 0);
            }
        }'''
if old_cleanup not in s:
    raise SystemExit('cleanupState Maledictus block not found')
s = s.replace(old_cleanup, new_cleanup, 1)

field_helpers = r'''    private static boolean readBooleanField(Object target, String name, boolean fallback) {
        Field field = findField(target.getClass(), name);
        if (field == null) return fallback;
        try {
            Object value = field.get(target);
            return value instanceof Boolean b ? b : fallback;
        } catch (IllegalAccessException ignored) {
            return fallback;
        }
    }

    private static void writeBooleanField(Object target, String name, boolean value) {
        Field field = findField(target.getClass(), name);
        if (field == null) return;
        try {
            field.setBoolean(target, value);
        } catch (IllegalAccessException ignored) {
        }
    }

'''
marker = '    private static Method findMethod(Class<?> type, String name, int parameterCount) {'
if marker not in s:
    raise SystemExit('findMethod marker not found')
s = s.replace(marker, field_helpers + marker, 1)

action_path.write_text(s)
print('v0.2.3 Maledictus stabilization applied')
