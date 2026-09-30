package io.github.pikczu77.upgrades.client;

import org.jspecify.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.world.entity.player.Player;

import io.github.pikczu77.upgrades.net.Payloads;
import io.github.pikczu77.upgrades.upgrade.Bonus;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeAccess;

/**
 * Upgrades of the players this client can see, keyed by entity id (sent by the server).
 */
public final class ClientUpgrades {
	private static final Int2LongOpenHashMap MASKS = new Int2LongOpenHashMap();
	private static final Int2IntOpenHashMap SELECTED = new Int2IntOpenHashMap();
	private static final Int2ObjectOpenHashMap<long[]> BONUSES = new Int2ObjectOpenHashMap<>();

	private ClientUpgrades() {
	}

	public static void install() {
		UpgradeAccess.clientMasks = player -> mask(player.getId());
		UpgradeAccess.clientBonuses = player -> bonuses(player.getId());
	}

	public static void accept(Payloads.SyncUpgrades payload) {
		MASKS.put(payload.entityId(), payload.mask());
		SELECTED.put(payload.entityId(), payload.selected());
		BONUSES.put(payload.entityId(), new long[] {payload.bonusesLow(), payload.bonusesHigh()});
	}

	public static long[] bonuses(int entityId) {
		long[] bits = BONUSES.get(entityId);
		return bits == null ? Bonus.empty() : bits;
	}

	public static long mask(int entityId) {
		return MASKS.get(entityId);
	}

	public static boolean has(Player player, Upgrade upgrade) {
		return Upgrade.has(mask(player.getId()), upgrade);
	}

	/** The power selected for the group, or null. */
	public static @Nullable Upgrade selected(int entityId, Upgrade.Group group) {
		int packed = (SELECTED.get(entityId) >> (group.ordinal() * 8)) & 0xFF;
		return packed == 0 || packed > Upgrade.VALUES.size() ? null : Upgrade.VALUES.get(packed - 1);
	}

	public static void clear() {
		MASKS.clear();
		SELECTED.clear();
		BONUSES.clear();
	}
}
