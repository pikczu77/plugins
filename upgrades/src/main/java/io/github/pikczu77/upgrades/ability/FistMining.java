package io.github.pikczu77.upgrades.ability;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeAccess;

/**
 * Fists that mine like a pickaxe. The tier follows the pickaxes growing out of the shoulder: wood, stone, iron,
 * diamond, netherite. Held items that are worse than the fist for a block are ignored.
 */
public final class FistMining {
	private static final ItemStack[] PICKAXES = {
			ItemStack.EMPTY,
			new ItemStack(Items.WOODEN_PICKAXE),
			new ItemStack(Items.STONE_PICKAXE),
			new ItemStack(Items.IRON_PICKAXE),
			new ItemStack(Items.DIAMOND_PICKAXE),
			new ItemStack(Items.NETHERITE_PICKAXE)
	};

	private FistMining() {
	}

	private static ItemStack fist(Player player) {
		return PICKAXES[Upgrade.fistTier(UpgradeAccess.mask(player))];
	}

	public static float destroySpeed(Player player, ItemStack held, BlockState state, float heldSpeed) {
		ItemStack fist = fist(player);
		return fist.isEmpty() ? heldSpeed : Math.max(heldSpeed, fist.getDestroySpeed(state));
	}

	public static boolean correctTool(Player player, BlockState state) {
		ItemStack fist = fist(player);
		return !fist.isEmpty() && fist.isCorrectToolForDrops(state);
	}
}
