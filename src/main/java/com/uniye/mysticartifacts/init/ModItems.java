package com.uniye.mysticartifacts.init;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.item.impl.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MysticArtifacts.MODID);

    public static final RegistryObject<Item> SCULK_ARROW = ITEMS.register("sculk_arrow",
            () -> new SculkArrowItem(new Item.Properties())
    );

    public static final RegistryObject<Item> DEMONIC_GESTATION = ITEMS.register("demonic_gestation",
            () -> new DemonicGestationItem(new Item.Properties().stacksTo(1))
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

    public static final RegistryObject<Item> ARTIFACT_SPIRIT = ITEMS.register("artifact_spirit",
            () -> new ArtifactSpiritItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRIEFER_SPEAR = ITEMS.register("griefer_spear",
            () -> new GrieferSpearItem(Tiers.IRON, 2, -2.8F, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SPEAR = ITEMS.register("spear",
            () -> new SpearItem(Tiers.DIAMOND, 3, (1.0F / 1.05F) - 4.0F, new Item.Properties()));

    public static final RegistryObject<Item> CODEX = ITEMS.register("codex",
            () -> new CodexItem(new Item.Properties()));

    public static final RegistryObject<Item> WITCH_POT = ITEMS.register("witch_pot",
            () -> new WitchPotItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ALL_SEEING_EYE = ITEMS.register("all_seeing_eye",
            () -> new AllSeeingEyeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SURVIVAL_JADE = ITEMS.register("survival_jade",
            () -> new SurvivalJadeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> EMERGENCY_PLAN = ITEMS.register("emergency_plan",
            () -> new EmergencyPlanItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ANCESTORS_LETTER = ITEMS.register("ancestors_letter",
            () -> new AncestorsLetterItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
