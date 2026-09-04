package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * SlashBlade slash.obj 的独立等价网格。
 *
 * <p>这里不读取 SlashBlade 文件，而是用同样的 24 段、双面、三层弧带
 * 在 MysticArtifacts 内部生成三角面。渲染器每次调用本方法对应 SlashBlade 的一次模型层渲染。</p>
 */
public final class KatanaSlashMesh {
    private static final int SEGMENTS = 24;
    private static final float[] RADII = {54.424282F, 84.891541F, 69.530182F, 100.0F};
    private static final float[] TOP_HEIGHT = {0.0F, 4.750058F, 3.317146F, 0.0F};
    private static final float[] UV_BANDS = {0.0F, 0.33333334F, 0.6666667F, 1.0F};

    private KatanaSlashMesh() {
    }

    /** 复刻 SlashBlade 的单层 OBJ 三角网格与 UV 滚动。 */
    public static void renderSlashBladeLayer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                             int color, float alpha, float uvOffset, float uvScale) {
        if (alpha <= 0.0F) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        for (int band = 0; band < RADII.length - 1; band++) {
            drawBand(pose, consumer, packedLight, color, alpha, uvOffset, uvScale,
                    RADII[band], TOP_HEIGHT[band], RADII[band + 1], TOP_HEIGHT[band + 1],
                    UV_BANDS[band], UV_BANDS[band + 1], 1.0F);
            drawBand(pose, consumer, packedLight, color, alpha, uvOffset, uvScale,
                    RADII[band], -TOP_HEIGHT[band], RADII[band + 1], -TOP_HEIGHT[band + 1],
                    UV_BANDS[band], UV_BANDS[band + 1], -1.0F);
        }
    }

    private static void drawBand(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                                 int color, float alpha, float uvOffset, float uvScale,
                                 float innerRadius, float innerY, float outerRadius, float outerY,
                                 float v0, float v1, float normalY) {
        for (int i = 0; i < SEGMENTS; i++) {
            int next = i + 1;
            float u0 = i / (float) SEGMENTS;
            float u1 = next / (float) SEGMENTS;
            float a0 = (float) (Math.PI * 2.0D * u0);
            float a1 = (float) (Math.PI * 2.0D * u1);

            float ix0 = (float) Math.sin(a0) * innerRadius;
            float iz0 = (float) Math.cos(a0) * innerRadius;
            float ox0 = (float) Math.sin(a0) * outerRadius;
            float oz0 = (float) Math.cos(a0) * outerRadius;
            float ix1 = (float) Math.sin(a1) * innerRadius;
            float iz1 = (float) Math.cos(a1) * innerRadius;
            float ox1 = (float) Math.sin(a1) * outerRadius;
            float oz1 = (float) Math.cos(a1) * outerRadius;

            triangle(pose, consumer, packedLight, color, alpha,
                    ix0, innerY, iz0, ox0, outerY, oz0, ox1, outerY, oz1,
                    u0, v0, u0, v1, u1, v1, uvOffset, uvScale, normalY);
            triangle(pose, consumer, packedLight, color, alpha,
                    ix0, innerY, iz0, ox1, outerY, oz1, ix1, innerY, iz1,
                    u0, v0, u1, v1, u1, v0, uvOffset, uvScale, normalY);
        }
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
