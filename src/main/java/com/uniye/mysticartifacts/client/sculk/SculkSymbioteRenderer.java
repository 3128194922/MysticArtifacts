package com.uniye.mysticartifacts.client.sculk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.client.trail.TrailSightClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SculkSymbioteRenderer {
    private SculkSymbioteRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) render(event);
    }

    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (!SculkSymbioteClientState.isActive() || level == null || minecraft.player == null) return;
        var markers = SculkSymbioteClientState.snapshot(level);
        if (markers.isEmpty()) return;

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        long now = level.getGameTime();
        float lifetime = Math.max(1.0F, TrailSightClientConfig.MARKER_LIFETIME.get());
        for (SculkSymbioteClientState.Marker marker : markers) {
            if (marker.position().distanceToSqr(camera) > 4096.0D) continue;
            float age = Math.max(0.0F, now - marker.tick());
            float alpha = 1.0F - Math.min(1.0F, age / lifetime);
            float radius = 0.18F + Math.min(1.2F, age / lifetime * 1.2F);
            drawMarker(poseStack, consumer, marker.position().add(0.0D, 0.04D, 0.0D), radius, alpha);
        }
        poseStack.popPose();
        buffer.endBatch(RenderType.debugQuads());
    }

    private static void drawMarker(PoseStack poseStack, VertexConsumer consumer, Vec3 center,
                                   float radius, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        int packedAlpha = Math.max(1, Math.min(220, Math.round(alpha * 220.0F)));
        quad(consumer, matrix, center.x - radius, center.y, center.z - 0.035D,
                center.x + radius, center.z + 0.035D, packedAlpha);
        quad(consumer, matrix, center.x - 0.035D, center.y, center.z - radius,
                center.x + 0.035D, center.z + radius, packedAlpha);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, double minX, double y, double minZ,
                             double maxX, double maxZ, int alpha) {
        consumer.vertex(matrix, (float) minX, (float) y, (float) minZ).color(38, 255, 210, alpha).endVertex();
        consumer.vertex(matrix, (float) maxX, (float) y, (float) minZ).color(38, 255, 210, alpha).endVertex();
        consumer.vertex(matrix, (float) maxX, (float) y, (float) maxZ).color(38, 255, 210, alpha).endVertex();
        consumer.vertex(matrix, (float) minX, (float) y, (float) maxZ).color(38, 255, 210, alpha).endVertex();
    }
}
