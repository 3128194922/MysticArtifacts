package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.init.ModDamageTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;

public class SwordPhantomEntity extends AbstractArrow implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> DISPLAY_ITEM = SynchedEntityData.defineId(SwordPhantomEntity.class, EntityDataSerializers.ITEM_STACK);

    private ItemStack visualItem = ItemStack.EMPTY;

    public SwordPhantomEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
        this.setSoundEvent(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP);
    }

    public SwordPhantomEntity(EntityType<? extends AbstractArrow> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
        this.pickup = Pickup.DISALLOWED;
        this.setSoundEvent(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DISPLAY_ITEM, ItemStack.EMPTY);
    }

    public void setVisualItem(ItemStack stack) {
        this.visualItem = stack.copy();
        this.visualItem.setCount(1);
        this.entityData.set(DISPLAY_ITEM, this.visualItem);
    }

    @Override
    public ItemStack getPickupItem() {
        return this.visualItem.copy();
    }
    
    public ItemStack getDisplayItem() {
        return this.entityData.get(DISPLAY_ITEM);
    }

    public void setBaseDamage(int dmg) {
        super.setBaseDamage((double)Math.max(1, dmg));
    }

    @Override
    public ItemStack getItem() {
        return this.getDisplayItem();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        int i = net.minecraft.util.Mth.ceil(this.getBaseDamage());
        if (this.isCritArrow()) {
            long j = (long)this.random.nextInt(i / 2 + 2);
            i = (int)Math.min(j + (long)i, 2147483647L);
        }

        Entity owner = this.getOwner();
        DamageSource damagesource = ModDamageTypes.getSource(this.level(), ModDamageTypes.PHANTOM_SWORD, this, owner);

        if (target.hurt(damagesource, (float)i)) {
            this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
            this.discard();
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.1D));
            this.setYRot(this.getYRot() + 180.0F);
            this.yRotO += 180.0F;
            if (!this.level().isClientSide && this.getDeltaMovement().lengthSqr() < 1.0E-7D) {
                if (this.pickup == AbstractArrow.Pickup.ALLOWED) {
                    this.spawnAtLocation(this.getPickupItem(), 0.1F);
                }
                this.discard();
            }
        }
    }
    
    @Override
    protected net.minecraft.sounds.SoundEvent getDefaultHitGroundSoundEvent() {
        return net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
