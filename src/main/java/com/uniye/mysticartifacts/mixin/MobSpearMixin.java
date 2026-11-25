package com.uniye.mysticartifacts.mixin;

import com.uniye.mysticartifacts.item.impl.SpearItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 怪物近战攻击入口：持长矛时改为按怪物自身头部视角进行穿透突刺。
 */
@Mixin(Mob.class)
public abstract class MobSpearMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void mysticartifacts$spearHurtTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide()) return;
        if (!(self.getMainHandItem().getItem() instanceof SpearItem spear)) return;

        aimAt(self, target);
        cir.setReturnValue(spear.stab(self, EquipmentSlot.MAINHAND, target));
    }

    @Inject(method = "getMeleeAttackRangeSqr", at = @At("RETURN"), cancellable = true)
    private void mysticartifacts$spearMeleeRange(LivingEntity target, CallbackInfoReturnable<Double> cir) {
        Mob self = (Mob) (Object) this;
        if (self.getMainHandItem().getItem() instanceof SpearItem spear) {
            double reach = spear.getMaxRange() * spear.getRangeScale(self);
            cir.setReturnValue(reach * reach);
        }
    }

    /** 把怪物头部/身体立即对准目标，保证“怪物视角”判定命中 */
    private static void aimAt(Mob mob, Entity target) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        double dy = target.getEyeY() - mob.getEyeY();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0D / Math.PI)));

        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setXRot(pitch);
    }
}
