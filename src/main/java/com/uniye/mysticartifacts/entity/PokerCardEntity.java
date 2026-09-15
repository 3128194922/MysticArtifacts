package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.init.ModDamageTypes;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PokerCardEntity extends AbstractArrow implements ItemSupplier, CustomOutlineEntity {
    private static final EntityDataAccessor<Boolean> RECALLING = SynchedEntityData.defineId(PokerCardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OUTLINE_COLOR = SynchedEntityData.defineId(PokerCardEntity.class, EntityDataSerializers.INT);
    private static final int WHITE_OUTLINE = 0xFFFFFF;
    private static final int RED_OUTLINE = 0xFF0000;
    
    private ItemStack pickupItemStack = new ItemStack(ModItems.POKER_CARD.get());
    private int lifeTime = 0;
    private final Set<Integer> piercedIds = new HashSet<>();

    public PokerCardEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setSilent(true);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    public PokerCardEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel, LivingEntity pShooter) {
        super(pEntityType, pShooter, pLevel);
        this.setPierceLevel((byte) 127);
        this.setSilent(true);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RECALLING, false);
        this.entityData.define(OUTLINE_COLOR, NO_OUTLINE_COLOR);
    }

    public void setRecalling(boolean recalling) {
        this.entityData.set(RECALLING, recalling);
    }

    public boolean isRecalling() {
        return this.entityData.get(RECALLING);
    }

    @Override
    public int getCustomOutlineColor() {
        return this.entityData.get(OUTLINE_COLOR);
    }

    @Override
    public boolean hasCustomOutlineRendering(Player player) {
        return hasCustomOutline();
    }

    private void setCustomOutlineColor(int color) {
        this.entityData.set(OUTLINE_COLOR, color);
    }

    public void startRecall() {
        setRecalling(true);
        this.inGround = false;
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public boolean isLandedState() {
        return this.inGround || this.getDeltaMovement().lengthSqr() < 1.0E-6D;
    }

    public void setItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            this.pickupItemStack = stack.copy();
            this.pickupItemStack.setCount(1);
        }
    }

    @Override
    public ItemStack getPickupItem() {
        return this.pickupItemStack.copy();
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.put("Item", this.pickupItemStack.save(new CompoundTag()));
        pCompound.putBoolean("Recalling", isRecalling());
        pCompound.putInt("LifeTime", lifeTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Item", 10)) {
            this.pickupItemStack = ItemStack.of(pCompound.getCompound("Item"));
        }
        setRecalling(pCompound.getBoolean("Recalling"));
        lifeTime = pCompound.getInt("LifeTime");
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            if (this.inGround && !isRecalling()) {
                lifeTime++;
                setCustomOutlineColor(lifeTime > 200 ? RED_OUTLINE : WHITE_OUTLINE);

                if (lifeTime > 240) { 
                    this.discard();
                }
            } else if (!this.inGround) {
                setCustomOutlineColor(NO_OUTLINE_COLOR);
            }
            
            if (isRecalling()) {
                Entity owner = getOwner();
                if (owner != null) {
                    Vec3 ownerPos = owner.position().add(0, owner.getEyeHeight() * 0.5, 0);
                    Vec3 dir = ownerPos.subtract(this.position()).normalize();
                    this.setDeltaMovement(dir);
                    this.setNoGravity(true);
                    this.noPhysics = true;
                    
                    double d0 = dir.x;
                    double d1 = dir.y;
                    double d2 = dir.z;
                    double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                    this.setYRot((float)(net.minecraft.util.Mth.atan2(d0, d2) * (double)(180F / (float)Math.PI)));
                    this.setXRot((float)(net.minecraft.util.Mth.atan2(d1, d3) * (double)(180F / (float)Math.PI)));
                    this.yRotO = this.getYRot();
                    this.xRotO = this.getXRot();

                    if (this.level() instanceof ServerLevel serverLevel) {
                         serverLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0, 0, 0, 0);
                    }
                    
                    if (this.position().distanceToSqr(ownerPos) < 2.0) {
                        this.discard();
                        return;
                    }
                    
                    List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.5));
                    for (LivingEntity target : targets) {
                        if (target != owner) {
                             if (target.invulnerableTime == 0) {
                                 target.hurt(ModDamageTypes.getSource(this.level(), ModDamageTypes.POKER_SLICE, this, owner), 6.0f);
                                 target.invulnerableTime = 0;
                             }
                        }
                    }
                } else {
                    this.discard();
                }
            }
        }
    }
    
    @Override
    protected boolean canHitEntity(Entity pEntity) {
        // 排除发射者与已命中的实体：127级穿透在 AbstractArrow.tick 的 while 循环中
        // 会反复调用 findHitEntity，若不排除已命中实体，会无限重复命中同一实体导致 tick 停滞。
        return super.canHitEntity(pEntity) && pEntity != this.getOwner() && !piercedIds.contains(pEntity.getId());
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        if (isRecalling()) return;
        
        Entity entity = pResult.getEntity();
        Entity owner = this.getOwner();
        if (entity == owner) return;

        // 无论目标是否可被伤害，都先记录已命中，确保 while 循环跳过它：
        // 既实现穿透（包括无敌目标不再被反弹），又避免重复命中同一实体造成死循环
        piercedIds.add(entity.getId());

        float damage = 4.0f;
        boolean hurt = entity.hurt(ModDamageTypes.getSource(this.level(), ModDamageTypes.VOID_SLICE, this, owner), damage);
        
        if (entity instanceof LivingEntity living) {
             living.invulnerableTime = 0;
        }

        // 仅当目标确实受到伤害时才调用 super.onHitEntity（保留原版箭矢伤害/音效/穿透记账）；
        // 目标无敌（hurt 返回 false）时不调用，避免 super 将箭矢反弹而无法穿透
        if (hurt) {
            super.onHitEntity(pResult);
        }
    }

    @Override
    public ItemStack getItem() {
        return this.pickupItemStack.copy();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
