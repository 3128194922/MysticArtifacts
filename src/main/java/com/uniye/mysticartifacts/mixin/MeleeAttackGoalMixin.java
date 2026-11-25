package com.uniye.mysticartifacts.mixin;

import com.uniye.mysticartifacts.item.impl.SpearItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * 让原版近战 AI 认识长矛：提前进入攻击距离、停在长矛最小射程、
 * 攻击后摇与长矛挥击时长对齐。
 */
@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    private int ticksUntilNextAttack;

    @Inject(method = "getAttackReachSqr", at = @At("RETURN"), cancellable = true)
    private void mysticartifacts$spearReach(LivingEntity target, CallbackInfoReturnable<Double> cir) {
        if (mob.getMainHandItem().getItem() instanceof SpearItem spear) {
            double reach = spear.getMaxRange() * spear.getRangeScale(mob) + 0.5D;
            cir.setReturnValue(reach * reach);
        }
    }

    @Inject(method = "resetAttackCooldown", at = @At("TAIL"))
    private void mysticartifacts$spearCooldown(CallbackInfo ci) {
        if (mob.getMainHandItem().getItem() instanceof SpearItem spear) {
            ticksUntilNextAttack = Math.max(ticksUntilNextAttack, spear.getSwingTicks());
        }
    }

    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z"))
    private boolean mysticartifacts$spearApproach(PathNavigation navigation, Entity target, double speed) {
        if (mob.getMainHandItem().getItem() instanceof SpearItem spear) {
            int stopDistance = Math.max(1, (int) Math.floor(spear.getMinRange() * spear.getRangeScale(mob)));
            @Nullable Path path = navigation.createPath(target, stopDistance);
            if (path != null) return navigation.moveTo(path, speed);
        }
        return navigation.moveTo(target, speed);
    }
}
