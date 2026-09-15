package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** 武士刀剑气的带厚度、渐隐和血刺轮廓的半月弧网格。 */
public final class KatanaSlashMesh {
    private static final int ARC_SEGMENTS = 40;
    private static final float HALF_MOON_ANGLE_DEGREES = 180.0F;
    private static final float INNER_RADIUS = 42.0F;
    private static final float OUTER_RADIUS = 100.0F;
    private static final float MAX_HALF_THICKNESS = 4.5F;
    private static final float OUTER_SPIKE = 8.0F;

    private KatanaSlashMesh() {
    }

    /** 保留旧调用入口，默认使用完整血刃轮廓。 */
    public static void renderHalfMoonLayer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                           int color, float alpha, float uvOffset, float uvScale) {
        renderBloodArcLayer(poseStack, consumer, packedLight, color, alpha, uvOffset, uvScale,
                1.0F, 1.0F, 1.0F);
    }

    /** 生成带三维厚度、两端渐隐和不规则血刺外沿的半月剑气。 */
    public static void renderBloodArcLayer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                           int color, float alpha, float uvOffset, float uvScale,
                                           float radiusScale, float thicknessScale, float spikeStrength) {
        if (alpha <= 0.0F) return;
        PoseStack.Pose pose = poseStack.last();
        for (int segment = 0; segment < ARC_SEGMENTS; segment++) {
            float t0 = segment / (float) ARC_SEGMENTS;
            float t1 = (segment + 1) / (float) ARC_SEGMENTS;
            float angle0 = radians(-HALF_MOON_ANGLE_DEGREES * 0.5F + HALF_MOON_ANGLE_DEGREES * t0);
            float angle1 = radians(-HALF_MOON_ANGLE_DEGREES * 0.5F + HALF_MOON_ANGLE_DEGREES * t1);

            float inner0 = INNER_RADIUS * radiusScale;
            float inner1 = inner0;
            float outer0 = OUTER_RADIUS * radiusScale + outerSpike(t0, spikeStrength);
            float outer1 = OUTER_RADIUS * radiusScale + outerSpike(t1, spikeStrength);
            float innerY0 = arcThickness(t0, thicknessScale);
            float innerY1 = arcThickness(t1, thicknessScale);
            float alpha0 = alpha * segmentAlpha(t0);
            float alpha1 = alpha * segmentAlpha(t1);

            float ix0 = sin(angle0) * inner0;
            float iz0 = cos(angle0) * inner0;
            float ox0 = sin(angle0) * outer0;
            float oz0 = cos(angle0) * outer0;
            float ix1 = sin(angle1) * inner1;
            float iz1 = cos(angle1) * inner1;
            float ox1 = sin(angle1) * outer1;
            float oz1 = cos(angle1) * outer1;

            drawTop(pose, consumer, packedLight, color, alpha0, alpha1,
                    ix0, innerY0, iz0, ox0, innerY0, oz0,
                    ix1, innerY1, iz1, ox1, innerY1, oz1, t0, t1, uvOffset, uvScale);
            drawBottom(pose, consumer, packedLight, color, alpha0, alpha1,
                    ix0, -innerY0, iz0, ox0, -innerY0, oz0,
                    ix1, -innerY1, iz1, ox1, -innerY1, oz1, t0, t1, uvOffset, uvScale);
            float midAngle = (angle0 + angle1) * 0.5F;
            drawSide(pose, consumer, packedLight, color, alpha0, alpha1,
                    ox0, innerY0, oz0, ox1, innerY1, oz1,
                    ox0, -innerY0, oz0, ox1, -innerY1, oz1,
                    sin(midAngle), cos(midAngle), t0, t1, uvOffset, uvScale);
            drawSide(pose, consumer, packedLight, color, alpha0, alpha1,
                    ix1, innerY1, iz1, ix0, innerY0, iz0,
                    ix1, -innerY1, iz1, ix0, -innerY0, iz0,
                    -sin(midAngle), -cos(midAngle), t0, t1, uvOffset, uvScale);
        }
    }

    private static float outerSpike(float progress, float strength) {
        return sin((float) (Math.PI * 11.0D * progress + 0.35D))
                * sin((float) (Math.PI * progress)) * OUTER_SPIKE * strength;
    }

    private static float segmentAlpha(float progress) {
        return (float) Math.pow(Math.sin(Math.PI * progress), 0.68D);
    }

    private static float arcThickness(float progress, float scale) {
        return sin((float) (Math.PI * progress)) * MAX_HALF_THICKNESS * scale;
    }

    private static void drawTop(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                                int color, float alpha0, float alpha1,
                                float ix0, float iy0, float iz0, float ox0, float oy0, float oz0,
                                float ix1, float iy1, float iz1, float ox1, float oy1, float oz1,
                                float u0, float u1, float uvOffset, float uvScale) {
        triangle(pose, consumer, packedLight, color, alpha0, alpha0, alpha1,
                ix0, iy0, iz0, ox0, oy0, oz0, ox1, oy1, oz1,
                u0, 0.0F, u0, 1.0F, u1, 1.0F, uvOffset, uvScale, 0.0F, 1.0F, 0.0F);
        triangle(pose, consumer, packedLight, color, alpha0, alpha1, alpha1,
                ix0, iy0, iz0, ox1, oy1, oz1, ix1, iy1, iz1,
                u0, 0.0F, u1, 1.0F, u1, 0.0F, uvOffset, uvScale, 0.0F, 1.0F, 0.0F);
    }

    private static void drawBottom(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                                   int color, float alpha0, float alpha1,
                                   float ix0, float iy0, float iz0, float ox0, float oy0, float oz0,
                                   float ix1, float iy1, float iz1, float ox1, float oy1, float oz1,
                                   float u0, float u1, float uvOffset, float uvScale) {
        triangle(pose, consumer, packedLight, color, alpha0, alpha1, alpha0,
                ix0, iy0, iz0, ox1, oy1, oz1, ox0, oy0, oz0,
                u0, 0.0F, u1, 1.0F, u0, 1.0F, uvOffset, uvScale, 0.0F, -1.0F, 0.0F);
        triangle(pose, consumer, packedLight, color, alpha1, alpha1, alpha0,
                ix0, iy0, iz0, ix1, iy1, iz1, ox1, oy1, oz1,
                u0, 0.0F, u1, 0.0F, u1, 1.0F, uvOffset, uvScale, 0.0F, -1.0F, 0.0F);
    }

    private static void drawSide(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                                 int color, float alpha0, float alpha1,
                                 float x0, float y0, float z0, float x1, float y1, float z1,
                                 float bx0, float by0, float bz0, float bx1, float by1, float bz1,
                                 float normalX, float normalZ, float u0, float u1,
                                 float uvOffset, float uvScale) {
        triangle(pose, consumer, packedLight, color, alpha0, alpha1, alpha1,
                x0, y0, z0, x1, y1, z1, bx1, by1, bz1,
                u0, 0.0F, u1, 0.0F, u1, 1.0F, uvOffset, uvScale, normalX, 0.0F, normalZ);
        triangle(pose, consumer, packedLight, color, alpha0, alpha1, alpha0,
                x0, y0, z0, bx1, by1, bz1, bx0, by0, bz0,
                u0, 0.0F, u1, 1.0F, u0, 1.0F, uvOffset, uvScale, normalX, 0.0F, normalZ);
    }

    private static void triangle(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                                 int color, float alpha0, float alpha1, float alpha2,
                                 float x0, float y0, float z0, float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float u0, float v0, float u1, float v1, float u2, float v2,
                                 float uvOffset, float uvScale, float normalX, float normalY, float normalZ) {
        vertex(pose, consumer, packedLight, color, alpha0, x0, y0, z0, u0, v0 * uvScale + uvOffset,
                normalX, normalY, normalZ);
        vertex(pose, consumer, packedLight, color, alpha1, x1, y1, z1, u1, v1 * uvScale + uvOffset,
                normalX, normalY, normalZ);
        vertex(pose, consumer, packedLight, color, alpha2, x2, y2, z2, u2, v2 * uvScale + uvOffset,
                normalX, normalY, normalZ);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                               int color, float alpha, float x, float y, float z,
                               float u, float v, float normalX, float normalY, float normalZ) {
        int vertexAlpha = Math.max(0, Math.min(255, (int) (255.0F * alpha)));
        consumer.vertex(pose.pose(), x, y, z)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, vertexAlpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), normalX, normalY, normalZ)
                .endVertex();
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static float sin(float value) {
        return (float) Math.sin(value);
    }

    private static float cos(float value) {
        return (float) Math.cos(value);
    }
}
