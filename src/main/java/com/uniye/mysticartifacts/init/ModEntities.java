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

    public static final RegistryObject<EntityType<VoidArrowEntity>> VOID_ARROW = ENTITIES.register("void_arrow",
            () -> EntityType.Builder.<VoidArrowEntity>of(VoidArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("void_arrow")
    );

    public static final RegistryObject<EntityType<TrackingArrowEntity>> TRACKING_ARROW = ENTITIES.register("tracking_arrow",
            () -> EntityType.Builder.<TrackingArrowEntity>of(TrackingArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build("tracking_arrow")
    );

    public static final RegistryObject<EntityType<SlimeArrow>> SLIME_ARROW = ENTITIES.register("slime_arrow",
            () -> EntityType.Builder.<SlimeArrow>of(SlimeArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("slime_arrow")
    );

    public static final RegistryObject<EntityType<NetherOfVoiceEntity>> NETHER_OF_VOICE = ENTITIES.register("nether_of_voice",
            () -> EntityType.Builder.<NetherOfVoiceEntity>of(NetherOfVoiceEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("nether_of_voice")
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

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
