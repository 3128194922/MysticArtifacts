package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class NetherOfVoiceEntity extends AbstractArrow implements ItemSupplier {
    private static final EntityDataAccessor<Integer> ARC_TOWARDS_ENTITY_ID =
            SynchedEntityData.defineId(NetherOfVoiceEntity.class, EntityDataSerializers.INT);
    private boolean stopSeeking;

    public NetherOfVoiceEntity(EntityType<? extends NetherOfVoiceEntity> type, Level level) {
        super(type, level);
    }

    public NetherOfVoiceEntity(EntityType<? extends NetherOfVoiceEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
    }

    public NetherOfVoiceEntity(Level level, LivingEntity shooter) {
        super(ModEntities.NETHER_OF_VOICE.get(), shooter, level);
    }

    public NetherOfVoiceEntity(Level level, double x, double y, double z) {
        super(ModEntities.NETHER_OF_VOICE.get(), x, y, z, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ARC_TOWARDS_ENTITY_ID, -1);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isCritArrow() {
        return false;
    }

    @Override
    public void setCritArrow(boolean critArrow) {
    }


    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide && !this.inGround && !this.stopSeeking && this.tickCount % 3 == 0) {
            double x = this.getX();
            double y = this.getY() + 0.1D;
            double z = this.getZ();
            float pitch = this.random.nextFloat(); 

            this.level().addParticle(
                    ParticleTypes.NOTE,
                    x, y, z,
                    pitch, 0.0D, 0.0D
            );
        }

        if (!this.level().isClientSide && !this.inGround && !this.stopSeeking) {
            if (this.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
                Vec3 lookVec = player.getLookAngle();
                Vec3 currentMotion = this.getDeltaMovement();
                
                double targetSpeed = Config.NetherOfVoiceSpeed;

                Vec3 targetDir = lookVec.normalize();
                Vec3 currentDir = currentMotion.normalize();
                
                double turnRate = Config.NetherOfVoiceTurnRate; 
                Vec3 newDir = currentDir.lerp(targetDir, turnRate).normalize();
                
                this.setDeltaMovement(newDir.scale(targetSpeed));
                this.hasImpulse = true; 
            }
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity entity) {
        this.stopSeeking = true;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (target instanceof LivingEntity living) {
            this.doPostHurtEffects(living);
        }

        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner) {
            target.hurt(this.damageSources().arrow(this, livingOwner), (float)this.getBaseDamage());
        } else {
            target.hurt(this.damageSources().arrow(this, this), (float)this.getBaseDamage());
        }

        this.stopSeeking = true;
    }


    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return null;
    }


    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.NETHER_OF_VOICE.get());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.NETHER_OF_VOICE.get());
    }
}
