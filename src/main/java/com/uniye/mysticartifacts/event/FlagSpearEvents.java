package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModDamageTypes;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.init.ModSounds;
import com.uniye.mysticartifacts.item.impl.FlagSpearItem;
import com.uniye.mysticartifacts.network.FlagSpearTrailPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 旗枪充能攻击：持有充能时左键攻击触发范围爆发（与插地爆发同款），
 * 范围为插地爆发的两倍，每次触发消耗 1 层充能。
 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public final class FlagSpearEvents {
    private static final double CHARGE_RANGE_MULTIPLIER = 2.0D;

    private FlagSpearEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isCanceled() || !(event.getTarget() instanceof LivingEntity primaryTarget)) return;

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModItems.FLAG_SPEAR.get())) return;

        int charge = FlagSpearItem.getCharge(stack);
        if (charge <= 0) return;

        FlagSpearItem.setCharge(stack, charge - 1);

        double range = Config.FlagSpearBurstRange * CHARGE_RANGE_MULTIPLIER;
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 center = player.position();
        AABB searchArea = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                candidate -> isValidCandidate(player, primaryTarget, candidate)
                        && player.distanceToSqr(candidate) <= range * range
        );

        DamageSource source = ModDamageTypes.getSource(player.level(), ModDamageTypes.SPEAR, player, player);
        for (LivingEntity candidate : candidates) {
            candidate.hurt(source, damage);
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.FLAG_SPEAR_BURST.get(),
                    SoundSource.PLAYERS,
                    0.9F,
                    1.0F
            );
            Vec3 trailCenter = center.add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
            FlagSpearTrailPacket.send(serverLevel, trailCenter, player.getYRot(), player.getRandom().nextLong());
        }
    }

    private static boolean isValidCandidate(Player player, LivingEntity primaryTarget, LivingEntity candidate) {
        return candidate != player
                && candidate != primaryTarget
                && candidate.isAlive()
                && !candidate.isSpectator()
                && !candidate.isInvulnerable()
                && (!(candidate instanceof Player targetPlayer) || player.canHarmPlayer(targetPlayer));
    }
}
