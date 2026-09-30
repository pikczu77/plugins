package io.github.pikczu77.upgrades.upgrade;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import io.github.pikczu77.upgrades.ability.Clones;
import io.github.pikczu77.upgrades.ability.Passives;
import io.github.pikczu77.upgrades.config.UpgradesConfig;
import io.github.pikczu77.upgrades.net.Payloads;
import io.github.pikczu77.upgrades.util.Screens;

/**
 * Keeps track of which upgrades every online player has. The set is always recomputed from the advancements, so
 * {@code /advancement grant|revoke} works too.
 */
public final class UpgradeManager {
	private static final Map<UUID, Long> MASKS = new HashMap<>();
	/** Selected power of every input group, per player (not saved: the newest power is picked again on join). */
	private static final Map<UUID, EnumMap<Upgrade.Group, Upgrade>> SELECTED = new HashMap<>();
	/** Upgrades unlocked during the current advancement award, announced once the vanilla chat message is out. */
	private static final Map<UUID, List<Upgrade>> PENDING = new HashMap<>();

	private UpgradeManager() {
	}

	public static long mask(Player player) {
		return MASKS.getOrDefault(player.getUUID(), 0L);
	}

	public static boolean has(Player player, Upgrade upgrade) {
		return Upgrade.has(mask(player), upgrade);
	}

	public static long compute(ServerPlayer player) {
		UpgradesConfig config = UpgradesConfig.get();
		long mask = 0L;

		for (Upgrade upgrade : Upgrade.VALUES) {
			if (!config.isEnabled(upgrade)) {
				continue;
			}

			AdvancementHolder holder = player.level().getServer().getAdvancements().get(upgrade.advancement);

			if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) {
				mask |= upgrade.bit();
			}
		}

		return mask;
	}

	/** Recomputes the upgrades of a player and tells everyone who can see them. */
	public static void refresh(ServerPlayer player) {
		long before = mask(player);
		long after = compute(player);
		MASKS.put(player.getUUID(), after);

		EnumMap<Upgrade.Group, Upgrade> selected = SELECTED.computeIfAbsent(player.getUUID(), uuid -> new EnumMap<>(Upgrade.Group.class));

		// A removed upgrade cannot stay selected; a new one becomes the selected power of its group (like in the video,
		// the newest power is always the one you use).
		selected.values().removeIf(upgrade -> !Upgrade.has(after, upgrade));

		for (Upgrade upgrade : Upgrade.VALUES) {
			boolean added = Upgrade.has(after, upgrade) && !Upgrade.has(before, upgrade);

			if (upgrade.group != Upgrade.Group.NONE && Upgrade.has(after, upgrade) && (added || !selected.containsKey(upgrade.group))) {
				selected.put(upgrade.group, upgrade);
			}
		}

		Passives.apply(player, after);

		if (before != after) {
			sync(player);
		}
	}

	public static void sync(ServerPlayer player) {
		Payloads.SyncUpgrades payload = payload(player);

		if (ServerPlayNetworking.canSend(player, Payloads.SyncUpgrades.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}

		for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
			if (watcher != player && ServerPlayNetworking.canSend(watcher, Payloads.SyncUpgrades.TYPE)) {
				ServerPlayNetworking.send(watcher, payload);
			}
		}
	}

	public static void sendTo(ServerPlayer watcher, ServerPlayer player) {
		if (ServerPlayNetworking.canSend(watcher, Payloads.SyncUpgrades.TYPE)) {
			ServerPlayNetworking.send(watcher, payload(player));
		}
	}

	private static Payloads.SyncUpgrades payload(ServerPlayer player) {
		int packed = 0;
		EnumMap<Upgrade.Group, Upgrade> selected = SELECTED.get(player.getUUID());

		if (selected != null) {
			for (Map.Entry<Upgrade.Group, Upgrade> entry : selected.entrySet()) {
				packed |= (entry.getValue().ordinal() + 1) << (entry.getKey().ordinal() * 8);
			}
		}

		return new Payloads.SyncUpgrades(player.getId(), mask(player), packed);
	}

	public static @Nullable Upgrade selected(ServerPlayer player, Upgrade.Group group) {
		EnumMap<Upgrade.Group, Upgrade> selected = SELECTED.get(player.getUUID());
		return selected == null ? null : selected.get(group);
	}

	/** Selects the next unlocked power of the group (the power key). */
	public static void cycle(ServerPlayer player, Upgrade.Group group) {
		long mask = mask(player);
		List<Upgrade> powers = new ArrayList<>();

		for (Upgrade upgrade : Upgrade.VALUES) {
			if (upgrade.group == group && Upgrade.has(mask, upgrade)) {
				powers.add(upgrade);
			}
		}

		if (powers.isEmpty()) {
			return;
		}

		EnumMap<Upgrade.Group, Upgrade> selected = SELECTED.computeIfAbsent(player.getUUID(), uuid -> new EnumMap<>(Upgrade.Group.class));
		Upgrade current = selected.get(group);
		int next = (powers.indexOf(current) + 1) % powers.size();
		selected.put(group, powers.get(next));
		Screens.actionBar(player, Component.literal(group == Upgrade.Group.SNEAK_CLICK ? "Kucnij + PPM: " : "PPM: ")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal(powers.get(next).displayName).withStyle(style(powers.get(next)))));
		sync(player);
	}

	/** Called by the advancement mixin when an advancement gets completed. */
	public static void onAdvancementDone(ServerPlayer player, AdvancementHolder holder) {
		Upgrade upgrade = Upgrade.byAdvancement(holder.id());

		if (upgrade != null && UpgradesConfig.get().isEnabled(upgrade)) {
			PENDING.computeIfAbsent(player.getUUID(), uuid -> new ArrayList<>()).add(upgrade);
		}
	}

	/** Called after the award finished (and vanilla printed the advancement message). */
	public static void afterAward(ServerPlayer player) {
		List<Upgrade> unlocked = PENDING.remove(player.getUUID());
		refresh(player);

		if (unlocked != null) {
			for (Upgrade upgrade : unlocked) {
				announce(player, upgrade);

				if (upgrade == Upgrade.MULTIPLICITY) {
					Clones.startMultiplicity(player);
				}
			}
		}
	}

	/** Called after an advancement got revoked. */
	public static void afterRevoke(ServerPlayer player) {
		refresh(player);
	}

	public static void announce(ServerPlayer player, Upgrade upgrade) {
		MinecraftServer server = player.level().getServer();

		for (ServerPlayer receiver : server.getPlayerList().getPlayers()) {
			for (String line : upgrade.lines) {
				receiver.sendSystemMessage(line(upgrade, line));
			}
		}

		if (UpgradesConfig.get().titles) {
			Screens.title(List.of(player), Component.literal("NOWE ULEPSZENIE!").withStyle(style -> style.withColor(0x55FF55)),
					Component.literal(upgrade.displayName).withStyle(style(upgrade)), 5, 50, 15);
		}

		Screens.sound(List.of(player), Screens.sound(SoundEvents.PLAYER_LEVELUP), 1.0F, 0.7F);
		Screens.sound(List.of(player), Screens.sound(SoundEvents.BEACON_POWER_SELECT), 1.0F, 1.4F);
	}

	/** "+ text" with the {highlighted} parts in the upgrade colour. */
	public static MutableComponent line(Upgrade upgrade, String text) {
		MutableComponent line = Component.literal("+ ").withStyle(style -> style.withColor(upgrade.color));
		StringBuilder part = new StringBuilder();
		boolean highlighted = false;

		for (char c : text.toCharArray()) {
			if (c == '{' || c == '}') {
				append(line, part, highlighted, upgrade);
				highlighted = c == '{';
			} else {
				part.append(c);
			}
		}

		append(line, part, highlighted, upgrade);
		return line;
	}

	private static void append(MutableComponent line, StringBuilder part, boolean highlighted, Upgrade upgrade) {
		if (part.isEmpty()) {
			return;
		}

		line.append(Component.literal(part.toString()).withStyle(highlighted ? style(upgrade) : Style.EMPTY.withColor(ChatFormatting.GRAY)));
		part.setLength(0);
	}

	public static Style style(Upgrade upgrade) {
		return Style.EMPTY.withColor(TextColor.fromRgb(upgrade.color)).withBold(true);
	}

	/** Game tests only: gives a (fake) player upgrades without advancements. */
	public static void setMaskForTest(Player player, long mask) {
		MASKS.put(player.getUUID(), mask);
	}

	public static void forget(ServerPlayer player) {
		MASKS.remove(player.getUUID());
		SELECTED.remove(player.getUUID());
		PENDING.remove(player.getUUID());
	}

	public static void reset() {
		MASKS.clear();
		SELECTED.clear();
		PENDING.clear();
	}

	/** Completes (or revokes) the advancement behind an upgrade, which then grants (or removes) the upgrade. */
	public static boolean setAdvancement(ServerPlayer player, Upgrade upgrade, boolean done) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(upgrade.advancement);

		if (holder == null) {
			return false;
		}

		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
		boolean changed = false;

		if (done) {
			for (String criterion : progress.getRemainingCriteria()) {
				changed |= player.getAdvancements().award(holder, criterion);
			}
		} else {
			for (String criterion : progress.getCompletedCriteria()) {
				changed |= player.getAdvancements().revoke(holder, criterion);
			}
		}

		return changed;
	}
}
