package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ExplodingArrowEntity extends AbstractArrow {

    public ExplodingArrowEntity(EntityType<? extends ExplodingArrowEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public ExplodingArrowEntity(EntityType<? extends ExplodingArrowEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
        this.pickup = Pickup.DISALLOWED;
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

    private void explode() {
        if (level().isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) this.level();

        serverLevel.explode(null, this.getX(), this.getY(), this.getZ(), 2.0f, Level.ExplosionInteraction.NONE);

        int count = Config.AirBurstNumber2 + this.random.nextInt(Config.AirBurstNumber2Random + 1);
        Entity ownerEntity = this.getOwner();
        LivingEntity owner = null;
        if (ownerEntity instanceof LivingEntity living) owner = living;

        for (int i = 1; i <= count; i++) {
            Vec3 randomDir = ScatterArrowDirection.create(this.random, i, count);

            FinalExplodingArrowEntity smallArrow;
            if (owner != null) {
                smallArrow = new FinalExplodingArrowEntity(ModEntities.FINAL_EXPLODING_ARROW.get(), serverLevel, owner);
            } else {
                smallArrow = new FinalExplodingArrowEntity(ModEntities.FINAL_EXPLODING_ARROW.get(), serverLevel);
            }
            smallArrow.setPos(this.getX(), this.getY(), this.getZ());
            smallArrow.setDeltaMovement(randomDir);

            serverLevel.addFreshEntity(smallArrow);
        }

        this.discard();
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }
}
