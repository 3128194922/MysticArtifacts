package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.item.impl.WitchPotItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public class WitchPotEvents {
    private static final int EFFECT_DURATION = 100;
    private static final double SPLASH_RANGE = 8.0;
    private static final double LINGERING_RANGE = 4.0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            var found = handler.findCurios(ModItems.WITCH_POT.get());
            for (var slotResult : found) {
                ItemStack stack = slotResult.stack();
                if (!WitchPotItem.hasPotionData(stack)) continue;

                int type = WitchPotItem.getPotionType(stack);
                List<MobEffectInstance> effects = WitchPotItem.getStoredEffects(stack);
                if (effects.isEmpty()) continue;

                switch (type) {
                    case WitchPotItem.TYPE_NORMAL -> applyTo(player, effects);
                    case WitchPotItem.TYPE_SPLASH -> applyToNearby(player, effects, SPLASH_RANGE, false);
                    case WitchPotItem.TYPE_LINGERING -> {
                        applyTo(player, effects);
                        applyToNearby(player, effects, LINGERING_RANGE, true);
                    }
                }
            }
        });
    }

    private static void applyTo(Player target, List<MobEffectInstance> effects) {
        for (MobEffectInstance effect : effects) {
            MobEffectInstance prolonged = new MobEffectInstance(
                    effect.getEffect(), EFFECT_DURATION, effect.getAmplifier(),
                    effect.isAmbient(), effect.isVisible(), effect.showIcon());
            target.addEffect(prolonged);
        }
    }

    private static void applyToNearby(Player source, List<MobEffectInstance> effects, double range, boolean includeSelf) {
        AABB aabb = source.getBoundingBox().inflate(range);
        for (Player target : source.level().getEntitiesOfClass(Player.class, aabb)) {
            if (target == source && !includeSelf) continue;
            applyTo(target, effects);
        }
    }
}
