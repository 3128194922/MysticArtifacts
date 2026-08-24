package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 先祖的信：三态饰品（正常 / 美德 / 折磨）。
 * 状态保存在佩戴者持久化 NBT 中：
 * - 正常：受到致死伤害时免疫一次并立即判定，50% 进入美德，50% 进入折磨。
 * - 美德：受伤 -25%，死亡时 75% 概率拒绝死亡（回满血并回到正常），最多持续一个游戏日。
 * - 折磨：受伤 +15%，受伤时 25% 概率直接死亡（无视图腾），且无法取下本饰品。
 * 死亡、睡觉会回到正常状态。
 */
public class AncestorsLetterItem extends Item implements ICurioItem {

    public static final int STATE_NORMAL = 0;
    public static final int STATE_VIRTUE = 1;
    public static final int STATE_TORMENT = 2;

    public static final String KEY_STATE = "AncestorsLetterState";
    public static final String KEY_VIRTUE_END = "AncestorsLetterVirtueEnd";

    // 客户端：由网络包写入的本地玩家状态，用于 HUD 图标与饰品槽 UI 判断
    public static volatile int clientState = STATE_NORMAL;

    public AncestorsLetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return true;
    }

    // 折磨状态下无法卸下饰品
    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (entity == null) return true;
        if (entity.level().isClientSide) {
            return clientState != STATE_TORMENT;
        }
        return getState(entity) != STATE_TORMENT;
    }

    public static boolean isWearing(LivingEntity livingEntity) {
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> !handler.findCurios(ModItems.ANCESTORS_LETTER.get()).isEmpty())
                .orElse(false);
    }

    public static int getState(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(KEY_STATE) ? data.getInt(KEY_STATE) : STATE_NORMAL;
    }

    public static void setState(LivingEntity entity, int state) {
        entity.getPersistentData().putInt(KEY_STATE, state);
    }

    public static boolean hasState(LivingEntity entity) {
        return entity.getPersistentData().contains(KEY_STATE);
    }

    public static long getVirtueEnd(LivingEntity entity) {
        return entity.getPersistentData().getLong(KEY_VIRTUE_END);
    }

    public static void setVirtueEnd(LivingEntity entity, long gameTime) {
        entity.getPersistentData().putLong(KEY_VIRTUE_END, gameTime);
    }

    public static void clearState(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(KEY_STATE);
        data.remove(KEY_VIRTUE_END);
    }
}
