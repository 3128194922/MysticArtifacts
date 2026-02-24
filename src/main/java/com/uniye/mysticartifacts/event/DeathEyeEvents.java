package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.DeathEyeItem;
import com.uniye.mysticartifacts.util.DeathEyeCutLine;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public class DeathEyeEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (event.getSource().getDirectEntity() != player) return;

        LivingEntity target = event.getEntity();
        if (!DeathEyeItem.isWearing(player)) return;
        if (player.distanceToSqr(target) > 36.0) return;

        DeathEyeCutLine.CutLine line = DeathEyeCutLine.compute(target, player.level().getGameTime());
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(10.0));

        double dist = distanceBetweenSegments(eye, end, line.from(), line.to());
        double threshold = Math.max(0.2, target.getBbWidth() * 0.15);
        if (dist <= threshold) {
            event.setAmount(event.getAmount() * 2.0f);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private static double distanceBetweenSegments(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
        Vec3 u = q1.subtract(p1);
        Vec3 v = q2.subtract(p2);
        Vec3 w = p1.subtract(p2);
        double a = u.dot(u);
        double b = u.dot(v);
        double c = v.dot(v);
        double d = u.dot(w);
        double e = v.dot(w);
        double D = a * c - b * b;
        double sc;
        double sN;
        double sD = D;
        double tc;
        double tN;
        double tD = D;

        double SMALL = 1.0e-8;

        if (D < SMALL) {
            sN = 0.0;
            sD = 1.0;
            tN = e;
            tD = c;
        } else {
            sN = (b * e - c * d);
            tN = (a * e - b * d);
            if (sN < 0.0) {
                sN = 0.0;
                tN = e;
                tD = c;
            } else if (sN > sD) {
                sN = sD;
                tN = e + b;
                tD = c;
            }
        }

        if (tN < 0.0) {
            tN = 0.0;
            if (-d < 0.0) {
                sN = 0.0;
            } else if (-d > a) {
                sN = sD;
            } else {
                sN = -d;
                sD = a;
            }
        } else if (tN > tD) {
            tN = tD;
            if ((-d + b) < 0.0) {
                sN = 0.0;
            } else if ((-d + b) > a) {
                sN = sD;
            } else {
                sN = (-d + b);
                sD = a;
            }
        }

        sc = (Math.abs(sN) < SMALL ? 0.0 : sN / sD);
        tc = (Math.abs(tN) < SMALL ? 0.0 : tN / tD);

        Vec3 dP = w.add(u.scale(sc)).subtract(v.scale(tc));
        return dP.length();
    }
}
