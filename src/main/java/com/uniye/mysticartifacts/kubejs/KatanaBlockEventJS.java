package com.uniye.mysticartifacts.kubejs;

import com.uniye.mysticartifacts.event.KatanaBlockEvent;
import dev.latvian.mods.kubejs.server.ServerEventJS;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** KubeJS payload for a successful Muramasa block. */
public class KatanaBlockEventJS extends ServerEventJS {
    private final LivingEntity blocker;
    private final Entity attacker;
    private final Projectile projectile;
    private final DamageSource damageSource;
    private final float blockedDamage;
    private final boolean perfect;

    public KatanaBlockEventJS(MinecraftServer server, KatanaBlockEvent event) {
        super(server);
        this.blocker = event.getBlocker();
        this.attacker = event.getAttacker();
        this.projectile = event.getProjectile();
        this.damageSource = event.getDamageSource();
        this.blockedDamage = event.getBlockedDamage();
        this.perfect = event.isPerfect();
    }

    public LivingEntity getBlocker() {
        return blocker;
    }

    public ServerPlayer getPlayer() {
        return blocker instanceof ServerPlayer player ? player : null;
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
