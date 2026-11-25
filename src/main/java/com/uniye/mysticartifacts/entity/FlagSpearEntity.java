package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModDamageTypes;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModSounds;
import com.uniye.mysticartifacts.item.impl.FlagSpearItem;
import com.uniye.mysticartifacts.network.FlagSpearTrailPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 右键掷出的旗枪幻影：飞行途中穿透敌人并造成投掷伤害，命中方块后插在落点，
 * 插住期间周期性对周围敌人造成范围伤害，并在每次爆发时广播轨迹特效。
 * 投掷不再消耗主手物品，实体本身也不是可带走的物品：投掷者靠近时回收，
 * 回收的充能直接加进玩家物品栏里的旗枪（主手优先），实体随即消失。
 */
public class FlagSpearEntity extends AbstractArrow implements ItemSupplier {
    /** 无充能外观的旗枪副本，仅用于实体渲染与回收判定 */
    private ItemStack spearStack = FlagSpearItem.createThrownDisplayStack();
    private float strikeDamage = 6.0F;
    private int burstTimer = 0;
    private int groundTime = 0;
    private final Set<Integer> piercedIds = new HashSet<>();

    public FlagSpearEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.setSilent(true);
        this.pickup = AbstractArrow.Pickup.ALLOWED;
    }

    public FlagSpearEntity(Level level, LivingEntity shooter, ItemStack stack, float strikeDamage) {
        super(ModEntities.FLAG_SPEAR.get(), shooter, level);
        this.setSilent(true);
        this.pickup = AbstractArrow.Pickup.ALLOWED;
        // 只借外观，不带主手旗枪的充能等数据，避免投掷物显示成满充能状态
        this.spearStack = FlagSpearItem.createThrownDisplayStack();
        this.strikeDamage = strikeDamage;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;
        if (!this.inGround) return;

        this.groundTime++;
        if (++this.burstTimer >= Config.FlagSpearBurstInterval) {
            this.burstTimer = 0;
            burst();
        }

        if (this.groundTime >= Config.FlagSpearGroundDuration) {
            // 幻影到期直接消失，不掉落旗枪物品（本体一直在玩家手里）
            this.discard();
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (this.level().isClientSide) return;
        // 发射者由 Projectile 保存并随实体持久化；无主人时也不允许他人回收。
        if (this.getOwner() != player) return;
        // 仅插住（或无物理）状态可回收，与原版箭一致
        if (!this.inGround && !this.isNoPhysics()) return;

        ItemStack pickup = this.getPickupItem();
        if (FlagSpearItem.isThrownMarker(pickup)) {
            // 幻影回收：给物品栏里的旗枪充能 +1（上限 MAX_CHARGE），并奖励 4s 迅捷
            if (FlagSpearItem.chargePlayerSpear(player)) {
                player.take(this, 1);
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED,
                        FlagSpearItem.PICKUP_SPEED_DURATION_TICKS,
                        0
                ));
            }
            // 物品栏里没有旗枪（或已满充能）时充能丢失，实体直接消失
            this.discard();
            return;
        }

        // 兜底分支：非幻影的旗枪物品（旧存档实体）仍按原逻辑入包
        if (!player.getInventory().add(pickup)) {
            this.spawnAtLocation(pickup);
            return;
        }
        player.take(this, 0);
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                FlagSpearItem.PICKUP_SPEED_DURATION_TICKS,
                0
        ));
        this.discard();
    }

    private void burst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        double range = Config.FlagSpearBurstRange;
        Entity owner = this.getOwner();
        AABB area = this.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidBurstTarget(owner, candidate)
                        && this.distanceToSqr(candidate) <= range * range
        );
        if (candidates.isEmpty()) return;

        DamageSource source = ModDamageTypes.getSource(this.level(), ModDamageTypes.SPEAR, this, owner);
        boolean anyHit = false;
        for (LivingEntity candidate : candidates) {
            candidate.invulnerableTime = 0;
            if (!candidate.hurt(source, this.strikeDamage)) continue;
            anyHit = true;
        }
        if (!anyHit) return;

        serverLevel.playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                ModSounds.FLAG_SPEAR_BURST.get(),
                SoundSource.PLAYERS,
                0.9F,
                1.0F
        );
        Vec3 center = this.position().add(0.0D, 0.25D, 0.0D);
        FlagSpearTrailPacket.send(
                serverLevel,
                center,
                serverLevel.getRandom().nextFloat() * 360.0F,
                serverLevel.getRandom().nextLong()
        );
    }

    private static boolean isValidBurstTarget(Entity owner, LivingEntity candidate) {
        if (candidate == owner) return false;
        if (!candidate.isAlive() || candidate.isSpectator() || candidate.isInvulnerable()) return false;
        if (candidate instanceof Player targetPlayer) {
            return !(owner instanceof Player shooterPlayer) || shooterPlayer.canHarmPlayer(targetPlayer);
        }
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        // 排除发射者与已穿透实体，避免 pierce 循环反复命中同一目标导致 tick 停滞
        return super.canHitEntity(entity) && entity != this.getOwner() && !piercedIds.contains(entity.getId());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 不调用 super：命中敌人后继续飞行（穿透），直到撞上方块插住
        Entity entity = result.getEntity();
        piercedIds.add(entity.getId());

        DamageSource source = ModDamageTypes.getSource(this.level(), ModDamageTypes.SPEAR, this, this.getOwner());
        boolean hurt = entity.hurt(source, this.strikeDamage);
        if (entity instanceof LivingEntity living) {
            living.invulnerableTime = 0;
        }

        if (hurt && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.TRIDENT_HIT,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.1F
            );
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.TRIDENT_HIT_GROUND,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
            serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    this.getX(),
                    this.getY() + 0.25D,
                    this.getZ(),
                    8,
                    0.2D,
                    0.2D,
                    0.2D,
                    0.05D
            );
        }
    }

    @Override
    public ItemStack getItem() {
        return this.spearStack.copy();
    }

    @Override
    public ItemStack getPickupItem() {
        // 交给 playerTouch 的「幻影」物品：带标记，回收时只转化为充能，绝不进入背包
        return FlagSpearItem.markThrown(this.spearStack.copy());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Item", this.spearStack.save(new CompoundTag()));
        tag.putFloat("StrikeDamage", this.strikeDamage);
        tag.putInt("BurstTimer", this.burstTimer);
        tag.putInt("GroundTime", this.groundTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // 只保留外观，旧的充能数据不带到投掷物上
        this.spearStack = FlagSpearItem.createThrownDisplayStack();
        this.strikeDamage = tag.getFloat("StrikeDamage");
        this.burstTimer = tag.getInt("BurstTimer");
        this.groundTime = tag.getInt("GroundTime");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
