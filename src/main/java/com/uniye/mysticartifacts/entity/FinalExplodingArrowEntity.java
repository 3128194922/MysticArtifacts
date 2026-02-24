package com.uniye.mysticartifacts.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class FinalExplodingArrowEntity extends AbstractArrow {
    public FinalExplodingArrowEntity(EntityType<? extends FinalExplodingArrowEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public FinalExplodingArrowEntity(EntityType<? extends FinalExplodingArrowEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explode();
    }

    private void explode() {
        if (!level().isClientSide()) {
            ((ServerLevel) level()).explode(null, this.getX(), this.getY(), this.getZ(), 2.0f, Level.ExplosionInteraction.NONE);
            this.discard();
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }
}
