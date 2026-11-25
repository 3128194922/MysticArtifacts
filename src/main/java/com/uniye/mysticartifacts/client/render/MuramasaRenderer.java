package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.KatanaState;
import com.uniye.mysticartifacts.item.impl.MuramasaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib renderer for Muramasa's ordinary and demon forms. */
public final class MuramasaRenderer extends GeoItemRenderer<MuramasaItem> {
    public MuramasaRenderer() {
        super(new Model());
    }

    @Override
    public void preRender(PoseStack poseStack, MuramasaItem item, BakedGeoModel model,
            MultiBufferSource bufferSource, VertexConsumer buffer, boolean reRender, float partialTick,
            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, item, model, bufferSource, buffer, reRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);

        boolean demonForm = KatanaState.isOpen(getCurrentItemStack(), Minecraft.getInstance().level);
        model.getBone("normal_form").ifPresent(bone -> bone.setHidden(demonForm));
        model.getBone("demon_form").ifPresent(bone -> bone.setHidden(!demonForm));
    }

    private static final class Model extends GeoModel<MuramasaItem> {
        private ResourceLocation resource(String path) {
            return new ResourceLocation(MysticArtifacts.MODID, path);
        }

        @Override
        public ResourceLocation getModelResource(MuramasaItem item) {
            return resource("geo/muramasa.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(MuramasaItem item) {
            return resource("textures/item/muramasa.png");
        }

        @Override
        public ResourceLocation getAnimationResource(MuramasaItem item) {
            return resource("animations/muramasa.animation.json");
        }
    }
}
