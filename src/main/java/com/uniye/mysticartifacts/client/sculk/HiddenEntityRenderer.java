package com.uniye.mysticartifacts.client.sculk;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** 保留原 renderer 的资源和裁剪判断，但在幽匿视觉开启时不提交实体模型。 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class HiddenEntityRenderer extends EntityRenderer<Entity> {
    private final EntityRenderer delegate;

    private HiddenEntityRenderer(EntityRendererProvider.Context context, EntityRenderer delegate) {
        super(context);
        this.delegate = delegate;
    }

    public static EntityRenderer<?> wrap(EntityRendererProvider.Context context, EntityRenderer<?> renderer) {
        if (renderer instanceof HiddenEntityRenderer) return renderer;
        return new HiddenEntityRenderer(context, renderer);
    }

    public EntityRenderer<?> delegate() {
        return delegate;
    }

    @Override
    public boolean shouldRender(Entity entity, Frustum frustum, double camX, double camY, double camZ) {
        return !SculkSymbioteClientState.isActive() && delegate.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    public Vec3 getRenderOffset(Entity entity, float partialTick) {
        return delegate.getRenderOffset(entity, partialTick);
    }

    @Override
    public void render(Entity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (!SculkSymbioteClientState.isActive()) {
            delegate.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        return delegate.getTextureLocation(entity);
    }
}
