package com.uniye.mysticartifacts.mixin;

import com.mojang.authlib.GameProfile;
import com.uniye.mysticartifacts.item.impl.SpearItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    @Shadow public Input input;
    @Shadow protected int sprintTriggerTime;

    public LocalPlayerMixin(ClientLevel level, GameProfile profile) {
        super(level, profile);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    public void mysticartifacts$compensateSpearMovement(CallbackInfo ci) {
        if (this.getUseItem().getItem() instanceof SpearItem) {
            input.leftImpulse *= 5.0F;
            input.forwardImpulse *= 5.0F;
            sprintTriggerTime = 7;
        }
    }
}
