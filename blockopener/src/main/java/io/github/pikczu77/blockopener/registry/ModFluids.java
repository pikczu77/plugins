package io.github.pikczu77.blockopener.registry;

import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.fluid.VoidWaterBlock;
import io.github.pikczu77.blockopener.fluid.VoidWaterFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModFluids {
	public static final FlowingFluid VOID_WATER = Registry.register(
		BuiltInRegistries.FLUID, BlockOpener.id("void_water"), new VoidWaterFluid.Source()
	);
	public static final FlowingFluid FLOWING_VOID_WATER = Registry.register(
		BuiltInRegistries.FLUID, BlockOpener.id("flowing_void_water"), new VoidWaterFluid.Flowing()
	);

	public static final Block VOID_WATER_BLOCK = Blocks.register(
		ResourceKey.create(Registries.BLOCK, BlockOpener.id("void_water")),
		properties -> new VoidWaterBlock(VOID_WATER, properties),
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.replaceable()
			.noCollision()
			.strength(100.0F)
			.pushReaction(PushReaction.DESTROY)
			.noLootTable()
			.liquid()
			.lightLevel(state -> 3)
			.sound(SoundType.EMPTY)
	);

	public static void init() {
	}

	private ModFluids() {
	}
}
