package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.item.impl.ArtifactSpiritItem;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
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
    private ArtifactAiLogic.State aiState = ArtifactAiLogic.State.FOLLOW;
    private int targetScanCooldown;

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

        if (this.isDashing()) {
            this.aiState = ArtifactAiLogic.State.ATTACK;
            tickDash();
        } else {
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            moveToFollowPos(owner);
            tickAi(owner);
        }
    }

    private void tickAi(Player owner) {
        if (this.targetScanCooldown > 0) {
            this.targetScanCooldown--;
        }

        boolean targetValid = this.target != null && isValidTarget(this.target, owner)
                && this.target.distanceToSqr(owner) <= Config.DemonicGestationChargeRange
                * Config.DemonicGestationChargeRange;
        if (!targetValid) {
            this.target = null;
        }

        boolean closeToFollowPoint = this.position().distanceToSqr(getFollowPosition(owner)) <= 1.0;
        this.aiState = ArtifactAiLogic.nextState(this.aiState, this.target != null, targetValid,
                this.attackCooldown <= 0, closeToFollowPoint);

        if (this.aiState == ArtifactAiLogic.State.SEARCH && this.targetScanCooldown <= 0
                && this.attackCooldown <= 0) {
            this.target = findTarget(owner);
            this.targetScanCooldown = 8;
            this.aiState = this.target == null
                    ? (closeToFollowPoint ? ArtifactAiLogic.State.FOLLOW : ArtifactAiLogic.State.SEARCH)
                    : ArtifactAiLogic.State.ATTACK;
        }

        if (this.aiState == ArtifactAiLogic.State.ATTACK && this.target != null
                && this.attackCooldown <= 0) {
            startDash();
        }
    }

    // ========== Follow (matches ArtifactSpirit default position) ==========

    private Vec3 getFollowPosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        double backComponent = -0.5;
        // When both 器灵(artifact_spirit) and 器魔 are worn, mirror to the left side
        // so the two spirits do not occupy the same follow point.
        double rightComponent = (ArtifactSpiritItem.isWearing(owner)) ? -0.866 : 0.866;
        Vec3 rearRight = look.scale(backComponent).add(right.scale(rightComponent)).normalize();
        double dist = Config.SpiritFollowDistance;
        return owner.position()
                .add(0, owner.getBbHeight() * 0.6, 0)
                .add(rearRight.scale(dist));
    }

    private void moveToFollowPos(LivingEntity owner) {
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
        boolean dualWorn = ArtifactSpiritItem.isWearing(owner);
        AABB area = owner.getBoundingBox().inflate(Config.DemonicGestationAttackRange);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(
                LivingEntity.class, area, e -> isValidTarget(e, owner));
        if (candidates.isEmpty()) return null;

        int activeTargetId = owner.getLastHurtMob() == null ? -1 : owner.getLastHurtMob().getId();
        int attackerId = owner.getLastHurtByMob() == null ? -1 : owner.getLastHurtByMob().getId();
        int spiritTargetId = findSpiritTargetId(owner);
        List<ArtifactAiLogic.Candidate> snapshots = candidates.stream()
                .map(candidate -> new ArtifactAiLogic.Candidate(
                        candidate.getId(),
                        candidate.distanceToSqr(owner),
                        candidate.isAlive(),
                        candidate instanceof Player player && player.isSpectator(),
                        candidate.isAlliedTo(owner),
                        hasLineOfSight(owner, candidate),
                        candidate.getId() == activeTargetId,
                        candidate.getId() == attackerId,
                        isTargetingOwner(candidate, owner),
                        candidate.getId() == spiritTargetId
                ))
                .toList();
        int selectedId = ArtifactAiLogic.chooseTarget(snapshots, activeTargetId, attackerId,
                ArtifactAiLogic.Role.DEMONIC, dualWorn);
        for (LivingEntity candidate : candidates) {
            if (candidate.getId() == selectedId) {
                return candidate;
            }
        }
        return null;
    }

    private int findSpiritTargetId(Player owner) {
        List<ArtifactSpiritEntity> spirits = this.level().getEntitiesOfClass(
                ArtifactSpiritEntity.class,
                owner.getBoundingBox().inflate(32.0),
                spirit -> owner.getUUID().equals(spirit.getOwnerUUID())
        );
        for (ArtifactSpiritEntity spirit : spirits) {
            Entity spiritTarget = spirit.getCombatTarget();
            if (spiritTarget != null && spiritTarget.isAlive()) {
                return spiritTarget.getId();
            }
        }
        return -1;
    }

    private static boolean isValidTarget(Entity candidate, Player owner) {
        if (!(candidate instanceof LivingEntity living)) return false;
        if (candidate == owner || !candidate.isAlive()) return false;
        if (candidate instanceof Player p && p.isSpectator()) return false;
        if (candidate.isInvulnerable() || candidate.isAlliedTo(owner)) return false;
        if (!hasLineOfSight(owner, candidate)) return false;

        return living == owner.getLastHurtMob()
                || isTargetingOwner(candidate, owner)
                || living.getLastHurtByMob() == owner;
    }

    private static boolean isTargetingOwner(Entity candidate, Player owner) {
        return candidate instanceof Mob mob && mob.getTarget() == owner;
    }

    private static boolean hasLineOfSight(LivingEntity owner, Entity target) {
        BlockHitResult hit = owner.level().clip(new ClipContext(
                owner.getEyePosition(),
                target.getEyePosition(),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                owner
        ));
        return hit.getType() == HitResult.Type.MISS;
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
        this.aiState = ArtifactAiLogic.State.COOLDOWN;
        this.targetScanCooldown = 8;
    }

    @Nullable
    public Entity getCombatTarget() {
        return this.target;
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
