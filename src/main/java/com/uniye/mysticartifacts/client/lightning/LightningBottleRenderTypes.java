package com.uniye.mysticartifacts.client.lightning;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class LightningBottleRenderTypes {
    private static final int NO_DEPTH_TEST_FUNCTION = 519;
    private static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard("lightning_bottle_additive", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO
                );
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    private static final RenderType OUTER = create("outer");
    private static final RenderType MIDDLE = create("middle");
    private static final RenderType INNER = create("inner");

    private LightningBottleRenderTypes() {
    }

    public static RenderType outer() {
        return OUTER;
    }

    public static RenderType middle() {
        return MIDDLE;
    }

    public static RenderType inner() {
        return INNER;
    }

    private static RenderType create(String layer) {
        return RenderType.create(
                "mysticartifacts_lightning_bottle_" + layer,
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", NO_DEPTH_TEST_FUNCTION))
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                        .createCompositeState(false)
        );
    }
}
