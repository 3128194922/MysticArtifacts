package com.uniye.mysticartifacts.client.render;

import com.uniye.mysticartifacts.item.impl.SwordSwarmCharm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;
import net.minecraft.client.model.EntityModel;

import java.util.List;

public class SwordSwarmCharmRenderer implements ICurioRenderer {
    private static final int[] LAYER_COUNTS = new int[]{6, 12, 24};
    private static final double[] LAYER_SPIN_SPEED = new double[]{3.0, 4.5, 6.0};
    private static final double[] LAYER_RADIUS_PULSE_SPEED = new double[]{0.0, 0.6, 0.8};
    private static final double RADIUS_PULSE_AMPLITUDE = 0.18;

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.level();
        List<ResourceLocation> queue = SwordSwarmCharm.getDisplayQueue(stack, level);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        poseStack.pushPose();
        
        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-bodyYaw));

        double anchorFactor = 0.5;
        poseStack.translate(0.0, entity.getBbHeight() * anchorFactor - 1.0, 0.0);
        
        double radius = 1.0;
        double floatSpeed = 0.08;
        double floatAmplitude = 0.08;
        double phaseStep = 0.7;
        double spinSpeedDegPerTick = 6.0;

        List<ResourceLocation> devoured = SwordSwarmCharm.getDevouredList(stack);
        List<ResourceLocation> source = devoured.isEmpty() ? queue : devoured;
        int layers = 2;
        double baseRadius = 1.6;
        double layerGap = 0.8;
        double radiusNoise = 0.25;
        double spinBase = (ageInTicks + partialTicks);

        int count = Math.min(6, queue.size());
        for (int i = 0; i < count; i++) {
            ResourceLocation id = queue.get(i);
            ItemStack visual = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
            if (visual.isEmpty()) continue;
            double stepDeg = count > 0 ? (360.0 / count) : 0.0;
            double spinDeg = (ageInTicks + partialTicks) * spinSpeedDegPerTick;
            double angleDeg = stepDeg * i + spinDeg;
            double angle = Math.toRadians(angleDeg);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            
            double t = (ageInTicks + partialTicks) * floatSpeed;
            double dy = Math.sin(t + i * phaseStep) * floatAmplitude;
            poseStack.pushPose();
            poseStack.translate(x, 0.3 + dy, z);

            double baseYawDeg = 0.0;
            double basePitchDeg = -180.0;
            double baseRollDeg = -45.0;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)(baseYawDeg)));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float)basePitchDeg));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float)baseRollDeg));
            
            itemRenderer.renderStatic(visual, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, level, entity.getId());
            poseStack.popPose();
        }
        if (!source.isEmpty()) {
            for (int l = 0; l < layers; l++) {
                double r = baseRadius + l * layerGap;
                int perLayer = LAYER_COUNTS[Math.min(l, LAYER_COUNTS.length - 1)];
                double spinDeg = spinBase * LAYER_SPIN_SPEED[Math.min(l, LAYER_SPIN_SPEED.length - 1)];
                double pulseSpeed = LAYER_RADIUS_PULSE_SPEED[Math.min(l, LAYER_RADIUS_PULSE_SPEED.length - 1)];
                for (int i = 0; i < perLayer; i++) {
                    ResourceLocation id = source.get((i + l * perLayer) % source.size());
                    ItemStack visual = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
                    if (visual.isEmpty()) continue;
                    double stepDeg = 360.0 / perLayer;
                    double angleDeg = stepDeg * i + spinDeg;
                    double angle = Math.toRadians(angleDeg);
                    double j = Math.sin((i + 31.0 * l) * 12.9898);
                    double rn = j;
                    double pulse = Math.sin(spinBase * pulseSpeed) * RADIUS_PULSE_AMPLITUDE;
                    double rr = r + rn * radiusNoise + pulse;
                    double x = Math.cos(angle) * rr;
                    double z = Math.sin(angle) * rr;
                    double t = (ageInTicks + partialTicks) * floatSpeed;
                    double dy = Math.sin(t + (i + l) * phaseStep) * floatAmplitude;
                    poseStack.pushPose();
                    poseStack.translate(x, 0.25 + dy + l * 0.08, z);
                    double yawDeg = angleDeg + 90.0;
                    double basePitchDeg = -180.0;
                    double baseRollDeg = -45.0;
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) yawDeg));
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) basePitchDeg));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) baseRollDeg));
                    itemRenderer.renderStatic(visual, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, level, entity.getId());
                    poseStack.popPose();
                }
            }
        }
        poseStack.popPose();
    }
}
