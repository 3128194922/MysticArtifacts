package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModDamageTypes;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** MysticArtifacts 自己的刀光实体；伤害源始终是持刀玩家。 */
public class KatanaSlashEntity extends Projectile {
    /** 居合冲刺的刀光：随玩家冲刺方向平移，创建时结算一次路径伤害。 */
    public static final int STYLE_DASH = 0;
    /** 鬼刀剑气：向玩家视线方向飞行，最大飞行 5 格，途中持续判定实体伤害。 */
    public static final int STYLE_GHOST = 1;

    private static final EntityDataAccessor<Integer> STYLE =
            SynchedEntityData.defineId(KatanaSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ROTATION_OFFSET =
            SynchedEntityData.defineId(KatanaSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_ROLL =
            SynchedEntityData.defineId(KatanaSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASE_SIZE =
            SynchedEntityData.defineId(KatanaSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final int MAX_LIFETIME = 10;
    private static final double GHOST_RANGE = 5.0D;
    private static final double GHOST_SPEED = 1.0D;

    private ItemStack attackStack = ItemStack.EMPTY;
    private final Set<UUID> hitTargets = new HashSet<>();
    private double distanceTravelled;

    public KatanaSlashEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static KatanaSlashEntity createDash(Level level, Player player, ItemStack stack,
                                               Vec3 dashVector, boolean damageTargets) {
        KatanaSlashEntity slash = new KatanaSlashEntity(ModEntities.KATANA_SLASH.get(), level);
        slash.setOwner(player);
        slash.setStyle(STYLE_DASH);
        slash.attackStack = stack.copy();
        slash.setPos(player.getX(), player.getY() + player.getBbHeight() * 0.55D, player.getZ());
        slash.setYRot(player.getYRot());
        slash.setXRot(player.getXRot());
        slash.setDeltaMovement(dashVector);
        level.addFreshEntity(slash);
        if (damageTargets) {
            slash.damageDashTargets(player, dashVector);
        }
        return slash;
    }

    /**
     * 鬼刀剑气：发射位置固定，飞行方向仅微小抖动、视觉滚转与尺寸随机，
     * 偏转不影响命中（判定盒远大于抖动偏移）。
     */
    public static KatanaSlashEntity createGhostSlash(Level level, Player player, ItemStack stack) {
        RandomSource random = level.random;
        KatanaSlashEntity slash = new KatanaSlashEntity(ModEntities.KATANA_SLASH.get(), level);
        slash.setOwner(player);
        slash.setStyle(STYLE_GHOST);
        slash.attackStack = stack.copy();
        slash.setRotationRoll((random.nextFloat() - 0.5F) * 90.0F);
        slash.setBaseSize(0.9F + random.nextFloat() * 0.5F);

        Vec3 look = player.getLookAngle();
        slash.setPos(player.getX() + look.x * 1.0D,
                player.getEyeY() - 0.35D + look.y * 1.0D,
                player.getZ() + look.z * 1.0D);
        float yaw = player.getYRot() + (random.nextFloat() - 0.5F) * 6.0F;
        float pitch = Mth.clamp(player.getXRot() + (random.nextFloat() - 0.5F) * 6.0F, -90.0F, 90.0F);
        slash.setDeltaMovement(Vec3.directionFromRotation(pitch, yaw).scale(GHOST_SPEED));
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

    public void setRotationRoll(float roll) {
        this.entityData.set(ROTATION_ROLL, roll);
    }

    public float getRotationOffset() {
        return this.entityData.get(ROTATION_OFFSET);
    }

    public float getRotationRoll() {
        return this.entityData.get(ROTATION_ROLL);
    }

    public void setBaseSize(float size) {
        this.entityData.set(BASE_SIZE, size);
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

        Vec3 motion = this.getDeltaMovement();
        if (getStyle() == STYLE_DASH) {
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            return;
        }

        boolean serverSide = !this.level().isClientSide;
        if (serverSide) {
            Entity owner = this.getOwner();
            if (!(owner instanceof Player player) || !player.isAlive()) {
                this.discard();
                return;
            }
        }

        // 飞行途中撞到方块则消散
        if (serverSide) {
            BlockHitResult blockHit = this.level().clip(new ClipContext(this.position(),
                    this.position().add(motion), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) {
                this.discard();
                return;
            }
        }

        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        this.distanceTravelled += motion.length();

        if (serverSide && this.getOwner() instanceof Player player) {
            damageGhostTargets(player);
        }
        if (this.distanceTravelled >= GHOST_RANGE) {
            this.discard();
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
            if (damageTarget(player, target, Config.KatanaDashDamageMultiplier,
                    player.damageSources().playerAttack(player))) {
                this.hitTargets.add(target.getUUID());
            }
        }
    }

    private void damageGhostTargets(Player player) {
        AABB hitBox = new AABB(this.position(), this.position()).inflate(1.5D, 1.0D, 1.5D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                target -> target != player && target.isAlive() && !target.isSpectator() && !target.isAlliedTo(player)
                        && !this.hitTargets.contains(target.getUUID()));
        DamageSource source = ModDamageTypes.getSource(this.level(),
                ModDamageTypes.KATANA_GHOST_SLASH, this, player);
        for (LivingEntity target : targets) {
            if (damageTarget(player, target, 1.0D, source)) {
                this.hitTargets.add(target.getUUID());
            }
        }
    }

    private boolean damageTarget(Player player, LivingEntity target, double multiplier, DamageSource source) {
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier
                + EnchantmentHelper.getDamageBonus(this.attackStack, target.getMobType()));
        if (target.hurt(source, damage)) {
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
        this.distanceTravelled = tag.getDouble("Travelled");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Style", getStyle());
        if (!this.attackStack.isEmpty()) {
            tag.put("AttackStack", this.attackStack.save(new CompoundTag()));
        }
        tag.putDouble("Travelled", this.distanceTravelled);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
