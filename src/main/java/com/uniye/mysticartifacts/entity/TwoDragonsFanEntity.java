package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
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

public class TwoDragonsFanEntity extends Projectile implements IEntityAdditionalSpawnData, ItemSupplier {

    private static final EntityDataAccessor<Boolean> IS_FIRE = SynchedEntityData.defineId(TwoDragonsFanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_RETURNING = SynchedEntityData.defineId(TwoDragonsFanEntity.class, EntityDataSerializers.BOOLEAN);

    private int bounceCount = 0;
    // private static final int MAX_BOUNCES = 6; // Moved to Config
    private static final float DAMAGE = 5.0f;
    private static final float SPEED = 1.0f;
    private static final float RETURN_SPEED = 1.5f;
    private static final int MAX_LIFETIME = 600; 

    private int lastHitEntityId = -1;
    private LivingEntity target;

    public TwoDragonsFanEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public TwoDragonsFanEntity(Level level, LivingEntity owner, boolean isFire) {
        this(ModEntities.TWO_DRAGONS_FAN.get(), level);
        this.setOwner(owner);
        this.setIsFire(isFire);
        this.setPos(owner.getX(), owner.getEyeY() - 0.5, owner.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(IS_FIRE, true);
        this.entityData.define(IS_RETURNING, false);
    }

    public void setIsFire(boolean isFire) {
        this.entityData.set(IS_FIRE, isFire);
    }

    public boolean isFire() {
        return this.entityData.get(IS_FIRE);
    }

    public void setReturning(boolean returning) {
        this.entityData.set(IS_RETURNING, returning);
    }

    public boolean isReturning() {
        return this.entityData.get(IS_RETURNING);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            Vec3 motion = this.getDeltaMovement();
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            return;
        }

        Entity owner = this.getOwner();
        
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        if (this.tickCount > MAX_LIFETIME) {
            setReturning(true);
        }

        if (isReturning()) {
            handleReturnLogic(owner);
        } else {
            handleAttackLogic(owner);
        }

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }
    
    private void handleReturnLogic(Entity owner) {
        Vec3 ownerPos = owner.position().add(0, owner.getBbHeight() * 0.5, 0);
        Vec3 toOwner = ownerPos.subtract(this.position()).normalize().scale(RETURN_SPEED);
        this.setDeltaMovement(toOwner);
        
        double distSqr = this.distanceToSqr(ownerPos);

        if (this.getBoundingBox().intersects(owner.getBoundingBox().inflate(0.5)) || distSqr < 2.0 || this.tickCount > MAX_LIFETIME + 100) {
            restoreToOwner(owner);
            this.discard();
        }
    }

    private void restoreToOwner(Entity owner) {
        if (owner instanceof Player player) {
            if (restoreToItem(player.getMainHandItem())) return;
            if (restoreToItem(player.getOffhandItem())) return;
            for (ItemStack stack : player.getInventory().items) {
                if (restoreToItem(stack)) return;
            }
        }
    }

    private boolean restoreToItem(ItemStack stack) {
        if (stack.getItem() instanceof TwoDragonsPlayBallItem) {
            CompoundTag nbt = stack.getOrCreateTag();
            boolean isFire = this.isFire();
            String key = isFire ? "has_fire" : "has_ice";
            
            if (!nbt.getBoolean(key)) {
                nbt.putBoolean(key, true);
                return true;
            }
        }
        return false;
    }

    private void handleAttackLogic(Entity owner) {
        if (target == null || !target.isAlive() || target.getId() == lastHitEntityId) {
            findNewTarget(owner);
            if (target == null) {
                setReturning(true);
                return;
            }
        }

        if (target != null) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            Vec3 toTarget = targetPos.subtract(this.position()).normalize().scale(SPEED);
            this.setDeltaMovement(toTarget);
            
            if (this.getBoundingBox().intersects(target.getBoundingBox())) {
                hitTarget(target);
            }
        } else {
            setReturning(true);
        }
    }

    private void findNewTarget(Entity owner) {
        AABB searchBox = this.getBoundingBox().inflate(16.0);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> {
            if (e == owner) return false;
            if (e.isAlliedTo(owner)) return false;
            if (e.getId() == lastHitEntityId) return false;
            if (!e.isAlive()) return false;
            if (e instanceof Player && ((Player)e).isCreative()) return false;
            return true;
        });

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double dist = candidate.distanceToSqr(this);
            if (dist < closestDist) {
                closestDist = dist;
                closest = candidate;
            }
        }

        this.target = closest;
    }

    private void hitTarget(LivingEntity hitEntity) {
        DamageSource source = new DamageSource(
            this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(isFire() ? ModDamageTypes.TWO_DRAGONS_PLAY_BALL_FIRE : ModDamageTypes.TWO_DRAGONS_PLAY_BALL_ICE),
            this,
            this.getOwner()
        );

        hitEntity.hurt(source, DAMAGE);
        
        lastHitEntityId = hitEntity.getId();
        target = null;
        
        bounceCount++;
        if (bounceCount >= Config.TwoDragonsMaxBounces) {
            setReturning(true);
        }
        
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.TWO_DRAGONS_PLAY_BALL_SPIN.get(), SoundSource.NEUTRAL, 1.0f, 1.5f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setIsFire(tag.getBoolean("IsFire"));
        this.setReturning(tag.getBoolean("IsReturning"));
        this.bounceCount = tag.getInt("BounceCount");
        this.lastHitEntityId = tag.getInt("LastHitEntityId");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("IsFire", isFire());
        tag.putBoolean("IsReturning", isReturning());
        tag.putInt("BounceCount", bounceCount);
        tag.putInt("LastHitEntityId", lastHitEntityId);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        Entity owner = this.getOwner();
        buffer.writeInt(owner != null ? owner.getId() : -1);
        buffer.writeBoolean(isFire());
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
        setIsFire(additionalData.readBoolean());
    }
}
