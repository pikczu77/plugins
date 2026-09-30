package io.github.pikczu77.upgrades.ability;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import io.github.pikczu77.upgrades.config.UpgradesConfig;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * Wires every power to its trigger and dispatches the empty-hand clicks.
 */
public final class Abilities {
	private Abilities() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(VeinMiner::afterBreak);
		PlayerBlockBreakEvents.BEFORE.register(Enchanted::beforeBreak);
		PlayerBlockBreakEvents.AFTER.register(Enchanted::afterBreak);
		PlayerBlockBreakEvents.CANCELED.register(Enchanted::canceledBreak);

		ServerLivingEntityEvents.AFTER_DAMAGE.register(Combat::afterDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Clones::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(Clones::afterDeath);

		UseItemCallback.EVENT.register(Obsidian::useItem);
		UseItemCallback.EVENT.register(BlazePower::useItem);
		UseBlockCallback.EVENT.register(Obsidian::useBlock);
		UseBlockCallback.EVENT.register(DealSniffer::useBlock);
		UseEntityCallback.EVENT.register(DealSniffer::useEntity);
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			long mask = UpgradeManager.mask(player);

			if (mask == 0L || !player.isAlive() || player.gameMode() == GameType.SPECTATOR) {
				continue;
			}

			Passives.tick(player, mask);
			GreenThumb.tick(player, mask);
			Combat.tick(player, mask);
			EyeSpy.tick(player, mask);
		}

		Nap.tick(server);
		DealSniffer.tick(server);
		HotLava.tick(server);
		PortalGun.tick(server);
		Clones.tick(server);
	}

	/**
	 * Right-click with an empty hand that vanilla did nothing with (sent by the client mod).
	 */
	public static void usePower(ServerPlayer player, boolean sneaking, int targetId) {
		if (!player.isAlive() || player.gameMode() == GameType.SPECTATOR || !player.getMainHandItem().isEmpty()) {
			return;
		}

		@Nullable Entity target = targetId >= 0 ? player.level().getEntity(targetId) : null;

		// A sniffed trade partner always opens its trades first.
		if (DealSniffer.tryOpen(player, target)) {
			return;
		}

		Upgrade.Group group = sneaking ? Upgrade.Group.SNEAK_CLICK : Upgrade.Group.CLICK;

		for (Upgrade power : powers(player, group)) {
			switch (power) {
				case GREEN_THUMB -> GreenThumb.till(player);
				case TRIGGER_FINGER -> Shooting.arrow(player);
				case PORTAL_GUN -> PortalGun.shoot(player);
				case DRAGON_WING -> Shooting.homingFireballs(player);
				case NAP -> Nap.start(player);
				case DEAL_SNIFFER -> DealSniffer.sniff(player);
				default -> {
				}
			}
		}
	}

	/** The powers a click fires: the selected one, or every unlocked one in the "all" mode. */
	private static List<Upgrade> powers(ServerPlayer player, Upgrade.Group group) {
		List<Upgrade> powers = new ArrayList<>();

		if (UpgradesConfig.get().powerMode == UpgradesConfig.PowerMode.ALL) {
			for (Upgrade upgrade : Upgrade.VALUES) {
				if (upgrade.group == group && UpgradeManager.has(player, upgrade)) {
					powers.add(upgrade);
				}
			}
		} else {
			Upgrade selected = UpgradeManager.selected(player, group);

			if (selected != null && UpgradeManager.has(player, selected)) {
				powers.add(selected);
			}
		}

		return powers;
	}

	public static void forget(ServerPlayer player) {
		EyeSpy.forget(player);
		Nap.forget(player);
		DealSniffer.forget(player);
		Shooting.forget(player);
		PortalGun.forget(player);
	}

	public static void reset() {
		EyeSpy.reset();
		Nap.reset();
		DealSniffer.reset();
		Shooting.reset();
		PortalGun.reset();
		HotLava.reset();
		Combat.reset();
		Clones.reset();
	}
}
