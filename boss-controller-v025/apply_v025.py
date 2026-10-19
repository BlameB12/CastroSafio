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


# -----------------------------------------------------------------------------
# Maledictus movement: restore the big native no-gravity jumps and landing flow.
# -----------------------------------------------------------------------------
s = move_path.read_text()
new_maledictus = r'''    private static void tickMaledictus(LivingEntity boss, LivingEntity target) {
        int state = getAttackState(boss);
        int tick = getAttackTicks(boss);

        // Phantom Arrow creation lives in Maledictus' AI Goals. Keep the v0.2.4 bridge.
        if (state == 2 && tick == 29 && target != null) {
            fireMaledictusPhantomVolley(boss, target, 6.0D, 1.8F);
        }
        if (state == 3 && tick == 50 && target != null) {
            fireMaledictusPhantomVolley(boss, target, 9.0D, 1.5F);
        }

        // Flying Shoot: native Goal sets Y velocity to 0.9 at tick 8 and enables Flying.
        // Flying disables gravity, so that 0.9 persists until tick 20, producing a large jump.
        // BossControl clears delta velocity every tick, therefore reproduce that displacement
        // directly instead of setting velocity once.
        if (state == 3) {
            if (tick == 8) invokeBooleanSetter(boss, "setFlying", true);
            if (tick >= 8 && tick < 20) {
                moveDirect(boss, new Vec3(0.0D, 0.90D, 0.0D));
            }
            if (tick >= 60) {
                invokeBooleanSetter(boss, "setFlying", false);
                if (!boss.onGround()) moveDirect(boss, new Vec3(0.0D, -0.42D, 0.0D));
            }
        }

        // State 4 is the real falling loop after Flying Shoot. Cataclysm itself switches 4 -> 5
        // as soon as the feet collide with ground/fluid. The action controller now adopts that
        // native transition instead of treating it as an error.
        if (state == 4) {
            invokeBooleanSetter(boss, "setFlying", false);
            if (!boss.onGround()) moveDirect(boss, new Vec3(0.0D, -0.82D, 0.0D));
        }

        // Back Step (state 10): one native jump impulse.
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

        // Spin Slashes: exactly two native impulses.
        if (state == 18 && (tick == 15 || tick == 29)) {
            double distance = target == null ? 6.5D : boss.distanceTo(target);
            double r = Math.max(2.5D, Math.min(6.5D, distance));
            moveTowardOrForward(boss, target, 0.35D * r, 1.35D, false);
        }

        // Combo: one push at tick 8 of each stage.
        if (state == 19 && tick == 8) {
            moveTowardOrForward(boss, target, 1.35D, 1.25D, false);
        }
        if (state == 21 && tick == 8) {
            moveTowardOrForward(boss, target, 1.50D, 1.25D, false);
        }

        // Flying Smash / Flying Halberd Smash.
        // Native code launches at 0.9 Y and then turns gravity off until the strike tick.
        // This is intentionally a BIG jump (~20-30 blocks depending on the variant).
        if (state == 8 || state == 24) {
            int strike = state == 8 ? 56 : 51;
            double dropFactor = state == 8 ? 0.125D : 0.145D;

            if (tick == 25) invokeBooleanSetter(boss, "setFlying", true);

            if (tick >= 25 && tick < strike) {
                Vec3 horizontal = horizontalToward(boss, target, 0.06D, 1.5D);
                moveDirect(boss, new Vec3(horizontal.x, 0.90D, horizontal.z));
            } else if (tick >= strike) {
                invokeBooleanSetter(boss, "setFlying", false);
                if (!boss.onGround()) {
                    double vertical = 1.05D;
                    if (target != null) {
                        vertical = Math.max(0.85D,
                                Math.min(2.60D, dropFactor * Math.abs(boss.getY() - target.getY())));
                    }
                    Vec3 horizontal = horizontalToward(boss, target, 0.42D, 0.6D);
                    moveDirect(boss, new Vec3(horizontal.x, -vertical, horizontal.z));
                }
            }
        }

        // Landing stages. Normally these Goals finish the attack after the real collision.
        if ((state == 9 || state == 25) && !boss.onGround()) {
            invokeBooleanSetter(boss, "setFlying", false);
            moveDirect(boss, new Vec3(0.0D, -1.00D, 0.0D));
        }

        // REAL grab approach is state 29 (Grab Loop), not state 28.
        if (state == 29 && tick >= 1 && tick <= 15) {
            moveTowardOrForward(boss, target, 0.72D, 1.35D, false);
        }

        // Successful grab: native Maledictus launches itself + victim with Y=1.2 at tick 19,
        // while Flying disables gravity until tick 30. Reproduce the full giant leap directly.
        if (state == 31) {
            if (tick == 19) invokeBooleanSetter(boss, "setFlying", true);

            if (tick >= 19 && tick < 30) {
                moveDirect(boss, new Vec3(0.0D, 1.20D, 0.0D));
            } else if (tick >= 30) {
                invokeBooleanSetter(boss, "setFlying", false);
                if (!boss.onGround()) {
                    double fall = Math.min(1.55D, 0.38D + (tick - 30) * 0.075D);
                    moveDirect(boss, new Vec3(0.0D, -fall, 0.0D));
                }
            }
        }

        // State 32 is the grab-success falling loop. The entity's own aiStep changes it to 33
        // on ground contact. Keep falling strongly until that native transition occurs.
        if (state == 32) {
            invokeBooleanSetter(boss, "setFlying", false);
            if (!boss.onGround()) moveDirect(boss, new Vec3(0.0D, -1.25D, 0.0D));
        }

        if (state == 33) {
            invokeBooleanSetter(boss, "setFlying", false);
        }
    }

    private static void maledictusChargeImpulse(LivingEntity boss, LivingEntity target) {
        if (target == null) {
            moveRelativeToLook(boss, 3.0D, 0.0D);
            return;
        }
        double distance = boss.distanceTo(target);
        double r = Math.max(0.0D, Math.min(7.0D, distance));
        double step = Math.min(4.25D, 0.9D * r);
        moveTowardOrForward(boss, target, step, 1.25D, false);
    }

    private static void maledictusBackstep(LivingEntity boss) {
        moveRelativeToLook(boss, -1.70D, 0.40D);
    }
'''

s = replace_method(
    s,
    '    private static void tickMaledictus(LivingEntity boss, LivingEntity target)',
    new_maledictus
)
move_path.write_text(s)


# -----------------------------------------------------------------------------
# Internal state sequencer: accept native transitions and never leave a boss stuck.
# -----------------------------------------------------------------------------
s = action_path.read_text()

new_tick_controlled = r'''    public static void tickControlledBoss(ServerPlayer player, LivingEntity boss) {
        ResourceLocation type = typeId(boss);
        if (IGNIS.equals(type)) return;
        BossProfile profile = PROFILES.get(type);
        if (profile == null || !boss.isAlive()) return;

        if (profile.mode() == Mode.INTERNAL) {
            // Recovery for a state left behind by an interrupted/old controller sequence.
            // Maledictus has no automatic phase state that should remain orphaned, so any
            // non-zero state without a tracked run is a stale attack and must be cleared.
            if (MALEDICTUS.equals(type)
                    && !INTERNAL_RUNS.containsKey(player.getUUID())
                    && getAttackState(boss) != 0) {
                resetMaledictusToIdle(boss, true);
            }
            tickInternalRun(player, boss, type);
        }

        tickAutomaticTransitions(player, boss, type, profile);
    }
'''
s = replace_method(
    s,
    '    public static void tickControlledBoss(ServerPlayer player, LivingEntity boss)',
    new_tick_controlled
)

new_tick_run = r'''    private static void tickInternalRun(ServerPlayer player, LivingEntity boss, ResourceLocation type) {
        InternalRun run = INTERNAL_RUNS.get(player.getUUID());
        if (run == null) return;

        int expectedState = run.variant().states()[run.step()];
        int currentState = getAttackState(boss);

        if (currentState != expectedState) {
            // Cataclysm has several REAL entity-side transitions that happen even with NoAI:
            // Maledictus 4->5 on landing, 8->9 on landing, 24->25 on landing and 32->33
            // during the successful grab. If the new state exists later in our sequence,
            // adopt it instead of deleting the run and leaving the boss permanently non-idle.
            int nativeStep = findRemainingStateStep(run.variant(), currentState, run.step() + 1);
            if (nativeStep >= 0) {
                run = new InternalRun(run.variant(), nativeStep);
                INTERNAL_RUNS.put(player.getUUID(), run);
                expectedState = currentState;
            } else if (currentState == 0) {
                INTERNAL_RUNS.remove(player.getUUID());
                return;
            } else {
                // Unexpected state = fail closed. Never strand a controlled boss in a state
                // where click/R are blocked forever.
                INTERNAL_RUNS.remove(player.getUUID());
                if (MALEDICTUS.equals(type)) {
                    resetMaledictusToIdle(boss, true);
                } else {
                    cleanupState(type, boss, currentState);
                    setAttackState(boss, 0);
                }
                return;
            }
        }

        int attackTicks = getAttackTicks(boss);

        // Native grab hit code sets Maledictus' private `grab` flag while state 29 is active.
        // Switch immediately to the real successful 31 -> 32 -> 33 sequence.
        if (MALEDICTUS.equals(type) && expectedState == 29 && readBooleanField(boss, "grab", false)) {
            writeBooleanField(boss, "grab", false);
            InternalVariant success = new InternalVariant(new int[]{31, 32, 33}, new int[]{60, 100, 35});
            setAttackState(boss, 31);
            setupState(type, boss, 31);
            INTERNAL_RUNS.put(player.getUUID(), new InternalRun(success, 0));
            return;
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
            if (MALEDICTUS.equals(type)) {
                // State 33 normally ejects its victim at tick 23. The explicit eject is a
                // safety net for latency/interrupted camera packets and prevents stuck riders.
                if (expectedState == 33) boss.ejectPassengers();
                invokeBooleanSetter(boss, "setFlying", false);
                invokeIntSetter(boss, "setWeapon", 0);
                writeBooleanField(boss, "grab", false);
                boss.setNoGravity(false);
            }
            setAttackState(boss, 0);
            INTERNAL_RUNS.remove(player.getUUID());
        }
    }
'''
s = replace_method(
    s,
    '    private static void tickInternalRun(ServerPlayer player, LivingEntity boss, ResourceLocation type)',
    new_tick_run
)

helpers = r'''    private static int findRemainingStateStep(InternalVariant variant, int state, int fromStep) {
        int[] states = variant.states();
        for (int i = Math.max(0, fromStep); i < states.length; i++) {
            if (states[i] == state) return i;
        }
        return -1;
    }

    private static void resetMaledictusToIdle(LivingEntity boss, boolean ejectPassengers) {
        invokeBooleanSetter(boss, "setFlying", false);
        invokeIntSetter(boss, "setWeapon", 0);
        writeBooleanField(boss, "grab", false);
        boss.setNoGravity(false);
        if (ejectPassengers) boss.ejectPassengers();
        setAttackState(boss, 0);
    }

'''
marker = '    private static InternalVariant chooseMaledictusNativeVariant(LivingEntity boss, String label) {'
if marker not in s:
    raise SystemExit('Maledictus helper insertion point not found')
s = s.replace(marker, helpers + marker, 1)

action_path.write_text(s)
print('v0.2.5 Maledictus giant-jump + state recovery applied')
