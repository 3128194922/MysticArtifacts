package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** MysticArtifacts 自己的刀光实体；伤害源始终是持刀玩家。 */
public class KatanaSlashEntity extends Projectile {
    public static final int STYLE_DASH = 0;
    public static final int STYLE_OPEN_SLASH = 1;

    private static final EntityDataAccessor<Integer> STYLE =
            SynchedEntityData.defineId(KatanaSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ROTATION_OFFSET =
            SynchedEntityData.defineId(KatanaSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_ROLL =
            SynchedEntityData.defineId(KatanaSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASE_SIZE =
            SynchedEntityData.defineId(KatanaSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final int MAX_LIFETIME = 10;

    private ItemStack attackStack = ItemStack.EMPTY;
    private final Set<UUID> hitTargets = new HashSet<>();

    public KatanaSlashEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static KatanaSlashEntity createDash(Level level, Player player, ItemStack stack, Vec3 dashVector) {
        KatanaSlashEntity slash = new KatanaSlashEntity(ModEntities.KATANA_SLASH.get(), level);
        slash.setOwner(player);
        slash.setStyle(STYLE_DASH);
        slash.attackStack = stack.copy();
        slash.setPos(player.getX(), player.getY() + player.getBbHeight() * 0.55D, player.getZ());
        slash.setYRot(player.getYRot());
        slash.setXRot(player.getXRot());
        slash.setDeltaMovement(dashVector);
        level.addFreshEntity(slash);
        slash.damageDashTargets(player, dashVector);
        return slash;
    }

    public static KatanaSlashEntity createOpenSlash(Level level, Player player, ItemStack stack) {
        KatanaSlashEntity slash = new KatanaSlashEntity(ModEntities.KATANA_SLASH.get(), level);
        slash.setOwner(player);
        slash.setStyle(STYLE_OPEN_SLASH);
        slash.attackStack = stack.copy();
        Vec3 look = player.getLookAngle();
        slash.setPos(player.getX() + look.x * 1.5D, player.getEyeY() - 0.5D + look.y * 1.5D,
                player.getZ() + look.z * 1.5D);
        slash.setYRot(player.getYRot());
        slash.setXRot(player.getXRot());
        level.addFreshEntity(slash);
        return slash;
    }

    public void setStyle(int style) {
        this.entityData.set(STYLE, style);
    }

    public int getStyle() {
        return this.entityData.get(STYLE);
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
        this.entityData.define(STYLE, STYLE_DASH);
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

            if (getStyle() == STYLE_OPEN_SLASH && this.tickCount == 1) {
                damageOpenTargets(player);
            }
        }

        if (getStyle() == STYLE_DASH) {
            this.setPos(this.getX() + this.getDeltaMovement().x,
                    this.getY() + this.getDeltaMovement().y,
                    this.getZ() + this.getDeltaMovement().z);
        }
    }

    private void damageDashTargets(Player player, Vec3 dashVector) {
        if (this.level().isClientSide) {
            return;
        }
        AABB hitBox = player.getBoundingBox().expandTowards(dashVector).inflate(1.0D, 0.8D, 1.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                target -> target != player && target.isAlive() && !target.isSpectator() && !target.isAlliedTo(player)
                        && !this.hitTargets.contains(target.getUUID()));
        for (LivingEntity target : targets) {
            if (damageTarget(player, target, Config.KatanaDashDamageMultiplier)) {
                this.hitTargets.add(target.getUUID());
            }
        }
    }

    private void damageOpenTargets(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 center = player.getEyePosition().add(look.scale(2.0D));
        AABB hitBox = new AABB(center, center).inflate(2.5D, 1.5D, 2.5D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                target -> target != player && target.isAlive() && !target.isSpectator() && !target.isAlliedTo(player)
                        && !this.hitTargets.contains(target.getUUID())
                        && target.position().subtract(player.position()).normalize().dot(look) > -0.25D);
        for (LivingEntity target : targets) {
            if (damageTarget(player, target, 1.0D)) {
                this.hitTargets.add(target.getUUID());
            }
        }
    }

    private boolean damageTarget(Player player, LivingEntity target, double multiplier) {
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier
                + EnchantmentHelper.getDamageBonus(this.attackStack, target.getMobType()));
        if (target.hurt(player.damageSources().playerAttack(player), damage)) {
            EnchantmentHelper.doPostHurtEffects(target, player);
            return true;
        }
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setStyle(tag.getInt("Style"));
        if (tag.contains("AttackStack")) {
            this.attackStack = ItemStack.of(tag.getCompound("AttackStack"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Style", getStyle());
        if (!this.attackStack.isEmpty()) {
            tag.put("AttackStack", this.attackStack.save(new CompoundTag()));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
