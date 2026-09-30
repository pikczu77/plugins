package io.github.pikczu77.upgrades.ability;

import java.util.ArrayDeque;
import java.util.Deque;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.config.UpgradesConfig;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * "Greatly enchanted" drops: while a player with the upgrade breaks a block or kills a mob, every dropped stack comes
 * out several times (see the Block, Entity and LivingEntity mixins).
 */
public final class Enchanted {
	/** Whether each block break in progress (they can nest with the vein miner) belongs to an enchanted player. */
	private static final Deque<Boolean> BREAKS = new ArrayDeque<>();
	/** Ids of the mobs dropping their loot right now; -1 when the killer has no enchanted body. */
	private static final Deque<Integer> DYING = new ArrayDeque<>();

	private Enchanted() {
	}

	public static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
		BREAKS.push(player instanceof ServerPlayer serverPlayer && UpgradeManager.has(serverPlayer, Upgrade.ENCHANTED));
		return true;
	}

	public static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
		BREAKS.poll();
	}

	public static void canceledBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
		BREAKS.poll();
	}

	/** How many times a dropped block stack comes out (1 = normal). */
	public static int blockCopies(RandomSource random) {
		return Boolean.TRUE.equals(BREAKS.peek()) ? roll(random) : 1;
	}

	public static void startMobLoot(LivingEntity mob, DamageSource source) {
		boolean enchanted = source.getEntity() instanceof ServerPlayer player && UpgradeManager.has(player, Upgrade.ENCHANTED);
		DYING.push(enchanted ? mob.getId() : -1);
	}

	public static void endMobLoot(LivingEntity mob) {
		DYING.poll();
	}

	/** Called before a mob drops a stack: spawns the extra copies. */
	public static void spawnExtraMobDrops(Entity entity, ServerLevel level, ItemStack stack, Vec3 offset) {
		Integer dying = DYING.peek();

		if (stack.isEmpty() || dying == null || dying != entity.getId()) {
			return;
		}

		int copies = roll(level.getRandom());

		for (int i = 1; i < copies; i++) {
			ItemEntity item = new ItemEntity(level, entity.getX() + offset.x, entity.getY() + offset.y, entity.getZ() + offset.z, stack.copy());
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
		}
	}

	private static int roll(RandomSource random) {
		UpgradesConfig config = UpgradesConfig.get();
		return config.dropsMin + random.nextInt(config.dropsMax - config.dropsMin + 1);
	}
}
