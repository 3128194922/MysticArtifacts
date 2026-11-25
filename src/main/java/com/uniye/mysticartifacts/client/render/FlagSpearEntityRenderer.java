package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.uniye.mysticartifacts.entity.FlagSpearEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 旗枪投射物渲染：按飞行/插住姿态旋转，物品渲染走 FlagSpearItemRenderer（BEWLR），
 * 旗帜飘动动画由其内部驱动。
 */
public class FlagSpearEntityRenderer extends CustomOutlineRenderer<FlagSpearEntity> {
    private final ItemRenderer itemRenderer;

    public FlagSpearEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    protected void renderContents(FlagSpearEntity entity, float entityYaw, float partialTicks,
                                  PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

        this.itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FlagSpearEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
