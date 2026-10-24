package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.client.render.FlagSpearItemRenderer;
import com.uniye.mysticartifacts.entity.FlagSpearEntity;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.network.FlagSpearChargeSyncPacket;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class FlagSpearItem extends SwordItem {
    /** 充能上限：最多 6 层，每层可触发一次左键范围爆发 */
    public static final int MAX_CHARGE = 6;
    public static final String CHARGE_TAG = "FlagSpearCharge";
    /** 投掷出的旗枪实体用于「回收即充能」的虚拟物品标记，防止副本进入背包 */
    public static final String THROWN_MARKER_TAG = "FlagSpearThrown";
    /** 右键投掷后玩家获得的迅捷时长：2s */
    public static final int THROW_SPEED_DURATION_TICKS = 40;
    /** 玩家回收投出的旗枪后获得的迅捷时长：4s */
    public static final int PICKUP_SPEED_DURATION_TICKS = 80;
    private static final int BAR_WIDTH_PIXELS = 13;
    private static final int CHARGE_BAR_COLOR = 0xFFD700;

    public FlagSpearItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Item.Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties.stacksTo(1));
    }

    // ---------------- 充能 ----------------

    public static int getCharge(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.min(MAX_CHARGE, Math.max(0, tag.getInt(CHARGE_TAG)));
    }

    public static void setCharge(ItemStack stack, int charge) {
        stack.getOrCreateTag().putInt(CHARGE_TAG, Math.max(0, Math.min(MAX_CHARGE, charge)));
    }

    /** 投掷实体携带的虚拟物品：只用于渲染，回收时转化为充能而不是掉落实体物品 */
    public static boolean isThrownMarker(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(THROWN_MARKER_TAG);
    }

    /** 打上虚拟标记（副本上操作，不影响玩家手持的那把） */
    public static ItemStack markThrown(ItemStack stack) {
        stack.getOrCreateTag().putBoolean(THROWN_MARKER_TAG, true);
        return stack;
    }

    /** 投出的旗枪外观：固定为无充能外观，避免实体渲染成满充能状态 */
    public static ItemStack createThrownDisplayStack() {
        ItemStack stack = new ItemStack(ModItems.FLAG_SPEAR.get());
        setCharge(stack, 0);
        return stack;
    }

    /**
     * 回收投出的旗枪时给玩家物品栏里的旗枪充能 +1。
     * 主手优先，其次背包内第一把；没有旗枪或已达上限则不生效（充能丢失）。
     *
     * @return 是否成功充能
     */
    public static boolean chargePlayerSpear(Player player) {
        int slot = FlagSpearChargeSyncPacket.findChargeSlot(player, ModItems.FLAG_SPEAR.get());
        if (slot < 0) return false;

        ItemStack target = player.getInventory().items.get(slot);
        int charge = getCharge(target);
        if (charge >= MAX_CHARGE) return false;

        int updated = charge + 1;
        setCharge(target, updated);
        // 立即同步到客户端，让充能条即时刷新
        FlagSpearChargeSyncPacket.send(player, slot, updated, MAX_CHARGE);
        return true;
    }

    // ------ 耐久条用于显示充能，本体无耐久损耗 ------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getCharge(stack) * BAR_WIDTH_PIXELS / (float) MAX_CHARGE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return CHARGE_BAR_COLOR;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 无耐久机制：普攻不损耗耐久
        return true;
    }

    // ---------------- 右键投掷 ----------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            // 快照攻击力：插地后的范围爆发沿用扔出瞬间的攻击力，不依赖投掷者在线
            float strikeDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            FlagSpearEntity spear = new FlagSpearEntity(level, player, stack, strikeDamage);
            spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(spear);

            // 主手旗枪保留：投掷的是「旗枪幻影」，回收后为背包里的旗枪充能而非直接入包
            player.getCooldowns().addCooldown(this, Config.FlagSpearThrowCooldown);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, THROW_SPEED_DURATION_TICKS, 0));
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private FlagSpearItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new FlagSpearItemRenderer();
                }
                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return HumanoidModel.ArmPose.ITEM;
            }
        });
    }
}
