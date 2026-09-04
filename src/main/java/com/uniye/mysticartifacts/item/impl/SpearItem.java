package com.uniye.mysticartifacts.item.impl;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import com.uniye.mysticartifacts.init.ModDamageTypes;
import com.uniye.mysticartifacts.util.Ease;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SpearItem extends SwordItem {

    protected static final float HITBOX_MARGIN = 0.3F;
    protected static final int DELAY_TICKS = 10;
    protected static final int CONTACT_COOLDOWN_TICKS = 10;
    protected static final int CHECK_INTERVAL = 3;

    protected final float maxRange;
    protected final float minRange;

    public SpearItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties,
                     float minRange, float maxRange) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties.stacksTo(1));
        this.minRange = minRange;
        this.maxRange = maxRange;
    }

    public SpearItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        this(tier, attackDamageModifier, attackSpeedModifier, properties, 1.5F, 4.5F);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = super.getAttributeModifiers(slot, stack);
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(modifiers);
            builder.put(ForgeMod.ENTITY_REACH.get(),
                    new AttributeModifier("Spear reach", 2.0D, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return modifiers;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        player.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return 72000;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public void onUseTick(Level level, @NotNull LivingEntity user, @NotNull ItemStack stack, int count) {
        if (!(user instanceof Player player) || level.isClientSide()) return;

        int chargeTime = getUseDuration(stack) - count;
        if (chargeTime < DELAY_TICKS) return;
        if (chargeTime % CHECK_INTERVAL != 0) return;

        pierceAttack(level, player, stack);
    }

    protected void pierceAttack(Level level, Player player, ItemStack stack) {
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        for (EntityHitResult entityHitResult : getHitEntitiesAlong(player, HITBOX_MARGIN, e -> canHitEntity(player, e) && isWithinRange(player, e))) {
            Entity entity = entityHitResult.getEntity();

            if (entity instanceof net.minecraftforge.entity.PartEntity<?> partEntity) {
                entity = partEntity.getParent();
            }

            float finalDamage = damage;
            if (entity instanceof LivingEntity livingTarget) {
                finalDamage += EnchantmentHelper.getDamageBonus(stack, livingTarget.getMobType());
            }

            DamageSource source = ModDamageTypes.getSource(level, ModDamageTypes.SPEAR, player, player);

            if (entity.hurt(source, finalDamage)) {
                stack.hurtEnemy(entity instanceof LivingEntity living ? living : null, player);
                if (entity instanceof LivingEntity living) {
                    EnchantmentHelper.doPostHurtEffects(living, player);
                }
                EnchantmentHelper.doPostDamageEffects(player, entity);
                player.setLastHurtMob(entity);
                player.stopUsingItem();
                player.getCooldowns().addCooldown(this, CONTACT_COOLDOWN_TICKS);
            }
        }
    }

    protected Collection<EntityHitResult> getHitEntitiesAlong(LivingEntity livingEntity, float f, Predicate<Entity> predicate) {
        Vec3 vec3 = getHeadLookAngle(livingEntity);
        Vec3 vec32 = livingEntity.getEyePosition();
        Vec3 vec33 = vec32.add(vec3.scale(effectiveMinRange(livingEntity)));
        double d = getKnownMovement(livingEntity).dot(vec3);
        Vec3 vec34 = vec32.add(vec3.scale((double) effectiveMaxRange(livingEntity) + Math.max(0.0, d)));
        return getHitEntitiesAlong(vec33, livingEntity, predicate, vec34, f, ClipContext.Block.COLLIDER)
                .map(blockHitResult -> List.of(), collection -> collection);
    }

    private static Either<BlockHitResult, Collection<EntityHitResult>> getHitEntitiesAlong(Vec3 vec3, Entity entity, Predicate<Entity> predicate, Vec3 vec32, float f, ClipContext.Block block) {
        Level level = entity.level();
        BlockHitResult blockHitResult = clipIncludingBorder(new ClipContext(vec3, vec32, block, ClipContext.Fluid.NONE, entity), level);
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            vec32 = blockHitResult.getLocation();
        }
        Collection<EntityHitResult> collection = getManyEntityHitResult(level, entity, vec3, vec32,
                AABB.ofSize(vec3, f, f, f).expandTowards(vec32.subtract(vec3)).inflate(1.0), predicate, f);
        if (!collection.isEmpty()) {
            return Either.right(collection);
        }
        return Either.left(blockHitResult);
    }

    private static Collection<EntityHitResult> getManyEntityHitResult(Level level, Entity entity, Vec3 vec32, Vec3 vec33, AABB aABB, Predicate<Entity> predicate, float f) {
        List<EntityHitResult> arrayList = new ArrayList<>();

        if (level == null || level.isClientSide() || entity == null || vec32 == null || vec33 == null) {
            return arrayList;
        }

        List<Entity> entities;
        try {
            entities = new ArrayList<>(level.getEntities(entity, aABB, predicate));
        } catch (Exception e) {
            return arrayList;
        }

        for (Entity entity2 : entities) {
            if (entity2 == null || entity2.isRemoved() || !entity2.isAlive()) continue;

            try {
                AABB aABB2 = entity2.getBoundingBox().inflate(f);
                if (aABB2.contains(vec32)) {
                    arrayList.add(new EntityHitResult(entity2, vec32));
                    continue;
                }
                Optional<Vec3> optional = aABB2.clip(vec32, vec33);
                optional.ifPresent(vec3 -> arrayList.add(new EntityHitResult(entity2, vec3)));
            } catch (Exception e) {
            }
        }

        return arrayList;
    }

    private static BlockHitResult clipIncludingBorder(ClipContext clipContext, Level level) {
        BlockHitResult blockHitResult = level.clip(clipContext);
        WorldBorder worldBorder = level.getWorldBorder();
        if (worldBorder.isWithinBounds(BlockPos.containing(clipContext.getFrom())) && !worldBorder.isWithinBounds(BlockPos.containing(blockHitResult.getLocation()))) {
            Vec3 vec3 = blockHitResult.getLocation().subtract(clipContext.getFrom());
            Direction direction = getApproximateNearest(vec3.x, vec3.y, vec3.z);
            Vec3 vec32 = clampVec3ToBound(blockHitResult.getLocation(), worldBorder);
            return new BlockHitResult(vec32, direction, BlockPos.containing(vec32), false);
        }
        return blockHitResult;
    }

    private static Vec3 clampVec3ToBound(Vec3 vec3, WorldBorder worldBorder) {
        return clampVec3ToBound(vec3.x, vec3.y, vec3.z, worldBorder);
    }

    private static Vec3 clampVec3ToBound(double d, double d2, double d3, WorldBorder worldBorder) {
        return new Vec3(Mth.clamp(d, worldBorder.getMinX(), worldBorder.getMaxX() - 1.0E-5f), d2, Mth.clamp(d3, worldBorder.getMinZ(), worldBorder.getMaxZ() - 1.0E-5f));
    }

    private static Direction getApproximateNearest(double d, double d2, double d3) {
        return getApproximateNearest((float) d, (float) d2, (float) d3);
    }

    private static Direction getApproximateNearest(float f, float f2, float f3) {
        Direction direction = Direction.NORTH;
        float f4 = Float.MIN_VALUE;
        for (Direction direction2 : Direction.values()) {
            float f5 = f * (float) direction2.getStepX() + f2 * (float) direction2.getStepY() + f3 * (float) direction2.getStepZ();
            if (f5 > f4) {
                f4 = f5;
                direction = direction2;
            }
        }
        return direction;
    }

    private float effectiveMinRange(Entity entity) {
        return minRange;
    }

    private float effectiveMaxRange(Entity entity) {
        float reachBonus = 0;
        if (entity instanceof LivingEntity living) {
            reachBonus = living.getAttribute(ForgeMod.ENTITY_REACH.get()) != null
                    ? (float) Math.max(0, living.getAttributeValue(ForgeMod.ENTITY_REACH.get()) - 3) : 0;
        }
        return maxRange + reachBonus;
    }

    protected boolean isWithinRange(Entity attacker, Entity target) {
        float range = effectiveMaxRange(attacker) + HITBOX_MARGIN + 1.0F;
        return attacker.distanceToSqr(target) <= range * range;
    }

    protected static boolean canHitEntity(Entity entity, Entity entity2) {
        if (!entity2.canBeHitByProjectile()) return false;
        if (entity2 instanceof Player player) {
            if (entity instanceof Player && !((Player) entity).canHarmPlayer(player)) return false;
        }
        if (entity.level().clip(new ClipContext(entity.getEyePosition(), entity2.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.MISS) {
            return false;
        }
        return !entity.isPassengerOfSameVehicle(entity2);
    }

    private static Vec3 getKnownMovement(Entity entity) {
        return entity.getDeltaMovement();
    }

    private static Vec3 getHeadLookAngle(Entity entity) {
        return calculateViewVector(entity.getXRot(), entity.getYHeadRot());
    }

    private static Vec3 calculateViewVector(float xRot, float yRot) {
        float f = xRot * 0.017453292F;
        float f1 = -yRot * 0.017453292F;
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (attacker instanceof Player player && !attacker.level().isClientSide) {
            leftClickPierceAttack(attacker.level(), player);
        }
        return false;
    }

    private void leftClickPierceAttack(Level level, Player player) {
        ItemStack stack = player.getMainHandItem();
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 lookVec = getHeadLookAngle(player);

        for (EntityHitResult entityHitResult : getHitEntitiesAlong(player, HITBOX_MARGIN, e -> canHitEntity(player, e) && isWithinRange(player, e))) {
            Entity entity = entityHitResult.getEntity();

            if (entity instanceof net.minecraftforge.entity.PartEntity<?> partEntity) {
                entity = partEntity.getParent();
            }

            float finalDamage = damage;
            if (entity instanceof LivingEntity livingTarget) {
                finalDamage += EnchantmentHelper.getDamageBonus(stack, livingTarget.getMobType());
            }

            DamageSource source = ModDamageTypes.getSource(level, ModDamageTypes.SPEAR, player, player);

            if (entity.hurt(source, finalDamage)) {
                if (entity instanceof LivingEntity living) {
                    EnchantmentHelper.doPostHurtEffects(living, player);
                }
                EnchantmentHelper.doPostDamageEffects(player, entity);
                player.setLastHurtMob(entity);
            }
        }

        player.push(lookVec.x * 0.38, 0.0, lookVec.z * 0.38);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity user, int timeLeft) {
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new SpearClient(this));
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.SWEEPING_EDGE) return false;
        if (enchantment.category == EnchantmentCategory.WEAPON) return true;
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return super.isBookEnchantable(stack, book)
                && !EnchantmentHelper.getEnchantments(book).containsKey(Enchantments.SWEEPING_EDGE);
    }

    public static class SpearClient implements IClientItemExtensions {
        private final SpearItem spear;

        public SpearClient(SpearItem spear) {
            this.spear = spear;
        }

        private static float progress(float f, float f2, float f3) {
            return Mth.clamp(Mth.inverseLerp(f, f2, f3), 0.0f, 1.0f);
        }

        private static float hitFeedbackAmount(float f) {
            return 0.4f * (Ease.outQuart(progress(f, 1.0f, 3.0f)) - Ease.inOutSine(progress(f, 3.0f, 10.0f)));
        }

        private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equipProcess) {
            int i = arm == HumanoidArm.RIGHT ? 1 : -1;
            poseStack.translate((double)((float)i * 0.56F), (double)(-0.52F + equipProcess * -0.6F), (double)-0.72F);
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                               ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
            int dir = arm == HumanoidArm.RIGHT ? 1 : -1;
            if (player.isUsingItem() && player.getUseItem() == itemInHand) {
                this.applyItemArmTransform(poseStack, arm, 0);
                float useTicks = itemInHand.getUseDuration() - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
                firstPersonUse(poseStack, useTicks, arm, dir);
                return true;
            } else if (swingProcess > 0.0F) {
                this.applyItemArmTransform(poseStack, arm, 0);
                firstPersonAttack(swingProcess, poseStack, dir, arm);
                return true;
            } else {
                this.applyItemArmTransform(poseStack, arm, equipProcess);
            }
            return true;
        }

        private void firstPersonUse(PoseStack poseStack, float useTicks, HumanoidArm arm, int dir) {
            int delay = 10;
            float raiseProgress = progress(useTicks, 0.0f, delay);
            float raiseIn = Ease.inOutBack(raiseProgress);
            float sway = Mth.sin(useTicks * 25.0f * Mth.DEG_TO_RAD) * 0.3f * (1.0f - raiseProgress);

            poseStack.translate(
                    (double)((float)dir * (raiseProgress * 0.15f + sway * 0.005f)),
                    (double)(raiseProgress * -0.075f),
                    0.0
            );
            poseStack.rotateAround(Axis.XP.rotationDegrees(-65.0f * raiseIn), 0.0f, 0.1f, 0.0f);
            poseStack.rotateAround(Axis.YN.rotationDegrees((float)dir * -90.0f * progress(raiseProgress, 0.5f, 0.55f)), (float)dir * 0.15f, 0.0f, 0.0f);
        }

        private void firstPersonAttack(float swingProcess, PoseStack poseStack, int dir, HumanoidArm arm) {
            float f2 = Ease.inOutSine(progress(swingProcess, 0.0f, 0.05f));
            float f3 = Ease.outBack(progress(swingProcess, 0.05f, 0.2f));
            float f4 = Ease.inOutExpo(progress(swingProcess, 0.4f, 1.0f));
            poseStack.translate((float)dir * 0.1f * (f2 - f3), -0.075f * (f2 - f4), 0.65f * (f2 - f3));
            poseStack.mulPose(Axis.XP.rotationDegrees(-70.0f * (f2 - f4)));
            poseStack.translate(0.0, 0.0, -0.25 * (double)(f4 - f3));
        }
    }
}
