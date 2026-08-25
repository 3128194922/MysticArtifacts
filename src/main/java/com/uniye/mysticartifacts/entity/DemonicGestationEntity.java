package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.item.impl.DemonicGestationItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DemonicGestationEntity extends Entity implements IEntityAdditionalSpawnData {

    private static final EntityDataAccessor<String> ITEM_ID =
            SynchedEntityData.defineId(DemonicGestationEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DASHING =
            SynchedEntityData.defineId(DemonicGestationEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUUID;
    private int attackCooldown;
    @Nullable
    private Entity target;
    @Nullable
    private Vec3 dashDir;
    @Nullable
    private Vec3 dashGoal;
    private Vec3 dashOrigin = Vec3.ZERO;
    private final Set<Integer> hitIds = new HashSet<>();

    public DemonicGestationEntity(EntityType<? extends DemonicGestationEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public DemonicGestationEntity(Player owner, ResourceLocation weaponId) {
        super(ModEntities.DEMONIC_GESTATION.get(), owner.level());
        this.ownerUUID = owner.getUUID();
        this.noCulling = true;
        this.setItemId(weaponId.toString());
        Vec3 follow = getFollowPosition(owner);
        this.setPos(follow.x, follow.y, follow.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ITEM_ID, "");
        this.entityData.define(DASHING, false);
    }

    // ========== Synched Data ==========

    public String getItemIdString() {
        return this.entityData.get(ITEM_ID);
    }

    public void setItemId(String id) {
        this.entityData.set(ITEM_ID, id);
    }

    @Nullable
    public ResourceLocation getStoredWeaponId() {
        String id = getItemIdString();
        if (id.isEmpty()) return null;
        return ResourceLocation.tryParse(id);
    }

    public ItemStack getDisplayItem() {
        ResourceLocation id = getStoredWeaponId();
        if (id != null) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return new ItemStack(ModItems.DEMONIC_GESTATION.get());
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Nullable
    public Player getOwnerPlayer() {
        if (this.ownerUUID == null) return null;
        return this.level().getPlayerByUUID(this.ownerUUID);
    }

    public boolean isDashing() {
        return this.entityData.get(DASHING);
    }

    private void setDashing(boolean value) {
        this.entityData.set(DASHING, value);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        Player owner = getOwnerPlayer();

        // Validate owner
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            this.discard();
            return;
        }

        // Check owner still wears the curio
        if (this.tickCount % 20 == 0 && !DemonicGestationItem.isWearing(owner)) {
            this.discard();
            return;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.isDashing()) {
            tickDash();
        } else {
            moveToFollowPos(owner);
            if (this.attackCooldown <= 0) {
                this.target = findTarget(owner);
                if (this.target != null) {
                    startDash();
                }
            }
        }
    }

    // ========== Follow (matches ArtifactSpirit default position) ==========

    private Vec3 getFollowPosition(Entity owner) {
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        double backComponent = -0.5;
        double rightComponent = 0.866;
        Vec3 rearRight = look.scale(backComponent).add(right.scale(rightComponent)).normalize();
        double dist = Config.SpiritFollowDistance;
        return owner.position()
                .add(0, owner.getBbHeight() * 0.6, 0)
                .add(rearRight.scale(dist));
    }

    private void moveToFollowPos(Entity owner) {
        Vec3 goal = getFollowPosition(owner);
        Vec3 cur = this.position();
        double dist = cur.distanceTo(goal);
        double speed = 0.06 + Math.min(dist * 0.03, 0.12);
        double x = lerp(cur.x, goal.x, speed);
        double y = lerp(cur.y, goal.y, speed);
        double z = lerp(cur.z, goal.z, speed);
        this.setDeltaMovement(x - cur.x, y - cur.y, z - cur.z);
        this.setPos(x, y, z);
    }

    // ========== Target acquisition ==========

    @Nullable
    private Entity findTarget(Player owner) {
        AABB area = owner.getBoundingBox().inflate(Config.DemonicGestationAttackRange);
        return this.level().getEntitiesOfClass(LivingEntity.class, area, e -> isValidTarget(e, owner))
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(owner), b.distanceToSqr(owner)))
                .orElse(null);
    }

    private static boolean isValidTarget(LivingEntity candidate, Player owner) {
        if (candidate == owner || !candidate.isAlive()) return false;
        if (candidate instanceof Player p && p.isSpectator()) return false;

        // Actively targeting the player, or last hurt by the player (neutral retaliation)
        boolean targeting = candidate instanceof Mob mob && mob.getTarget() == owner;
        boolean retaliating = candidate.getLastHurtByMob() == owner;
        return targeting || retaliating;
    }

    // ========== Dash (straight-line charge & pierce) ==========

    private void startDash() {
        if (this.target == null) return;
        Vec3 from = this.position();
        Vec3 to = this.target.position().add(0, this.target.getBbHeight() * 0.5, 0);
        Vec3 diff = to.subtract(from);
        if (diff.lengthSqr() < 1e-4) return;

        this.dashDir = diff.normalize();
        this.dashGoal = to;
        this.dashOrigin = from;
        this.hitIds.clear();
        setDashing(true);
        updateHeading(this.dashDir);

        this.level().playSound(null, from.x, from.y, from.z,
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    private void tickDash() {
        Vec3 dir = this.dashDir;
        if (dir == null) {
            endDash();
            return;
        }

        Vec3 cur = this.position();

        // Mild homing so a moving target is still reached (keeps path mostly straight)
        if (this.target != null && this.target.isAlive()) {
            Vec3 toTarget = this.target.position().add(0, this.target.getBbHeight() * 0.5, 0)
                    .subtract(cur);
            if (toTarget.lengthSqr() > 1e-6) {
                dir = toTarget.normalize();
                this.dashDir = dir;
                updateHeading(dir);
            }
        }

        Vec3 next = cur.add(dir.scale(Config.DemonicGestationChargeSpeed));
        this.setDeltaMovement(dir.scale(Config.DemonicGestationChargeSpeed));
        this.setPos(next.x, next.y, next.z);

        // Pierce every target in the path once, damage sourced from the player
        AABB box = this.getBoundingBox().inflate(0.4);
        Player owner = getOwnerPlayer();
        DamageSource source = this.level().damageSources().mobProjectile(this, owner);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != owner && e.isAlive() && !this.hitIds.contains(e.getId()))) {
            living.hurt(source, (float) Config.DemonicGestationDamage);
            this.hitIds.add(living.getId());
        }

        // End dash when goal reached, range exceeded, or target is gone
        boolean reachedGoal = this.dashGoal != null && next.distanceToSqr(this.dashGoal) < 0.7;
        boolean exceededRange = cur.distanceToSqr(this.dashOrigin)
                > Config.DemonicGestationChargeRange * Config.DemonicGestationChargeRange;
        boolean targetGone = (this.target == null || !this.target.isAlive())
                && this.dashGoal != null && next.distanceToSqr(this.dashGoal) < 1.2;
        if (reachedGoal || exceededRange || targetGone) {
            endDash();
        }
    }

    private void updateHeading(Vec3 dir) {
        float yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yRotO = yaw;
    }

    private void endDash() {
        setDashing(false);
        this.setDeltaMovement(Vec3.ZERO);
        this.target = null;
        this.dashDir = null;
        this.dashGoal = null;
        this.hitIds.clear();
        this.attackCooldown = Config.DemonicGestationAttackCooldown;
    }

    private static double lerp(double from, double to, double factor) {
        return from + (to - from) * factor;
    }

    // ========== Persistence ==========

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("ItemId")) {
            this.setItemId(tag.getString("ItemId"));
        }
        this.attackCooldown = tag.getInt("Cooldown");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putString("ItemId", getItemIdString());
        tag.putInt("Cooldown", this.attackCooldown);
    }

    // ========== Networking ==========

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.ownerUUID != null ? this.ownerUUID : new UUID(0, 0));
        buffer.writeUtf(getItemIdString());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.ownerUUID = buffer.readUUID();
        if (this.ownerUUID.equals(new UUID(0, 0))) {
            this.ownerUUID = null;
        }
        this.setItemId(buffer.readUtf());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}