package io.github.pikczu77.blockopener.opening;

import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.ability.TempBlocks;
import io.github.pikczu77.blockopener.progress.Progress;
import io.github.pikczu77.blockopener.progress.SecretItem;
import io.github.pikczu77.blockopener.registry.ModItems;
import io.github.pikczu77.blockopener.settings.ModSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class BlockOpening {
	/** Blocks that can never be opened (technical blocks, portals, and things that would soft-lock a run). */
	public static final TagKey<Block> UNOPENABLE = TagKey.create(Registries.BLOCK, BlockOpener.id("unopenable"));

	private static final Map<UUID, Long> LAST_OPEN = new HashMap<>();

	public static void init() {
		UseBlockCallback.EVENT.register(BlockOpening::onUseBlock);
		ServerTickEvents.END_SERVER_TICK.register(OpeningAnimations::tick);
		ServerEntityEvents.ENTITY_LOAD.register(TransientEntities::onEntityLoad);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			OpeningAnimations.finishAll();
			TransientEntities.clear();
			LAST_OPEN.clear();
		});
	}

	/**
	 * The Block Opener wins over the block's own right-click action (so bells, chests or crafting tables
	 * get opened too, like in the video). Sneak + right-click uses the block normally instead.
	 */
	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(ModItems.BLOCK_OPENER) || player.isSpectator()) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (player.isSecondaryUseActive()) {
			return state.useWithoutItem(level, player, hit);
		}
		if (!canOpen(level, pos, state)) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			if (!serverPlayer.mayUseItemAt(pos, hit.getDirection(), stack) || !serverLevel.mayInteract(serverPlayer, pos)) {
				return InteractionResult.FAIL;
			}
			long now = serverLevel.getGameTime();
			Long last = LAST_OPEN.get(serverPlayer.getUUID());
			if (last != null && now - last < ModSettings.get(serverLevel.getServer()).openCooldown()) {
				return InteractionResult.FAIL;
			}
			LAST_OPEN.put(serverPlayer.getUUID(), now);
			open(serverLevel, pos, serverPlayer);
			serverPlayer.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		}
		return InteractionResult.SUCCESS;
	}

	public static boolean canOpen(Level level, BlockPos pos, BlockState state) {
		return !state.isAir()
			&& !state.is(UNOPENABLE)
			&& !(state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)
			&& !OpeningAnimations.isAnimating(level, pos)
			&& !TempBlocks.isTemporary(level, pos);
	}

	/**
	 * Opens the block at {@code pos}: removes it, rolls its loot and plays the opening animation,
	 * which pops the loot out a few ticks later. Returns the rolled loot.
	 */
	public static List<ItemStack> open(ServerLevel level, BlockPos pos, @Nullable ServerPlayer player) {
		BlockState state = level.getBlockState(pos);
		List<ItemStack> loot = OpeningLoot.roll(level, pos, state, player, ModSettings.get(level.getServer()).lootRolls());

		// Waterlogged blocks leave their water behind. Containers spill their contents on removal.
		level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
		level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
		level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.7F, 1.4F);
		level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.8F);

		OpeningAnimations.start(level, pos, state, loot, player);
		return loot;
	}

	/**
	 * Opens every block within {@code radius} of {@code center} at once, without the animation
	 * (the Anvil Chestplate slam: "a bunch of iron whenever we crouch jump... it's Christmas").
	 * Bedrock is skipped so slamming at the bottom of the world never digs into the void.
	 *
	 * @return how many blocks were opened
	 */
	public static int openArea(ServerLevel level, Vec3 center, double radius, @Nullable ServerPlayer player) {
		int rolls = ModSettings.get(level.getServer()).lootRolls();
		BlockPos origin = BlockPos.containing(center);
		int r = Mth.ceil(radius);
		List<BlockPos> targets = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
			if (Vec3.atCenterOf(pos).distanceToSqr(center) <= radius * radius) {
				targets.add(pos.immutable());
			}
		}
		targets.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(center)));

		int opened = 0;
		for (BlockPos pos : targets) {
			BlockState state = level.getBlockState(pos);
			if (!canOpen(level, pos, state) || state.is(Blocks.BEDROCK) || player != null && !level.mayInteract(player, pos)) {
				continue;
			}
			List<ItemStack> loot = OpeningLoot.roll(level, pos, state, player, rolls);
			level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
			level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
			level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
			Vec3 at = Vec3.atCenterOf(pos);
			for (ItemStack stack : loot) {
				ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack,
					level.random.triangle(0.0, 0.15), 0.25 + level.random.nextDouble() * 0.2, level.random.triangle(0.0, 0.15));
				item.setPickUpDelay(10);
				level.addFreshEntity(item);
			}
			if (player != null) {
				loot.stream().map(SecretItem::of).flatMap(Optional::stream).findFirst()
					.ifPresent(secret -> Progress.onSecretFound(player, secret, at));
			}
			opened++;
		}
		return opened;
	}

	private BlockOpening() {
	}
}
