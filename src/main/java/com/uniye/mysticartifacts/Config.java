package com.uniye.mysticartifacts;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue AIRBURST_NUMBER = BUILDER
            .comment("Number of bullets in Airburst phase 1")
            .defineInRange("AirBurstNumber", 12, 1, 255);
    private static final ForgeConfigSpec.IntValue AIRBURST_NUMBER_RANDOM = BUILDER
            .comment("Random range for Airburst phase 1 bullets")
            .defineInRange("AirBurstNumberRandom", 8, 1, 255);
    private static final ForgeConfigSpec.IntValue AIRBURST_NUMBER_2 = BUILDER
            .comment("Number of bullets in Airburst phase 2")
            .defineInRange("AirBurstNumber2", 3, 1, 255);
    private static final ForgeConfigSpec.IntValue AIRBURST_NUMBER_2_RANDOM = BUILDER
            .comment("Random range for Airburst phase 2 bullets")
            .defineInRange("AirBurstNumber2Random", 3, 1, 255);
    private static final ForgeConfigSpec.ConfigValue<Double> AIRBURST_PROXIMITY_RADIUS = BUILDER
            .comment("Airburst proximity detonation radius (blocks)")
            .defineInRange("AirBurstProximityRadius", 3.0, 0.0, 1024.0);
    private static final ForgeConfigSpec.IntValue TNT_ARROW_BOUNCES = BUILDER
            .comment("Slime Arrow bounces")
            .defineInRange("TNTArrowBounces", 3, 1, 255);
    private static final ForgeConfigSpec.ConfigValue<Double> TNT_ARROW_DAMAGE = BUILDER
            .comment("Slime Arrow direct hit damage")
            .defineInRange("TNTArrowDamage", 1.0, 0.0, 255.0);
    private static final ForgeConfigSpec.ConfigValue<Double> TNT_ARROW_MIN_VELOCITY = BUILDER
            .comment("Slime Arrow minimum velocity to bounce")
            .defineInRange("TNTArrowMinVelocity", 0.5, 0.0, 255.0);
    private static final ForgeConfigSpec.ConfigValue<Double> TNT_ARROW_GLOW_RADIUS = BUILDER
            .comment("Slime Arrow glowing effect radius")
            .defineInRange("TNTArrowGlowRadius", 10.0, 0.0, 255.0);

    private static final ForgeConfigSpec.ConfigValue<Double> NETHER_OF_VOICE_SPEED = BUILDER
            .comment("Nether of Voice movement speed")
            .defineInRange("NetherOfVoiceSpeed", 0.5, 0.0, 10.0);
    private static final ForgeConfigSpec.ConfigValue<Double> NETHER_OF_VOICE_TURN_RATE = BUILDER
            .comment("Nether of Voice turn rate")
            .defineInRange("NetherOfVoiceTurnRate", 0.15, 0.0, 1.0);
    private static final ForgeConfigSpec.ConfigValue<Double> NETHER_OF_VOICE_DAMAGE = BUILDER
            .comment("Nether of Voice base damage")
            .defineInRange("NetherOfVoiceDamage", 10.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.ConfigValue<Double> ENDER_KUNAI_MAX_DISTANCE = BUILDER
            .comment("Ender Kunai max teleport distance (blocks)")
            .defineInRange("EnderKunaiMaxDistance", 1000.0, 0.0, 100000.0);

    private static final ForgeConfigSpec.IntValue TWO_DRAGONS_MAX_BOUNCES = BUILDER
            .comment("Two Dragons Play Ball max bounces (target seeking count)")
            .defineInRange("TwoDragonsMaxBounces", 6, 1, 100);
    private static final ForgeConfigSpec.IntValue TWO_DRAGONS_ROTATION_TIME = BUILDER
            .comment("Two Dragons Play Ball rotation time (ticks)")
            .defineInRange("TwoDragonsRotationTime", 800, 0, 10000);
    private static final ForgeConfigSpec.IntValue TWO_DRAGONS_COOLDOWN = BUILDER
            .comment("Two Dragons Play Ball item cooldown (ticks)")
            .defineInRange("TwoDragonsCooldown", 40, 0, 10000);

    private static final ForgeConfigSpec.IntValue KATANA_STACKS_PER_ATTACK = BUILDER
            .comment("Katana stacks gained per attack")
            .defineInRange("KatanaStacksPerAttack", 1, 1, 100);
    private static final ForgeConfigSpec.IntValue KATANA_STACKS_BLOCK_COST = BUILDER
            .comment("Katana stacks consumed on block")
            .defineInRange("KatanaStacksBlockCost", 25, 0, 100);
    private static final ForgeConfigSpec.IntValue KATANA_PERFECT_BLOCK_WINDOW = BUILDER
            .comment("Katana perfect block window (ticks)")
            .defineInRange("KatanaPerfectBlockWindow", 20, 0, 100);
    private static final ForgeConfigSpec.ConfigValue<Double> KATANA_DASH_DAMAGE_MULTIPLIER = BUILDER
            .comment("Katana dash damage multiplier")
            .defineInRange("KatanaDashDamageMultiplier", 10.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.ConfigValue<Double> POKER_CARD_RETRIEVAL_DISTANCE = BUILDER
            .comment("Poker Card retrieval distance")
            .defineInRange("PokerCardRetrievalDistance", 20.0, 0.0, 255.0);
    private static final ForgeConfigSpec.IntValue POKER_CARD_COOLDOWN = BUILDER
            .comment("Poker Card left click cooldown (ticks)")
            .defineInRange("PokerCardCooldown", 13, 0, 1000);

    private static final ForgeConfigSpec.ConfigValue<Double> DEATH_EYE_RENDER_RANGE = BUILDER
            .comment("Death Eye execution line render range")
            .defineInRange("DeathEyeRenderRange", 36.0, 0.0, 255.0);

    private static final ForgeConfigSpec.IntValue QUANTUM_KEY_EXPIRATION = BUILDER
            .comment("Quantum Key expiration time (ticks)")
            .defineInRange("QuantumKeyExpiration", 1200, 0, 100000);
    private static final ForgeConfigSpec.IntValue VOID_ARROW_LIFETIME = BUILDER
            .comment("Void Arrow lifetime (ticks)")
            .defineInRange("VoidArrowLifetime", 100, 1, 100000);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int AirBurstNumber;
    public static int AirBurstNumberRandom;
    public static int AirBurstNumber2;
    public static int AirBurstNumber2Random;
    public static double AirBurstProximityRadius;
    public static int TNTArrowBounces;
    public static double TNTArrowDamage;
    public static double TNTArrowMinVelocity;
    public static double TNTArrowGlowRadius;
    
    public static double NetherOfVoiceSpeed;
    public static double NetherOfVoiceTurnRate;
    public static double NetherOfVoiceDamage;
    
    public static double EnderKunaiMaxDistance;
    
    public static int TwoDragonsMaxBounces;
    public static int TwoDragonsRotationTime;
    public static int TwoDragonsCooldown;
    
    public static int KatanaStacksPerAttack;
    public static int KatanaStacksBlockCost;
    public static int KatanaPerfectBlockWindow;
    public static double KatanaDashDamageMultiplier;
    
    public static double PokerCardRetrievalDistance;
    public static int PokerCardCooldown;
    
    public static double DeathEyeRenderRange;
    
    public static int QuantumKeyExpiration;
    public static int VoidArrowLifetime;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        AirBurstNumber = AIRBURST_NUMBER.get();
        AirBurstNumberRandom = AIRBURST_NUMBER_RANDOM.get();
        AirBurstNumber2 = AIRBURST_NUMBER_2.get();
        AirBurstNumber2Random = AIRBURST_NUMBER_2_RANDOM.get();
        AirBurstProximityRadius = AIRBURST_PROXIMITY_RADIUS.get();
        TNTArrowBounces = TNT_ARROW_BOUNCES.get();
        TNTArrowDamage = TNT_ARROW_DAMAGE.get();
        TNTArrowMinVelocity = TNT_ARROW_MIN_VELOCITY.get();
        TNTArrowGlowRadius = TNT_ARROW_GLOW_RADIUS.get();
        
        NetherOfVoiceSpeed = NETHER_OF_VOICE_SPEED.get();
        NetherOfVoiceTurnRate = NETHER_OF_VOICE_TURN_RATE.get();
        NetherOfVoiceDamage = NETHER_OF_VOICE_DAMAGE.get();
        
        EnderKunaiMaxDistance = ENDER_KUNAI_MAX_DISTANCE.get();
        
        TwoDragonsMaxBounces = TWO_DRAGONS_MAX_BOUNCES.get();
        TwoDragonsRotationTime = TWO_DRAGONS_ROTATION_TIME.get();
        TwoDragonsCooldown = TWO_DRAGONS_COOLDOWN.get();
        
        KatanaStacksPerAttack = KATANA_STACKS_PER_ATTACK.get();
        KatanaStacksBlockCost = KATANA_STACKS_BLOCK_COST.get();
        KatanaPerfectBlockWindow = KATANA_PERFECT_BLOCK_WINDOW.get();
        KatanaDashDamageMultiplier = KATANA_DASH_DAMAGE_MULTIPLIER.get();
        
        PokerCardRetrievalDistance = POKER_CARD_RETRIEVAL_DISTANCE.get();
        PokerCardCooldown = POKER_CARD_COOLDOWN.get();
        
        DeathEyeRenderRange = DEATH_EYE_RENDER_RANGE.get();
        
        QuantumKeyExpiration = QUANTUM_KEY_EXPIRATION.get();
        VoidArrowLifetime = VOID_ARROW_LIFETIME.get();
    }
}
