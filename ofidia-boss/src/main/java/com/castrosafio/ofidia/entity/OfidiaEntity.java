package com.castrosafio.ofidia.entity;

import com.castrosafio.ofidia.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OfidiaEntity extends Monster {
    private static final int PART_COUNT = 7;
    private final int[] partIds = new int[PART_COUNT];
    private int slashCooldown;
    private int songCooldown = 100;

    public OfidiaEntity(EntityType<? extends OfidiaEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1200.0D)
            .add(Attributes.ATTACK_DAMAGE, 14.0D)
            .add(Attributes.ARMOR, 16.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.94D)
            .add(Attributes.MOVEMENT_SPEED, 0.26D)
            .add(Attributes.FOLLOW_RANGE, 96.0D);
    }

    @Override protected void registerGoals() {}
    @Override public boolean isPushedByFluid() { return false; }
    @Override protected boolean removeWhenFarAway(double distance) { return false; }
    @Override public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(18.0D, 14.0D, 18.0D); }

    public int getPhase() {
        float pct = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        return pct <= 0.35F ? 3 : pct <= 0.70F ? 2 : 1;
    }

    public Vec3 getPartPosition(int index) {
        int i = Mth.clamp(index, 0, PART_COUNT - 1);
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double sway = Math.sin(this.tickCount * 0.10D) * 0.22D;

        return switch (i) {
            case 0 -> this.position().add(0.0D, 6.2D, 0.0D).add(right.scale(sway));
            case 1 -> this.position().add(0.0D, 4.1D, 0.0D);
            case 2 -> this.position().add(0.0D, 4.0D, 0.0D).add(right.scale(3.0D)).add(forward.scale(0.4D));
            case 3 -> this.position().add(0.0D, 4.0D, 0.0D).subtract(right.scale(3.0D)).add(forward.scale(0.4D));
            case 4 -> this.position().add(0.0D, 2.2D, 0.0D).subtract(forward.scale(2.0D));
            case 5 -> this.position().add(0.0D, 1.2D, 0.0D).subtract(forward.scale(4.0D)).add(right.scale(Math.sin(this.tickCount * 0.12D) * 0.8D));
            default -> this.position().add(0.0D, 0.7D, 0.0D).subtract(forward.scale(6.0D)).add(right.scale(Math.sin(this.tickCount * 0.12D - 0.8D) * 1.4D));
        };
    }

    private void ensureParts(ServerLevel level) {
        for (int i = 0; i < PART_COUNT; i++) {
            Entity existing = partIds[i] == 0 ? null : level.getEntity(partIds[i]);
            if (existing instanceof OfidiaPartEntity p && p.getParentId() == this.getId()) continue;
            OfidiaPartEntity part = ModEntities.OFIDIA_PART.get().create(level);
            if (part != null) {
                part.setParent(this.getId(), i);
                Vec3 p = getPartPosition(i);
                part.moveTo(p.x, p.y, p.z, this.getYRot(), 0.0F);
                level.addFreshEntity(part);
                partIds[i] = part.getId();
            }
        }
    }

    private void discardParts(ServerLevel level) {
        for (int id : partIds) {
            if (id != 0) {
                Entity e = level.getEntity(id);
                if (e != null) e.discard();
            }
        }
    }

    private void sing(ServerLevel level) {
        int phase = getPhase();
        double radius = phase == 3 ? 20.0D : phase == 2 ? 17.0D : 14.0D;
        float damage = phase == 3 ? 8.0F : phase == 2 ? 6.0F : 4.0F;
        Vec3 center = this.position().add(0.0D, 4.3D, 0.0D);

        level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z,
            phase == 3 ? 180 : 110, 4.2D, 2.2D, 4.2D, 0.18D);
        level.sendParticles(ParticleTypes.WITCH, center.x, center.y + 0.8D, center.z,
            phase == 3 ? 80 : 45, 3.2D, 1.5D, 3.2D, 0.08D);

        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(radius),
            p -> !p.isSpectator() && !p.isCreative())) {
            double d = player.distanceTo(this);
            if (d <= radius) {
                player.hurt(this.damageSources().mobAttack(this), damage);
                Vec3 push = player.position().subtract(this.position());
                if (push.lengthSqr() > 0.001D) {
                    push = push.normalize().scale(phase == 3 ? 1.45D : 0.95D);
                    player.push(push.x, 0.35D + phase * 0.08D, push.z);
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        this.setAirSupply(this.getMaxAirSupply());
        this.fallDistance = 0.0F;
        if (slashCooldown > 0) slashCooldown--;
        if (songCooldown > 0) songCooldown--;

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (!this.isAlive()) {
                discardParts(serverLevel);
                return;
            }

            if (this.tickCount % 20 == 1) ensureParts(serverLevel);

            if (!this.isNoAi()) {
                Player target = serverLevel.getNearestPlayer(this, 96.0D);
                if (target != null && !target.isSpectator() && !target.isCreative()) {
                    Vec3 targetPoint = target.getEyePosition().add(0.0D, -1.0D, 0.0D);
                    Vec3 origin = this.position().add(0.0D, 3.8D, 0.0D);
                    Vec3 to = targetPoint.subtract(origin);
                    double dist = to.length();

                    if (dist > 0.001D) {
                        Vec3 dir = to.normalize();
                        double speed = getPhase() == 3 ? 0.22D : getPhase() == 2 ? 0.18D : 0.145D;
                        if (dist > 8.5D) {
                            Vec3 drift = dir.scale(speed)
                                .add(0.0D, Math.sin(this.tickCount * 0.085D) * 0.022D, 0.0D);
                            this.move(MoverType.SELF, drift);
                        } else {
                            this.move(MoverType.SELF, new Vec3(0.0D, Math.sin(this.tickCount * 0.08D) * 0.012D, 0.0D));
                        }

                        float yaw = (float)(Mth.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F;
                        this.setYRot(Mth.rotLerp(0.15F, this.getYRot(), yaw));
                        this.yBodyRot = this.getYRot();
                        this.yHeadRot = this.getYRot();
                    }

                    if (dist < 9.0D && slashCooldown <= 0) {
                        float damage = getPhase() == 3 ? 20.0F : getPhase() == 2 ? 17.0F : 14.0F;
                        target.hurt(this.damageSources().mobAttack(this), damage);
                        Vec3 push = target.position().subtract(this.position());
                        if (push.lengthSqr() > 0.001D) {
                            push = push.normalize().scale(getPhase() == 3 ? 1.5D : 1.05D);
                            target.push(push.x, 0.45D, push.z);
                        }
                        slashCooldown = getPhase() == 3 ? 18 : getPhase() == 2 ? 24 : 30;
                    }

                    if (songCooldown <= 0) {
                        sing(serverLevel);
                        songCooldown = getPhase() == 3 ? 55 : getPhase() == 2 ? 75 : 105;
                    }
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            discardParts(serverLevel);
        }
        super.remove(reason);
    }
}
