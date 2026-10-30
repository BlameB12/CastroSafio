package com.castrosafio.abyssor.entity;

import com.castrosafio.abyssor.ModEntities;
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

public class AbyssorEntity extends Monster {
    private static final int PART_COUNT = 7;
    private final int[] partIds = new int[PART_COUNT];
    private int meleeCooldown;

    public AbyssorEntity(EntityType<? extends AbyssorEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1000.0D)
            .add(Attributes.ATTACK_DAMAGE, 12.0D)
            .add(Attributes.ARMOR, 14.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.90D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.FOLLOW_RANGE, 96.0D);
    }

    @Override protected void registerGoals() {}
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(30.0D, 12.0D, 30.0D); }

    public int getPhase() {
        float pct = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        return pct <= 0.30F ? 3 : pct <= 0.65F ? 2 : 1;
    }

    public Vec3 getPartPosition(int index) {
        int i = Mth.clamp(index, 0, PART_COUNT - 1);
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double distance = 4.2D + i * 4.0D;
        double sway = Math.sin(this.tickCount * 0.13D - i * 0.72D) * (0.35D + i * 0.25D);
        double vertical = Math.sin(this.tickCount * 0.10D - i * 0.48D) * 0.42D - i * 0.08D;
        return this.position().subtract(forward.scale(distance)).add(right.scale(sway)).add(0.0D, vertical, 0.0D);
    }

    private void ensureParts(ServerLevel level) {
        for (int i = 0; i < PART_COUNT; i++) {
            Entity existing = partIds[i] == 0 ? null : level.getEntity(partIds[i]);
            if (existing instanceof AbyssorPartEntity p && p.getParentId() == this.getId()) continue;
            AbyssorPartEntity part = ModEntities.ABYSSOR_PART.get().create(level);
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

    @Override public void tick() {
        super.tick();
        this.setNoGravity(true);
        this.setAirSupply(this.getMaxAirSupply());
        this.fallDistance = 0.0F;
        if (meleeCooldown > 0) meleeCooldown--;
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (!this.isAlive()) { discardParts(serverLevel); return; }
            if (this.tickCount % 20 == 1) ensureParts(serverLevel);
            if (!this.isNoAi()) {
                Player target = serverLevel.getNearestPlayer(this, 96.0D);
                if (target != null && !target.isSpectator() && !target.isCreative()) {
                    Vec3 to = target.getEyePosition().subtract(this.position().add(0.0D, 1.5D, 0.0D));
                    double dist = to.length();
                    if (dist > 0.001D) {
                        Vec3 dir = to.normalize();
                        double speed = getPhase() == 3 ? 0.24D : getPhase() == 2 ? 0.19D : 0.15D;
                        Vec3 drift = dir.scale(speed).add(0.0D, Math.sin(this.tickCount * 0.11D) * 0.025D, 0.0D);
                        this.move(MoverType.SELF, drift);
                        float yaw = (float)(Mth.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F;
                        this.setYRot(Mth.rotLerp(0.18F, this.getYRot(), yaw));
                        this.yBodyRot = this.getYRot();
                        this.yHeadRot = this.getYRot();
                    }
                    if (dist < 7.5D && meleeCooldown <= 0) {
                        float damage = getPhase() == 3 ? 18.0F : getPhase() == 2 ? 15.0F : 12.0F;
                        target.hurt(this.damageSources().mobAttack(this), damage);
                        Vec3 push = target.position().subtract(this.position()).normalize().scale(getPhase() == 3 ? 1.6D : 1.0D);
                        target.push(push.x, 0.45D, push.z);
                        meleeCooldown = getPhase() == 3 ? 18 : 26;
                    }
                }
            }
        }
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) discardParts(serverLevel);
        super.remove(reason);
    }
}
