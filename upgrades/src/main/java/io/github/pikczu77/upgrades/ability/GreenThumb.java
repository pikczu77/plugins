package io.github.pikczu77.upgrades.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import io.github.pikczu77.upgrades.upgrade.Bonus;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * The hoe on the head: an empty-hand right-click tills (and plants) the ground in a 3×3 area, and crops around the
 * player grow almost instantly.
 */
public final class GreenThumb {
	private static final int GROW_RADIUS = 6;
	private static final Block[] CROPS = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};

	private GreenThumb() {
	}

	public static void till(ServerPlayer player) {
		HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);

		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		ServerLevel level = player.level();
		BlockPos center = blockHit.getBlockPos();
		boolean tilled = false;
		// Serious Dedication (netherite hoe) bonus: 5×5 instead of 3×3.
		int radius = UpgradeManager.hasSpecial(player, Bonus.Special.GREEN_PLUS) ? 2 : 1;

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
			BlockState state = level.getBlockState(pos);
			BlockState above = level.getBlockState(pos.above());

			if (!above.isAir() && !above.canBeReplaced()) {
				continue;
			}

			if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH)) {
				if (above.canBeReplaced() && !above.isAir()) {
					level.destroyBlock(pos.above(), false);
				}

				level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 11);
				// "All the food I could ever want": the fresh farmland gets planted right away.
				Block crop = CROPS[level.getRandom().nextInt(CROPS.length)];
				level.setBlock(pos.above(), crop.defaultBlockState(), 3);
				tilled = true;
			} else if (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
				level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 11);
				tilled = true;
			}
		}

		if (tilled) {
			level.playSound(null, center, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			player.swing(InteractionHand.MAIN_HAND, true);
		}
	}

	/** A few random crops around the player get bone-mealed every tick. */
	public static void tick(ServerPlayer player, long mask) {
		if (!Upgrade.has(mask, Upgrade.GREEN_THUMB) || player.tickCount % 4 != 0) {
			return;
		}

		ServerLevel level = player.level();
		RandomSource random = player.getRandom();
		BlockPos origin = player.blockPosition();
		boolean plus = UpgradeManager.hasSpecial(player, Bonus.Special.GREEN_PLUS);
		int range = plus ? GROW_RADIUS + 4 : GROW_RADIUS;

		for (int i = 0; i < (plus ? 12 : 6); i++) {
			BlockPos pos = origin.offset(random.nextInt(range * 2 + 1) - range, random.nextInt(5) - 2,
					random.nextInt(range * 2 + 1) - range);
			BlockState state = level.getBlockState(pos);

			if (!(state.is(BlockTags.CROPS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.COCOA))
					|| !(state.getBlock() instanceof BonemealableBlock growable) || !growable.isValidBonemealTarget(level, pos, state)) {
				continue;
			}

			growable.performBonemeal(level, random, pos, state);
			level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 6);
		}
	}
}
