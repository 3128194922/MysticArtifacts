package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Comparator;
import java.util.List;

public class SculkArrow extends AbstractArrow {

    private static final int DESPAWN_DELAY = 600;
    private int targetEntityId = -1;
    private int groundTicks = 0;

    public SculkArrow(EntityType<? extends SculkArrow> type, Level level) {
        super(type, level);
    }

    public SculkArrow(EntityType<? extends SculkArrow> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.inGround) {
                this.groundTicks++;
                if (this.groundTicks >= DESPAWN_DELAY) {
                    this.discard();
                }
                return;
            }

            Entity found = findNearbyMovingEntity();
            if (found != null) {
                this.targetEntityId = found.getId();
            }

            if (this.targetEntityId != -1) {
                Entity target = this.level().getEntity(this.targetEntityId);
                if (target == null || !target.isAlive()) {
                    this.targetEntityId = -1;
                } else {
                    Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
                    Vec3 toTarget = targetPos.subtract(this.position()).normalize().scale(0.6);
                    Vec3 blended = this.getDeltaMovement().scale(0.7).add(toTarget.scale(0.3));
                    this.setDeltaMovement(blended);
                    this.hasImpulse = true;

                    if (this.getBoundingBox().intersects(target.getBoundingBox())) {
                        if (target instanceof LivingEntity living && target != this.getOwner()) {
                            this.doPostHurtEffects(living);
                            super.onHit(new EntityHitResult(target));
                        }
                    }
                }
            }
        }
    }

    private Entity findNearbyMovingEntity() {
        Entity owner = this.getOwner();
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(6.0), e -> {
            if (e == owner) return false;
            if (!e.isAlive()) return false;
            if (e instanceof Player p && p.isCreative()) return false;
            if (owner != null && e.isAlliedTo(owner)) return false;
            return e.getDeltaMovement().lengthSqr() > 0.01;
        });

        if (entities.isEmpty()) return null;

        entities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
        return entities.get(0);
    }

    @Override
    protected void onHit(HitResult result) {
        if (result instanceof EntityHitResult entityResult) {
            Entity target = entityResult.getEntity();
            if (target != this.getOwner() && target instanceof LivingEntity) {
                this.targetEntityId = -1;
                super.onHit(result);
                return;
            }
        }
        super.onHit(result);
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        this.targetEntityId = -1;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.targetEntityId = tag.getInt("TargetEntityId");
        this.groundTicks = tag.getInt("GroundTicks");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TargetEntityId", this.targetEntityId);
        tag.putInt("GroundTicks", this.groundTicks);
    }
}
