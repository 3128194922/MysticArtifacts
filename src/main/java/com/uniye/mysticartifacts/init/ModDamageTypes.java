package com.uniye.mysticartifacts.init;

import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> COLLAPSING = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "collapsing"));
    public static final ResourceKey<DamageType> NOPAL_PRICK = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "nopal_prick"));
    public static final ResourceKey<DamageType> CEASING = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "ceasing"));
    public static final ResourceKey<DamageType> ROCKET_PUNCH = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "rocket_punch"));
    public static final ResourceKey<DamageType> WALL_SLAM = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "wall_slam"));
    public static final ResourceKey<DamageType> TWO_DRAGONS_PLAY_BALL_FIRE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "two_dragons_play_ball_fire"));
    public static final ResourceKey<DamageType> TWO_DRAGONS_PLAY_BALL_ICE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "two_dragons_play_ball_ice"));
    public static final ResourceKey<DamageType> TWO_DRAGONS_PLAY_BALL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "two_dragons_play_ball"));
    public static final ResourceKey<DamageType> VOID_SLICE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "void_slice"));
    public static final ResourceKey<DamageType> POKER_SLICE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "poker_slice"));
    public static final ResourceKey<DamageType> PHANTOM_SWORD = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "phantom_sword"));
    public static final ResourceKey<DamageType> IAIDO = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MysticArtifacts.MODID, "iaido"));

    public static DamageSource getSimpleDamageSource(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }

    public static DamageSource getSource(Level level, ResourceKey<DamageType> type, @javax.annotation.Nullable net.minecraft.world.entity.Entity direct, @javax.annotation.Nullable net.minecraft.world.entity.Entity causing) {
        return new DamageSource(
            level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),
            direct,
            causing
        );
    }
}
