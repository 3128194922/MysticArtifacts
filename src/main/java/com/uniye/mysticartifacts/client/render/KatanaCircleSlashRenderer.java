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

/** 开鞘右键范围技使用同一套 SlashBlade SlashEffectRenderer 表现。 */
public class KatanaCircleSlashRenderer extends EntityRenderer<KatanaCircleSlashEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MysticArtifacts.MODID, "textures/entity/katana_slash.png");
    private static final RenderType COLOR = KatanaRenderTypes.blend(TEXTURE);
    private static final RenderType COLOR_WRITE = KatanaRenderTypes.colorWrite(TEXTURE);
    private static final RenderType LUMINOUS = KatanaRenderTypes.luminous(TEXTURE);

    public KatanaCircleSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(KatanaCircleSlashEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float lifetime = 9.0F;
        float age = entity.tickCount + partialTicks;
        float progress = Math.min(lifetime, age) / lifetime;
        float remaining = Math.min(lifetime, Math.max(0.0F, lifetime - age)) / lifetime;
        float baseAlpha = (float) (-Math.pow(remaining - 1.0F, 4.0D) + 1.0D);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
                -Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getRotationRoll()));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getRotationOffset() - 135.0F * progress));
        poseStack.scale(1.0F, 0.25F, 1.0F);
        poseStack.scale(1.2F, 1.2F, 1.2F);

        renderSlashBladeLayers(entity, poseStack, buffer, packedLight, baseAlpha, progress);
        poseStack.popPose();
    }

    private static void renderSlashBladeLayers(KatanaCircleSlashEntity entity, PoseStack poseStack,
                                               MultiBufferSource buffer, int packedLight,
                                               float baseAlpha, float progress) {
        int alpha = (int) (255.0F * baseAlpha);
        float baseSize = entity.getBaseSize();

        poseStack.pushPose();
        float darkScale = baseSize * Mth.lerp(progress, 0.035F, 0.03F);
        poseStack.scale(darkScale, 0.03F, darkScale);
        KatanaSlashMesh.renderSlashBladeLayer(poseStack, buffer.getBuffer(COLOR), packedLight,
                0x222222, alpha / 255.0F, -0.8F + progress * 0.3F, 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        float colorScale = baseSize * Mth.lerp(progress, 0.03F, 0.035F);
        poseStack.scale(colorScale, 0.03F, colorScale);
        KatanaSlashMesh.renderSlashBladeLayer(poseStack, buffer.getBuffer(COLOR_WRITE), packedLight,
                0xFFFFFF, alpha / 255.0F, -0.35F + progress * -0.15F, 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        float whiteScale = baseSize * Mth.lerp(progress, 0.03F, 0.0375F);
        poseStack.scale(whiteScale, 0.03F, whiteScale);
        KatanaSlashMesh.renderSlashBladeLayer(poseStack, buffer.getBuffer(LUMINOUS), packedLight,
                0x404040, alpha / 255.0F, -0.5F + progress * -0.2F, 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(colorScale, 0.03F, colorScale);
        KatanaSlashMesh.renderSlashBladeLayer(poseStack, buffer.getBuffer(LUMINOUS), packedLight,
                0xFFFFFF, alpha / 255.0F, -0.35F + progress * -0.15F, 1.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KatanaCircleSlashEntity entity) {
        return TEXTURE;
    }
}
