package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.entity.ArtifactSpiritEntity;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class ArtifactSpiritItem extends Item implements ICurioItem {

    private static final String TAG_STORED_WEAPON = "StoredWeapon";
    private static final ResourceLocation SCORCHER_ID = ResourceLocation.fromNamespaceAndPath("dungeonnowloading", "scorcher");
    private static final ResourceLocation SOUL_SCORCHER_ID = ResourceLocation.fromNamespaceAndPath("dungeonnowloading", "soul_scorcher");

    public ArtifactSpiritItem(Properties properties) {
        super(properties);
    }

    // ========== Weapon Compatibility ==========

    public static boolean isSupportedWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() instanceof BowItem) return true;
        if (stack.getItem() instanceof CrossbowItem) return true;
        if (stack.getItem() instanceof SplashPotionItem) return true;
        if (stack.getItem() instanceof LingeringPotionItem) return true;

        // PotatoCannonItem from Create — safe check without compile dependency
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && "create".equals(id.getNamespace()) && "potato_cannon".equals(id.getPath())) {
            return true;
        }

        // Scorcher from DungeonNowLoading — safe check without compile dependency
        if (isScorcher(stack)) return true;

        return false;
    }

    private static boolean isScorcher(ItemStack stack) {
        if (!ModList.get().isLoaded("dungeonnowloading") || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return SCORCHER_ID.equals(id) || SOUL_SCORCHER_ID.equals(id);
    }

    private static boolean isSoulScorcher(ItemStack stack) {
        if (!ModList.get().isLoaded("dungeonnowloading") || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return SOUL_SCORCHER_ID.equals(id);
    }

    // ========== NBT Helpers ==========

    public static ResourceLocation getStoredWeapon(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_STORED_WEAPON, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_STORED_WEAPON));
            if (id != null) return id;
        }
        return null;
    }

    public static boolean hasStoredWeapon(ItemStack stack) {
        return getStoredWeapon(stack) != null;
    }

    public static void setStoredWeapon(ItemStack stack, ResourceLocation weaponId) {
        stack.getOrCreateTag().putString(TAG_STORED_WEAPON, weaponId.toString());
    }

    // ========== Ammo Compatibility ==========

    public static boolean isAmmoForWeapon(ItemStack ammo, ResourceLocation weaponId) {
        if (ammo.isEmpty() || weaponId == null) return false;
        Item weaponItem = ForgeRegistries.ITEMS.getValue(weaponId);
        if (weaponItem == null) return false;

        // Bow → arrows
        if (weaponItem instanceof BowItem) {
            return ammo.getItem() instanceof net.minecraft.world.item.ArrowItem;
        }
        // Crossbow → arrows, firework rockets, or firework-tag arrows
        if (weaponItem instanceof CrossbowItem) {
            return ammo.getItem() instanceof net.minecraft.world.item.ArrowItem
                    || ammo.is(net.minecraft.world.item.Items.FIREWORK_ROCKET)
                    || ammo.is(net.minecraft.tags.ItemTags.create(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "firework")));
        }
        // Potions → matching potion type (any potion of same class)
        if (weaponItem instanceof SplashPotionItem) {
            return ammo.getItem() instanceof SplashPotionItem;
        }
        if (weaponItem instanceof LingeringPotionItem) {
            return ammo.getItem() instanceof LingeringPotionItem;
        }
        // PotatoCannon → valid ammo check done at fire time (needs level.registryAccess())
        // Here we do a loose pre-check: Create must be loaded and item must not be empty
        if ("create".equals(weaponId.getNamespace()) && "potato_cannon".equals(weaponId.getPath())) {
            return !ammo.isEmpty() && ModList.get().isLoaded("create");
        }
        // Scorcher → coal/charcoal
        if (SCORCHER_ID.equals(weaponId) || SOUL_SCORCHER_ID.equals(weaponId)) {
            return ammo.is(net.minecraft.world.item.Items.COAL)
                    || ammo.is(net.minecraft.world.item.Items.CHARCOAL);
        }
        return false;
    }

    // ========== ICurioItem Implementation ==========

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return hasStoredWeapon(stack);
    }

    @Override
    public void onEquip(SlotContext context, ItemStack prevStack, ItemStack stack) {
        if (context.entity().level().isClientSide) return;
        if (!(context.entity() instanceof Player player)) return;
        if (!hasStoredWeapon(stack)) return;

        // Remove any existing spirit entity for this player
        removeSpiritFor(player);

        // Spawn new spirit entity
        ResourceLocation weaponId = getStoredWeapon(stack);
        ArtifactSpiritEntity spirit = new ArtifactSpiritEntity(player, weaponId);
        player.level().addFreshEntity(spirit);
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        if (context.entity().level().isClientSide) return;
        if (!(context.entity() instanceof Player player)) return;

        removeSpiritFor(player);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;

        ItemStack other = slot.getItem();
        if (other.isEmpty()) return false;

        if (isSupportedWeapon(other)) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(other.getItem());
            if (id != null) {
                if (hasStoredWeapon(stack)) {
                    ResourceLocation oldId = getStoredWeapon(stack);
                    Item oldWeapon = ForgeRegistries.ITEMS.getValue(oldId);
                    if (oldWeapon != null) {
                        player.getInventory().placeItemBackInInventory(new ItemStack(oldWeapon));
                    }
                    other.shrink(1);
                    setStoredWeapon(stack, id);
                    player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.1F);
                } else {
                    other.shrink(1);
                    setStoredWeapon(stack, id);
                    player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
                }
                return true;
            }
        }
        return false;
    }

    // ========== Spirit Entity Management ==========

    private static void removeSpiritFor(Player player) {
        Level level = player.level();
        List<ArtifactSpiritEntity> spirits = level.getEntitiesOfClass(
                ArtifactSpiritEntity.class,
                player.getBoundingBox().inflate(64.0),
                e -> player.getUUID().equals(e.getOwnerUUID())
        );
        for (ArtifactSpiritEntity spirit : spirits) {
            spirit.discard();
        }
    }

    public static boolean isWearing(LivingEntity livingEntity) {
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> !handler.findCurios(ModItems.ARTIFACT_SPIRIT.get()).isEmpty())
                .orElse(false);
    }
}
