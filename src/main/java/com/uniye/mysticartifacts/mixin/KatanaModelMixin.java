package com.uniye.mysticartifacts.mixin;

import com.uniye.mysticartifacts.item.impl.MuramasaItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class KatanaModelMixin<T extends LivingEntity> {

    @Final
    @Shadow
    public ModelPart rightArm;

    @Final
    @Shadow
    public ModelPart leftArm;

    @Inject(method = "poseRightArm", at = @At(value = "HEAD"), cancellable = true)
    private void renderRight(T entity, CallbackInfo info){
        if (entity.isUsingItem() && entity.getUseItem().getItem() instanceof MuramasaItem && entity.getMainArm() == HumanoidArm.RIGHT) {
            this.rightArm.xRot = this.rightArm.xRot * 0.5F - 0.9424778F;
            this.rightArm.yRot = -0.5235988F;
            info.cancel();
        }
    }

    @Inject(method = "poseLeftArm", at = @At(value = "HEAD"), cancellable = true)
    private void renderLeft(T entity, CallbackInfo info){
        if (entity.isUsingItem() && entity.getUseItem().getItem() instanceof MuramasaItem && entity.getMainArm() == HumanoidArm.LEFT) {
            this.leftArm.xRot = this.leftArm.xRot * 0.5F - 0.9424778F;
            this.leftArm.yRot = 0.5235988F;
            info.cancel();
        }
    }
}
