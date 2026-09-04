package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.entity.KatanaSlashEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class KatanaSlashRenderer extends EntityRenderer<KatanaSlashEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MysticArtifacts.MODID, "textures/entity/katana_slash.png");

    public KatanaSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(KatanaSlashEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float progress = Mth.clamp((entity.tickCount + partialTicks) / 10.0F, 0.0F, 1.0F);
        float alpha = 1.0F - progress * progress;
        float rotation = 60.0F - 200.0F * progress;
        float scale = entity.getStyle() == KatanaSlashEntity.STYLE_DASH ? 1.8F : 1.5F;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(scale, scale * 0.28F, scale);

        RenderType renderType = RenderType.entityTranslucent(TEXTURE);
        KatanaSlashMesh.renderArc(poseStack, buffer.getBuffer(renderType), packedLight,
                alpha * 0.48F, 1.08F, 0.40F);
        KatanaSlashMesh.renderArc(poseStack, buffer.getBuffer(renderType), packedLight,
                alpha, 1.00F, 0.24F);
        KatanaSlashMesh.renderArc(poseStack, buffer.getBuffer(renderType), packedLight,
                alpha * 0.80F, 0.93F, 0.12F);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KatanaSlashEntity entity) {
        return TEXTURE;
    }
}
