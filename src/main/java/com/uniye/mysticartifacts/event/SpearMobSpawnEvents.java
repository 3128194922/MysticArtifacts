package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.Config;
import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 让僵尸/猪灵类怪物有概率带着紫水晶长矛生成，便于展示怪物持矛 AI。
 * 只在新生成（非读档）且主手为空时生效。
 */
@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public final class SpearMobSpawnEvents {
    private SpearMobSpawnEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) return;

        double chance = Config.SpearMobSpawnChance;
        if (chance <= 0.0D) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob instanceof Zombie || mob instanceof Piglin)) return;
        if (!mob.getMainHandItem().isEmpty()) return;
        if (mob.getRandom().nextDouble() >= chance) return;

        Item spear = mob.getRandom().nextDouble() < Config.SpearMobGrieferChance
                ? ModItems.GRIEFER_SPEAR.get()
                : ModItems.SPEAR.get();
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(spear));
        mob.setDropChance(EquipmentSlot.MAINHAND, (float) Config.SpearMobDropChance);
    }
}
