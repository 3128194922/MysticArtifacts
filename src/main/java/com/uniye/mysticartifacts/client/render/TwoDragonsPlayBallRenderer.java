package com.uniye.mysticartifacts.client.render;

import com.uniye.mysticartifacts.entity.TwoDragonsPlayBallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class TwoDragonsPlayBallRenderer extends EntityRenderer<TwoDragonsPlayBallEntity> {

    public TwoDragonsPlayBallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static final ResourceLocation FIRE_TEXTURE = new ResourceLocation("mysticartifacts", "textures/entity/two_dragons_play_ball_fire.png");
    private static final ResourceLocation ICE_TEXTURE = new ResourceLocation("mysticartifacts", "textures/entity/two_dragons_play_ball_ice.png");

    @Override
    public void render(TwoDragonsPlayBallEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        poseStack.translate(0.0D, 0.25D, 0.0D);
        
        float spin = (entity.tickCount + partialTicks) * 40.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        
        poseStack.scale(1.5F, 1.5F, 1.5F);
        
        renderTexture(entity, poseStack, buffer, packedLight);
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    private void renderTexture(TwoDragonsPlayBallEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();
        float size = 0.5f;
        float min = -size;
        float max = size;
        
        vertexConsumer.vertex(pose.pose(), min, min, 0).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0, 0, 1).endVertex();
        vertexConsumer.vertex(pose.pose(), max, min, 0).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0, 0, 1).endVertex();
        vertexConsumer.vertex(pose.pose(), max, max, 0).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0, 0, 1).endVertex();
        vertexConsumer.vertex(pose.pose(), min, max, 0).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0, 0, 1).endVertex();
        
        vertexConsumer.vertex(pose.pose(), min, max, 0).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0, 0, -1).endVertex();
        vertexConsumer.vertex(pose.pose(), max, max, 0).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0, 0, -1).endVertex();
        vertexConsumer.vertex(pose.pose(), max, min, 0).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0, 0, -1).endVertex();
        vertexConsumer.vertex(pose.pose(), min, min, 0).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(pose.normal(), 0, 0, -1).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(TwoDragonsPlayBallEntity entity) {
        return entity.getIsFire() ? FIRE_TEXTURE : ICE_TEXTURE;
    }
}
