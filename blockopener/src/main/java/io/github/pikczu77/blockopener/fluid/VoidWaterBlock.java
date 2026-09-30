package io.github.pikczu77.blockopener.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jspecify.annotations.Nullable;

public class VoidWaterBlock extends LiquidBlock {
	public VoidWaterBlock(FlowingFluid fluid, Properties properties) {
		super(fluid, properties);
	}

	/** Only the Bedrock Bucket can drain void water; ordinary buckets leave it alone. */
	@Override
	public ItemStack pickupBlock(@Nullable LivingEntity entity, LevelAccessor level, BlockPos pos, BlockState state) {
		return ItemStack.EMPTY;
	}
}
