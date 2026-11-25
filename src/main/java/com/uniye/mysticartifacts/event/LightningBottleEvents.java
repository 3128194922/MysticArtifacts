package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.config.LightningBottleConfig;
import com.uniye.mysticartifacts.init.ModDamageTypes;
import com.uniye.mysticartifacts.init.ModSounds;
import com.uniye.mysticartifacts.item.impl.LightningBottleItem;
import com.uniye.mysticartifacts.lightning.LightningBottleLogic;
import com.uniye.mysticartifacts.network.LightningBottlePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public final class LightningBottleEvents {
    private static final double CHAIN_RANGE = 8.0D;
    private static final float CHAIN_DAMAGE = 4.0F;
    private static final double FIELD_RADIUS = 5.0D;
    private static final float FIELD_DAMAGE = 4.0F;

    private LightningBottleEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F) return;
        if (event.getSource().is(ModDamageTypes.LIGHTNING_BOTTLE_CHAIN)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity primaryTarget = event.getEntity();
        if (!LightningBottleItem.isWearing(player)) return;

        int maxTargets = LightningBottleLogic.effectiveMaxTargets(
                LightningBottleConfig.MAX_TARGETS.get(),
                player.level().isRaining()
        );
        if (maxTargets <= 0) return;

        // Merge the primary hit's bonus so its existing hurt frames cannot swallow it.
        event.setAmount(event.getAmount() + CHAIN_DAMAGE);
        DamageSource chainSource = ModDamageTypes.getSource(player.level(), ModDamageTypes.LIGHTNING_BOTTLE_CHAIN, player, player);
        AABB searchArea = primaryTarget.getBoundingBox().inflate(CHAIN_RANGE);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                candidate -> candidate.isAlive()
                        && candidate != player
                        && candidate != primaryTarget
                        && !candidate.isSpectator()
                        && (!(candidate instanceof Player targetPlayer) || player.canHarmPlayer(targetPlayer))
        );
        candidates.sort(Comparator.comparingDouble(primaryTarget::distanceToSqr));

        List<Vec3> path = new ArrayList<>();
        path.add(player.getEyePosition());
        path.add(primaryTarget.position().add(0.0D, primaryTarget.getBbHeight() * 0.5D, 0.0D));
        List<LivingEntity> struck = new ArrayList<>();
        for (LivingEntity candidate : candidates) {
            if (struck.size() >= maxTargets) break;
            if (!candidate.hurt(chainSource, CHAIN_DAMAGE)) continue;
            struck.add(candidate);
            path.add(candidate.position().add(0.0D, candidate.getBbHeight() * 0.5D, 0.0D));
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null,
                    primaryTarget.getX(),
                    primaryTarget.getY(),
                    primaryTarget.getZ(),
                    ModSounds.LIGHTNING_BOTTLE_ZAP.get(),
                    SoundSource.PLAYERS,
                    0.9F,
                    1.0F
            );
            ItemStack bottle = LightningBottleItem.findEquippedBottle(player).orElse(ItemStack.EMPTY);
            boolean fieldReleased = !bottle.isEmpty() && LightningBottleItem.addThunderPower(
                    bottle,
                    1 + struck.size(),
                    LightningBottleConfig.MAX_THUNDER_POWER.get()
            );
            if (fieldReleased) {
                releaseLightningField(player);
            }
            LightningBottlePacket.send(
                    serverLevel,
                    primaryTarget.position(),
                    path,
                    fieldReleased ? player.position() : null
            );
        }
    }

    private static void releaseLightningField(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.LIGHTNING_BOTTLE_BURST.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        AABB area = player.getBoundingBox().inflate(FIELD_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target.isAlive()
                        && target != player
                        && !target.isSpectator()
                        && target.distanceToSqr(player) <= FIELD_RADIUS * FIELD_RADIUS
                        && (!(target instanceof Player targetPlayer) || player.canHarmPlayer(targetPlayer))
        );
        DamageSource fieldSource = ModDamageTypes.getSource(level, ModDamageTypes.LIGHTNING_BOTTLE_CHAIN, player, player);
        for (LivingEntity target : targets) {
            target.hurt(fieldSource, FIELD_DAMAGE);
        }
    }
}
