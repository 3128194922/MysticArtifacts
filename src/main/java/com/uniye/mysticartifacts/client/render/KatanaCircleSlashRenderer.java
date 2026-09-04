package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.entity.KatanaCircleSlashEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class KatanaCircleSlashRenderer extends EntityRenderer<KatanaCircleSlashEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MysticArtifacts.MODID, "textures/entity/katana_circle_slash.png");

    public KatanaCircleSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(KatanaCircleSlashEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float progress = Mth.clamp((entity.tickCount + partialTicks) / 9.0F, 0.0F, 1.0F);
        float alpha = 1.0F - progress * progress;
        float rotation = (entity.tickCount + partialTicks) * 18.0F;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(1.0F, 0.38F, 1.0F);
        RenderType renderType = RenderType.entityTranslucent(TEXTURE);
        KatanaSlashMesh.renderRing(poseStack, buffer.getBuffer(renderType), packedLight,
                alpha * 0.55F, 2.15F, 2.72F);
        KatanaSlashMesh.renderRing(poseStack, buffer.getBuffer(renderType), packedLight,
                alpha, 2.28F, 2.58F);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KatanaCircleSlashEntity entity) {
        return TEXTURE;
    }
}
