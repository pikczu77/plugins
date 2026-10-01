package io.github.pikczu77.blockopener.client;

import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.network.ModPayloads;
import io.github.pikczu77.blockopener.registry.ModEntities;
import io.github.pikczu77.blockopener.registry.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class BlockOpenerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.MOSSPHERE, context -> new ThrownItemRenderer<>(context, 1.25F, true));
		EntityRendererRegistry.register(ModEntities.HONEY_BLOB, context -> new ThrownItemRenderer<>(context, 0.9F, true));
		ClientPlayNetworking.registerGlobalReceiver(ModPayloads.ItemActivationPayload.TYPE,
			(payload, context) -> context.client().gameRenderer.displayItemActivation(payload.stack()));

		FluidRenderHandlerRegistry.INSTANCE.register(ModFluids.VOID_WATER, ModFluids.FLOWING_VOID_WATER, new SimpleFluidRenderHandler(
			BlockOpener.id("block/void_water_still"), BlockOpener.id("block/void_water_flow"), -1
		));
		BlockRenderLayerMap.putFluids(ChunkSectionLayer.TRANSLUCENT, ModFluids.VOID_WATER, ModFluids.FLOWING_VOID_WATER);

		KeyBinds.init();
		Tooltips.init();
		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, BlockOpener.id("tracker"), new TrackerHud());
	}
}
