package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class SlimeArrow extends AbstractArrow {

    private int curBounces = 0;

    public SlimeArrow(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.setBaseDamage(Config.TNTArrowDamage);
    }

    public SlimeArrow(EntityType<? extends AbstractArrow> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
        this.setBaseDamage(Config.TNTArrowDamage);
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.BLOCK) {
            if (curBounces >= Config.TNTArrowBounces || this.isInWater() || this.getDeltaMovement().lengthSqr() < Config.TNTArrowMinVelocity * Config.TNTArrowMinVelocity) {
                super.onHit(result);
                return;
            }

            BlockHitResult blockHit = (BlockHitResult) result;
            Vec3 motion = this.getDeltaMovement();

            switch (blockHit.getDirection()) {
                case UP, DOWN -> this.setDeltaMovement(motion.x, -motion.y, motion.z);
                case NORTH, SOUTH -> this.setDeltaMovement(motion.x, motion.y, -motion.z);
                case EAST, WEST -> this.setDeltaMovement(-motion.x, motion.y, motion.z);
            }

            if (!level().isClientSide) {
                AABB box = new AABB(
                        this.getX() - Config.TNTArrowGlowRadius, this.getY() - Config.TNTArrowGlowRadius, this.getZ() - Config.TNTArrowGlowRadius,
                        this.getX() + Config.TNTArrowGlowRadius, this.getY() + Config.TNTArrowGlowRadius, this.getZ() + Config.TNTArrowGlowRadius
                );
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box)) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200));
                }
                level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            double horiz = motion.horizontalDistance();
            this.setXRot((float) (Mth.atan2(motion.y, horiz) * (180F / Math.PI)));
            this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180F / Math.PI)));

            // 播放幽匿方块的音效
            this.playSound(SoundEvents.SCULK_BLOCK_HIT, 1.0F, 1.0F);

            curBounces++;

        } else {
            super.onHit(result);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }


    @Override
    public void tick() {
        super.tick();
        if (!this.inGround) {
            Vec3 motion = this.getDeltaMovement();
            this.level().addParticle(ParticleTypes.ITEM_SLIME, this.getX(), this.getY(), this.getZ(), -motion.x, -motion.y + 0.2D, -motion.z);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Bounces", curBounces);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.curBounces = tag.getInt("Bounces");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
