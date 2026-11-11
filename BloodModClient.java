package com.example.bloodmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.render.RenderLayer;

public class BloodModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Reuse the water textures, tinted dark red.
        FluidRenderHandlerRegistry.INSTANCE.register(
                BloodMod.BLOOD_STILL, BloodMod.BLOOD_FLOWING,
                SimpleFluidRenderHandler.coloredWater(0x8A0303));
        BlockRenderLayerMap.INSTANCE.putFluids(
                RenderLayer.getTranslucent(), BloodMod.BLOOD_STILL, BloodMod.BLOOD_FLOWING);
    }
}
