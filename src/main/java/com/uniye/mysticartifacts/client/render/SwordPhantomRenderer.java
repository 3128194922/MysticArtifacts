package com.uniye.mysticartifacts.client.render;

import com.uniye.mysticartifacts.entity.SwordPhantomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

public class SwordPhantomRenderer extends EntityRenderer<SwordPhantomEntity> {
    private final ItemRenderer itemRenderer;
    private final float scale;

    public SwordPhantomRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.scale = 1.0F;
    }

    @Override
    public void render(SwordPhantomEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        boolean stationary = entity.getDeltaMovement().lengthSqr() < 1.0E-6 || entity.isNoPhysics();
        float yaw = stationary ? entity.getYRot() : Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = stationary ? entity.getXRot() : Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
        poseStack.translate(0.0, 0.0, -0.02); 
        poseStack.scale(this.scale, this.scale, this.scale);
        ItemStack itemStack = entity.getDisplayItem();
        this.itemRenderer.renderStatic(itemStack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordPhantomEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
