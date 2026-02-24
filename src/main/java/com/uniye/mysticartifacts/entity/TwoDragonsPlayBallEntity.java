package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.init.ModDamageTypes;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModSounds;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.item.impl.TwoDragonsPlayBallItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class TwoDragonsPlayBallEntity extends Projectile implements IEntityAdditionalSpawnData, ItemSupplier {

    private float radius = 1.5f;
    private float spinSpeed = 0.1f;
    private int duration = 800;
    private float damage = 5.0f;
    private float spinOffset = 0f;
    private boolean isFire = true; 

    public TwoDragonsPlayBallEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public TwoDragonsPlayBallEntity(Level level, LivingEntity owner, float damage, int duration, boolean isFire) {
        super(ModEntities.TWO_DRAGONS_PLAY_BALL.get(), level);
        this.setOwner(owner);
        this.damage = damage;
        this.duration = duration;
        this.setIsFire(isFire);
        
        this.setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.33, owner.getZ());
    }
    
    public void setSpinOffset(float offset) {
        this.spinOffset = offset;
    }
    
    public boolean isFire() {
        return this.isFire;
    }

    @Override
    protected void defineSynchedData() {
       this.entityData.define(IS_FIRE, true);
    }
    
    private static final EntityDataAccessor<Boolean> IS_FIRE = SynchedEntityData.defineId(TwoDragonsPlayBallEntity.class, EntityDataSerializers.BOOLEAN);

    public void setIsFire(boolean isFire) {
        this.entityData.set(IS_FIRE, isFire);
    }

    public boolean getIsFire() {
        return this.entityData.get(IS_FIRE);
    }

    @Override
    public void tick() {
        super.tick();

        Entity owner = this.getOwner();

        if (!this.level().isClientSide) {
            if (owner == null || !owner.isAlive() || (owner instanceof Player && ((Player) owner).isSpectator())) {
                this.discard();
                return;
            }

            if (this.tickCount >= this.duration) {
                this.discard();
                return;
            }

            if (this.tickCount % 15 == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.TWO_DRAGONS_PLAY_BALL_SPIN.get(), SoundSource.NEUTRAL, 0.5f, 1.0f);
            }

            this.checkCollision();
        }

        if (owner != null) {
            this.updateMotion(owner);
        }
    }
    
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide) {
             Entity owner = this.getOwner();
             if (owner instanceof Player player) {
                 resetActiveState(player);
             }
        }
    }
    
    private void resetActiveState(Player player) {
        boolean isFire = this.getIsFire();
        String activeKey = isFire ? "active_fire" : "active_ice";
        
        resetItemState(player.getMainHandItem(), activeKey);
        resetItemState(player.getOffhandItem(), activeKey);
        for (ItemStack stack : player.getInventory().items) {
             resetItemState(stack, activeKey);
        }
    }
    
    private void resetItemState(ItemStack stack, String activeKey) {
        if (stack.getItem() instanceof TwoDragonsPlayBallItem) {
            CompoundTag nbt = stack.getOrCreateTag();
            if (nbt.contains(activeKey)) {
                nbt.putBoolean(activeKey, false);
            }
        }
    }

    private void updateMotion(Entity owner) {
        Vec3 center = owner.position().add(0.0, owner.getBbHeight() * 0.33, 0.0);
        
        float currentAngle = this.tickCount * this.spinSpeed + this.spinOffset;
        
        double x = center.x + Math.cos(currentAngle) * radius;
        double z = center.z + Math.sin(currentAngle) * radius;
        double y = center.y;

        Vec3 newPos = new Vec3(x, y, z);
        Vec3 oldPos = this.position();
        this.setDeltaMovement(newPos.subtract(oldPos));

        this.setPos(x, y, z);
    }

    private void checkCollision() {
        AABB aabb = this.getBoundingBox().inflate(0.5);
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, aabb);
        
        for (LivingEntity target : list) {
            if (target != this.getOwner() && !target.isAlliedTo(this.getOwner())) {
                DamageSource source = new DamageSource(
                    this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(getIsFire() ? ModDamageTypes.TWO_DRAGONS_PLAY_BALL_FIRE : ModDamageTypes.TWO_DRAGONS_PLAY_BALL_ICE),
                    this,
                    this.getOwner()
                );

                target.hurt(source, this.damage);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.damage = tag.getFloat("Damage");
        this.duration = tag.getInt("Duration");
        if (tag.contains("SpinOffset")) {
            this.spinOffset = tag.getFloat("SpinOffset");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", this.damage);
        tag.putInt("Duration", this.duration);
        tag.putFloat("SpinOffset", this.spinOffset);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int lerpSteps, boolean teleport) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        Entity owner = this.getOwner();
        buffer.writeInt(owner != null ? owner.getId() : -1);
        buffer.writeFloat(this.spinOffset);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.TWO_DRAGONS_PLAY_BALL.get());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        int ownerId = additionalData.readInt();
        if (ownerId != -1) {
            Entity owner = this.level().getEntity(ownerId);
            if (owner != null) {
                this.setOwner(owner);
            }
        }
        this.spinOffset = additionalData.readFloat();
    }
}
