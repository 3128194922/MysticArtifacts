package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class TrackingArrowEntity extends AbstractArrow {
    private static final EntityDataAccessor<Integer> TARGET_ENTITY_ID =
            SynchedEntityData.defineId(TrackingArrowEntity.class, EntityDataSerializers.INT);
    private boolean stopSeeking;

    public TrackingArrowEntity(EntityType<? extends TrackingArrowEntity> type, Level level) {
        super(type, level);
    }

    public TrackingArrowEntity(EntityType<? extends TrackingArrowEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_ENTITY_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        int id = this.getTargetEntityId();

        if (!this.inGround && !this.stopSeeking) {
            if (id == -1) {
                if (!this.level().isClientSide) {
                    Entity closest = null;
                    Entity owner = this.getOwner();
                    float searchRadius = Math.min(10.0F, 3.0F + (this.tickCount / 4.0F));

                    for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(searchRadius), this::canHitEntity)) {
                        if ((closest == null || entity.distanceTo(this) < closest.distanceTo(this))
                                && !this.ownedBy(entity)
                                && (owner == null || !entity.isAlliedTo(owner))) {
                            closest = entity;
                        }
                    }

                    if (closest != null) {
                        this.setTargetEntityId(closest.getId());
                    }
                }
            } else {
                Entity target = this.level().getEntity(id);
                if (target != null) {
                    Vec3 toTarget = target.position().add(0.0D, 0.65F * target.getBbHeight(), 0.0D).subtract(this.position());
                    if (toTarget.length() > target.getBbWidth()) {
                        this.setDeltaMovement(this.getDeltaMovement().scale(0.3F).add(toTarget.normalize().scale(0.7F)));
                    }
                }
            }
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity entity) {
        this.stopSeeking = true;
        super.doPostHurtEffects(entity);
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.TRACKING_ARROW.get());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private int getTargetEntityId() {
        return this.entityData.get(TARGET_ENTITY_ID);
    }

    private void setTargetEntityId(int id) {
        this.entityData.set(TARGET_ENTITY_ID, id);
    }
}
