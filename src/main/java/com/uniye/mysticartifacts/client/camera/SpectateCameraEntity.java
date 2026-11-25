package com.uniye.mysticartifacts.client.camera;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SpectateCameraEntity extends Entity {

    public SpectateCameraEntity(Level level) {
        super(EntityType.MARKER, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void follow(Player target) {
        Vec3 oldEye = target.getEyePosition(0.0F);
        Vec3 currentEye = target.getEyePosition(1.0F);
        this.xOld = oldEye.x;
        this.yOld = oldEye.y;
        this.zOld = oldEye.z;
        this.setPosRaw(currentEye.x, currentEye.y, currentEye.z);
        this.yRotO = target.getViewYRot(0.0F);
        this.setYRot(target.getViewYRot(1.0F));
        this.xRotO = target.getViewXRot(0.0F);
        this.setXRot(target.getViewXRot(1.0F));
    }

    @Override
    public float getEyeHeight(Pose pose) {
        return 0.0F;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
