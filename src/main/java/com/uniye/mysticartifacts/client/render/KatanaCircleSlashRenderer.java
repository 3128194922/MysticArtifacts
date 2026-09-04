package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.entity.KatanaCircleSlashEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

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
        poseStack.pushPose();
        float age = entity.tickCount + partialTicks;
        float fade = Math.max(0.0F, 1.0F - age / 9.0F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 120.0F + age * 20.0F));
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + i * 24.0F));
            poseStack.scale(2.6F, 2.6F, 2.6F);
            renderQuad(poseStack, consumer, packedLight, fade);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void renderQuad(PoseStack poseStack, VertexConsumer consumer, int packedLight, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        float size = 0.5F;
        vertex(consumer, pose, -size, -size, 0.0F, 0.0F, 1.0F, packedLight, alpha);
        vertex(consumer, pose, size, -size, 0.0F, 1.0F, 1.0F, packedLight, alpha);
        vertex(consumer, pose, size, size, 0.0F, 1.0F, 0.0F, packedLight, alpha);
        vertex(consumer, pose, -size, size, 0.0F, 0.0F, 0.0F, packedLight, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int packedLight, float alpha) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, (int) (255.0F * alpha))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(KatanaCircleSlashEntity entity) {
        return TEXTURE;
    }
}
