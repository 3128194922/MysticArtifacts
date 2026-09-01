package com.uniye.mysticartifacts.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on the Forge event bus after Muramasa successfully blocks an attack.
 * The event is server-side and is also used as the bridge for the optional KubeJS integration.
 */
public class KatanaBlockEvent extends Event {
    private final LivingEntity blocker;
    private final Entity attacker;
    private final Projectile projectile;
    private final DamageSource damageSource;
    private final float blockedDamage;
    private final boolean perfect;

    public KatanaBlockEvent(LivingEntity blocker, Entity attacker, Projectile projectile,
                            DamageSource damageSource, float blockedDamage, boolean perfect) {
        this.blocker = blocker;
        this.attacker = attacker;
        this.projectile = projectile;
        this.damageSource = damageSource;
        this.blockedDamage = blockedDamage;
        this.perfect = perfect;
    }

    public LivingEntity getBlocker() {
        return blocker;
    }

    public Entity getAttacker() {
        return attacker;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public float getBlockedDamage() {
        return blockedDamage;
    }

    public boolean isPerfect() {
        return perfect;
    }
}
