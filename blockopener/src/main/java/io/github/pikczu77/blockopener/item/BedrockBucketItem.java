package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.registry.ModFluids;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Bedrock Bucket (from bedrock): pours Void Water and never runs empty. Sneak + right-click drains
 * the void water you are looking at.
 */
public class BedrockBucketItem extends Item {
	private static final int DRAIN_LIMIT = 64;

	public BedrockBucketItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean drain = player.isShiftKeyDown();
		BlockHitResult hit = getPlayerPOVHitResult(level, player, drain ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos pos = hit.getBlockPos();
		Direction face = hit.getDirection();
		if (drain) {
			int drained = drain(serverLevel, player, pos);
			if (drained == 0) {
				return InteractionResult.FAIL;
			}
			serverLevel.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 0.6F);
			return InteractionResult.SUCCESS;
		}

		BlockState clicked = level.getBlockState(pos);
		BlockPos target = clicked.canBeReplaced() && !clicked.getFluidState().isSource() ? pos : pos.relative(face);
		if (!player.mayUseItemAt(target, face, stack) || !serverLevel.mayInteract(player, target) || !level.getBlockState(target).canBeReplaced()) {
			return InteractionResult.FAIL;
		}
		serverLevel.setBlock(target, ModFluids.VOID_WATER.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL_IMMEDIATE);
		serverLevel.playSound(null, target, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 0.5F);
		serverLevel.playSound(null, target, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.15F, 1.8F);
		serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.1);
		player.getCooldowns().addCooldown(stack, 8);
		return InteractionResult.SUCCESS;
	}

	/** Removes the connected void water around {@code start}. */
	private static int drain(ServerLevel level, Player player, BlockPos start) {
		Deque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(start);
		int drained = 0;
		while (!queue.isEmpty() && drained < DRAIN_LIMIT) {
			BlockPos pos = queue.poll();
			if (!seen.add(pos) || !level.getBlockState(pos).is(ModFluids.VOID_WATER_BLOCK) || !level.mayInteract(player, pos)) {
				continue;
			}
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			drained++;
			for (Direction direction : Direction.values()) {
				queue.add(pos.relative(direction));
			}
		}
		return drained;
	}
}
