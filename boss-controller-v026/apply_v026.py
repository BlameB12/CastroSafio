from pathlib import Path
import sys

root = Path(sys.argv[1])
move_path = root / 'src/main/java/com/castrosafio/bosscontroller/control/CataclysmBossMovementBridge.java'
s = move_path.read_text()

# Shoot must fire even if auto-target selection finds nothing. Homing target is optional.
s = s.replace('''        if (state == 2 && tick == 29 && target != null) {\n            fireMaledictusPhantomVolley(boss, target, 6.0D, 1.8F);\n        }\n        if (state == 3 && tick == 50 && target != null) {\n            fireMaledictusPhantomVolley(boss, target, 9.0D, 1.5F);\n        }''', '''        if (state == 2 && tick == 29) {\n            fireMaledictusPhantomVolley(boss, target, 6.0D, 1.8F);\n        }\n        if (state == 3 && tick == 50) {\n            fireMaledictusPhantomVolley(boss, target, 9.0D, 1.5F);\n        }''')


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

new_fire = r'''    private static void fireMaledictusPhantomVolley(LivingEntity boss, LivingEntity target,
                                                     double spreadDegrees, float speed) {
        if (boss.level().isClientSide) return;
        try {
            Class<?> arrowClass = Class.forName(
                    "com.github.L_Ender.cataclysm.entity.projectile.Phantom_Arrow_Entity");

            // Resolve Cataclysm's registered EntityType as a constructor fallback. This makes the
            // bridge survive constructor signature changes between Cataclysm/Minecraft versions.
            Object phantomType = BuiltInRegistries.ENTITY_TYPE.get(
                    ResourceLocation.fromNamespaceAndPath("cataclysm", "phantom_arrow"));

            Method setBaseDamage = findMethod(arrowClass, "setBaseDamage", 1);
            Method shoot = findMethod(arrowClass, "shoot", 5);
            if (shoot == null) {
                CastroSafioBossController.LOGGER.warn("Maledictus Shoot: Phantom Arrow shoot() no encontrado");
                return;
            }

            double baseDamage = readMaledictusPhantomArrowDamage();
            int rage = invokeIntGetter(boss, "getRageMeter", 0);
            double finalDamage = baseDamage + baseDamage * rage * 0.05D;

            // Native homing shot when a target exists; otherwise use the boss look vector so R
            // still visibly fires the four arrows while testing alone.
            Vec3 aim;
            if (target != null) {
                aim = new Vec3(
                        target.getX() - boss.getX(),
                        target.getY(0.3333333333333333D) - boss.getEyeY(),
                        target.getZ() - boss.getZ());
            } else {
                aim = boss.getLookAngle().normalize().scale(20.0D);
            }
            if (aim.lengthSqr() < 1.0E-6D) aim = new Vec3(0.0D, 0.0D, 1.0D);

            double offsetAngle = Math.toRadians(spreadDegrees);
            int spawned = 0;
            for (int i = 0; i < 4; i++) {
                double angle = (i - 1.5D) * offsetAngle;
                double x = aim.x * Math.cos(angle) + aim.z * Math.sin(angle);
                double z = -aim.x * Math.sin(angle) + aim.z * Math.cos(angle);
                double distance = Math.sqrt(x * x + z * z);
                double y = aim.y + distance * 0.15D;

                Object arrow = createMaledictusPhantomArrow(arrowClass, phantomType, boss, target);
                if (!(arrow instanceof net.minecraft.world.entity.Entity entity)) {
                    CastroSafioBossController.LOGGER.warn("Maledictus Shoot: no se pudo construir Phantom Arrow #{}", i + 1);
                    continue;
                }

                // Fallback-created arrows need their native fields initialized explicitly.
                Method setOwner = findCompatibleMethod(arrowClass, "setOwner", boss);
                if (setOwner != null) setOwner.invoke(arrow, boss);
                Field finalTargetField = findField(arrowClass, "finalTarget");
                if (finalTargetField != null) finalTargetField.set(arrow, target);
                entity.setPos(boss.getX(), boss.getEyeY() - 0.1D, boss.getZ());

                if (setBaseDamage != null) setBaseDamage.invoke(arrow, finalDamage);
                shoot.invoke(arrow, x, y, z, speed, 1.0F);
                if (boss.level().addFreshEntity(entity)) spawned++;
            }

            if (spawned > 0) {
                boss.playSound(net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT, 1.0F,
                        1.0F / (boss.getRandom().nextFloat() * 0.4F + 0.8F));
            } else {
                CastroSafioBossController.LOGGER.warn(
                        "Maledictus Shoot: se intentaron 4 Phantom Arrows pero ninguna se agrego al mundo");
            }
        } catch (Throwable exception) {
            // Do not silently swallow linkage/signature changes: leave a useful log and keep the
            // boss controller alive instead of crashing the server.
            CastroSafioBossController.LOGGER.warn(
                    "No se pudieron disparar las Phantom Arrows de Maledictus", exception);
        }
    }
'''
s = replace_method(s, '    private static void fireMaledictusPhantomVolley(LivingEntity boss, LivingEntity target,', new_fire)

helper = r'''    private static Object createMaledictusPhantomArrow(Class<?> arrowClass, Object phantomType,
                                                            LivingEntity boss, LivingEntity target)
            throws ReflectiveOperationException {
        // Known Cataclysm constructor: (Level, LivingEntity shooter, LivingEntity target).
        for (java.lang.reflect.Constructor<?> constructor : arrowClass.getConstructors()) {
            Class<?>[] p = constructor.getParameterTypes();
            if (p.length == 3
                    && p[0].isInstance(boss.level())
                    && p[1].isInstance(boss)
                    && (target == null || p[2].isInstance(target))) {
                return constructor.newInstance(boss.level(), boss, target);
            }
        }

        // Version fallback: (EntityType, Level).
        for (java.lang.reflect.Constructor<?> constructor : arrowClass.getConstructors()) {
            Class<?>[] p = constructor.getParameterTypes();
            if (p.length == 2 && phantomType != null
                    && p[0].isInstance(phantomType) && p[1].isInstance(boss.level())) {
                return constructor.newInstance(phantomType, boss.level());
            }
        }

        // Older/alternate fallback: (EntityType, x, y, z, Level).
        for (java.lang.reflect.Constructor<?> constructor : arrowClass.getConstructors()) {
            Class<?>[] p = constructor.getParameterTypes();
            if (p.length == 5 && phantomType != null
                    && p[0].isInstance(phantomType) && p[4].isInstance(boss.level())) {
                return constructor.newInstance(phantomType, boss.getX(), boss.getEyeY() - 0.1D,
                        boss.getZ(), boss.level());
            }
        }

        throw new NoSuchMethodException("No compatible Phantom_Arrow_Entity constructor");
    }

'''
marker = '    private static double readMaledictusPhantomArrowDamage() {'
if marker not in s:
    raise SystemExit('Phantom Arrow config helper marker not found')
s = s.replace(marker, helper + marker, 1)

move_path.write_text(s)
print('v0.2.6 robust Maledictus Phantom Arrow spawning applied')
