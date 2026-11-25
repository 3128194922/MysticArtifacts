package com.uniye.mysticartifacts.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 延迟爆炸箭：命中生物立刻爆炸，命中方块后引信 40 tick 再爆炸。
 * 移植自 KubeJS 的 rainbow:tnt_arrow（EntityJS arrow）。
 */
public class TntArrowEntity extends AbstractArrow {

    public static final int BLOCK_FUSE_TICKS = 40;
    public static final float EXPLOSION_STRENGTH = 3.0F;

    private int fuse = -1;

    public TntArrowEntity(EntityType<? extends TntArrowEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
        this.setBaseDamage(0.5D);
        this.setKnockback(2);
    }

    public TntArrowEntity(EntityType<? extends TntArrowEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
        this.pickup = Pickup.DISALLOWED;
        this.setBaseDamage(0.5D);
        this.setKnockback(2);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide()) {
            return;
        }
        if (this.fuse < 0) {
            this.fuse = BLOCK_FUSE_TICKS;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved() || this.level().isClientSide()) {
            return;
        }
        if (this.fuse > 0) {
            this.fuse--;
            if (this.fuse == 0) {
                explode();
            }
        }
    }

    private void explode() {
        if (this.level().isClientSide() || this.isRemoved()) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            this.discard();
            return;
        }

        serverLevel.explode(this, this.getX(), this.getY() - 1.0D, this.getZ(),
                EXPLOSION_STRENGTH, Level.ExplosionInteraction.NONE);
        this.discard();
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TntFuse", this.fuse);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TntFuse")) {
            this.fuse = tag.getInt("TntFuse");
        }
    }
}
