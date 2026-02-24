package com.uniye.mysticartifacts.util;

import com.uniye.mysticartifacts.MysticArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TagInit {
    public static final TagKey<Block> MASTERKEY_TAG = TagKey.create(
            Registries.BLOCK,
            new ResourceLocation(MysticArtifacts.MODID, "master_key")
    );

    public static final TagKey<Item> IS_SWORD = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(MysticArtifacts.MODID, "is_sword")
    );

    public static final TagKey<Item> FORGE_SWORDS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation("forge", "swords")
    );
}
