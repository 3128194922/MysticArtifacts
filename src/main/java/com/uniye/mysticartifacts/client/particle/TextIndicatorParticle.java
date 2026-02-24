package com.uniye.mysticartifacts.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class TextIndicatorParticle extends Particle {

    private static final ParticleGroup GROUP = new ParticleGroup(1000);
    private final Component text;
    private final int color;
    private final int outlineColor;
    private final boolean outline;
    private final float baseScale;
    private float scale;
    private float prevScale;

    public TextIndicatorParticle(ClientLevel level, double x, double y, double z, String text, int color, boolean outline, Integer outlineColor, float size) {
        super(level, x, y, z);
        this.lifetime = 15 + level.random.nextInt(5);
        this.text = Component.literal(text);
        this.color = color;
        this.outlineColor = outlineColor != null ? outlineColor : 0x330000;
        this.outline = outline;
        this.baseScale = size;
        this.scale = 1.0F;
        this.yd = 0.2F + Math.random() * 0.2F;
        this.gravity = 1.3F;
    }

    @Override
    public void tick() {
        super.tick();
        float t = age / (float) lifetime;
        this.prevScale = scale;
        this.scale = 1.0F - t;
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float partialTicks) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 cam = camera.getPosition();
        double rx = Mth.lerp(partialTicks, this.xo, this.x);
        double ry = Mth.lerp(partialTicks, this.yo, this.y);
        double rz = Mth.lerp(partialTicks, this.zo, this.z);
        float s = this.getScale(partialTicks) * 0.035F * baseScale;
        PoseStack stack = new PoseStack();
        stack.pushPose();
        stack.translate(rx - cam.x, ry - cam.y, rz - cam.z);
        stack.mulPose(camera.rotation());
        stack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        float xoff = -Minecraft.getInstance().font.width(text) / 2.0F;
        stack.scale(s, s, s);
        stack.translate(0.0F, -2.0F, 0.0F);
        if (outline) {
            Minecraft.getInstance().font.drawInBatch8xOutline(text.getVisualOrderText(), xoff, 0.0F, color, outlineColor, stack.last().pose(), bufferSource, 15728880);
        } else {
            Minecraft.getInstance().font.drawInBatch(text.getVisualOrderText(), xoff, 0.0F, color, false, stack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        }
        bufferSource.endBatch();
        stack.popPose();
    }

    private float getScale(float partialTicks) {
        return prevScale + (scale - prevScale) * partialTicks;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public Optional<ParticleGroup> getParticleGroup() {
        return Optional.of(GROUP);
    }
}
