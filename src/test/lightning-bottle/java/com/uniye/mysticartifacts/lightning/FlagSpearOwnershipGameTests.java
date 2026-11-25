package com.uniye.mysticartifacts.lightning;

import com.mojang.authlib.GameProfile;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.entity.FlagSpearEntity;
import com.uniye.mysticartifacts.init.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(MysticArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class FlagSpearOwnershipGameTests {
    @GameTest(template = "lightning_bottle_test", timeoutTicks = 40)
    public static void only_owner_can_recover_spear(GameTestHelper h) {
        var owner = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "spear-owner"));
        var other = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "spear-other"));
        var spear = new FlagSpearEntity(ModEntities.FLAG_SPEAR.get(), h.getLevel());
        spear.setOwner(owner);
        spear.setNoPhysics(true);

        spear.playerTouch(other);
        h.assertTrue(!spear.isRemoved(), "A different player cannot recover or remove the spear");
        spear.playerTouch(owner);
        h.assertTrue(spear.isRemoved(), "The owner can recover the spear");

        var unowned = new FlagSpearEntity(ModEntities.FLAG_SPEAR.get(), h.getLevel());
        unowned.setNoPhysics(true);
        unowned.playerTouch(other);
        h.assertTrue(!unowned.isRemoved(), "A spear without an owner cannot be recovered by anyone");
        h.succeed();
    }
}
