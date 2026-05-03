package com.uniye.mysticartifacts.item.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FireworkGauntletItem extends Item {
    public static final String TAG_LOADED_FIREWORK = "LoadedFirework";
    public static final String TAG_DASH_TICKS = "FireworkGauntletDashTicks";
    public static final String TAG_DASH_FLIGHT = "FireworkGauntletDashFlight";
    public static final String TAG_DASH_X = "FireworkGauntletDashX";
    public static final String TAG_DASH_Y = "FireworkGauntletDashY";
    public static final String TAG_DASH_Z = "FireworkGauntletDashZ";
    public static final String TAG_DASH_HIT_IDS = "FireworkGauntletHitIds";

    private static final int COOLDOWN_TICKS = 120;

    public FireworkGauntletItem(Properties properties) {
        super(properties.stacksTo(1).durability(1024).fireResistant());
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        ItemStack other = slot.getItem();
        if (!other.is(Items.FIREWORK_ROCKET)) {
            return false;
        }

        boolean hadLoaded = hasLoadedFirework(stack);
        ItemStack allRockets = other.copy();
        stack.getOrCreateTag().put(TAG_LOADED_FIREWORK, allRockets.save(new CompoundTag()));
        other.setCount(0);
        player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, hadLoaded ? 1.1F : 0.9F);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        ItemStack gauntlet = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(gauntlet);
        }

        ItemStack loadedFirework = getLoadedFirework(gauntlet);
        if (loadedFirework.isEmpty()) {
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            return InteractionResultHolder.fail(gauntlet);
        }

        int flight = getFlight(loadedFirework);
        int dashTicks = flight * 20;

        if (!level.isClientSide) {
            CompoundTag data = player.getPersistentData();
            data.putInt(TAG_DASH_TICKS, dashTicks);
            data.putInt(TAG_DASH_FLIGHT, flight);
            data.putDouble(TAG_DASH_X, player.getLookAngle().x);
            data.putDouble(TAG_DASH_Y, player.getLookAngle().y);
            data.putDouble(TAG_DASH_Z, player.getLookAngle().z);
            data.putIntArray(TAG_DASH_HIT_IDS, new int[0]);

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            gauntlet.hurtAndBreak(1, player, breaker -> breaker.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            consumeLoadedFirework(gauntlet);
        }

        return InteractionResultHolder.consume(gauntlet);
    }

    public static boolean hasLoadedFirework(ItemStack stack) {
        return stack.hasTag() && stack.getTag() != null && stack.getTag().contains(TAG_LOADED_FIREWORK, 10);
    }

    public static ItemStack getLoadedFirework(ItemStack stack) {
        if (!hasLoadedFirework(stack)) {
            return ItemStack.EMPTY;
        }
        CompoundTag fireworkTag = stack.getTag().getCompound(TAG_LOADED_FIREWORK);
        return ItemStack.of(fireworkTag);
    }

    private static void clearLoadedFirework(ItemStack stack) {
        if (stack.hasTag() && stack.getTag() != null) {
            stack.getTag().remove(TAG_LOADED_FIREWORK);
        }
    }

    private static void consumeLoadedFirework(ItemStack stack) {
        ItemStack loaded = getLoadedFirework(stack);
        if (loaded.isEmpty()) {
            clearLoadedFirework(stack);
            return;
        }
        loaded.shrink(1);
        if (loaded.isEmpty()) {
            clearLoadedFirework(stack);
            return;
        }
        stack.getOrCreateTag().put(TAG_LOADED_FIREWORK, loaded.save(new CompoundTag()));
    }

    public static int getFlight(ItemStack firework) {
        CompoundTag tag = firework.getTag();
        if (tag == null || !tag.contains("Fireworks", 10)) {
            return 1;
        }
        CompoundTag fireworksTag = tag.getCompound("Fireworks");
        int raw = fireworksTag.contains("Flight", 1) ? fireworksTag.getByte("Flight") : 1;
        return Math.max(1, raw);
    }
}
