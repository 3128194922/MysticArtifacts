package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** 武士刀剑气的独立半月弧带网格。 */
public final class KatanaSlashMesh {
    private static final int ARC_SEGMENTS = 32;
    private static final float HALF_MOON_ANGLE_DEGREES = 180.0F;
    private static final float INNER_RADIUS = 42.0F;
    private static final float OUTER_RADIUS = 100.0F;
    private static final float MAX_HALF_THICKNESS = 4.0F;

    private KatanaSlashMesh() {
    }

    /** 生成带两端渐隐和中段加厚的半月形剑气。 */
    public static void renderHalfMoonLayer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                           int color, float alpha, float uvOffset, float uvScale) {
        if (alpha <= 0.0F) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        for (int segment = 0; segment < ARC_SEGMENTS; segment++) {
            float t0 = segment / (float) ARC_SEGMENTS;
            float t1 = (segment + 1) / (float) ARC_SEGMENTS;
            float angle0 = (float) Math.toRadians(-HALF_MOON_ANGLE_DEGREES * 0.5F
                    + HALF_MOON_ANGLE_DEGREES * t0);
            float angle1 = (float) Math.toRadians(-HALF_MOON_ANGLE_DEGREES * 0.5F
                    + HALF_MOON_ANGLE_DEGREES * t1);

            float innerX0 = (float) Math.sin(angle0) * INNER_RADIUS;
            float innerZ0 = (float) Math.cos(angle0) * INNER_RADIUS;
            float outerX0 = (float) Math.sin(angle0) * OUTER_RADIUS;
            float outerZ0 = (float) Math.cos(angle0) * OUTER_RADIUS;
            float innerX1 = (float) Math.sin(angle1) * INNER_RADIUS;
            float innerZ1 = (float) Math.cos(angle1) * INNER_RADIUS;
            float outerX1 = (float) Math.sin(angle1) * OUTER_RADIUS;
            float outerZ1 = (float) Math.cos(angle1) * OUTER_RADIUS;

            float innerY0 = arcThickness(t0);
            float innerY1 = arcThickness(t1);
            float outerY0 = innerY0;
            float outerY1 = innerY1;

            drawTop(pose, consumer, packedLight, color, alpha,
                    innerX0, innerY0, innerZ0, outerX0, outerY0, outerZ0,
                    innerX1, innerY1, innerZ1, outerX1, outerY1, outerZ1,
                    t0, t1, uvOffset, uvScale);
            drawBottom(pose, consumer, packedLight, color, alpha,
                    innerX0, -innerY0, innerZ0, outerX0, -outerY0, outerZ0,
                    innerX1, -innerY1, innerZ1, outerX1, -outerY1, outerZ1,
                    t0, t1, uvOffset, uvScale);
        }
    }

    private static float arcThickness(float progress) {
        return (float) Math.sin(Math.PI * progress) * MAX_HALF_THICKNESS;
    }

    private static void drawTop(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                                int color, float alpha,
                                float ix0, float iy0, float iz0, float ox0, float oy0, float oz0,
                                float ix1, float iy1, float iz1, float ox1, float oy1, float oz1,
                                float u0, float u1, float uvOffset, float uvScale) {
        triangle(pose, consumer, packedLight, color, alpha,
                ix0, iy0, iz0, ox0, oy0, oz0, ox1, oy1, oz1,
                u0, 0.0F, u0, 1.0F, u1, 1.0F, uvOffset, uvScale, 1.0F);
        triangle(pose, consumer, packedLight, color, alpha,
                ix0, iy0, iz0, ox1, oy1, oz1, ix1, iy1, iz1,
                u0, 0.0F, u1, 1.0F, u1, 0.0F, uvOffset, uvScale, 1.0F);
    }

    private static void drawBottom(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                                  int color, float alpha,
                                  float ix0, float iy0, float iz0, float ox0, float oy0, float oz0,
                                  float ix1, float iy1, float iz1, float ox1, float oy1, float oz1,
                                  float u0, float u1, float uvOffset, float uvScale) {
        triangle(pose, consumer, packedLight, color, alpha,
                ix0, iy0, iz0, ox1, oy1, oz1, ox0, oy0, oz0,
                u0, 0.0F, u1, 1.0F, u0, 1.0F, uvOffset, uvScale, -1.0F);
        triangle(pose, consumer, packedLight, color, alpha,
                ix0, iy0, iz0, ix1, iy1, iz1, ox1, oy1, oz1,
                u0, 0.0F, u1, 0.0F, u1, 1.0F, uvOffset, uvScale, -1.0F);
    }

    private static void triangle(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                                 int color, float alpha,
                                 float x0, float y0, float z0, float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float u0, float v0, float u1, float v1, float u2, float v2,
                                 float uvOffset, float uvScale, float normalY) {
        vertex(pose, consumer, packedLight, color, alpha, x0, y0, z0,
                u0, v0 * uvScale + uvOffset, normalY);
        vertex(pose, consumer, packedLight, color, alpha, x1, y1, z1,
                u1, v1 * uvScale + uvOffset, normalY);
        vertex(pose, consumer, packedLight, color, alpha, x2, y2, z2,
                u2, v2 * uvScale + uvOffset, normalY);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                               int color, float alpha, float x, float y, float z,
                               float u, float v, float normalY) {
        int vertexAlpha = Math.abs(y) < 0.0001F ? 0 : (int) (255.0F * alpha);
        consumer.vertex(pose.pose(), x, y, z)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, vertexAlpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, normalY, 0.0F)
                .endVertex();
    }
}
