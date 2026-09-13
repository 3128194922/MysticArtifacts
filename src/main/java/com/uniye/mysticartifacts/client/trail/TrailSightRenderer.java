package com.uniye.mysticartifacts.client.trail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrailSightRenderer {
    private static final float FOOTPRINT_LENGTH = 0.28F;
    private static final float FOOTPRINT_WIDTH = 0.12F;
    private static final double FOOTPRINT_SIDE_OFFSET = 0.11D;
    private static final double FOOTPRINT_Y_OFFSET = 0.018D;
    private static final int RED = 72;
    private static final int GREEN = 220;
    private static final int BLUE = 255;

    private TrailSightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        render(event);
    }

    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) return;

        Map<UUID, TrailBuffer> tracks = TrailSightTracker.getTracks();
        if (tracks.isEmpty()) return;

        long currentTick = level.getGameTime();
        int retentionTicks = Math.max(1, TrailSightClientConfig.retentionTicks());
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (TrailBuffer trail : tracks.values()) {
            List<TrailSample> samples = trail.samples();
            for (TrailSample sample : samples) {
                long age = Math.max(0L, currentTick - sample.tick());
                float alpha = 1.0F - Math.min(1.0F, age / (float) retentionTicks);
                if (alpha <= 0.0F) continue;

                Vec3 direction = horizontalDirection(sample);
                Vec3 normal = new Vec3(-direction.z, 0.0D, direction.x);
                double y = groundY(level, sample) + FOOTPRINT_Y_OFFSET;
                Vec3 center = new Vec3(sample.x(), y, sample.z());
                drawFootprint(poseStack, consumer, center.add(normal.scale(-FOOTPRINT_SIDE_OFFSET)),
                        direction, alpha);
                drawFootprint(poseStack, consumer, center.add(normal.scale(FOOTPRINT_SIDE_OFFSET)),
                        direction, alpha);
            }
        }

        poseStack.popPose();
        buffer.endBatch(RenderType.debugQuads());
    }

    private static Vec3 horizontalDirection(TrailSample sample) {
        Vec3 direction = new Vec3(sample.directionX(), 0.0D, sample.directionZ());
        if (direction.lengthSqr() < 0.000001D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return direction.normalize();
    }

    private static double groundY(ClientLevel level, TrailSample sample) {
        BlockPos feet = BlockPos.containing(sample.x(), sample.y(), sample.z());
        if (!level.getBlockState(feet.below()).isAir()) {
            return feet.getY();
        }
        return sample.y();
    }

    private static void drawFootprint(PoseStack poseStack, VertexConsumer consumer,
                                      Vec3 center, Vec3 direction, float alpha) {
        Vec3 normal = new Vec3(-direction.z, 0.0D, direction.x);
        Vec3 front = center.add(direction.scale(FOOTPRINT_LENGTH * 0.5D));
        Vec3 back = center.subtract(direction.scale(FOOTPRINT_LENGTH * 0.5D));
        Vec3 halfWidth = normal.scale(FOOTPRINT_WIDTH * 0.5D);
        Vec3 frontLeft = front.subtract(halfWidth);
        Vec3 frontRight = front.add(halfWidth);
        Vec3 backRight = back.add(halfWidth);
        Vec3 backLeft = back.subtract(halfWidth);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        int packedAlpha = Math.max(1, Math.min(220, Math.round(alpha * 220.0F)));
        vertex(consumer, poseMatrix, frontLeft, packedAlpha);
        vertex(consumer, poseMatrix, frontRight, packedAlpha);
        vertex(consumer, poseMatrix, backRight, packedAlpha);
        vertex(consumer, poseMatrix, backLeft, packedAlpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Vec3 position, int alpha) {
        consumer.vertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .color(RED, GREEN, BLUE, alpha)
                .endVertex();
    }
}
