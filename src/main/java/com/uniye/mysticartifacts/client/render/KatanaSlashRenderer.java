package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.entity.KatanaSlashEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 武士刀直线剑气：普通冲刺是血刃，妖刀飞斩带残影和脉冲核心。 */
public class KatanaSlashRenderer extends EntityRenderer<KatanaSlashEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MysticArtifacts.MODID, "textures/entity/katana_slash.png");
    private static final RenderType COLOR = KatanaRenderTypes.blend(TEXTURE);
    private static final RenderType COLOR_WRITE = KatanaRenderTypes.colorWrite(TEXTURE);
    private static final RenderType LUMINOUS = KatanaRenderTypes.luminous(TEXTURE);
    private static final float FORWARD_ALIGNMENT_DEGREES = 180.0F;
    private static final float DEFAULT_LIFETIME = 10.0F;
    private static final float DEFAULT_GHOST_LIFETIME = 12.0F;
    private static final int AFTERIMAGE_COUNT = 3;

    public KatanaSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(KatanaSlashEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float lifetime = entity.getStyle() == KatanaSlashEntity.STYLE_GHOST
                ? (Config.KatanaGhostSlashRange > 0.0D
                ? (float) Config.KatanaGhostSlashRange
                : DEFAULT_GHOST_LIFETIME)
                : DEFAULT_LIFETIME;
        float progress = Mth.clamp(age / lifetime, 0.0F, 1.0F);
        float opening = Mth.clamp(progress / 0.16F, 0.0F, 1.0F);
        float closing = 1.0F - Mth.clamp((progress - 0.56F) / 0.44F, 0.0F, 1.0F);
        float glowPulse = 0.9F + 0.1F * Mth.sin(age * 3.8F + entity.getId() * 0.17F);
        float baseAlpha = opening * closing * (0.91F + 0.09F * glowPulse);
        int segments = renderSegments(entity);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
                -Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F
                        + FORWARD_ALIGNMENT_DEGREES));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getRotationRoll()));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getRotationOffset() - 135.0F * progress));
        poseStack.scale(1.0F, 0.25F, 1.0F);
        poseStack.scale(entity.getBaseSize() * Mth.lerp(opening, 0.92F, 1.08F), 1.0F, 1.0F);

        if (entity.getStyle() == KatanaSlashEntity.STYLE_GHOST) {
            renderEchoArc(poseStack, buffer, packedLight, baseAlpha, progress, 0.16F, -0.13F, segments);
            renderEchoArc(poseStack, buffer, packedLight, baseAlpha, progress, 0.08F, 0.11F, segments);
            renderAfterimages(poseStack, buffer, packedLight, baseAlpha, progress, glowPulse, segments);
            KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(COLOR), packedLight,
                    0x26030D, baseAlpha * 0.88F, -0.78F + progress * 0.22F, 1.08F,
                    1.08F, 1.35F, 1.25F, segments);
            KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(COLOR_WRITE), packedLight,
                    0xA8112E, baseAlpha, -0.35F - progress * 0.12F, 1.0F,
                    1.0F, 1.0F, 1.0F, segments);
            KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(LUMINOUS), packedLight,
                    0xFF4D62, baseAlpha * 0.8F, -0.48F - progress * 0.16F, 1.0F,
                    0.96F, 0.58F, 0.65F, segments);
            renderCoreArc(poseStack, buffer, packedLight, baseAlpha, progress, 0.9F, segments);
            KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(LUMINOUS), packedLight,
                    0xFFFFFF, baseAlpha * (0.42F + glowPulse * 0.2F), -0.22F - progress * 0.12F, 0.92F,
                    0.78F, 0.34F, 0.16F, segments);
        } else {
            KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(COLOR), packedLight,
                    0x21030B, baseAlpha * 0.82F, -0.72F + progress * 0.18F, 1.0F,
                    1.0F, 1.15F, 1.0F, segments);
            KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(COLOR_WRITE), packedLight,
                    0x8E1025, baseAlpha, -0.34F - progress * 0.1F, 1.0F,
                    0.98F, 0.9F, 0.75F, segments);
            KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(LUMINOUS), packedLight,
                    0xFF4355, baseAlpha * 0.72F, -0.46F - progress * 0.12F, 1.0F,
                    0.94F, 0.48F, 0.45F, segments);
            renderCoreArc(poseStack, buffer, packedLight, baseAlpha * 0.88F, progress, 0.78F, segments);
        }
        poseStack.popPose();
    }

    private static void renderAfterimages(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                          float alpha, float progress, float glowPulse, int segments) {
        for (int index = 1; index <= AFTERIMAGE_COUNT; index++) {
            float trail = index / (float) (AFTERIMAGE_COUNT + 1);
            float trailAlpha = alpha * (0.28F - trail * 0.045F) * (0.84F + glowPulse * 0.16F);
            poseStack.pushPose();
            poseStack.translate(-trail * 5.0D, trail * 0.06D, trail * 1.4D);
            poseStack.mulPose(Axis.YP.rotationDegrees(trail * 18.0F - progress * 9.0F));
            poseStack.scale(1.0F - trail * 0.04F, 1.0F, 1.0F - trail * 0.04F);
            KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(COLOR), packedLight,
                    0x650817, trailAlpha, -0.64F + trail * 0.12F, 1.0F,
                    1.0F, 0.8F, 1.2F, segments);
            KatanaSlashMesh.renderHalfMoonLayer(poseStack, buffer.getBuffer(LUMINOUS), packedLight,
                    0xFF3154, trailAlpha * 0.42F, -0.28F + trail * 0.1F, 1.0F, segments);
            poseStack.popPose();
        }
    }

    private static void renderEchoArc(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                      float alpha, float progress, float scale, float offset, int segments) {
        poseStack.pushPose();
        poseStack.translate(0.0D, offset, 0.0D);
        poseStack.scale(scale, 1.0F, scale);
        KatanaSlashMesh.renderBloodArcLayer(poseStack, buffer.getBuffer(COLOR), packedLight,
                0x4C0716, alpha * 0.22F, -0.68F + progress * 0.15F, 1.0F,
                1.0F, 0.7F, 1.55F, segments);
        poseStack.popPose();
    }

    private static void renderCoreArc(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                      float alpha, float progress, float scale, int segments) {
        poseStack.pushPose();
        poseStack.scale(scale, 1.0F, scale);
        KatanaSlashMesh.renderHalfMoonLayer(poseStack, buffer.getBuffer(LUMINOUS), packedLight,
                0xFFE4E4, alpha * (0.7F + progress * 0.3F),
                -0.3F - progress * 0.14F, 1.0F, segments);
        poseStack.popPose();
    }

    private static int renderSegments(KatanaSlashEntity entity) {
        double distanceSqr = Minecraft.getInstance().gameRenderer.getMainCamera()
                .getPosition().distanceToSqr(entity.position());
        return KatanaSlashRenderProfile.segmentsForDistanceSqr(distanceSqr);
    }

    @Override
    public ResourceLocation getTextureLocation(KatanaSlashEntity entity) {
        return TEXTURE;
    }
}
