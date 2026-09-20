package com.uniye.mysticartifacts.client.flag;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FlagSpearRenderer {
    private FlagSpearRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        List<FlagSpearClientState.Trail> trails = FlagSpearClientState.snapshot(level);
        if (trails.isEmpty()) return;

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(FlagSpearRenderTypes.luminous());
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        long now = level.getGameTime();
        for (FlagSpearClientState.Trail trail : trails) {
            if (trail.center().distanceToSqr(camera) > 4096.0D) continue;
            float age = Math.max(0.0F, now - trail.tick());
            float fade = 1.0F - Math.min(1.0F, age / FlagSpearClientState.LIFETIME_TICKS);
            drawTrails(poseStack, consumer, trail, now, fade);
        }

        poseStack.popPose();
        buffer.endBatch(FlagSpearRenderTypes.luminous());
    }

    private static void drawTrails(PoseStack poseStack, VertexConsumer consumer,
                                   FlagSpearClientState.Trail trail, long now, float fade) {
        Matrix4f matrix = poseStack.last().pose();
        double yaw = Math.toRadians(-trail.yaw());
        double yawCos = Math.cos(yaw);
        double yawSin = Math.sin(yaw);
        for (int trailIndex = 0; trailIndex < FlagSpearTrailProfile.TRAIL_COUNT; trailIndex++) {
            drawRibbon(poseStack, consumer, matrix, trail, now, fade, trailIndex, yawCos, yawSin, 1.0F);
            drawRibbon(poseStack, consumer, matrix, trail, now, fade, trailIndex, yawCos, yawSin, 0.42F);
        }
    }

    private static void drawRibbon(PoseStack poseStack, VertexConsumer consumer, Matrix4f matrix,
                                   FlagSpearClientState.Trail trail, long now, float fade,
                                   int trailIndex, double yawCos, double yawSin, float brightness) {
        Vec3 center = trail.center();
        Vec3 previous = null;
        Vec3 previousOffset = null;
        for (int sample = 0; sample < FlagSpearTrailProfile.SAMPLES_PER_TRAIL; sample++) {
            double progress = sample / (double) (FlagSpearTrailProfile.SAMPLES_PER_TRAIL - 1);
            double angle = FlagSpearTrailProfile.phaseFor(trailIndex)
                    + progress * Math.PI * 2.0D
                    + now * 0.06D;
            double localX = Math.cos(angle) * FlagSpearTrailProfile.radiusAt(sample);
            double localZ = Math.sin(angle) * FlagSpearTrailProfile.radiusAt(sample);
            double rotatedX = localX * yawCos - localZ * yawSin;
            double rotatedZ = localX * yawSin + localZ * yawCos;
            double y = center.y + 0.18D + trailIndex * 0.07D
                    + FlagSpearTrailProfile.verticalWave(trailIndex, sample, now - trail.tick());
            Vec3 point = new Vec3(center.x + rotatedX, y, center.z + rotatedZ);
            double halfWidth = FlagSpearTrailProfile.halfWidth(trailIndex, sample, now - trail.tick())
                    * (brightness > 0.5F ? 1.0D : 0.38D);
            Vec3 offset = new Vec3(0.0D, halfWidth, 0.0D);

            if (previous != null) {
                addQuad(consumer, matrix, previous.add(previousOffset), previous.subtract(previousOffset),
                        point.subtract(offset), point.add(offset), fade * (brightness > 0.5F ? 0.38F : 0.95F));
            }
            previous = point;
            previousOffset = offset;
        }
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix,
                                Vec3 firstTop, Vec3 firstBottom, Vec3 secondBottom, Vec3 secondTop,
                                float alpha) {
        int packedAlpha = Math.max(1, Math.min(255, Math.round(alpha * 255.0F)));
        vertex(consumer, matrix, firstTop, packedAlpha);
        vertex(consumer, matrix, firstBottom, packedAlpha);
        vertex(consumer, matrix, secondBottom, packedAlpha);
        vertex(consumer, matrix, secondTop, packedAlpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, int alpha) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(255, 0, 0, alpha)
                .endVertex();
    }
}
