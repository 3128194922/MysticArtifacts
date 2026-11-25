package com.uniye.mysticartifacts.mixin;

import com.mojang.authlib.GameProfile;
import com.uniye.mysticartifacts.item.impl.SpearItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    public LocalPlayerMixin(ClientLevel level, GameProfile profile) {
        super(level, profile);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean mysticartifacts$skipSpearUseSlowdown(LocalPlayer player) {
        return player.isUsingItem() && !(player.getUseItem().getItem() instanceof SpearItem);
    }

    @Redirect(method = "canStartSprinting", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean mysticartifacts$allowSpearSprintStart(LocalPlayer player) {
        return player.isUsingItem() && !(player.getUseItem().getItem() instanceof SpearItem);
    }
}
