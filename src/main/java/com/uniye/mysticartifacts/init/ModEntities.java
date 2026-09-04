package com.uniye.mysticartifacts.init;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MysticArtifacts.MODID);

    public static final RegistryObject<EntityType<DemonicGestationEntity>> DEMONIC_GESTATION = ENTITIES.register("demonic_gestation",
            () -> EntityType.Builder.<DemonicGestationEntity>of(DemonicGestationEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("demonic_gestation")
    );

    public static final RegistryObject<EntityType<SculkArrow>> SCULK_ARROW = ENTITIES.register("sculk_arrow",
            () -> EntityType.Builder.<SculkArrow>of(SculkArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("sculk_arrow")
    );

    public static final RegistryObject<EntityType<AirburstArrowEntity>> AIRBURST_ARROW = ENTITIES.register("airburst_arrow",
            () -> EntityType.Builder.<AirburstArrowEntity>of(AirburstArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("airburst_arrow")
    );

    public static final RegistryObject<EntityType<ExplodingArrowEntity>> EXPLODING_ARROW = ENTITIES.register("exploding_arrow",
            () -> EntityType.Builder.<ExplodingArrowEntity>of(ExplodingArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("exploding_arrow")
    );

    public static final RegistryObject<EntityType<FinalExplodingArrowEntity>> FINAL_EXPLODING_ARROW = ENTITIES.register("final_exploding_arrow",
            () -> EntityType.Builder.<FinalExplodingArrowEntity>of(FinalExplodingArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("final_exploding_arrow")
    );

    public static final RegistryObject<EntityType<EnderKunaiEntity>> ENDER_KUNAI = ENTITIES.register("ender_kunai",
            () -> EntityType.Builder.<EnderKunaiEntity>of(EnderKunaiEntity::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("ender_kunai")
    );

    public static final RegistryObject<EntityType<TwoDragonsPlayBallEntity>> TWO_DRAGONS_PLAY_BALL = ENTITIES.register("two_dragons_play_ball",
            () -> EntityType.Builder.<TwoDragonsPlayBallEntity>of(TwoDragonsPlayBallEntity::new, MobCategory.MISC)
                    .sized(1.3f, 0.15f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("two_dragons_play_ball")
    );

    public static final RegistryObject<EntityType<TwoDragonsFanEntity>> TWO_DRAGONS_FAN = ENTITIES.register("two_dragons_fan",
            () -> EntityType.Builder.<TwoDragonsFanEntity>of(TwoDragonsFanEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("two_dragons_fan")
    );

    public static final RegistryObject<EntityType<PokerCardEntity>> POKER_CARD = ENTITIES.register("poker_card",
            () -> EntityType.Builder.<PokerCardEntity>of(PokerCardEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("poker_card")
    );

    public static final RegistryObject<EntityType<SwordPhantomEntity>> SWORD_PHANTOM = ENTITIES.register("sword_phantom",
            () -> EntityType.Builder.<SwordPhantomEntity>of(SwordPhantomEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("sword_phantom")
    );

    public static final RegistryObject<EntityType<FlameProjectileEntity>> FLAME_PROJECTILE = ENTITIES.register("flame_projectile",
            () -> EntityType.Builder.<FlameProjectileEntity>of(FlameProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build("flame_projectile")
    );

    public static final RegistryObject<EntityType<ArtifactSpiritEntity>> ARTIFACT_SPIRIT = ENTITIES.register("artifact_spirit",
            () -> EntityType.Builder.<ArtifactSpiritEntity>of(ArtifactSpiritEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("artifact_spirit")
    );

    public static final RegistryObject<EntityType<KatanaSlashEntity>> KATANA_SLASH = ENTITIES.register("katana_slash",
            () -> EntityType.Builder.<KatanaSlashEntity>of(KatanaSlashEntity::new, MobCategory.MISC)
                    .sized(4.0F, 4.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("katana_slash")
    );

    public static final RegistryObject<EntityType<KatanaCircleSlashEntity>> KATANA_CIRCLE_SLASH = ENTITIES.register("katana_circle_slash",
            () -> EntityType.Builder.<KatanaCircleSlashEntity>of(KatanaCircleSlashEntity::new, MobCategory.MISC)
                    .sized(4.0F, 4.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("katana_circle_slash")
    );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
