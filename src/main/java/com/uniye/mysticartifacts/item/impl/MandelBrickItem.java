package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MandelBrickItem extends Item {
    private static final String TAG_UNLOCKED = "Unlocked";

    public MandelBrickItem(Properties properties) {
        super(properties.rarity(Rarity.EPIC).stacksTo(1));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack other = slot.getItem();
        if (other.isEmpty()) return false;

        if (other.getItem() instanceof QuantumKeyItem) {
            boolean isClient = player.level().isClientSide;

            if (isUnlocked(stack)) {
                return true; 
            }

            boolean expired = QuantumKeyItem.isExpired(other, player.level());
            
            if (!expired) {
                player.playSound(ModSounds.QUANTUM_KEY_UNLOCK.get(), 1.0f, 1.0f);

                other.shrink(1);
                setUnlocked(stack, true);

                if (!isClient) {
                    player.inventoryMenu.broadcastChanges();
                }
            } else {
                player.playSound(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft", "block.note_block.bass")), 1.0f, 0.5f);
            }
            return true;
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isUnlocked(stack)) {
            if (!level.isClientSide) {
                if (player instanceof ServerPlayer serverPlayer) {
                    ResourceLocation lootTableId = new ResourceLocation(MysticArtifacts.MODID, "gameplay/mandel_brick_reward");
                    LootTable lootTable = serverPlayer.server.getLootData().getLootTable(lootTableId);
                    
                    LootParams params = new LootParams.Builder(serverPlayer.serverLevel())
                            .withParameter(LootContextParams.THIS_ENTITY, player)
                            .withParameter(LootContextParams.ORIGIN, player.position())
                            .create(LootContextParamSets.GIFT);

                    List<ItemStack> loot = lootTable.getRandomItems(params);
                    for (ItemStack item : loot) {
                        if (!player.getInventory().add(item)) {
                            player.drop(item, false);
                        }
                    }
                    
                    stack.shrink(1);
                }
            }
            player.playSound(ModSounds.MANDEL_OPEN.get(), 1.0f, 1.0f);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    public static boolean isUnlocked(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_UNLOCKED);
    }

    public static void setUnlocked(ItemStack stack, boolean unlocked) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_UNLOCKED, unlocked);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (isUnlocked(stack)) {
            tooltipComponents.add(Component.translatable("item.mysticartifacts.mandel_brick.unlocked").withStyle(ChatFormatting.GREEN));
        } else {
            tooltipComponents.add(Component.translatable("item.mysticartifacts.mandel_brick.locked").withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
