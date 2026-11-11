package com.example.bloodmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class BloodMod implements ModInitializer {
    public static final String MOD_ID = "bloodmod";

    // Order matters: the block needs the fluids, the bucket needs the still fluid.
    public static final FlowableFluid BLOOD_STILL = Registry.register(
            Registries.FLUID, Identifier.of(MOD_ID, "blood"), new BloodFluid.Still());
    public static final FlowableFluid BLOOD_FLOWING = Registry.register(
            Registries.FLUID, Identifier.of(MOD_ID, "flowing_blood"), new BloodFluid.Flowing());
    public static final Block BLOOD_BLOCK = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "blood"),
            new BloodBlock(BLOOD_STILL, AbstractBlock.Settings.copy(Blocks.WATER)));
    public static final Item BLOOD_BUCKET = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "blood_bucket"),
            new BucketItem(BLOOD_STILL, new Item.Settings().recipeRemainder(Items.BUCKET).maxCount(1)));

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(BLOOD_BUCKET));

        // Pets are saved as plain mobs with a tag; re-attach their behavior whenever they load.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MobEntity mob) {
                UUID owner = BloodMinions.ownerOf(mob);
                if (owner != null) {
                    BloodMinions.makePet(mob, owner);
                }
            }
        });
    }
}
