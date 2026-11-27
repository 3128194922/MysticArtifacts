package com.uniye.mysticartifacts.util;

import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MysticArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class PseudoExplosionGameTests {
    @GameTest(template = "lightning_bottle_test", timeoutTicks = 40)
    public static void damages_inside_radius_only(GameTestHelper h) {
        Cow near = h.spawn(EntityType.COW, new Vec3(3.5, 2, 3.5));
        Cow far = h.spawn(EntityType.COW, new Vec3(9.0, 2, 3.5));
        float nearBefore = near.getHealth();
        float farBefore = far.getHealth();

        Vec3 center = h.absoluteVec(new Vec3(5.5, 3.0, 3.5));
        PseudoExplosion.detonate(h.getLevel(), null, h.getLevel().damageSources().generic(),
                center.x, center.y, center.z, 2.0F);

        h.assertTrue(near.getHealth() < nearBefore,
                "Cow inside the AABB blast must take damage, before " + nearBefore
                        + " after " + near.getHealth());
        h.assertTrue(far.getHealth() == farBefore,
                "Cow outside the blast radius must stay unharmed, before " + farBefore
                        + " after " + far.getHealth());
        h.succeed();
    }

    @GameTest(template = "lightning_bottle_test", timeoutTicks = 40)
    public static void leaves_blocks_untouched(GameTestHelper h) {
        BlockPos glassPos = new BlockPos(5, 3, 3);
        h.setBlock(glassPos, Blocks.GLASS.defaultBlockState());

        Vec3 center = h.absoluteVec(new Vec3(5.5, 3.5, 3.5));
        PseudoExplosion.detonate(h.getLevel(), null, h.getLevel().damageSources().generic(),
                center.x, center.y, center.z, 2.0F);

        h.assertBlockPresent(Blocks.GLASS, glassPos);
        h.succeed();
    }
}
