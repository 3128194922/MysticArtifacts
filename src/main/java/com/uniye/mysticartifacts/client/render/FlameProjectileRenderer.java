package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uniye.mysticartifacts.entity.FlameProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class FlameProjectileRenderer extends EntityRenderer<FlameProjectileEntity> {

    public FlameProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FlameProjectileEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Visual effect is handled by particles spawned in the entity's tick()
    }

    @Override
    public ResourceLocation getTextureLocation(FlameProjectileEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
