from pathlib import Path
import sys

root = Path(sys.argv[1])
p = root / 'src/main/java/com/castrosafio/bosscontroller/control/CataclysmBossMovementBridge.java'
s = p.read_text()
old = '''        // Chain Jump. The original Scylla_Flying Goal performs the large vertical launch at tick 9\n        // (and is manually ticked above). Keep only horizontal homing here so we do not add a\n        // second fake vertical arc on top of Cataclysm's native 1.7+ jump impulse.\n        if (state == 15 && tick >= 9 && tick <= 46) {\n            Vec3 horizontal = horizontalToward(boss, target, 0.20D, 2.0D);\n            if (horizontal.lengthSqr() > 1.0E-8D) {\n                moveDirect(boss, new Vec3(horizontal.x, 0.0D, horizontal.z));\n            }\n        } else if (state == 16) {\n            // Pull/falling section: horizontal velocity from the native Goal would be cleared, so\n            // keep homing directly while gravity/vertical movement remains entity-controlled.\n            Vec3 horizontal = horizontalToward(boss, target, 0.18D, 1.8D);\n            if (horizontal.lengthSqr() > 1.0E-8D) {\n                moveDirect(boss, new Vec3(horizontal.x, 0.0D, horizontal.z));\n            }\n        }'''
new = '''        // Chain Jump: Scylla sets Flying/no-gravity, while BossControl intentionally clears\n        // vertical delta for no-gravity entities. Therefore reproduce the large native jump with\n        // direct displacement, otherwise the original 1.7+ launch is erased before it can move.\n        if (state == 15 && tick >= 9 && tick <= 46) {\n            double t = tick - 9.0D;\n            double heightBonus = target == null ? 0.0D\n                    : Math.max(0.0D, Math.min(0.80D, (target.getY() - boss.getY()) * 0.05D));\n            double dy = (1.35D + heightBonus) - t * 0.090D;\n            Vec3 horizontal = horizontalToward(boss, target, 0.20D, 2.0D);\n            moveDirect(boss, new Vec3(horizontal.x, dy, horizontal.z));\n        } else if (state == 16) {\n            // Pull/falling section: make the landing automatic and target-directed.\n            Vec3 horizontal = horizontalToward(boss, target, 0.18D, 1.8D);\n            moveDirect(boss, new Vec3(horizontal.x, -0.55D, horizontal.z));\n        }'''
if old not in s:
    raise SystemExit('Scylla Chain Jump block not found')
s = s.replace(old, new, 1)
p.write_text(s)
print('v0.2.7b Scylla giant Chain Jump direct physics applied')
