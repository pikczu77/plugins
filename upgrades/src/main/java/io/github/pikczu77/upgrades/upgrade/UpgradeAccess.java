package io.github.pikczu77.upgrades.upgrade;

import java.util.function.ToLongFunction;

import net.minecraft.world.entity.player.Player;

/**
 * Upgrades of a player on either side. Code shared by the client and the server (mining speed prediction,
 * projectile deflection) asks here; the client mod installs its synced copy on start.
 */
public final class UpgradeAccess {
	public static ToLongFunction<Player> clientMasks = player -> 0L;

	private UpgradeAccess() {
	}

	public static long mask(Player player) {
		return player.level().isClientSide() ? clientMasks.applyAsLong(player) : UpgradeManager.mask(player);
	}

	public static boolean has(Player player, Upgrade upgrade) {
		return Upgrade.has(mask(player), upgrade);
	}
}
