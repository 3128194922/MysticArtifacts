package com.uniye.mysticartifacts.client.deathscythe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.DeathScytheItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathScytheOutlineRenderer {
    private static final int OUTLINE_RED = 190;
    private static final int OUTLINE_GREEN = 35;
    private static final int OUTLINE_BLUE = 95;
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

    private DeathScytheOutlineRenderer() {
    }

    @SubscribeEvent
    public static void renderAfterEntities(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) return;

        if (!isHoldingScythe(minecraft.player.getMainHandItem(), minecraft.player.getOffhandItem())) return;
        UUID targetUUID = DeathScytheTargetClientState.targetUUID();
        if (targetUUID == null) return;
        Entity target = findEntity(level, targetUUID);
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return;

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        OutlineBufferSource outlineBuffer = minecraft.renderBuffers().outlineBufferSource();
        outlineBuffer.setColor(OUTLINE_RED, OUTLINE_GREEN, OUTLINE_BLUE, 255);
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        Vec3 targetPosition = target.getPosition(event.getPartialTick());
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        dispatcher.render(target, targetPosition.x, targetPosition.y, targetPosition.z,
                Mth.lerp(event.getPartialTick(), target.yRotO, target.getYRot()), event.getPartialTick(), poseStack,
                new OutlineOnlyBufferSource(outlineBuffer), dispatcher.getPackedLightCoords(target, event.getPartialTick()));
        poseStack.popPose();
        outlineBuffer.endOutlineBatch();
    }

    private static boolean isHoldingScythe(ItemStack mainHand, ItemStack offHand) {
        return mainHand.getItem() instanceof DeathScytheItem
                || offHand.getItem() instanceof DeathScytheItem;
    }

    private static Entity findEntity(ClientLevel level, UUID targetUUID) {
        for (Entity entity : level.entitiesForRendering()) {
            if (targetUUID.equals(entity.getUUID())) return entity;
        }
        return null;
    }

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
            return outlineType == null ? NO_OP_VERTEX_CONSUMER : outlineBuffer.getBuffer(outlineType);
        }
    }
}
