package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uniye.mysticartifacts.client.model.FlagSpearModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class FlagSpearItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final FlagSpearModel<Entity> model;

    public FlagSpearItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
        this.model = new FlagSpearModel<>(modelSet.bakeLayer(FlagSpearModel.LAYER_LOCATION));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
		model.animateBanner((Util.getMillis() % 120000L) * 0.02F);

        RenderType renderType = RenderType.entityCutoutNoCull(FlagSpearModel.TEXTURE);
        VertexConsumer consumer = Minecraft.getInstance().getItemRenderer()
                .getFoilBuffer(bufferSource, renderType, true, stack.hasFoil());
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
