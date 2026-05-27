package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class TrackingArrowEntity extends Projectile implements IEntityAdditionalSpawnData, ItemSupplier {

    private static final String DATA_KEY = "mysticartifacts_diviner_stone";
    private static final String ATTACKER_ID_KEY = "attacker_id";
    private static final EntityDataAccessor<Boolean> IS_ATTACKING = SynchedEntityData.defineId(TrackingArrowEntity.class, EntityDataSerializers.BOOLEAN);

    private Entity target;

    public TrackingArrowEntity(EntityType<? extends TrackingArrowEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public TrackingArrowEntity(Level level, LivingEntity owner) {
        super(ModEntities.TRACKING_ARROW.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.33, owner.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(IS_ATTACKING, false);
    }

    private boolean isAttacking() {
        return this.entityData.get(IS_ATTACKING);
    }

    private void setAttacking(boolean value) {
        this.entityData.set(IS_ATTACKING, value);
    }

    @Override
    public void tick() {
        super.tick();

        Entity owner = this.getOwner();

        if (!this.level().isClientSide) {
            if (owner == null || !owner.isAlive() || (owner instanceof Player p && p.isSpectator())) {
                releaseAttackerRole(owner);
                this.discard();
                return;
            }

            if (this.tickCount > Config.DivinerStoneMaxLifetime) {
                releaseAttackerRole(owner);
                this.discard();
                return;
            }

            if (this.isAttacking()) {
                if (this.target != null && this.target.isAlive()) {
                    handleAttackTarget(owner);
                } else {
                    this.setAttacking(false);
                    this.target = null;
                    releaseAttackerRole(owner);
                }
            } else {
                if (this.tickCount % 10 == 0) {
                    this.target = findTarget(owner);
                    if (this.target != null && tryClaimAttackerRole(owner)) {
                        this.setAttacking(true);
                    } else {
                        this.target = null;
                    }
                }
            }
        }

        if (owner != null && !this.isAttacking()) {
            handleOrbit(owner);
        } else if (this.level().isClientSide && this.isAttacking()) {
            Vec3 motion = this.getDeltaMovement();
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        }
    }

    private void handleOrbit(Entity owner) {
        double angle = this.tickCount * Config.DivinerStoneOrbitSpeed;
        double x = owner.getX() + Math.cos(angle) * Config.DivinerStoneOrbitRadius;
        double z = owner.getZ() + Math.sin(angle) * Config.DivinerStoneOrbitRadius;
        double y = owner.getY() + owner.getBbHeight() * 0.33;

        Vec3 newPos = new Vec3(x, y, z);
        Vec3 oldPos = this.position();
        this.setDeltaMovement(newPos.subtract(oldPos));
        this.setPos(x, y, z);
    }

    private Entity findTarget(Entity owner) {
        Entity target = null;

        if (owner instanceof LivingEntity livingOwner) {
            LivingEntity hurtMob = livingOwner.getLastHurtMob();
            if (hurtMob != null && hurtMob.isAlive() && hurtMob != owner) {
                target = hurtMob;
            }

            if (target == null) {
                LivingEntity hurtByMob = livingOwner.getLastHurtByMob();
                if (hurtByMob != null && hurtByMob.isAlive() && hurtByMob != owner) {
                    target = hurtByMob;
                }
            }
        }

        if (target != null && target.distanceTo(owner) > Config.DivinerStoneAttackRange) {
            return null;
        }

        return target;
    }

    private boolean tryClaimAttackerRole(Entity owner) {
        if (!(owner instanceof Player player)) return false;

        CompoundTag data = player.getPersistentData();
        CompoundTag root;
        if (data.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            root = data.getCompound(DATA_KEY);
        } else {
            root = new CompoundTag();
            root.putInt(ATTACKER_ID_KEY, -1);
            data.put(DATA_KEY, root);
        }

        int currentAttackerId = root.getInt(ATTACKER_ID_KEY);
        if (currentAttackerId == -1 || currentAttackerId == this.getId()) {
            root.putInt(ATTACKER_ID_KEY, this.getId());
            return true;
        }

        Entity currentAttacker = this.level().getEntity(currentAttackerId);
        if (currentAttacker == null || !currentAttacker.isAlive()) {
            root.putInt(ATTACKER_ID_KEY, this.getId());
            return true;
        }

        return false;
    }

    private void releaseAttackerRole(Entity owner) {
        if (!(owner instanceof Player player)) return;
        CompoundTag data = player.getPersistentData();
        if (data.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag root = data.getCompound(DATA_KEY);
            if (root.getInt(ATTACKER_ID_KEY) == this.getId()) {
                root.putInt(ATTACKER_ID_KEY, -1);
            }
        }
    }

    private void handleAttackTarget(Entity owner) {
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 toTarget = targetPos.subtract(this.position()).normalize().scale(1.0);
        this.setDeltaMovement(toTarget);
        this.setPos(this.getX() + toTarget.x, this.getY() + toTarget.y, this.getZ() + toTarget.z);

        if (this.getBoundingBox().intersects(target.getBoundingBox())) {
            target.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null),
                    (float) Config.DivinerStoneDamage);
            releaseAttackerRole(owner);
            this.discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide) {
            releaseAttackerRole(this.getOwner());
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.TRACKING_ARROW.get());
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        Entity owner = this.getOwner();
        buffer.writeInt(owner != null ? owner.getId() : -1);
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
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int lerpSteps, boolean teleport) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
