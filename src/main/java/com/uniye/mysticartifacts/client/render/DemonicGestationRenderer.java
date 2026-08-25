package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.uniye.mysticartifacts.entity.DemonicGestationEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DemonicGestationRenderer extends EntityRenderer<DemonicGestationEntity> {

    private final ItemRenderer itemRenderer;
    private static final float SCALE = 1.2F;

    public DemonicGestationRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DemonicGestationEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Bobbing animation
        double bob = Math.sin((entity.tickCount + partialTicks) * 0.1) * 0.15;

        if (entity.isDashing()) {
            // Point weapon along the charge direction (yaw set by updateHeading)
            float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
            float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw + 180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
            poseStack.translate(0.0, -0.1 + bob, 0.75);
        } else {
            // Idle — slow spin
            float spin = (entity.tickCount + partialTicks) * 2.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
            poseStack.translate(0.0, 0.2 + bob, 0.0);
        }

        poseStack.scale(SCALE, SCALE, SCALE);

        ItemStack displayItem = entity.getDisplayItem();
        if (!displayItem.isEmpty()) {
            this.itemRenderer.renderStatic(displayItem, ItemDisplayContext.FIXED,
                    packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DemonicGestationEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}