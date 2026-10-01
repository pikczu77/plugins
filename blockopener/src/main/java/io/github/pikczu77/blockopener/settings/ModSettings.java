package io.github.pikczu77.blockopener.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.pikczu77.blockopener.registry.ModAttachments;
import net.minecraft.server.MinecraftServer;

/**
 * World settings, changed with {@code /blockopener settings}.
 *
 * @param explosionsBreakBlocks  ability explosions (pumpkins, slam, kicks, fireballs, TNT cannon) destroy terrain
 * @param explosionsDestroyItems ability explosions blow up dropped items, like vanilla TNT (the slam never does:
 *                               it would destroy the loot it just opened)
 * @param announceFinds          broadcast secret item finds in chat and play the reveal fanfare
 * @param lootRolls              how many times the opening loot table is rolled per block
 * @param openCooldown           ticks between two openings by the same player
 * @param extendedItems          12 more secret items on top of the video's 10
 */
public record ModSettings(
	boolean explosionsBreakBlocks, boolean explosionsDestroyItems, boolean announceFinds, int lootRolls, int openCooldown, boolean extendedItems
) {
	public static final ModSettings DEFAULT = new ModSettings(true, true, true, 1, 4, false);

	public static final Codec<ModSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("explosions_break_blocks", DEFAULT.explosionsBreakBlocks).forGetter(ModSettings::explosionsBreakBlocks),
		Codec.BOOL.optionalFieldOf("explosions_destroy_items", DEFAULT.explosionsDestroyItems).forGetter(ModSettings::explosionsDestroyItems),
		Codec.BOOL.optionalFieldOf("announce_finds", DEFAULT.announceFinds).forGetter(ModSettings::announceFinds),
		Codec.intRange(1, 16).optionalFieldOf("loot_rolls", DEFAULT.lootRolls).forGetter(ModSettings::lootRolls),
		Codec.intRange(0, 200).optionalFieldOf("open_cooldown", DEFAULT.openCooldown).forGetter(ModSettings::openCooldown),
		Codec.BOOL.optionalFieldOf("extended_items", DEFAULT.extendedItems).forGetter(ModSettings::extendedItems)
	).apply(instance, ModSettings::new));

	public static ModSettings get(MinecraftServer server) {
		return server.overworld().getAttachedOrCreate(ModAttachments.SETTINGS);
	}

	public static void set(MinecraftServer server, ModSettings settings) {
		server.overworld().setAttached(ModAttachments.SETTINGS, settings);
	}

	public ModSettings withExplosionsBreakBlocks(boolean value) {
		return new ModSettings(value, this.explosionsDestroyItems, this.announceFinds, this.lootRolls, this.openCooldown, this.extendedItems);
	}

	public ModSettings withExplosionsDestroyItems(boolean value) {
		return new ModSettings(this.explosionsBreakBlocks, value, this.announceFinds, this.lootRolls, this.openCooldown, this.extendedItems);
	}

	public ModSettings withAnnounceFinds(boolean value) {
		return new ModSettings(this.explosionsBreakBlocks, this.explosionsDestroyItems, value, this.lootRolls, this.openCooldown, this.extendedItems);
	}

	public ModSettings withLootRolls(int value) {
		return new ModSettings(this.explosionsBreakBlocks, this.explosionsDestroyItems, this.announceFinds, value, this.openCooldown, this.extendedItems);
	}

	public ModSettings withOpenCooldown(int value) {
		return new ModSettings(this.explosionsBreakBlocks, this.explosionsDestroyItems, this.announceFinds, this.lootRolls, value, this.extendedItems);
	}

	public ModSettings withExtendedItems(boolean value) {
		return new ModSettings(this.explosionsBreakBlocks, this.explosionsDestroyItems, this.announceFinds, this.lootRolls, this.openCooldown, value);
	}
}
