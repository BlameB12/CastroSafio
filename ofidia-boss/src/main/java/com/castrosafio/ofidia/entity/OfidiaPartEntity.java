package com.castrosafio.ofidia.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class OfidiaPartEntity extends Entity {
    private static final EntityDataAccessor<Integer> PARENT_ID =
        SynchedEntityData.defineId(OfidiaPartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PART_INDEX =
        SynchedEntityData.defineId(OfidiaPartEntity.class, EntityDataSerializers.INT);

    public OfidiaPartEntity(EntityType<? extends OfidiaPartEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PARENT_ID, 0);
        builder.define(PART_INDEX, 0);
    }

    public void setParent(int parentId, int index) {
        this.entityData.set(PARENT_ID, parentId);
        this.entityData.set(PART_INDEX, index);
    }

    public int getParentId() { return this.entityData.get(PARENT_ID); }
    public int getPartIndex() { return this.entityData.get(PART_INDEX); }

    private OfidiaEntity parent() {
        Entity e = this.level().getEntity(getParentId());
        return e instanceof OfidiaEntity o ? o : null;
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        OfidiaEntity boss = parent();
        if (boss == null || !boss.isAlive()) {
            if (!this.level().isClientSide()) this.discard();
            return;
        }
        Vec3 p = boss.getPartPosition(getPartIndex());
        this.setPos(p.x, p.y, p.z);
        this.setYRot(boss.getYRot());
    }

    @Override public boolean isPickable() { return true; }
    @Override public boolean isAttackable() { return true; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        OfidiaEntity boss = parent();
        return boss != null && boss.hurt(source, amount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(PARENT_ID, tag.getInt("ParentId"));
        this.entityData.set(PART_INDEX, tag.getInt("PartIndex"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ParentId", getParentId());
        tag.putInt("PartIndex", getPartIndex());
    }
}
