package com.uniye.mysticartifacts.entity;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.init.ModEntities;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class VoidArrowEntity extends AbstractArrow {

    public VoidArrowEntity(EntityType<? extends VoidArrowEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPierceLevel((byte) 127);
    }

    public VoidArrowEntity(EntityType<? extends VoidArrowEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level);
        this.setNoGravity(true);
        this.setPierceLevel((byte) 127);
    }

    @Override
    public void tick() {
        super.tick();

        // 保持恒定速度
        double speed = 5.0; // 自定义速度
        this.setDeltaMovement(this.getDeltaMovement().normalize().scale(speed));

        if (this.tickCount >= Math.max(1, Config.VoidArrowLifetime)) {
            this.discard();
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.VOID_ARROW.get());
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
