package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AirburstArrowEntity extends AbstractArrow {

    private int tickCount = 0;
    private Vec3 shooterPos = null;

    public AirburstArrowEntity(EntityType<? extends AirburstArrowEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public AirburstArrowEntity(EntityType<? extends AirburstArrowEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
        this.pickup = Pickup.DISALLOWED;
        this.shooterPos = shooter.position();
    }

    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult result) {
        super.onHitEntity(result);
        explode();
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
        super.onHitBlock(result);
        explode();
    }

    @Override
    public void tick() {
        if (!this.isAlive() || this.level() == null) {
            return;
        }
        
        try {
            super.tick();
            
            tickCount++;
            
            if (shooterPos == null && this.getOwner() != null) {
                shooterPos = this.getOwner().position();
            }
            
            double r = Config.AirBurstProximityRadius;
            AABB box = new AABB(
                    this.getX() - r, this.getY() - r, this.getZ() - r,
                    this.getX() + r, this.getY() + r, this.getZ() + r
            );
            Entity owner = this.getOwner();
            boolean hasTarget = !this.level().getEntitiesOfClass(LivingEntity.class, box, e -> {
                if (e == owner) return false;
                return e.isAlive();
            }).isEmpty();
            if (hasTarget) explode();
        } catch (Exception e) {
            this.discard();
        }
    }

    private void explode() {
        if (level().isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) this.level();

        int count = Config.AirBurstNumber + this.random.nextInt(Config.AirBurstNumberRandom);

        Entity ownerEntity = this.getOwner();
        LivingEntity owner = null;
        if (ownerEntity instanceof LivingEntity living) {
            owner = living;
        }

        for (int i = 0; i < count; i++) {
            double theta = random.nextDouble() * 2 * Math.PI; 
            double phi = Math.acos(2 * random.nextDouble() - 1); 

            double dx = Math.sin(phi) * Math.cos(theta)* 0.3;
            double dy = Math.cos(phi) * 0.3; 
            double dz = Math.sin(phi) * Math.sin(theta)* 0.3;

            Vec3 finalVec = new Vec3(dx, dy, dz).scale(1.5); 

            ExplodingArrowEntity childArrow;
            if (owner != null) {
                childArrow = new ExplodingArrowEntity(ModEntities.EXPLODING_ARROW.get(), serverLevel, owner);
            } else {
                childArrow = new ExplodingArrowEntity(ModEntities.EXPLODING_ARROW.get(), serverLevel);
            }
            
            childArrow.setPos(this.getX(), this.getY(), this.getZ());
            childArrow.setDeltaMovement(finalVec);
            serverLevel.addFreshEntity(childArrow);
        }

        this.discard();
    }


    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("tickCount", tickCount);
        if (shooterPos != null) {
            tag.putDouble("shooterX", shooterPos.x);
            tag.putDouble("shooterY", shooterPos.y);
            tag.putDouble("shooterZ", shooterPos.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tickCount = tag.getInt("tickCount");
        if (tag.contains("shooterX")) {
            shooterPos = new Vec3(tag.getDouble("shooterX"), tag.getDouble("shooterY"), tag.getDouble("shooterZ"));
        }
    }
}
