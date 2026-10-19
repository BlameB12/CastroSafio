from pathlib import Path
import sys

root = Path(sys.argv[1])
move_path = root / 'src/main/java/com/castrosafio/bosscontroller/control/CataclysmBossMovementBridge.java'
s = move_path.read_text()

needle = '''        int state = getAttackState(boss);\n        int tick = getAttackTicks(boss);\n\n        // IMPORTANT: Cataclysm's Maledictus movement Goals use ONE impulse at an exact tick.'''
insert = '''        int state = getAttackState(boss);\n        int tick = getAttackTicks(boss);\n\n        // Maledictus' Phantom Arrow creation lives inside its AI Goals. With NoAI the animation\n        // still plays but no projectile is spawned, so recreate only the original projectile tick.\n        // Ground Shoot: tick 29, 4 arrows, 6 degree spread, speed 1.8.\n        if (state == 2 && tick == 29 && target != null) {\n            fireMaledictusPhantomVolley(boss, target, 6.0D, 1.8F);\n        }\n        // Flying Shoot: tick 50, 4 arrows, 9 degree spread, speed 1.5.\n        if (state == 3 && tick == 50 && target != null) {\n            fireMaledictusPhantomVolley(boss, target, 9.0D, 1.5F);\n        }\n\n        // IMPORTANT: Cataclysm's Maledictus movement Goals use ONE impulse at an exact tick.'''
if needle not in s:
    raise SystemExit('Maledictus tick insertion point not found')
s = s.replace(needle, insert, 1)

marker = '    // -------------------- Lionfish animation bosses --------------------\n'
helpers = r'''    /**
     * Mirrors Maledictus_Bow / Maledictus_Flying_Bow without a compile-time Cataclysm
     * dependency. The projectile is the real Cataclysm Phantom_Arrow_Entity, including its
     * native homing behavior. Reflection keeps Boss Controller optional when Cataclysm is absent.
     */
    private static void fireMaledictusPhantomVolley(LivingEntity boss, LivingEntity target,
                                                     double spreadDegrees, float speed) {
        if (target == null || boss.level().isClientSide) return;
        try {
            Class<?> arrowClass = Class.forName(
                    "com.github.L_Ender.cataclysm.entity.projectile.Phantom_Arrow_Entity");
            java.lang.reflect.Constructor<?> constructor = arrowClass.getConstructor(
                    net.minecraft.world.level.Level.class, LivingEntity.class, LivingEntity.class);
            Method setBaseDamage = findMethod(arrowClass, "setBaseDamage", 1);
            Method shoot = findMethod(arrowClass, "shoot", 5);
            if (shoot == null) {
                CastroSafioBossController.LOGGER.warn("No se encontro Phantom Arrow shoot() para Maledictus");
                return;
            }

            double baseDamage = readMaledictusPhantomArrowDamage();
            int rage = invokeIntGetter(boss, "getRageMeter", 0);
            double finalDamage = baseDamage + baseDamage * rage * 0.05D;

            double d1 = target.getX() - boss.getX();
            double d2 = target.getY(0.3333333333333333D) - boss.getY();
            double d3 = target.getZ() - boss.getZ();
            double offsetAngle = Math.toRadians(spreadDegrees);

            for (int i = 0; i < 4; i++) {
                double angle = (i - 1.5D) * offsetAngle;
                double x = d1 * Math.cos(angle) + d3 * Math.sin(angle);
                double z = -d1 * Math.sin(angle) + d3 * Math.cos(angle);
                double distance = Math.sqrt(x * x + z * z);

                Object arrow = constructor.newInstance(boss.level(), boss, target);
                if (setBaseDamage != null) setBaseDamage.invoke(arrow, finalDamage);
                shoot.invoke(arrow, x, d2 + distance * 0.15D, z, speed, 1.0F);

                if (arrow instanceof net.minecraft.world.entity.Entity entity) {
                    boss.level().addFreshEntity(entity);
                }
                boss.playSound(net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT, 1.0F,
                        1.0F / (boss.getRandom().nextFloat() * 0.4F + 0.8F));
            }
        } catch (ReflectiveOperationException exception) {
            CastroSafioBossController.LOGGER.warn(
                    "No se pudieron disparar las Phantom Arrows de Maledictus", exception);
        }
    }

    private static double readMaledictusPhantomArrowDamage() {
        try {
            Class<?> configClass = Class.forName(
                    "com.github.L_Ender.cataclysm.config.CMCommonConfig$Maledictus");
            Field field = configClass.getDeclaredField("PhantomArrowDamage");
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof Number number) return number.doubleValue();
        } catch (ReflectiveOperationException ignored) {
        }
        // Cataclysm's default value. Only used if the config field was renamed in a build.
        return 5.0D;
    }

'''
if marker not in s:
    raise SystemExit('Lionfish marker not found')
s = s.replace(marker, helpers + marker, 1)
move_path.write_text(s)
print('v0.2.4 Maledictus Phantom Arrow bridge applied')
