package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.network.FlagSpearTrailPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public final class FlagSpearEvents {
    public static final double RANGE = 5.0D;

    private FlagSpearEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isCanceled() || !(event.getTarget() instanceof LivingEntity primaryTarget)) return;

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModItems.FLAG_SPEAR.get())) return;

        Vec3 center = player.position();
        AABB searchArea = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                candidate -> isValidCandidate(player, primaryTarget, candidate)
                        && player.distanceToSqr(candidate) <= RANGE * RANGE
        );

        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity candidate : candidates) {
            float finalDamage = damage + EnchantmentHelper.getDamageBonus(stack, candidate.getMobType());
            if (!candidate.hurt(player.level().damageSources().playerAttack(player), finalDamage)) continue;

            EnchantmentHelper.doPostHurtEffects(candidate, player);
            EnchantmentHelper.doPostDamageEffects(player, candidate);
            player.setLastHurtMob(candidate);
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 trailCenter = center.add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
            FlagSpearTrailPacket.send(serverLevel, trailCenter, player.getYRot());
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
