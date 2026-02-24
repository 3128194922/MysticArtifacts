package com.uniye.mysticartifacts.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.uniye.mysticartifacts.item.impl.MuramasaItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class KatanaRenderMixin {

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V", shift = At.Shift.AFTER))
    private void renderKatanaBlock(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack matrices, MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
        if (stack.getItem() instanceof MuramasaItem && player.isUsingItem() && player.getUseItemRemainingTicks() > 0) {
             boolean flag = (hand == InteractionHand.MAIN_HAND);
             HumanoidArm arm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
             
             int horizontal = arm == HumanoidArm.RIGHT ? 1 : -1;
             matrices.translate((float)horizontal * -0.14142136F, 0.08F, 0.14142136F);
             matrices.mulPose(Axis.XP.rotationDegrees(-102.25F));
             matrices.mulPose(Axis.YP.rotationDegrees((float)horizontal * 13.365F));
             matrices.mulPose(Axis.ZP.rotationDegrees((float)horizontal * 78.05F));
        }
    }
}
