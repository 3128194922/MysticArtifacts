package com.uniye.mysticartifacts.init;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.ModArmorMaterials;
import com.uniye.mysticartifacts.item.impl.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MysticArtifacts.MODID);

    public static final RegistryObject<Item> RUBBER = ITEMS.register("rubber", ()-> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SLIME_ARROW = ITEMS.register("slime_arrow",
            () -> new SlimeArrowItem(new Item.Properties())
    );

    public static final RegistryObject<Item> VOID_ARROW = ITEMS.register("void_arrow",
            () -> new VoidArrowItem(new Item.Properties())
    );

    public static final RegistryObject<Item> DEMOCRACY_HELMET = ITEMS.register(
            "democracy_helmet",
            () -> new ArmorItem(ModArmorMaterials.DEMOCRACY, ArmorItem.Type.HELMET, new Item.Properties())
    );

    public static final RegistryObject<Item> DEMOCRACY_CHESTPLATE = ITEMS.register(
            "democracy_chestplate",
            () -> new ArmorItem(ModArmorMaterials.DEMOCRACY, ArmorItem.Type.CHESTPLATE, new Item.Properties())
    );

    public static final RegistryObject<Item> DEMOCRACY_LEGGINGS = ITEMS.register(
            "democracy_leggings",
            () -> new ArmorItem(ModArmorMaterials.DEMOCRACY, ArmorItem.Type.LEGGINGS, new Item.Properties())
    );

    public static final RegistryObject<Item> DEMOCRACY_BOOTS = ITEMS.register(
            "democracy_boots",
            () -> new ArmorItem(ModArmorMaterials.DEMOCRACY, ArmorItem.Type.BOOTS, new Item.Properties())
    );



    public static final RegistryObject<Item> NETHER_OF_VOICE = ITEMS.register(
            "nether_of_voice",
            () -> new NetherOfVoiceItem(new Item.Properties())
    );

    public static final RegistryObject<Item> AIRBURST_ARROW = ITEMS.register(
            "airburst_arrow",
            () -> new AirburstArrowItem(new Item.Properties())
    );
    public static final RegistryObject<Item> EXPLODING_ARROW = ITEMS.register(
            "exploding_arrow",
            () -> new ExplodingArrowItem(new Item.Properties())
    );
    public static final RegistryObject<Item> FINAL_EXPLODING_ARROW = ITEMS.register(
            "final_exploding_arrow",
            () -> new FinalExplodingArrowItem(new Item.Properties())
    );

    public static final RegistryObject<Item> ENDER_KUNAI = ITEMS.register("ender_kunai",
            () -> new EnderKunaiItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> TWO_DRAGONS_PLAY_BALL = ITEMS.register("two_dragons_play_ball",
            () -> new TwoDragonsPlayBallItem(new Item.Properties()));

    public static final RegistryObject<Item> KATANA = ITEMS.register("katana",
            () -> new MuramasaItem(new Item.Properties()));

    public static final RegistryObject<Item> POKER_CARD = ITEMS.register("poker_card",
            () -> new PokerCardItem(new Item.Properties().stacksTo(54)));

    public static final RegistryObject<Item> POKER_CARD_PROJECTILE = ITEMS.register("poker_card_projectile",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DEATH_EYE = ITEMS.register("death_eye",
            () -> new DeathEyeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SWORD_SWARM_CHARM = ITEMS.register("sword_swarm_charm",
            () -> new SwordSwarmCharm(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> QUANTUM_KEY = ITEMS.register("quantum_key",
            () -> new QuantumKeyItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> MANDEL_BRICK = ITEMS.register("mandel_brick",
            () -> new MandelBrickItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
