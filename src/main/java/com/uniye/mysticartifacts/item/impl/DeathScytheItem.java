package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.client.ClientModEvents;
import com.uniye.mysticartifacts.network.NetworkHandler;
import com.uniye.mysticartifacts.network.DeathScytheSlashPacket;
import com.uniye.mysticartifacts.util.DeathScytheLogic;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;

public class DeathScytheItem extends Item implements GeoItem {
    private static final String TAG_TARGET_UUID = "TargetUUID";
    private static final String TAG_ENERGY_UNTIL = "EnergyUntil";
    private static final String TAG_SLASH_SEQUENCE = "SlashSequence";
    private static final String TAG_ENERGY_DISPLAY_TICKS = "EnergyDisplayTicks";
    private static final String TAG_ENERGY_DURATION_TICKS = "EnergyDurationTicks";
    private static final int ENERGY_BAR_COLOR = 0x79213F;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DeathScytheItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ClientModEvents.registerDeathScytheRenderer(consumer);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "scythe", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.death_scythe.idle")))
                .triggerableAnim("slash", RawAnimation.begin().thenPlay("animation.death_scythe.slash")));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }
        // Player base attack damage is 1.0; the config describes the total with this item held.
        return ImmutableMultimap.of(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                        Config.DeathScytheAttackDamage - 1.0D, AttributeModifier.Operation.ADDITION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public void triggerSlash(ServerPlayer player, ItemStack stack) {
        triggerAnim(player, GeoItem.getOrAssignId(stack, player.serverLevel()), "scythe", "slash");
    }

    public static UUID getTargetUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_TARGET_UUID) ? tag.getUUID(TAG_TARGET_UUID) : null;
    }

    public static void setTargetUUID(ItemStack stack, UUID targetUUID) {
        stack.getOrCreateTag().putUUID(TAG_TARGET_UUID, targetUUID);
    }

    public static void clearTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_TARGET_UUID);
            tag.remove(TAG_ENERGY_UNTIL);
            tag.remove(TAG_SLASH_SEQUENCE);
            tag.remove(TAG_ENERGY_DISPLAY_TICKS);
            tag.remove(TAG_ENERGY_DURATION_TICKS);
        }
    }

    public static long getEnergyUntil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(TAG_ENERGY_UNTIL);
    }

    public static void setEnergyUntil(ItemStack stack, long energyUntil) {
        stack.getOrCreateTag().putLong(TAG_ENERGY_UNTIL, energyUntil);
    }

    private static int getEnergyDurationTicks(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt(TAG_ENERGY_DURATION_TICKS));
    }

    public static int getRemainingEnergyTicks(ItemStack stack, long gameTime) {
        long remaining = getEnergyUntil(stack) - gameTime;
        return (int) Math.max(0L, Math.min(getEnergyDurationTicks(stack), remaining));
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDisplayEnergyTicks(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int duration = Math.max(1, getEnergyDurationTicks(stack));
        return Math.max(0, Math.min(13, 13 * getDisplayEnergyTicks(stack) / duration));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ENERGY_BAR_COLOR;
    }

    private static int getDisplayEnergyTicks(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        int ticks = tag == null ? 0 : tag.getInt(TAG_ENERGY_DISPLAY_TICKS);
        return Math.max(0, Math.min(getEnergyDurationTicks(stack), ticks));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player && !attacker.level().isClientSide) {
            if (target.isAlive()) {
                int energyDurationTicks = Config.DeathScytheEnergyTicks;
                setTargetUUID(stack, target.getUUID());
                setEnergyUntil(stack, attacker.level().getGameTime() + energyDurationTicks);
                stack.getOrCreateTag().putInt(TAG_ENERGY_DURATION_TICKS, energyDurationTicks);
            } else {
                clearTarget(stack);
            }
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!(holder instanceof Player)) {
            return;
        }
        if (level.isClientSide) {
            int remaining = getTargetUUID(stack) == null ? 0
                    : getRemainingEnergyTicks(stack, level.getGameTime());
            CompoundTag tag = stack.getTag();
            if (remaining > 0) {
                if (tag == null || tag.getInt(TAG_ENERGY_DISPLAY_TICKS) != remaining) {
                    stack.getOrCreateTag().putInt(TAG_ENERGY_DISPLAY_TICKS, remaining);
                }
            } else if (tag != null) {
                tag.remove(TAG_ENERGY_DISPLAY_TICKS);
            }
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID targetUUID = getTargetUUID(stack);
        if (targetUUID == null) {
            CompoundTag tag = stack.getTag();
            if (tag != null && (tag.contains(TAG_TARGET_UUID)
                    || tag.contains(TAG_ENERGY_UNTIL) || tag.contains(TAG_SLASH_SEQUENCE)
                    || tag.contains(TAG_ENERGY_DISPLAY_TICKS) || tag.contains(TAG_ENERGY_DURATION_TICKS))) {
                clearTarget(stack);
            }
            return;
        }
        if (!DeathScytheLogic.hasEnergy(getEnergyUntil(stack), level.getGameTime())
                || !(serverLevel.getEntity(targetUUID) instanceof LivingEntity target)
                || !target.isAlive() || target.level() != serverLevel) {
            clearTarget(stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        UUID targetUUID = getTargetUUID(stack);
        if (!DeathScytheLogic.hasEnergy(getEnergyUntil(stack), level.getGameTime())) {
            clearTarget(stack);
            return InteractionResultHolder.fail(stack);
        }
        Entity resolved = targetUUID == null ? null : serverLevel.getEntity(targetUUID);
        if (!(resolved instanceof LivingEntity target) || !target.isAlive() || target.level() != serverLevel) {
            clearTarget(stack);
            return InteractionResultHolder.fail(stack);
        }

        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean damaged = target.hurt(level.damageSources().playerAttack(player), damage);
        if (!damaged) {
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, Config.DeathScytheRightClickCooldown);
        int sequence = DeathScytheLogic.nextSequence(stack.getOrCreateTag().getInt(TAG_SLASH_SEQUENCE));
        stack.getOrCreateTag().putInt(TAG_SLASH_SEQUENCE, sequence);
        triggerSlash(serverPlayer, stack);
        // Task 6 supplies the server-to-client packet; no client data selects the target or damage.
        NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                new DeathScytheSlashPacket(player.getEyePosition(), target.position(), sequence,
                        Config.DeathScytheSlashEffectTicks, level.random.nextLong()));
        if (!target.isAlive()) {
            clearTarget(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
