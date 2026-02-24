package com.uniye.mysticartifacts.init;

import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MysticArtifacts.MODID);

    public static final RegistryObject<CreativeModeTab> MYSTIC_ARTIFACTS_TAB = CREATIVE_MODE_TABS.register("mystic_artifacts_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.KATANA.get()))
                    .title(Component.translatable("creativetab.mysticartifacts_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.RUBBER.get());
                        pOutput.accept(ModItems.SLIME_ARROW.get());
                        pOutput.accept(ModItems.DEMOCRACY_HELMET.get());
                        pOutput.accept(ModItems.DEMOCRACY_CHESTPLATE.get());
                        pOutput.accept(ModItems.DEMOCRACY_LEGGINGS.get());
                        pOutput.accept(ModItems.DEMOCRACY_BOOTS.get());
                        pOutput.accept(ModItems.NETHER_OF_VOICE.get());
                        pOutput.accept(ModItems.AIRBURST_ARROW.get());
                        pOutput.accept(ModItems.EXPLODING_ARROW.get());
                        pOutput.accept(ModItems.FINAL_EXPLODING_ARROW.get());
                        pOutput.accept(ModItems.ENDER_KUNAI.get());
                        pOutput.accept(ModItems.TWO_DRAGONS_PLAY_BALL.get());
                        pOutput.accept(ModItems.KATANA.get());
                        pOutput.accept(ModItems.POKER_CARD.get());
                        pOutput.accept(ModItems.DEATH_EYE.get());
                        pOutput.accept(ModItems.SWORD_SWARM_CHARM.get());
                        pOutput.accept(ModItems.QUANTUM_KEY.get());
                        pOutput.accept(ModItems.MANDEL_BRICK.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
