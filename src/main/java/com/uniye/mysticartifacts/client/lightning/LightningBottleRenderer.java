package com.uniye.mysticartifacts.client.lightning;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LightningBottleRenderer {
    private static final int SEGMENTS_PER_BOLT = 5;
    private static final float OUTER_WIDTH = 0.09F;
    private static final float MIDDLE_WIDTH = 0.0475F;
    private static final float INNER_WIDTH = 0.021F;
    private static final float[] OUTER_COLOR = {0.16F, 0.30F, 1.0F};
    private static final float[] MIDDLE_COLOR = {0.12F, 0.72F, 1.0F};
    private static final float[] INNER_COLOR = {0.82F, 0.98F, 1.0F};
    private static final ResourceLocation CHARGED_CREEPER_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/creeper/creeper_armor.png");
    private static final double FIELD_RADIUS = 5.0D;
    private static final double FIELD_UV_SPEED_U = 0.055D;
    private static final double FIELD_UV_SPEED_V = 0.037D;

    private LightningBottleRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        List<LightningBottleClientState.Chain> chains =
                LightningBottleClientState.snapshot(minecraft.level);
        List<LightningBottleClientState.Field> fields =
                LightningBottleClientState.fieldSnapshot(minecraft.level);
        if (chains.isEmpty() && fields.isEmpty()) return;

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        double animationTime = minecraft.level.getGameTime() + minecraft.getFrameTime();
        float uvOffsetU = (float) ((animationTime * FIELD_UV_SPEED_U) % 1.0D);
        float uvOffsetV = (float) ((animationTime * FIELD_UV_SPEED_V) % 1.0D);
        RenderType fieldTextureType = RenderType.energySwirl(
                CHARGED_CREEPER_TEXTURE, uvOffsetU, uvOffsetV);
        VertexConsumer outer = buffer.getBuffer(LightningBottleRenderTypes.outer());
        VertexConsumer middle = buffer.getBuffer(LightningBottleRenderTypes.middle());
        VertexConsumer inner = buffer.getBuffer(LightningBottleRenderTypes.inner());
        VertexConsumer fieldTexture = fields.isEmpty()
                ? null
                : buffer.getBuffer(fieldTextureType);

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        long gameTime = minecraft.level.getGameTime();
        for (LightningBottleClientState.Chain chain : chains) {
            float alpha = 1.0F - Math.min(1.0F,
                    (gameTime - chain.tick()) / (float) LightningBottleClientState.LIFETIME_TICKS);
            drawPath(poseStack, outer, chain.path(), chain.tick(), gameTime, alpha,
                    OUTER_WIDTH, OUTER_COLOR, 0.55F, 0.0F);
            drawPath(poseStack, middle, chain.path(), chain.tick(), gameTime, alpha,
                    MIDDLE_WIDTH, MIDDLE_COLOR, 0.82F, 101.0F);
            drawPath(poseStack, inner, chain.path(), chain.tick(), gameTime, alpha,
                    INNER_WIDTH, INNER_COLOR, 1.0F, 211.0F);
        }
        for (LightningBottleClientState.Field field : fields) {
            float alpha = 1.0F - Math.min(1.0F,
                    (gameTime - field.tick()) / (float) LightningBottleClientState.FIELD_LIFETIME_TICKS);
            drawField(poseStack, fieldTexture, field, gameTime, alpha);
        }
        poseStack.popPose();
        if (fieldTexture != null) {
            buffer.endBatch(fieldTextureType);
        }
        buffer.endBatch(LightningBottleRenderTypes.outer());
        buffer.endBatch(LightningBottleRenderTypes.middle());
        buffer.endBatch(LightningBottleRenderTypes.inner());
    }

    private static void drawField(PoseStack poseStack, VertexConsumer fieldTexture,
                                  LightningBottleClientState.Field field, long gameTime, float alpha) {
        Vec3 center = field.center();
        double progress = Math.min(1.0D, Math.max(0.0D,
                (gameTime - field.tick()) / (double) LightningBottleClientState.FIELD_LIFETIME_TICKS));
        double radius = 0.35D + (FIELD_RADIUS - 0.35D) * progress;
        drawTexturedCube(poseStack, fieldTexture, center, radius, alpha * 0.82F);
    }

    private static void drawTexturedCube(PoseStack poseStack, VertexConsumer consumer,
                                         Vec3 center, double radius, float alpha) {
        if (consumer == null || alpha <= 0.0F) return;

        double minX = center.x - radius;
        double maxX = center.x + radius;
        double minY = center.y - radius;
        double maxY = center.y + radius;
        double minZ = center.z - radius;
        double maxZ = center.z + radius;

        // 六个带 UV 的贴图面组成一个整体立方体，并随动画从中心向外扩散。
        addTexturedQuad(poseStack, consumer,
                minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                0.0F, 1.0F, 0.0F, alpha);
        addTexturedQuad(poseStack, consumer,
                minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ,
                0.0F, -1.0F, 0.0F, alpha);
        addTexturedQuad(poseStack, consumer,
                minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                -1.0F, 0.0F, 0.0F, alpha);
        addTexturedQuad(poseStack, consumer,
                maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                1.0F, 0.0F, 0.0F, alpha);
        addTexturedQuad(poseStack, consumer,
                minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ,
                0.0F, 0.0F, -1.0F, alpha);
        addTexturedQuad(poseStack, consumer,
                maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ,
                0.0F, 0.0F, 1.0F, alpha);
    }

    private static void addTexturedQuad(PoseStack poseStack, VertexConsumer consumer,
                                        double x1, double y1, double z1,
                                        double x2, double y2, double z2,
                                        double x3, double y3, double z3,
                                        double x4, double y4, double z4,
                                        float normalX, float normalY, float normalZ,
                                        float alpha) {
        PoseStack.Pose pose = poseStack.last();
        addTexturedVertex(consumer, pose, x1, y1, z1, 0.0F, 0.0F, normalX, normalY, normalZ, alpha);
        addTexturedVertex(consumer, pose, x2, y2, z2, 1.0F, 0.0F, normalX, normalY, normalZ, alpha);
        addTexturedVertex(consumer, pose, x3, y3, z3, 1.0F, 1.0F, normalX, normalY, normalZ, alpha);
        addTexturedVertex(consumer, pose, x4, y4, z4, 0.0F, 1.0F, normalX, normalY, normalZ, alpha);
    }

    private static void addTexturedVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                          double x, double y, double z,
                                          float u, float v,
                                          float normalX, float normalY, float normalZ,
                                          float alpha) {
        consumer.vertex(pose.pose(), (float) x, (float) y, (float) z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), normalX, normalY, normalZ)
                .endVertex();
    }

    private static void drawPath(PoseStack poseStack, VertexConsumer consumer,
                                 List<Vec3> path, long tick, long gameTime, float alpha,
                                 float width, float[] color, float alphaScale, float layerSeed) {
        for (int i = 0; i + 1 < path.size(); i++) {
            long seed = tick + i * 31L + (long) layerSeed;
            drawBolt(poseStack, consumer, path.get(i), path.get(i + 1), seed,
                    gameTime, alpha * alphaScale, width, color);
        }
    }

    private static void drawBolt(PoseStack poseStack, VertexConsumer consumer,
                                 Vec3 from, Vec3 to, long seed, long gameTime,
                                 float alpha, float width, float[] color) {
        Vec3 direction = to.subtract(from);
        double length = direction.length();
        if (length < 0.001D) return;

        Vec3 forward = direction.scale(1.0D / length);
        Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.001D) {
            side = forward.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 vertical = forward.cross(side).normalize();

        Vec3 previous = from;
        for (int i = 1; i <= SEGMENTS_PER_BOLT; i++) {
            double progress = i / (double) SEGMENTS_PER_BOLT;
            Vec3 current = (i == SEGMENTS_PER_BOLT) ? to : from.lerp(to, progress);
            if (i < SEGMENTS_PER_BOLT) {
                long value = seed * 31L + i * 17L;
                double jitterScale = Math.min(0.18D, length * 0.035D);
                double timePhase = gameTime * 0.46D + i * 1.37D + seed * 0.003D;
                double sideOffset = ((value & 7L) - 3.5D) * jitterScale
                        + Math.sin(timePhase) * jitterScale * 0.62D;
                double verticalOffset = ((((value >> 3) & 7L) - 3.5D)
                        * Math.min(0.12D, length * 0.025D))
                        + Math.cos(timePhase * 1.17D) * Math.min(0.10D, length * 0.02D);
                current = current.add(side.scale(sideOffset)).add(vertical.scale(verticalOffset));
            }
            float segmentFade = 0.72F + 0.28F
                    * (float) Math.sin((i / (double) SEGMENTS_PER_BOLT) * Math.PI);
            float[] gradientColor = gradientColor(color, i / (float) SEGMENTS_PER_BOLT);
            drawRibbon(poseStack, consumer, previous, current, width * segmentFade,
                    gradientColor, alpha * segmentFade);
            previous = current;
        }
    }

    private static float[] gradientColor(float[] color, float progress) {
        float highlight = progress * 0.22F;
        return new float[]{
                Math.min(1.0F, color[0] + highlight),
                Math.min(1.0F, color[1] + highlight * 0.55F),
                Math.min(1.0F, color[2] + highlight * 0.18F)
        };
    }

    private static void drawRibbon(PoseStack poseStack, VertexConsumer consumer,
                                   Vec3 from, Vec3 to, float width,
                                   float[] color, float alpha) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 0.000001D) return;
        Vec3 forward = direction.normalize();
        Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.000001D) {
            side = forward.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize().scale(width);
        addQuad(poseStack, consumer, from.add(side), from.subtract(side),
                to.subtract(side), to.add(side), color, alpha);
    }

    private static void addQuad(PoseStack poseStack, VertexConsumer consumer,
                                Vec3 firstTop, Vec3 firstBottom, Vec3 secondBottom, Vec3 secondTop,
                                float[] color, float alpha) {
        Matrix4f matrix = poseStack.last().pose();
        vertex(consumer, matrix, firstTop, color, alpha);
        vertex(consumer, matrix, firstBottom, color, alpha);
        vertex(consumer, matrix, secondBottom, color, alpha);
        vertex(consumer, matrix, secondTop, color, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position,
                               float[] color, float alpha) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(color[0], color[1], color[2], alpha)
                .endVertex();
    }
}
