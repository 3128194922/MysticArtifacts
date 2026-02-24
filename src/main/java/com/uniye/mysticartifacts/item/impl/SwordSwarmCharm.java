package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.util.TagInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class SwordSwarmCharm extends Item implements ICurioItem {
    public SwordSwarmCharm(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack other = slot.getItem();
        if (other.isEmpty()) return false;

        if (canEat(other)) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(other.getItem());
            if (id != null && !hasEaten(stack, id)) {
                other.shrink(1);
                addEaten(stack, id);
                player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_BURP, 1.0f, 1.0f);
                return true;
            }
        }
        return false;
    }

    private boolean canEat(ItemStack stack) {
        if (stack.is(TagInit.IS_SWORD)) return true;
        if (stack.is(TagInit.FORGE_SWORDS)) return true;
        return stack.is(ItemTags.SWORDS);
    }

    private boolean hasEaten(ItemStack stack, ResourceLocation id) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("EatenSwords", Tag.TAG_LIST)) {
            ListTag list = tag.getList("EatenSwords", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                if (list.getString(i).equals(id.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addEaten(ItemStack stack, ResourceLocation id) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list;
        if (tag.contains("EatenSwords", Tag.TAG_LIST)) {
            list = tag.getList("EatenSwords", Tag.TAG_STRING);
        } else {
            list = new ListTag();
            tag.put("EatenSwords", list);
        }
        list.add(StringTag.valueOf(id.toString()));
    }

    public static int getDevouredCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("EatenSwords", Tag.TAG_LIST)) {
            return tag.getList("EatenSwords", Tag.TAG_STRING).size();
        }
        return 0;
    }

    public static List<ResourceLocation> getDevouredList(ItemStack stack) {
        List<ResourceLocation> listOut = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("EatenSwords", Tag.TAG_LIST)) {
            ListTag list = tag.getList("EatenSwords", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) listOut.add(id);
            }
        }
        return listOut;
    }
    
    public static ResourceLocation getCharmDisplayModelId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("CharmDisplayModel", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("CharmDisplayModel"));
            if (id != null) return id;
        }
        ResourceLocation def = ForgeRegistries.ITEMS.getKey(ModItems.KATANA.get());
        return def != null ? def : new ResourceLocation("minecraft", "diamond_sword");
    }
    
    public static void setCharmDisplayModelId(ItemStack stack, ResourceLocation id) {
        stack.getOrCreateTag().putString("CharmDisplayModel", id.toString());
    }
    
    public static ItemStack getCharmDisplayItem(ItemStack stack) {
        ResourceLocation id = getCharmDisplayModelId(stack);
        ItemStack visual = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
        if (visual.isEmpty()) {
            visual = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        }
        return visual;
    }
    
    public static List<ResourceLocation> getDisplayQueue(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrCreateTag();
        List<ResourceLocation> queue = new ArrayList<>();
        if (tag.contains("DisplayQueue", Tag.TAG_LIST)) {
            ListTag list = tag.getList("DisplayQueue", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) queue.add(id);
            }
        }
        if (queue.isEmpty()) {
            if (getDevouredList(stack).isEmpty()) {
                return queue;
            }
            seedQueue(stack, level);
            return getDisplayQueue(stack, level);
        }
        return queue;
    }
    
    public static void seedQueue(ItemStack stack, Level level) {
        List<ResourceLocation> devoured = getDevouredList(stack);
        if (devoured.isEmpty()) return;
        net.minecraft.util.RandomSource random = level.getRandom();
        ResourceLocation exclude = getCharmDisplayModelId(stack);
        ListTag list = new ListTag();
        int count = Math.min(6, devoured.size());
        List<ResourceLocation> pool = new ArrayList<>();
        for (ResourceLocation id : devoured) {
            if (id != null && !id.equals(exclude)) pool.add(id);
        }
        if (pool.isEmpty()) {
            pool.addAll(devoured);
        }
        for (int i = 0; i < count; i++) {
            ResourceLocation pick = pool.get(random.nextInt(pool.size()));
            list.add(StringTag.valueOf(pick.toString()));
        }
        stack.getOrCreateTag().put("DisplayQueue", list);
    }
    
    public static ResourceLocation popNextAndAppendRandom(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrCreateTag();
        List<ResourceLocation> devoured = getDevouredList(stack);
        if (devoured.isEmpty()) return null;
        net.minecraft.util.RandomSource random = level.getRandom();
        ListTag list = tag.contains("DisplayQueue", Tag.TAG_LIST) ? tag.getList("DisplayQueue", Tag.TAG_STRING) : new ListTag();
        ResourceLocation next = null;
        if (!list.isEmpty()) {
            next = ResourceLocation.tryParse(list.getString(0));
            list.remove(0);
        }
        ResourceLocation exclude = getCharmDisplayModelId(stack);
        List<ResourceLocation> pool = new ArrayList<>();
        for (ResourceLocation id : devoured) {
            if (id != null && !id.equals(exclude)) pool.add(id);
        }
        if (pool.isEmpty()) {
            pool.addAll(devoured);
        }
        ResourceLocation append = pool.get(random.nextInt(pool.size()));
        list.add(StringTag.valueOf(append.toString()));
        tag.put("DisplayQueue", list);
        return next != null ? next : append;
    }

    public static boolean isWearing(LivingEntity livingEntity) {
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> !handler.findCurios(ModItems.SWORD_SWARM_CHARM.get()).isEmpty())
                .orElse(false);
    }
}
