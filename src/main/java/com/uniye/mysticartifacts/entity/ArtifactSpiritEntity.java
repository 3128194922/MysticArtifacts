package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.item.impl.ArtifactSpiritItem;
import net.minecraft.core.RegistryAccess;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraftforge.fml.ModList;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.UUID;

public class ArtifactSpiritEntity extends Entity implements IEntityAdditionalSpawnData {

    private static final EntityDataAccessor<String> ITEM_ID = SynchedEntityData.defineId(ArtifactSpiritEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_ATTACKING = SynchedEntityData.defineId(ArtifactSpiritEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(ArtifactSpiritEntity.class, EntityDataSerializers.INT);

    private static final ResourceLocation CLANGING_HOWL_FLAMETHROWER_ID = ResourceLocation.fromNamespaceAndPath("clanginghowl", "flamethrower");
    private static final ResourceLocation CLANGING_HOWL_BLAZE_FUEL_CYLINDER_ID = ResourceLocation.fromNamespaceAndPath("clanginghowl", "blaze_fuel_cylinder");
    private static final ResourceLocation POTATO_CANNON_ID = ResourceLocation.fromNamespaceAndPath("create", "potato_cannon");

    @Nullable
    private Entity target;
    private int attackCooldown;
    private int returnTimer;
    @Nullable
    private UUID ownerUUID;

    // ClangingHowl flamethrower state machine
    // 0=IDLE, 1=STARTUP (预热), 2=FIRING (喷火), 3=OVERHEAT (过热冷却)
    private int clangingHowlPhase = 0;
    private int clangingHowlPhaseTimer = 0;
    private int clangingHowlFuel = 0;
    private int clangingHowlLastTargetId = -1;

    private static final int CLANGING_HOWL_STARTUP_TICKS = 20;
    private static final int CLANGING_HOWL_MAX_FIRING_TICKS = 120;
    private static final int CLANGING_HOWL_OVERHEAT_TICKS = 40;
    private static final int CLANGING_HOWL_MAX_FUEL = 1600;
    private static final int CLANGING_HOWL_FUEL_PER_CYLINDER = 1600;
    private static final int CLANGING_HOWL_FUEL_PER_CONSUMPTION = 5;
    private static final int CLANGING_HOWL_FUEL_INTERVAL_TICKS = 20;
    private static final float CLANGING_HOWL_DAMAGE = 4.0F;
    private static final String CLANGING_HOWL_FUEL_TAG = "ClangingHowlFuel";

    public ArtifactSpiritEntity(EntityType<? extends ArtifactSpiritEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.attackCooldown = 0;
    }

    public ArtifactSpiritEntity(Player owner, ResourceLocation weaponId) {
        super(ModEntities.ARTIFACT_SPIRIT.get(), owner.level());
        this.ownerUUID = owner.getUUID();
        this.noCulling = true;
        this.setItemId(weaponId.toString());
        Vec3 followPos = getFollowPosition(owner);
        this.setPos(followPos.x, followPos.y, followPos.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ITEM_ID, "");
        this.entityData.define(IS_ATTACKING, false);
        this.entityData.define(TARGET_ID, -1);
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
        return new ItemStack(Items.BOW);
    }

    public boolean isAttacking() {
        return this.entityData.get(IS_ATTACKING);
    }

    private void setAttacking(boolean value) {
        this.entityData.set(IS_ATTACKING, value);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    @Nullable
    public Player getOwnerPlayer() {
        if (this.ownerUUID == null || this.level() == null) return null;
        return this.level().getPlayerByUUID(this.ownerUUID);
    }

    // ========== Tick ==========

    @Override
    public void tick() {
        super.tick();

        Player owner = getOwnerPlayer();

        if (!this.level().isClientSide) {
            // Validate owner
            if (owner == null || !owner.isAlive() || owner.isSpectator()) {
                this.discard();
                return;
            }

            // Check if owner still has spirit equipped
            if (this.tickCount % 20 == 0) {
                if (!ArtifactSpiritItem.isWearing(owner)) {
                    this.discard();
                    return;
                }
            }

            // Tick cooldown (only for non-flamethrower weapons)
            if (!isClangingHowlFlamethrower() && attackCooldown > 0) {
                attackCooldown--;
            }

            // Always update target from owner's combat state (every tick)
            Entity ownerTarget = getOwnerTarget(owner);
            if (ownerTarget != null) {
                if (this.target == null || this.target.getId() != ownerTarget.getId()) {
                    // Target switched — reset flamethrower state if needed
                    if (isClangingHowlFlamethrower() && clangingHowlPhase > 0) {
                        resetClangingHowlState();
                    }
                    this.target = ownerTarget;
                    this.entityData.set(TARGET_ID, this.target.getId());
                    this.setAttacking(true);
                    this.attackCooldown = Math.max(this.attackCooldown, 5);
                }
            }

            // State machine
            if (this.isAttacking()) {
                if (this.target != null && this.target.isAlive()) {
                    handleRangedAttack(owner);
                } else {
                    this.setAttacking(false);
                    this.target = null;
                    this.entityData.set(TARGET_ID, -1);
                    if (isClangingHowlFlamethrower()) resetClangingHowlState();
                    this.returnTimer = 10;
                }
            } else {
                if (returnTimer > 0) {
                    returnTimer--;
                }
                if (isClangingHowlFlamethrower()) resetClangingHowlState();
            }
        }

        // Movement (both sides) — always follow, face target when attacking
        if (owner != null) {
            handleFollow(owner);
            if (this.isAttacking()) {
                faceTarget();
            }
        }
    }

    // ========== Follow (smooth) ==========

    private void handleFollow(Entity owner) {
        Vec3 targetPos = getFollowPosition(owner);
        double dist = this.position().distanceTo(targetPos);

        // Adaptive lerp: faster when far, slower when close → smooth deceleration
        double speed = 0.06 + Math.min(dist * 0.03, 0.12);
        if (returnTimer > 0) {
            speed += 0.04; // slightly faster return after attack
        }

        double x = lerp(this.getX(), targetPos.x, speed);
        double y = lerp(this.getY(), targetPos.y, speed);
        double z = lerp(this.getZ(), targetPos.z, speed);

        this.setDeltaMovement(x - this.getX(), y - this.getY(), z - this.getZ());
        this.setPos(x, y, z);
    }

    private Vec3 getFollowPosition(Entity owner) {
        // Right-rear: behind (opposite of look) + right offset
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        // Angle: ~120° offset = half-back + right component
        double backComponent = -0.5;
        double rightComponent = 0.866; // sin(60°) ≈ 0.866
        Vec3 rearRight = look.scale(backComponent).add(right.scale(rightComponent)).normalize();
        double dist = Config.SpiritFollowDistance;
        return owner.position()
                .add(0, owner.getBbHeight() * 0.6, 0)
                .add(rearRight.scale(dist));
    }

    // ========== Face Target (smooth interpolation) ==========

    private void faceTarget() {
        Entity currentTarget = this.target;
        if (currentTarget == null && this.level().isClientSide) {
            int id = this.entityData.get(TARGET_ID);
            if (id >= 0) {
                currentTarget = this.level().getEntity(id);
            }
        }
        if (currentTarget == null) return;

        // Compute desired angles toward target
        Vec3 selfPos = this.position();
        Vec3 targetPos = currentTarget.position().add(0, currentTarget.getBbHeight() * 0.5, 0);
        Vec3 diff = targetPos.subtract(selfPos);
        double dx = diff.x;
        double dz = diff.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float desiredPitch = horizontal < 1e-4 ? 0 : (float) (-Mth.atan2(diff.y, horizontal) * (180.0 / Math.PI));

        // Smooth rotation toward desired angles
        float currentYaw = this.getYRot();
        float currentPitch = this.getXRot();

        // Rotation speed scales with angle difference (faster when facing away, slower when nearly aligned)
        float yawDiff = Mth.wrapDegrees(desiredYaw - currentYaw);
        float pitchDiff = desiredPitch - currentPitch;

        float rotSpeed = 0.12F; // base rotation speed per tick (~7°/tick at 60° diff)
        float newYaw = currentYaw + yawDiff * rotSpeed;
        float newPitch = currentPitch + pitchDiff * rotSpeed;

        this.setYRot(newYaw);
        this.setXRot(Mth.clamp(newPitch, -80, 80));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    // ========== Ranged Attack ==========

    private boolean isClangingHowlFlamethrower() {
        ResourceLocation weaponId = getStoredWeaponId();
        return ModList.get().isLoaded("clanginghowl") && CLANGING_HOWL_FLAMETHROWER_ID.equals(weaponId);
    }

    private void resetClangingHowlState() {
        clangingHowlPhase = 0;
        clangingHowlPhaseTimer = 0;
        clangingHowlLastTargetId = -1;
    }

    private void handleRangedAttack(Player owner) {
        if (this.target == null) return;
        ResourceLocation weaponId = getStoredWeaponId();
        if (weaponId == null) return;

        // === ClangingHowl flamethrower state machine (per-tick) ===
        if (isClangingHowlFlamethrower()) {
            tickClangingHowlStateMachine(owner);
            return;
        }

        // === Non-flamethrower weapons (single shot per cooldown) ===
        if (attackCooldown > 0) return;

        ItemStack consumedAmmo = ItemStack.EMPTY;

        // Potato cannon has its own ammo validation via level.registryAccess()
        if (weaponId.equals(POTATO_CANNON_ID)) {
            consumedAmmo = consumePotatoCannonAmmo(owner);
        } else if (!isClangingHowlFlamethrower()) {
            consumedAmmo = consumeAmmoFromEnderChest(owner, weaponId);
        }

        if (!weaponId.equals(POTATO_CANNON_ID) && !isClangingHowlFlamethrower() && consumedAmmo.isEmpty()) {
            this.setAttacking(false);
            this.target = null;
            this.entityData.set(TARGET_ID, -1);
            this.returnTimer = 10;
            this.attackCooldown = 20;
            return;
        }

        fireSingleProjectile(owner, weaponId, consumedAmmo);
    }

    // ========== ClangingHowl Flamethrower State Machine ==========

    private void tickClangingHowlStateMachine(Player owner) {
        ResourceLocation weaponId = getStoredWeaponId();
        if (weaponId == null) return;

        Vec3 muzzle = this.position().add(0, 0.25, 0);

        switch (clangingHowlPhase) {
            case 0: // IDLE → STARTUP
                startClangingHowlFiring(owner, muzzle);
                break;

            case 1: // STARTUP (预热)
                clangingHowlPhaseTimer--;
                if (clangingHowlPhaseTimer <= 0) {
                    clangingHowlPhase = 2;
                    clangingHowlPhaseTimer = CLANGING_HOWL_MAX_FIRING_TICKS;
                    this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z,
                            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 0.5F);
                }
                break;

            case 2: // FIRING (喷火)
                // Match ClangingHowl onUseTick: consume on the first firing tick, then every 20 ticks.
                if (clangingHowlPhaseTimer % CLANGING_HOWL_FUEL_INTERVAL_TICKS == 0) {
                    if (clangingHowlFuel < CLANGING_HOWL_FUEL_PER_CONSUMPTION) {
                        if (!refillClangingHowlFuelFromEnderChest(owner)) {
                            // Out of fuel → OVERHEAT
                            clangingHowlPhase = 3;
                            clangingHowlPhaseTimer = CLANGING_HOWL_OVERHEAT_TICKS;
                            return;
                        }
                    }
                    clangingHowlFuel -= CLANGING_HOWL_FUEL_PER_CONSUMPTION;
                }

                // Shoot flame every tick
                shootClangingHowlFlame(owner, muzzle);
                clangingHowlPhaseTimer--;

                if (clangingHowlPhaseTimer <= 0) {
                    // Max firing duration → OVERHEAT
                    clangingHowlPhase = 3;
                    clangingHowlPhaseTimer = CLANGING_HOWL_OVERHEAT_TICKS;
                }
                break;

            case 3: // OVERHEAT (过热冷却)
                clangingHowlPhaseTimer--;
                if (clangingHowlPhaseTimer <= 0) {
                    clangingHowlPhase = 0; // Return to IDLE — can restart
                }
                break;
        }
    }

    private void startClangingHowlFiring(Player owner, Vec3 muzzle) {
        if (clangingHowlFuel < CLANGING_HOWL_FUEL_PER_CONSUMPTION && !refillClangingHowlFuelFromEnderChest(owner)) {
            this.attackCooldown = 20; // No fuel — wait before retry
            return;
        }

        clangingHowlPhase = 1;
        clangingHowlPhaseTimer = CLANGING_HOWL_STARTUP_TICKS;
        clangingHowlLastTargetId = this.target != null ? this.target.getId() : -1;

        this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 0.8F);
    }

    private void shootClangingHowlFlame(Player owner, Vec3 muzzle) {
        Vec3 targetPos = this.target.position().add(0, this.target.getBbHeight() * 0.5, 0);
        Vec3 aim = targetPos.subtract(muzzle).normalize();

        // Random spread
        double spread = 0.5;
        aim = aim.add(
                (this.random.nextDouble() - 0.5) * spread,
                (this.random.nextDouble() - 0.5) * spread,
                (this.random.nextDouble() - 0.5) * spread
        ).normalize();

        FlameProjectileEntity flame = new FlameProjectileEntity(owner, this.level());
        flame.setPos(muzzle.x, muzzle.y, muzzle.z);
        flame.setDamage(CLANGING_HOWL_DAMAGE);
        flame.setSoul(false);
        flame.setDeltaMovement(aim.scale(0.4));

        this.level().addFreshEntity(flame);

        // Sound periodically (every 5 ticks)
        if (clangingHowlPhaseTimer % 5 == 0) {
            this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z,
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4F,
                    0.5F + this.random.nextFloat() * 0.5F);
        }
    }

    // ========== Single Projectile Fire ==========

    private void fireSingleProjectile(Player owner, ResourceLocation weaponId, ItemStack consumedAmmo) {
        Vec3 muzzle = this.position().add(0, 0.25, 0);
        Vec3 targetPos = this.target.position().add(0, this.target.getBbHeight() * 0.5, 0);
        Vec3 aim = targetPos.subtract(muzzle);
        Item weaponItem = ForgeRegistries.ITEMS.getValue(weaponId);

        if (weaponItem instanceof BowItem) {
            fireArrow(owner, muzzle, aim, false);
            this.attackCooldown = 20;
        } else if (weaponItem instanceof CrossbowItem) {
            fireCrossbowShot(owner, muzzle, aim, consumedAmmo);
            this.attackCooldown = 25;
        } else if (weaponItem instanceof SplashPotionItem || weaponItem instanceof LingeringPotionItem) {
            firePotion(owner, muzzle, aim, weaponItem);
            this.attackCooldown = Config.SpiritAttackCooldown;
        } else if (weaponId.equals(POTATO_CANNON_ID)) {
            if (consumedAmmo.isEmpty()) return; // should not happen
            firePotatoCannonShot(owner, muzzle, aim, consumedAmmo);
            this.attackCooldown = 15;
        }
    }

    private void fireArrow(Player owner, Vec3 muzzle, Vec3 aim, boolean noGravity) {
        Arrow arrow = new Arrow(this.level(), muzzle.x, muzzle.y, muzzle.z);
        arrow.setOwner(owner);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;

        double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
        if (!noGravity) {
            aim = aim.add(0, horizontal * 0.2, 0);
        }
        arrow.shoot(aim.x, aim.y, aim.z, 1.6F, 0.0F);
        if (noGravity) {
            arrow.setNoGravity(true);
        }
        arrow.setBaseDamage(Config.SpiritDamage);

        this.level().addFreshEntity(arrow);
        this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z,
                SoundEvents.SKELETON_SHOOT, SoundSource.PLAYERS, 0.8F,
                1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }

    private void fireCrossbowShot(Player owner, Vec3 muzzle, Vec3 aim, ItemStack ammo) {
        double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);

        boolean isFireworkRocket = ammo.is(Items.FIREWORK_ROCKET);
        boolean isNoGravityArrow = !isFireworkRocket && !ammo.isEmpty()
                && ammo.is(net.minecraft.tags.ItemTags.create(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "firework")));

        if (isFireworkRocket) {
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    this.level(), ammo, muzzle.x, muzzle.y, muzzle.z, true);
            rocket.setOwner(owner);
            rocket.shoot(aim.x, aim.y, aim.z, 1.6F, 0.0F);
            this.level().addFreshEntity(rocket);
        } else {
            Arrow arrow = new Arrow(this.level(), muzzle.x, muzzle.y, muzzle.z);
            arrow.setOwner(owner);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setPierceLevel((byte) 2);

            if (isNoGravityArrow) {
                arrow.setNoGravity(true);
                arrow.shoot(aim.x, aim.y, aim.z, 1.6F, 0.0F);
            } else {
                arrow.shoot(aim.x, aim.y + horizontal * 0.2F, aim.z, 1.6F, 0.0F);
            }
            arrow.setBaseDamage(Config.SpiritDamage + 2.0);
            this.level().addFreshEntity(arrow);
        }

        this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z,
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void firePotion(Player owner, Vec3 muzzle, Vec3 aim, Item weaponItem) {
        ThrownPotion potion = new ThrownPotion(this.level(), owner);
        potion.setItem(new ItemStack(weaponItem));
        potion.setPos(muzzle.x, muzzle.y, muzzle.z);

        double speed = 1.1;
        Vec3 velocity = aim.normalize().scale(speed);
        potion.setDeltaMovement(velocity);

        this.level().addFreshEntity(potion);
        this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z,
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private void firePotatoCannonShot(Player owner, Vec3 muzzle, Vec3 aim, ItemStack ammo) {
        if (!ModList.get().isLoaded("create")) {
            // Fallback: generic projectile
            Arrow arrow = new Arrow(this.level(), muzzle.x, muzzle.y, muzzle.z);
            arrow.setOwner(owner);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setNoGravity(true);
            arrow.shoot(aim.x, aim.y, aim.z, 2.5F, 0.3F);
            arrow.setBaseDamage(Config.SpiritDamage + 3.0);
            arrow.setCritArrow(true);
            this.level().addFreshEntity(arrow);
            this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.3F, 1.2F);
            return;
        }

        // Look up Create's entity type at runtime
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(
                ResourceLocation.fromNamespaceAndPath("create", "potato_projectile"));
        if (entityType == null) {
            firePotatoCannonShot(owner, muzzle, aim, ItemStack.EMPTY); // fallback
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            EntityType<? extends AbstractHurtingProjectile> projectileType =
                    (EntityType<? extends AbstractHurtingProjectile>) entityType;

            AbstractHurtingProjectile projectile = projectileType.create(this.level());
            projectile.setOwner(owner);
            projectile.setPos(muzzle.x, muzzle.y, muzzle.z);

            // Set ammo item via reflection (setItem method on PotatoProjectileEntity)
            if (!ammo.isEmpty()) {
                try {
                    java.lang.reflect.Method setItem = projectile.getClass()
                            .getMethod("setItem", ItemStack.class);
                    setItem.invoke(projectile, ammo.copy());
                } catch (NoSuchMethodException ignored) {
                    // Older Create version without setItem
                }
            }

            Vec3 velocity = Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(2.5);
            projectile.setDeltaMovement(velocity);

            this.level().addFreshEntity(projectile);
            this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4F, 1.0F);
        } catch (Exception e) {
            // If anything fails, discard silently
        }
    }

    // ========== Target Finding ==========

    /**
     * Gets the owner's current combat target (real-time, no caching).
     * Priority: lastHurtMob (player's attack target) > lastHurtByMob (attacker)
     */
    @Nullable
    private Entity getOwnerTarget(Entity owner) {
        if (!(owner instanceof LivingEntity livingOwner)) return null;

        // Player's attack target (highest priority)
        LivingEntity hurtMob = livingOwner.getLastHurtMob();
        if (hurtMob != null && hurtMob.isAlive() && hurtMob != owner
                && hurtMob.distanceTo(owner) <= Config.SpiritAttackRange) {
            return hurtMob;
        }

        // Entity attacking the player
        LivingEntity hurtByMob = livingOwner.getLastHurtByMob();
        if (hurtByMob != null && hurtByMob.isAlive() && hurtByMob != owner
                && hurtByMob.distanceTo(owner) <= Config.SpiritAttackRange) {
            return hurtByMob;
        }

        return null;
    }

    // ========== Ammo from Ender Chest ==========

    /** Refill flamethrower fuel from the first matching ender chest slot. */
    private boolean refillClangingHowlFuelFromEnderChest(@Nullable Player player) {
        if (!ModList.get().isLoaded("clanginghowl") || player == null) return false;

        var enderChest = player.getEnderChestInventory();
        if (enderChest == null) return false;

        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            ItemStack stack = enderChest.getItem(i);
            if (!stack.isEmpty() && CLANGING_HOWL_BLAZE_FUEL_CYLINDER_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    enderChest.setItem(i, ItemStack.EMPTY);
                }
                clangingHowlFuel = Math.min(CLANGING_HOWL_MAX_FUEL,
                        clangingHowlFuel + CLANGING_HOWL_FUEL_PER_CYLINDER);
                return true;
            }
        }
        return false;
    }

    /** Consume one matching ammo from ender chest. Returns the consumed stack (copy), or EMPTY. */
    private ItemStack consumeAmmoFromEnderChest(Player player, @Nullable ResourceLocation weaponId) {
        if (weaponId == null) return ItemStack.EMPTY;
        var enderChest = player.getEnderChestInventory();
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            ItemStack stack = enderChest.getItem(i);
            if (!stack.isEmpty() && ArtifactSpiritItem.isAmmoForWeapon(stack, weaponId)) {
                ItemStack consumed = stack.copyWithCount(1);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    enderChest.setItem(i, ItemStack.EMPTY);
                }
                return consumed;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Consume valid potato cannon ammo from ender chest, validated against Create's
     * PotatoCannonProjectileType registry (via reflection, no compile dependency).
     */
    private ItemStack consumePotatoCannonAmmo(Player player) {
        if (!ModList.get().isLoaded("create")) return ItemStack.EMPTY;
        var enderChest = player.getEnderChestInventory();
        RegistryAccess registryAccess = player.level().registryAccess();
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            ItemStack stack = enderChest.getItem(i);
            if (stack.isEmpty()) continue;
            if (isValidPotatoCannonAmmo(registryAccess, stack)) {
                ItemStack result = stack.copyWithCount(1);
                stack.shrink(1);
                if (stack.isEmpty()) enderChest.setItem(i, ItemStack.EMPTY);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Reflection-based call to PotatoCannonProjectileType.getTypeForItem(RegistryAccess, Item). */
    private static boolean isValidPotatoCannonAmmo(RegistryAccess registryAccess, ItemStack stack) {
        try {
            Class<?> typeClass = Class.forName(
                    "com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType");
            java.lang.reflect.Method getTypeForItem = typeClass.getMethod("getTypeForItem",
                    RegistryAccess.class, Item.class);
            Object result = getTypeForItem.invoke(null, registryAccess, stack.getItem());
            return result instanceof java.util.Optional && ((java.util.Optional<?>) result).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    // ========== Helper ==========

    private static double lerp(double from, double to, double factor) {
        return from + (to - from) * factor;
    }

    // ========== Networking ==========

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.ownerUUID != null ? this.ownerUUID : new UUID(0, 0));
        buffer.writeUtf(getItemIdString());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.ownerUUID = additionalData.readUUID();
        if (this.ownerUUID.equals(new UUID(0, 0))) {
            this.ownerUUID = null;
        }
        this.setItemId(additionalData.readUtf());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("ItemId")) {
            this.setItemId(tag.getString("ItemId"));
        }
        this.attackCooldown = tag.getInt("AttackCooldown");
        this.clangingHowlPhase = tag.getInt("ClangingHowlPhase");
        this.clangingHowlPhaseTimer = tag.getInt("ClangingHowlPhaseTimer");
        this.clangingHowlFuel = Mth.clamp(tag.getInt(CLANGING_HOWL_FUEL_TAG), 0, CLANGING_HOWL_MAX_FUEL);
        this.clangingHowlLastTargetId = tag.getInt("ClangingHowlLastTargetId");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putString("ItemId", getItemIdString());
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putInt("ClangingHowlPhase", clangingHowlPhase);
        tag.putInt("ClangingHowlPhaseTimer", clangingHowlPhaseTimer);
        tag.putInt(CLANGING_HOWL_FUEL_TAG, clangingHowlFuel);
        tag.putInt("ClangingHowlLastTargetId", clangingHowlLastTargetId);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int lerpSteps, boolean teleport) {
        // No-op
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
