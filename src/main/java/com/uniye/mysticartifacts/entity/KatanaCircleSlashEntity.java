package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.item.impl.KatanaState;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 武士刀开鞘右键的玩家中心三段范围刀光。 */
public class KatanaCircleSlashEntity extends Projectile {
    private static final EntityDataAccessor<Float> ROTATION_OFFSET =
            SynchedEntityData.defineId(KatanaCircleSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_ROLL =
            SynchedEntityData.defineId(KatanaCircleSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASE_SIZE =
            SynchedEntityData.defineId(KatanaCircleSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final int MAX_LIFETIME = KatanaState.CIRCLE_ATTACK_DURATION_TICKS;
    private static final double RADIUS = 4.0D;
    private static final double DAMAGE_MULTIPLIER = 0.75D;

    private ItemStack attackStack = ItemStack.EMPTY;
    private ItemStack stateStack = ItemStack.EMPTY;

    public KatanaCircleSlashEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static KatanaCircleSlashEntity create(Level level, Player player, ItemStack stack) {
        KatanaCircleSlashEntity slash = new KatanaCircleSlashEntity(ModEntities.KATANA_CIRCLE_SLASH.get(), level);
        slash.setOwner(player);
        slash.attackStack = stack.copy();
        slash.stateStack = stack;
        slash.setPos(player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ());
        slash.setYRot(player.getYRot() - 22.5F);
        level.addFreshEntity(slash);
        return slash;
    }

    public float getRotationOffset() {
        return this.entityData.get(ROTATION_OFFSET);
    }

    public float getRotationRoll() {
        return this.entityData.get(ROTATION_ROLL);
    }

    public float getBaseSize() {
        return this.entityData.get(BASE_SIZE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ROTATION_OFFSET, 0.0F);
        this.entityData.define(ROTATION_ROLL, 0.0F);
        this.entityData.define(BASE_SIZE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > MAX_LIFETIME) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (!(owner instanceof Player player) || !player.isAlive()) {
                this.discard();
                return;
            }

            this.setPos(player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ());
            if (this.tickCount == 1 || this.tickCount == 4 || this.tickCount == 7) {
                pulse(player);
                if (this.tickCount == 7) {
                    KatanaState.close(this.stateStack.isEmpty() ? player.getMainHandItem() : this.stateStack);
                }
            }
        }
    }

    private void pulse(Player player) {
        AABB hitBox = player.getBoundingBox().inflate(RADIUS, 2.0D, RADIUS);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                target -> target != player && target.isAlive() && !target.isSpectator() && !target.isAlliedTo(player));
        Set<UUID> hitTargets = new HashSet<>();
        for (LivingEntity target : targets) {
            if (!hitTargets.add(target.getUUID())) {
                continue;
            }
            float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_MULTIPLIER
                    + EnchantmentHelper.getDamageBonus(this.attackStack, target.getMobType()));
            if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                EnchantmentHelper.doPostHurtEffects(target, player);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("AttackStack")) {
            this.attackStack = ItemStack.of(tag.getCompound("AttackStack"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!this.attackStack.isEmpty()) {
            tag.put("AttackStack", this.attackStack.save(new CompoundTag()));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
