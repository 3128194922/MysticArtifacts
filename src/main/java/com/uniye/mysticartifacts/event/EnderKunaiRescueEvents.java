package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.entity.EnderKunaiEntity;
import com.uniye.mysticartifacts.util.EnderKunaiTracker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public class EnderKunaiRescueEvents {

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        EnderKunaiEntity kunai = EnderKunaiTracker.findNearestGroundedKunai(player);
        if (kunai == null) {
            return;
        }

        double destX = kunai.getX();
        double destY = kunai.getY();
        double destZ = kunai.getZ();

        player.connection.teleport(destX, destY, destZ, player.getYRot(), player.getXRot());
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.fallDistance = 0.0F;
        event.setCanceled(true);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, destX, destY, destZ, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.PORTAL, destX, destY, destZ, 16, 0.5, 0.5, 0.5, 0.1);
        }

        kunai.discard();
    }
}
