package com.uniye.mysticartifacts.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** SlashBlade 普通混合与发光混合渲染状态的 MysticArtifacts 独立实现。 */
public final class KatanaRenderTypes extends RenderStateShard {
    private static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard("katana_additive_transparency", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    private KatanaRenderTypes() {
        super("katana_render_types", () -> { }, () -> { });
    }

    public static RenderType blend(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, true))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(true);
        return RenderType.create("katana_blend", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLES, 256, true, false, state);
    }

    public static RenderType colorWrite(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, true))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("katana_color_write", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLES, 256, false, true, state);
    }

    public static RenderType luminous(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setCullState(NO_CULL)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, true, true))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("katana_luminous", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLES, 256, false, true, state);
    }
}
