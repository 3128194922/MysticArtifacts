package com.uniye.mysticartifacts.client.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.entity.EnderKunaiEntity;
import com.uniye.mysticartifacts.network.NetworkHandler;
import com.uniye.mysticartifacts.network.TeleportToKunaiPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, value = Dist.CLIENT)
public class KunaiTeleportHandler {

    @SubscribeEvent
    public static void onRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        if (mc.crosshairPickEntity != null) {
            boolean isCrosshairValid = false;
            if (mc.crosshairPickEntity instanceof EnderKunaiEntity) {
                isCrosshairValid = true;
            }
            
            if (isCrosshairValid) {
                NetworkHandler.INSTANCE.sendToServer(new TeleportToKunaiPacket(mc.crosshairPickEntity.getId()));
                event.setCanceled(true);
                event.setSwingHand(true);
                return;
            }
        }
        
        double range = 64.0; 
        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        Vec3 viewVec = mc.player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.scale(range));
        AABB searchBox = mc.player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player,
                eyePos,
                endPos,
                searchBox,
                (entity) -> {
                    if (entity == mc.player) return false;
                    return entity instanceof EnderKunaiEntity;
                },
                range * range
        );
        
        if (entityHit != null) {
            boolean isValidTarget = false;
            if (entityHit.getEntity() instanceof EnderKunaiEntity) {
                isValidTarget = true;
            }

            if (!isValidTarget) {
                return;
            }

            Vec3 hitVec = entityHit.getLocation();
            double distToEntity = eyePos.distanceToSqr(hitVec);
            
            BlockHitResult blockHit = mc.level.clip(new ClipContext(
                    eyePos,
                    hitVec,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mc.player
            ));

            boolean blocked = false;
            if (blockHit.getType() != HitResult.Type.MISS) {
                double distToBlock = eyePos.distanceToSqr(blockHit.getLocation());
                if (distToBlock < distToEntity - 0.1) {
                    blocked = true;
                }
            }

            if (!blocked) {
                NetworkHandler.INSTANCE.sendToServer(new TeleportToKunaiPacket(entityHit.getEntity().getId()));
                event.setCanceled(true);
                event.setSwingHand(true);
            }
        }
    }
}
