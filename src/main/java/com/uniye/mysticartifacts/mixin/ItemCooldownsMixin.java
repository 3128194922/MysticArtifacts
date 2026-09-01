package com.uniye.mysticartifacts.mixin;

import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.item.impl.EmergencyPlanItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ServerItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家（服务端）获得任意物品冷却时，若佩戴了「紧急预案」且其不在冷却状态，
 * 则抵消该冷却，并把同等时长的冷却转移给「紧急预案」。
 * 客户端通过 ClientboundCooldownPacket 接收的是转移后的结果，无需额外处理。
 */
@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin {

    @Inject(method = "addCooldown(Lnet/minecraft/world/item/Item;I)V",
            at = @At("HEAD"), cancellable = true)
    private void mysticartifacts$absorbByEmergencyPlan(Item item, int duration, CallbackInfo ci) {
        // 紧急预案自身的冷却直接放行，避免递归
        if (item == ModItems.EMERGENCY_PLAN.get()) return;
        // 非正向冷却无需处理
        if (duration <= 0) return;
        // 仅服务端玩家的冷却可被转移（服务端为权威，客户端同步包已被转移）
        if (!((Object) this instanceof ServerItemCooldowns)) return;

        ServerPlayer player = ((ServerItemCooldownsAccessor) (Object) this).mysticartifacts$getPlayer();
        if (player == null) return;
        // 未佩戴紧急预案：不生效
        if (!EmergencyPlanItem.isWearing(player)) return;
        // 紧急预案正在冷却中：无法生效
        if (EmergencyPlanItem.isOnCooldown(player)) return;

        // 抵消原冷却，将同等时长的冷却转移给紧急预案
        ci.cancel();
        player.getCooldowns().addCooldown(ModItems.EMERGENCY_PLAN.get(), duration);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.4F);
    }
}
