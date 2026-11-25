package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uniye.mysticartifacts.entity.CustomOutlineEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;

/**
 * Base renderer that draws an entity normally and then emits its silhouette to the
 * vanilla outline target using the color supplied by {@link CustomOutlineEntity}.
 */
public abstract class CustomOutlineRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final VertexConsumer NO_OP_VERTEX_CONSUMER = new VertexConsumer() {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    };

    protected CustomOutlineRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public final void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight) {
        this.renderContents(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        int color = this.getCustomOutlineColor(entity);
        if (color != CustomOutlineEntity.NO_OUTLINE_COLOR) {
            OutlineBufferSource outlineBuffer = Minecraft.getInstance().renderBuffers().outlineBufferSource();
            outlineBuffer.setColor(
                    FastColor.ARGB32.red(color),
                    FastColor.ARGB32.green(color),
                    FastColor.ARGB32.blue(color),
                    255
            );
            this.renderContents(entity, entityYaw, partialTicks, poseStack,
                    new OutlineOnlyBufferSource(outlineBuffer), packedLight);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    protected int getCustomOutlineColor(T entity) {
        return entity instanceof CustomOutlineEntity outlineEntity
                ? outlineEntity.getCustomOutlineColor()
                : CustomOutlineEntity.NO_OUTLINE_COLOR;
    }

    protected abstract void renderContents(T entity, float entityYaw, float partialTicks,
                                            PoseStack poseStack, MultiBufferSource buffer, int packedLight);

    @Override
    public abstract ResourceLocation getTextureLocation(T entity);

    private static final class OutlineOnlyBufferSource implements MultiBufferSource {
        private final OutlineBufferSource outlineBuffer;

        private OutlineOnlyBufferSource(OutlineBufferSource outlineBuffer) {
            this.outlineBuffer = outlineBuffer;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            RenderType outlineType = renderType.isOutline()
                    ? renderType
                    : renderType.outline().orElse(null);
            return outlineType == null ? NO_OP_VERTEX_CONSUMER : this.outlineBuffer.getBuffer(outlineType);
        }
    }
}
