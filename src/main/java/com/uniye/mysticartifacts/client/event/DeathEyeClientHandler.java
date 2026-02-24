package com.uniye.mysticartifacts.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.DeathEyeItem;
import com.uniye.mysticartifacts.util.DeathEyeCutLine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathEyeClientHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        if (!DeathEyeItem.isWearing(player)) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        long gameTime = player.level().getGameTime();
        AABB range = player.getBoundingBox().inflate(Config.DeathEyeRenderRange); // Match event range

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, range, e -> e.isAlive() && e != player)) {
            DeathEyeCutLine.CutLine line = DeathEyeCutLine.compute(target, gameTime);
            int color = line.color();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            drawLine(poseStack, consumer, line.from(), line.to(), r, g, b, 1.0f);
        }

        poseStack.popPose();
        // Don't endBatch here, let Minecraft handle it or it might mess up other rendering?
        // Actually, buffer.getBuffer(RenderType.lines()) might return a shared buffer.
        // If we want immediate drawing we should use a custom buffer or ensure we don't break things.
        // But the original code does endBatch(). I'll remove it to be safer, or check if it's needed.
        // RenderType.lines() is usually safe to batch.
        // However, if I don't endBatch, lines might not render if nothing else flushes it.
        // But endBatch() on the main buffer source might flush everything else too, which is fine in AFTER_ENTITIES.
        buffer.endBatch(RenderType.lines());
    }

    private static void drawLine(PoseStack poseStack, VertexConsumer consumer, Vec3 from, Vec3 to, float r, float g, float b, float a) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        float nx = (float) (to.x - from.x);
        float ny = (float) (to.y - from.y);
        float nz = (float) (to.z - from.z);
        float invLen = (float) (1.0 / Math.sqrt(nx * nx + ny * ny + nz * nz));
        nx *= invLen;
        ny *= invLen;
        nz *= invLen;

        consumer.vertex(pose, (float) from.x, (float) from.y, (float) from.z).color(r, g, b, a).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, (float) to.x, (float) to.y, (float) to.z).color(r, g, b, a).normal(normal, nx, ny, nz).endVertex();
    }
}
