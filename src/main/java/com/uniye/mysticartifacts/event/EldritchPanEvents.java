package com.uniye.mysticartifacts.event;

import com.uniye.mysticartifacts.MysticArtifacts;
import com.uniye.mysticartifacts.init.ModItems;
import com.uniye.mysticartifacts.item.impl.EldritchPanItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID)
public final class EldritchPanEvents {
    private static final UUID ATTACK_DAMAGE_MODIFIER_ID = UUID.fromString("e93f7408-d7f1-4df1-a28f-43c2e16b004e");
    private static final String ATTACK_DAMAGE_MODIFIER_NAME = "eldritch_pan";

    private EldritchPanEvents() {
    }

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.ELDRITCH_PAN.get())) {
            return;
        }

        int foodCount = EldritchPanItem.getFoodCount(stack);
        if (foodCount <= 0) {
            return;
        }

        event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID,
                ATTACK_DAMAGE_MODIFIER_NAME,
                foodCount,
                AttributeModifier.Operation.ADDITION
        ));
    }
}
