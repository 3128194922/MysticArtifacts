package com.uniye.mysticartifacts.client.deathscythe;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathScytheSlashRenderer {
    private static final RenderStateShard.TransparencyStateShard ALPHA_BLEND =
            new RenderStateShard.TransparencyStateShard("death_scythe_alpha", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });
    private static final RenderStateShard.TransparencyStateShard ADDITIVE_BLEND =
            new RenderStateShard.TransparencyStateShard("death_scythe_additive", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });
    private static final RenderType COLOR = createType("color", ALPHA_BLEND);
    private static final RenderType GLOW = createType("glow", ADDITIVE_BLEND);
    private static final double VISIBLE_DISTANCE_SQR = 96.0D * 96.0D;

    private DeathScytheSlashRenderer() {
    }

    @SubscribeEvent
    public static void renderAfterEntities(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        List<DeathScytheClientState.Slash> slashes = DeathScytheClientState.snapshot(level);
        if (slashes.isEmpty()) return;

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer color = buffers.getBuffer(COLOR);
        VertexConsumer glow = buffers.getBuffer(GLOW);
        double now = level.getGameTime() + minecraft.getFrameTime();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (DeathScytheClientState.Slash slash : slashes) {
            if (slash.origin().distanceToSqr(camera) > VISIBLE_DISTANCE_SQR
                    && slash.target().distanceToSqr(camera) > VISIBLE_DISTANCE_SQR) continue;
            double age = now - slash.startTick();
            if (age < 0.0D || age >= slash.durationTicks()) continue;
            drawSlash(poseStack.last().pose(), color, glow, slash, camera, age);
        }
        poseStack.popPose();
        buffers.endBatch(COLOR);
        buffers.endBatch(GLOW);
    }

    private static void drawSlash(Matrix4f matrix, VertexConsumer color, VertexConsumer glow,
                                  DeathScytheClientState.Slash slash, Vec3 camera, double age) {
        Vec3 direction = slash.target().subtract(slash.origin());
        if (direction.lengthSqr() < 1.0E-6D) return;
        Vec3 forward = direction.normalize();
        Vec3 midpoint = slash.origin().add(slash.target()).scale(0.5D);
        Vec3 side = forward.cross(camera.subtract(midpoint));
        if (side.lengthSqr() < 1.0E-6D) side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) side = forward.cross(new Vec3(1.0D, 0.0D, 0.0D));
        side = side.normalize();

        double progress = Math.min(1.0D, (age + 0.5D) / slash.durationTicks());
        double head = Math.min(1.0D, 0.04D + progress * 1.14D);
        double tail = Math.max(0.0D, head - 0.32D);
        double phase = ((slash.seed() >>> 8) & 1023L) * (Math.PI * 2.0D / 1024.0D);
        double sway = Math.sin(phase + age * 0.42D) * 0.035D;
        Vec3 from = slash.origin().lerp(slash.target(), tail).add(side.scale(sway));
        Vec3 to = slash.origin().lerp(slash.target(), head).add(side.scale(sway));
        float fade = (float) Math.min(1.0D, (slash.durationTicks() - age) / 3.0D);
        float pulse = 0.92F + 0.08F * (float) Math.sin(phase + age * 0.75D);

        ribbon(matrix, color, from, to, side, 0.115D, 100, 8, 25, fade * 0.72F);
        ribbon(matrix, color, from, to, side, 0.054D, 25, 5, 36, fade * 0.95F);
        ribbon(matrix, glow, from, to, side, 0.016D, 242, 35, 64, fade * pulse);
    }

    private static void ribbon(Matrix4f matrix, VertexConsumer consumer, Vec3 from, Vec3 to,
                               Vec3 side, double halfWidth, int red, int green, int blue, float alpha) {
        Vec3 offset = side.scale(halfWidth);
        int opacity = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        vertex(matrix, consumer, from.add(offset), red, green, blue, opacity);
        vertex(matrix, consumer, from.subtract(offset), red, green, blue, opacity);
        vertex(matrix, consumer, to.subtract(offset), red, green, blue, opacity);
        vertex(matrix, consumer, to.add(offset), red, green, blue, opacity);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, Vec3 point,
                               int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha).endVertex();
    }

    private static RenderType createType(String name, RenderStateShard.TransparencyStateShard blend) {
        return RenderType.create("mysticartifacts_death_scythe_" + name,
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                        .setTransparencyState(blend)
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                        .createCompositeState(false));
    }
}
