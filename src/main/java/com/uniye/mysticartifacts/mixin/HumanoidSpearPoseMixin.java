package com.uniye.mysticartifacts.mixin;

import com.uniye.mysticartifacts.client.spear.SpearThirdPersonPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 玩家与普通人形生物（骷髅、灾厄村民等）的第三人称长矛动作 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidSpearPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void mysticartifacts$spearPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                           float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        // ageInTicks == 0 是第一人称手臂渲染的调用，交给 IClientItemExtensions 处理
        if (ageInTicks <= 0.0F) return;
        SpearThirdPersonPose.apply((HumanoidModel<?>) (Object) this, entity);
    }
}
