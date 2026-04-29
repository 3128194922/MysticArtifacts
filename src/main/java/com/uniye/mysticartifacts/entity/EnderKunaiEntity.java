package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.util.EnderKunaiTracker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class EnderKunaiEntity extends AbstractArrow {
    private static final EntityDataAccessor<Boolean> IS_GLOWING_KUNAI = SynchedEntityData.defineId(EnderKunaiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_ITEM = "Item";
    private static final String TAG_OWNER_UUID = "OwnerUUID";
    private int groundTimer = 0;
    private ItemStack pickupItemStack = new ItemStack(ModItems.ENDER_KUNAI.get());
    private UUID ownerUuid;
    private boolean timedOut;
    private boolean removedHandled;

    public EnderKunaiEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public EnderKunaiEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel, LivingEntity pShooter) {
        super(pEntityType, pShooter, pLevel);
        this.pickup = Pickup.ALLOWED;
        this.ownerUuid = pShooter.getUUID();
    }

    @Override
    public ItemStack getPickupItem() {
        return this.pickupItemStack.copy();
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
                this.timedOut = true;
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
        if (!stack.isEmpty()) {
            this.pickupItemStack = stack.copy();
            this.pickupItemStack.setCount(1);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(TAG_ITEM, this.pickupItemStack.save(new CompoundTag()));
        if (this.ownerUuid != null) {
            tag.putUUID(TAG_OWNER_UUID, this.ownerUuid);
        }
        tag.putInt("GroundTimer", this.groundTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_ITEM, 10)) {
            this.pickupItemStack = ItemStack.of(tag.getCompound(TAG_ITEM));
        }
        if (tag.hasUUID(TAG_OWNER_UUID)) {
            this.ownerUuid = tag.getUUID(TAG_OWNER_UUID);
        } else if (this.getOwner() != null) {
            this.ownerUuid = this.getOwner().getUUID();
        }
        this.groundTimer = tag.getInt("GroundTimer");
    }

    @Override
    public void remove(RemovalReason pReason) {
        if (!this.level().isClientSide && !this.removedHandled) {
            this.removedHandled = true;
            ServerPlayer ownerPlayer = this.ownerUuid == null ? null : this.level().getServer().getPlayerList().getPlayer(this.ownerUuid);
            if (ownerPlayer != null) {
                EnderKunaiTracker.removeKunai(ownerPlayer, this.getUUID());
                if (this.timedOut) {
                    ownerPlayer.displayClientMessage(Component.literal("末影苦无消失了"), false);
                }
            }
        }
        super.remove(pReason);
    }
}
