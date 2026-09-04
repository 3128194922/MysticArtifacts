package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** 拔刀剑式连续弧形刀光网格，只使用 MysticArtifacts 自有材质。 */
public final class KatanaSlashMesh {
    private KatanaSlashMesh() {
    }

    public static void renderArc(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                 float alpha, float radius, float width) {
        int segments = 20;
        float start = (float) Math.toRadians(-78.0D);
        float end = (float) Math.toRadians(78.0D);
        float innerRadius = radius - width * 0.5F;
        float outerRadius = radius + width * 0.5F;
        for (int i = 0; i < segments; i++) {
            float u0 = i / (float) segments;
            float u1 = (i + 1) / (float) segments;
            float a0 = start + (end - start) * u0;
            float a1 = start + (end - start) * u1;
            float innerX0 = (float) Math.sin(a0) * innerRadius;
            float innerZ0 = (float) Math.cos(a0) * innerRadius;
            float outerX0 = (float) Math.sin(a0) * outerRadius;
            float outerZ0 = (float) Math.cos(a0) * outerRadius;
            float innerX1 = (float) Math.sin(a1) * innerRadius;
            float innerZ1 = (float) Math.cos(a1) * innerRadius;
            float outerX1 = (float) Math.sin(a1) * outerRadius;
            float outerZ1 = (float) Math.cos(a1) * outerRadius;
            quad(consumer, poseStack.last(), innerX0, 0.035F, innerZ0, outerX0, 0.035F, outerZ0,
                    outerX1, 0.035F, outerZ1, innerX1, 0.035F, innerZ1,
                    u0, u1, packedLight, alpha, 1.0F);
            quad(consumer, poseStack.last(), innerX1, -0.035F, innerZ1, outerX1, -0.035F, outerZ1,
                    outerX0, -0.035F, outerZ0, innerX0, -0.035F, innerZ0,
                    u0, u1, packedLight, alpha, -1.0F);
        }
    }

    public static void renderRing(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                  float alpha, float innerRadius, float outerRadius) {
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            float u0 = i / (float) segments;
            float u1 = (i + 1) / (float) segments;
            double a0 = Math.PI * 2.0D * u0;
            double a1 = Math.PI * 2.0D * u1;
            float innerX0 = (float) Math.sin(a0) * innerRadius;
            float innerZ0 = (float) Math.cos(a0) * innerRadius;
            float outerX0 = (float) Math.sin(a0) * outerRadius;
            float outerZ0 = (float) Math.cos(a0) * outerRadius;
            float innerX1 = (float) Math.sin(a1) * innerRadius;
            float innerZ1 = (float) Math.cos(a1) * innerRadius;
            float outerX1 = (float) Math.sin(a1) * outerRadius;
            float outerZ1 = (float) Math.cos(a1) * outerRadius;
            quad(consumer, poseStack.last(), innerX0, 0.035F, innerZ0, outerX0, 0.035F, outerZ0,
                    outerX1, 0.035F, outerZ1, innerX1, 0.035F, innerZ1,
                    u0, u1, packedLight, alpha, 1.0F);
            quad(consumer, poseStack.last(), innerX1, -0.035F, innerZ1, outerX1, -0.035F, outerZ1,
                    outerX0, -0.035F, outerZ0, innerX0, -0.035F, innerZ0,
                    u0, u1, packedLight, alpha, -1.0F);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float u0, float u1, int packedLight, float alpha, float normalY) {
        vertex(consumer, pose, x0, y0, z0, u0, 1.0F, packedLight, alpha, normalY);
        vertex(consumer, pose, x1, y1, z1, u0, 0.0F, packedLight, alpha, normalY);
        vertex(consumer, pose, x2, y2, z2, u1, 0.0F, packedLight, alpha, normalY);
        vertex(consumer, pose, x3, y3, z3, u1, 1.0F, packedLight, alpha, normalY);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v,
                               int packedLight, float alpha, float normalY) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, (int) (255.0F * alpha))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, normalY, 0.0F)
                .endVertex();
    }
}
