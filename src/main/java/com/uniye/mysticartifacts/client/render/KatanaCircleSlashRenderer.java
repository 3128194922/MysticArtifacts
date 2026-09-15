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

/** 妖刀范围技：三段错相环斩，不再把直线半月直接拉成圆。 */
public class KatanaCircleSlashRenderer extends EntityRenderer<KatanaCircleSlashEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MysticArtifacts.MODID, "textures/entity/katana_slash.png");
    private static final RenderType COLOR = KatanaRenderTypes.blend(TEXTURE);
    private static final RenderType COLOR_WRITE = KatanaRenderTypes.colorWrite(TEXTURE);
    private static final RenderType LUMINOUS = KatanaRenderTypes.luminous(TEXTURE);
    private static final float FORWARD_ALIGNMENT_DEGREES = 180.0F;
    private static final float LIFETIME = 9.0F;

    public KatanaCircleSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(KatanaCircleSlashEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float progress = Mth.clamp(age / LIFETIME, 0.0F, 1.0F);
        float opening = Mth.clamp(progress / 0.2F, 0.0F, 1.0F);
        float closing = 1.0F - Mth.clamp((progress - 0.66F) / 0.34F, 0.0F, 1.0F);
        float pulse = 0.92F + 0.08F * Mth.sin(age * 1.3F);
        float alpha = opening * closing * pulse;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
                -Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F
                        + FORWARD_ALIGNMENT_DEGREES));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getRotationOffset() + age * 18.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getRotationRoll()));
        poseStack.scale(entity.getBaseSize() * Mth.lerp(opening, 0.84F, 1.12F), 0.2F, 1.0F);

        renderCircleBloodArc(poseStack, buffer, packedLight, alpha, progress, 0.0F, 1.0F);
        renderCircleBloodArc(poseStack, buffer, packedLight, alpha * 0.72F, progress, 120.0F, 0.92F);
        renderCircleBloodArc(poseStack, buffer, packedLight, alpha * 0.48F, progress, 240.0F, 0.84F);
        renderCoreRing(poseStack, buffer, packedLight, alpha, progress);
        poseStack.popPose();
    }

    private static void renderCircleBloodArc(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                             float alpha, float progress, float rotation, float scale) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation - progress * 65.0F));
        poseStack.scale(scale, 1.0F, scale);
        KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(COLOR), packedLight,
                0x25030C, alpha * 0.78F, -0.72F + progress * 0.16F, 1.0F,
                1.0F, 1.2F, 1.35F);
        KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(COLOR_WRITE), packedLight,
                0x9F112A, alpha, -0.34F - progress * 0.11F, 1.0F,
                0.98F, 0.88F, 0.85F);
        poseStack.popPose();
    }

    private static void renderCoreRing(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                       float alpha, float progress) {
        poseStack.pushPose();
        poseStack.scale(0.9F + progress * 0.08F, 1.0F, 0.9F + progress * 0.08F);
        KatanaSlashMesh.renderHalfMoonLayer(poseStack, buffer.getBuffer(LUMINOUS), packedLight,
                0xFFE0E0, alpha * 0.68F, -0.3F - progress * 0.12F, 1.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KatanaCircleSlashEntity entity) {
        return TEXTURE;
    }
}
