package com.uniye.mysticartifacts.client.spear;

import com.uniye.mysticartifacts.item.impl.SpearItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 长矛第三人称姿态：只处理玩家蓄力举起。
 * 静止/挥击使用原版手臂动作；僵尸等生物保持原手臂姿势，由 mob_holding 模型决定矛的朝向。
 */
public final class SpearThirdPersonPose {
    private SpearThirdPersonPose() {
    }

    public static void apply(HumanoidModel<?> model, LivingEntity entity) {
        if (!(entity instanceof Player)) return;

        ItemStack stack = entity.getMainHandItem();
        if (!(stack.getItem() instanceof SpearItem)) return;
        if (!entity.isUsingItem() || !(entity.getUseItem().getItem() instanceof SpearItem)) return;

        HumanoidArm arm = entity.getMainArm();
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        int sign = arm == HumanoidArm.RIGHT ? 1 : -1;

        // 与原版 THROW_SPEAR / 僵尸手臂一致的举起姿态，副手不受影响
        part.xRot = -Mth.HALF_PI + model.head.xRot;
        part.yRot = sign * (model.head.yRot - 0.1F);
        part.zRot = 0.0F;
    }
}
