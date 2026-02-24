package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;

public class EnderKunaiEntity extends AbstractArrow {
    private static final EntityDataAccessor<Boolean> IS_GLOWING_KUNAI = SynchedEntityData.defineId(EnderKunaiEntity.class, EntityDataSerializers.BOOLEAN);
    private int groundTimer = 0;

    public EnderKunaiEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public EnderKunaiEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel, LivingEntity pShooter) {
        super(pEntityType, pShooter, pLevel);
        this.pickup = Pickup.ALLOWED;
    }

    @Override
    public ItemStack getPickupItem() {
        return new ItemStack(ModItems.ENDER_KUNAI.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_GLOWING_KUNAI, false);
    }

    public boolean isVisualGlowing() {
        return this.entityData.get(IS_GLOWING_KUNAI);
    }

    public boolean isInGround() {
        return this.inGround;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.inGround) {
            this.groundTimer++;
            
            if (!this.level().isClientSide && !this.entityData.get(IS_GLOWING_KUNAI)) {
                this.entityData.set(IS_GLOWING_KUNAI, true);
                this.setGlowingTag(true);
            }
            
            if (this.groundTimer > 1200) {
                this.discard();
            }
            
            if (this.level().isClientSide && this.groundTimer % 2 == 0) {
                for (int i = 0; i < 2; ++i) {
                    double d0 = this.getX() + (this.random.nextDouble() - 0.5D) * 2.0D;
                    double d1 = this.getY() + this.random.nextDouble() * 2.0D;
                    double d2 = this.getZ() + (this.random.nextDouble() - 0.5D) * 2.0D;
                    this.level().addParticle(ParticleTypes.PORTAL, d0, d1, d2, (this.getX() - d0), (this.getY() - d1), (this.getZ() - d2));
                }
                if (this.groundTimer % 10 == 0) {
                     this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
                }
            }
        } else {
            this.groundTimer = 0;
        }
    }
    
    @Override
    protected void onHitEntity(EntityHitResult pResult) {
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    
    public void setItem(ItemStack stack) {
    }
}
