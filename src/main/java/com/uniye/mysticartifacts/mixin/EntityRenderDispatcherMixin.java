package com.uniye.mysticartifacts.mixin;

import com.uniye.mysticartifacts.client.sculk.SculkSymbioteClientState;
import com.uniye.mysticartifacts.item.impl.TrailSightItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 佩戴幽匿共生体时隐藏所有实体，只保留方块与自定义声音标记。 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void mysticartifacts$hideEntities(Entity entity, double x, double y, double z,
                                                                  float entityYaw, float partialTick,
                                                                  PoseStack poseStack,
                                                                  MultiBufferSource buffer,
                                                                  int packedLight, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (SculkSymbioteClientState.isActive()
                || minecraft.player != null && TrailSightItem.isWearing(minecraft.player)) {
            ci.cancel();
        }
    }
}
