package com.uniye.mysticartifacts.init;

import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MysticArtifacts.MODID);

    public static final RegistryObject<SoundEvent> HEARTBEAT = SOUNDS.register("entity.ceasing",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MysticArtifacts.MODID, "entity.ceasing")));
    public static final RegistryObject<SoundEvent> BELL_TOLL = SOUNDS.register("entity.bell_toll",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MysticArtifacts.MODID, "entity.bell_toll")));
    public static final RegistryObject<SoundEvent> BELL_TOLL_FAIL = SOUNDS.register("entity.bell_toll_fail",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MysticArtifacts.MODID, "entity.bell_toll_fail")));
    public static final RegistryObject<SoundEvent> TWO_DRAGONS_PLAY_BALL_SPIN = SOUNDS.register("entity.two_dragons_play_ball_spin",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MysticArtifacts.MODID, "entity.two_dragons_play_ball_spin")));
    public static final RegistryObject<SoundEvent> KATANA_BLOCK = SOUNDS.register("entity.katana_block",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MysticArtifacts.MODID, "entity.katana_block")));
    public static final RegistryObject<SoundEvent> POKER_THROW = SOUNDS.register("entity.poker_throw",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MysticArtifacts.MODID, "entity.poker_throw")));
    public static final RegistryObject<SoundEvent> POKER_RECALL = SOUNDS.register("entity.poker_recall",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MysticArtifacts.MODID, "entity.poker_recall")));
    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
