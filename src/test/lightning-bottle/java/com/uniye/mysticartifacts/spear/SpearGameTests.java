package com.uniye.mysticartifacts.spear;

import com.mojang.authlib.GameProfile;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(MysticArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class SpearGameTests {
    @GameTest(template = "lightning_bottle_test", timeoutTicks = 40)
    public static void mob_stabs_from_its_own_view(GameTestHelper h) {
        Cow target = h.spawn(EntityType.COW, new Vec3(5.5, 2, 3.5));
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new Vec3(2.5, 2, 3.5));
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SPEAR.get()));

        float before = target.getHealth();
        boolean hit = zombie.doHurtTarget(target);

        h.assertTrue(hit, "Zombie spear thrust should hit a target in front of it");
        h.assertTrue(target.getHealth() < before, "Target should lose health from the mob stab");
        h.succeed();
    }

    @GameTest(template = "lightning_bottle_test", timeoutTicks = 40)
    public static void mob_spear_respects_its_own_range(GameTestHelper h) {
        Cow target = h.spawn(EntityType.COW, new Vec3(9.5, 2, 3.5));
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new Vec3(2.5, 2, 3.5));
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SPEAR.get()));

        float before = target.getHealth();
        boolean hit = zombie.doHurtTarget(target);

        h.assertTrue(!hit, "Zombie spear should not reach a target seven blocks away");
        h.assertTrue(target.getHealth() == before, "Out-of-range target keeps full health");
        h.succeed();
    }

    @GameTest(template = "lightning_bottle_test", timeoutTicks = 40)
    public static void player_stab_does_not_double_dip(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "spear-test"));
        player.setPos(h.absoluteVec(new Vec3(2.5, 2, 3.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPEAR.get()));
        Cow target = h.spawn(EntityType.COW, new Vec3(5.0, 2, 3.5));
        player.setYRot(-90.0F);
        player.setYHeadRot(-90.0F);
        player.setXRot(10.0F);

        float before = target.getHealth();
        float attackDamage = (float) player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        player.attack(target);
        float dealt = before - target.getHealth();

        h.assertTrue(Math.abs(dealt - attackDamage) < 0.01F,
                "Spear left click should deal one stab of " + attackDamage
                        + " damage, but dealt " + dealt + " (before " + before + ", after "
                        + target.getHealth() + ")");
        h.succeed();
    }
}
