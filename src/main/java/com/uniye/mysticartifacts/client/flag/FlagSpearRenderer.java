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
        VertexConsumer glowConsumer = buffer.getBuffer(FlagSpearRenderTypes.glow());
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        double now = level.getGameTime() + minecraft.getFrameTime();
        for (FlagSpearClientState.Trail trail : trails) {
            if (trail.center().distanceToSqr(camera) > 4096.0D) continue;
            double age = now - trail.tick();
            if (age < 0.0D) continue;
            float fade = 1.0F - (float) Math.min(1.0D,
                    age / FlagSpearClientState.LIFETIME_TICKS);
            drawTrails(poseStack, glowConsumer, trail, age, fade, 1.75F, 0.34F, false);
            drawTrails(poseStack, consumer, trail, age, fade, 1.0F, 1.0F, true);
        }

        poseStack.popPose();
        buffer.endBatch(FlagSpearRenderTypes.glow());
        buffer.endBatch(FlagSpearRenderTypes.luminous());
    }

    private static void drawTrails(PoseStack poseStack, VertexConsumer consumer,
                                   FlagSpearClientState.Trail trail, double age, float fade,
                                   float widthScale, float alphaScale, boolean corePasses) {
        Matrix4f matrix = poseStack.last().pose();
        double yaw = Math.toRadians(-trail.yaw());
        double yawCos = Math.cos(yaw);
        double yawSin = Math.sin(yaw);
        for (int trailIndex = 0; trailIndex < FlagSpearTrailProfile.TRAIL_COUNT; trailIndex++) {
            drawRibbon(poseStack, consumer, matrix, trail, age, fade, trailIndex,
                    yawCos, yawSin, 1.0F, widthScale, alphaScale);
            if (corePasses) {
                drawRibbon(poseStack, consumer, matrix, trail, age, fade, trailIndex,
                        yawCos, yawSin, 0.42F, widthScale, alphaScale);
            }
        }
    }

    private static void drawRibbon(PoseStack poseStack, VertexConsumer consumer, Matrix4f matrix,
                                   FlagSpearClientState.Trail trail, double age, float fade,
                                   int trailIndex, double yawCos, double yawSin, float brightness,
                                   float widthScale, float alphaScale) {
        double headProgress = Math.min(1.0D,
                age / Math.max(1.0D, FlagSpearClientState.LIFETIME_TICKS - 1.0D));
        double tailProgress = Math.max(0.0D, headProgress - FlagSpearTrailProfile.MOVING_TRAIL_LENGTH);
        double progressSpan = headProgress - tailProgress;
        if (progressSpan <= 0.001D) return;

        Vec3 center = trail.center();
        Vec3 previous = null;
        Vec3 previousOffset = null;
        for (int sample = 0; sample < FlagSpearTrailProfile.SAMPLES_PER_TRAIL; sample++) {
            double sampleProgress = sample / (double) (FlagSpearTrailProfile.SAMPLES_PER_TRAIL - 1);
            double progress = tailProgress + progressSpan * sampleProgress;
            Vec3 point = center.add(rotateAroundY(
                    FlagSpearSphericalPath.point(trail.seed(), trailIndex, progress, age), yawCos, yawSin));
            Vec3 nextPoint = center.add(rotateAroundY(
                    FlagSpearSphericalPath.point(trail.seed(), trailIndex,
                            Math.min(1.0D, progress + 0.015D), age), yawCos, yawSin));
            Vec3 tangent = nextPoint.subtract(point);
            if (tangent.lengthSqr() < 1.0E-8D && previous != null) {
                tangent = point.subtract(previous);
            }
            if (tangent.lengthSqr() < 1.0E-8D) continue;
            tangent = tangent.normalize();

            Vec3 radial = point.subtract(center).normalize();
            Vec3 ribbonDirection = tangent.cross(radial);
            if (ribbonDirection.lengthSqr() < 1.0E-8D) {
                ribbonDirection = tangent.cross(new Vec3(0.0D, 1.0D, 0.0D));
            }
            if (ribbonDirection.lengthSqr() < 1.0E-8D) continue;
            double halfWidth = FlagSpearTrailProfile.halfWidth(trailIndex, sample, age)
                    * (brightness > 0.5F ? 1.0D : 0.38D) * widthScale;
            Vec3 offset = ribbonDirection.normalize().scale(halfWidth);

            if (previous != null && previousOffset != null) {
                double edgeFade = Math.min(1.0D,
                        Math.min(sampleProgress / 0.18D, (1.0D - sampleProgress) / 0.18D));
                float alpha = fade * (float) edgeFade
                        * (brightness > 0.5F ? 0.38F : 0.95F) * alphaScale;
                addQuad(consumer, matrix, previous.add(previousOffset), previous.subtract(previousOffset),
                        point.subtract(offset), point.add(offset), alpha);
            }
            previous = point;
            previousOffset = offset;
        }
    }

    private static Vec3 rotateAroundY(Vec3 value, double yawCos, double yawSin) {
        return new Vec3(
                value.x * yawCos - value.z * yawSin,
                value.y,
                value.x * yawSin + value.z * yawCos
        );
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
