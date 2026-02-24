package com.uniye.mysticartifacts.client.render;

import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class ModArrowRenderer extends ArrowRenderer<AbstractArrow> {
    private final ResourceLocation texture;

    public ModArrowRenderer(EntityRendererProvider.Context context, String textureName) {
        super(context);
        this.texture = new ResourceLocation(MysticArtifacts.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractArrow entity) {
        return texture;
    }
}
