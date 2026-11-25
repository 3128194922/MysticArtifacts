package com.uniye.mysticartifacts.mixin;

import com.uniye.mysticartifacts.item.impl.SpearItem;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 僵尸类模型默认把手臂固定在 -90°，长矛会横向挂在手边。
 * 持矛时改为参考 Backported Spears 的持矛手臂姿态：手臂约 -44°，攻击时下压前刺。
 */
@Mixin(AbstractZombieModel.class)
public abstract class ZombieSpearPoseMixin<T extends Monster> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/Monster;FFFFF)V", at = @At("TAIL"))
    private void mysticartifacts$spearArmPose(T entity, float limbSwing, float limbSwingAmount,
                                              float ageInTicks, float netHeadYaw, float headPitch,
                                              CallbackInfo ci) {
        AbstractZombieModel<T> model = (AbstractZombieModel<T>) (Object) this;
        applyArmPose(model, entity, HumanoidArm.RIGHT);
        applyArmPose(model, entity, HumanoidArm.LEFT);
    }

    private static <T extends Monster> void applyArmPose(AbstractZombieModel<T> model, T entity, HumanoidArm arm) {
        EquipmentSlot slot = entity.getMainArm() == arm ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        ItemStack stack = entity.getItemBySlot(slot);
        if (!(stack.getItem() instanceof SpearItem)) return;

        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.xRot = model.head.xRot - Mth.HALF_PI + 0.8F;
        part.yRot = -0.1F * model.head.yRot;
        part.zRot = 0.0F;

        if (model.attackTime > 0.0F) {
            part.xRot -= Mth.sin(model.attackTime * Mth.PI) * 0.8F;
        }
    }
}
