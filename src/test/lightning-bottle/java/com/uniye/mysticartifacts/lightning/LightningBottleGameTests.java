package com.uniye.mysticartifacts.lightning;

import com.mojang.authlib.GameProfile;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.config.LightningBottleConfig;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.item.impl.LightningBottleItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

@GameTestHolder(MysticArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class LightningBottleGameTests {
    @GameTest(template = "lightning_bottle_test", timeoutTicks = 40)
    public static void single_target_repeated_hits_and_player_owned_field(GameTestHelper h) {
        int originalMaximum = LightningBottleConfig.MAX_TARGETS.get();
        int originalCapacity = LightningBottleConfig.MAX_THUNDER_POWER.get();
        var player = FakePlayerFactory.get(h.getLevel(),
                new GameProfile(UUID.randomUUID(), "bottle-test"));
        try {
            LightningBottleConfig.MAX_TARGETS.set(1);
            LightningBottleConfig.MAX_THUNDER_POWER.set(3);
            player.setPos(h.absoluteVec(new Vec3(2.5, 2, 3.5)));
            var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            ItemStack bottle = new ItemStack(ModItems.LIGHTNING_BOTTLE.get());
            inventory.setEquippedCurio("curio", 0, bottle);
            h.assertTrue(LightningBottleItem.isWearing(player), "Test player wears a real Curios bottle");

            Cow primary = cow(h, 3.5);
            primary.hurt(h.getLevel().damageSources().playerAttack(player), 2);
            h.assertTrue(primary.getHealth() == 44,
                    "A lone target takes the original 2 damage plus one 4 damage chain hit");
            h.assertTrue(LightningBottleItem.getThunderPower(bottle) == 1,
                    "A lone target grants exactly one charge without recursive triggers");
            h.assertTrue(!player.getCooldowns().isOnCooldown(ModItems.LIGHTNING_BOTTLE.get()),
                    "A successful chain never adds an item cooldown");

            primary.invulnerableTime = 0;
            primary.hurt(h.getLevel().damageSources().playerAttack(player), 2);
            h.assertTrue(primary.getHealth() == 38 && LightningBottleItem.getThunderPower(bottle) == 2,
                    "A second attack in the same tick triggers the bottle again");

            primary.setPos(h.absoluteVec(new Vec3(13.5, 2, 3.5)));
            Cow nearby = cow(h, 3.5);
            primary.invulnerableTime = 0;
            primary.hurt(h.getLevel().damageSources().playerAttack(player), 2);
            h.assertTrue(primary.getHealth() == 32 && nearby.getHealth() == 46,
                    "A lone distant target charges the field, which hits an entity near the player");
            h.assertTrue(nearby.getLastDamageSource() != null
                            && nearby.getLastDamageSource().getEntity() == player
                            && nearby.getLastDamageSource().is(DamageTypes.LIGHTNING_BOLT),
                    "Bottle damage retains lightning type and credits its player");
            h.assertTrue(LightningBottleItem.getThunderPower(bottle) == 0,
                    "Field damage does not recursively recharge or trigger the bottle");

            LightningBottleConfig.MAX_THUNDER_POWER.set(10000);
            primary.setPos(h.absoluteVec(new Vec3(3.5, 2, 3.5)));
            nearby.setPos(h.absoluteVec(new Vec3(6.5, 2, 3.5)));
            Cow beyondLimit = cow(h, 7.5);
            primary.invulnerableTime = 0;
            nearby.invulnerableTime = 0;
            primary.hurt(h.getLevel().damageSources().playerAttack(player), 2);
            h.assertTrue(primary.getHealth() == 26 && nearby.getHealth() == 42
                            && beyondLimit.getHealth() == 50,
                    "Primary bonus preserves the configured limit of one additional chain target");
            h.assertTrue(LightningBottleItem.getThunderPower(bottle) == 2,
                    "Primary and successful additional hit each grant exactly one charge");
            h.assertTrue(nearby.getLastDamageSource().getEntity() == player
                            && nearby.getLastDamageSource().is(DamageTypes.LIGHTNING_BOLT),
                    "Additional chain damage retains lightning type and credits the player");

            LightningBottleConfig.MAX_TARGETS.set(0);
            primary.invulnerableTime = 0;
            primary.hurt(h.getLevel().damageSources().playerAttack(player), 2);
            h.assertTrue(primary.getHealth() == 24 && LightningBottleItem.getThunderPower(bottle) == 2,
                    "Configuring zero targets still disables the bottle");
            h.succeed();
        } finally {
            LightningBottleConfig.MAX_TARGETS.set(originalMaximum);
            LightningBottleConfig.MAX_THUNDER_POWER.set(originalCapacity);
            player.discard();
        }
    }

    private static Cow cow(GameTestHelper h, double x) {
        Cow cow = h.spawnWithNoFreeWill(EntityType.COW, new Vec3(x, 2, 3.5));
        cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50);
        cow.setHealth(50);
        return cow;
    }
}
