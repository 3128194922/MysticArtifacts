package com.uniye.mysticartifacts.client.render;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.DeathScytheItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class DeathScytheRenderer extends GeoItemRenderer<DeathScytheItem> {
    public DeathScytheRenderer() {
        super(new Model());
    }

    private static final class Model extends GeoModel<DeathScytheItem> {
        @Override
        public ResourceLocation getModelResource(DeathScytheItem item) {
            return new ResourceLocation(MysticArtifacts.MODID, "geo/death_scythe.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(DeathScytheItem item) {
            return new ResourceLocation(MysticArtifacts.MODID, "textures/item/death_scythe.png");
        }

        @Override
        public ResourceLocation getAnimationResource(DeathScytheItem item) {
            return new ResourceLocation(MysticArtifacts.MODID, "animations/death_scythe.animation.json");
        }
    }
}
