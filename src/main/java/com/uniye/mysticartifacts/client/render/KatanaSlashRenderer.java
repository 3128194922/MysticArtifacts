package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.entity.KatanaSlashEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

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
        poseStack.pushPose();
        float age = entity.tickCount + partialTicks;
        float fade = Math.max(0.0F, 1.0F - age / 10.0F);
        float scale = entity.getStyle() == KatanaSlashEntity.STYLE_DASH ? 2.6F : 2.1F;
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + age * 18.0F));
        poseStack.scale(scale, scale, scale);
        renderQuad(poseStack, buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)),
                packedLight, fade, 0.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void renderQuad(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                   float alpha, float rotation) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        PoseStack.Pose pose = poseStack.last();
        float size = 0.5F;
        vertex(consumer, pose, -size, -size, 0.0F, 0.0F, 1.0F, packedLight, alpha, 1.0F);
        vertex(consumer, pose, size, -size, 0.0F, 1.0F, 1.0F, packedLight, alpha, 1.0F);
        vertex(consumer, pose, size, size, 0.0F, 1.0F, 0.0F, packedLight, alpha, 1.0F);
        vertex(consumer, pose, -size, size, 0.0F, 0.0F, 0.0F, packedLight, alpha, 1.0F);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int packedLight, float alpha, float normalZ) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, (int) (255.0F * alpha))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 0.0F, normalZ)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(KatanaSlashEntity entity) {
        return TEXTURE;
    }
}
