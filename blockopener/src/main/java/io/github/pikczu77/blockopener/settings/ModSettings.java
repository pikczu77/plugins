package io.github.pikczu77.blockopener.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.pikczu77.blockopener.registry.ModAttachments;
import net.minecraft.server.MinecraftServer;

/**
 * World settings, changed with {@code /blockopener settings}.
 *
 * @param explosionsBreakBlocks ability explosions (pumpkins, slam, kicks, fireballs, TNT cannon) destroy terrain
 * @param announceFinds         broadcast secret item finds in chat and play the reveal fanfare
 * @param lootRolls             how many times the opening loot table is rolled per block
 * @param openCooldown          ticks between two openings by the same player
 */
public record ModSettings(boolean explosionsBreakBlocks, boolean announceFinds, int lootRolls, int openCooldown) {
	public static final ModSettings DEFAULT = new ModSettings(true, true, 1, 4);

	public static final Codec<ModSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("explosions_break_blocks", DEFAULT.explosionsBreakBlocks).forGetter(ModSettings::explosionsBreakBlocks),
		Codec.BOOL.optionalFieldOf("announce_finds", DEFAULT.announceFinds).forGetter(ModSettings::announceFinds),
		Codec.intRange(1, 16).optionalFieldOf("loot_rolls", DEFAULT.lootRolls).forGetter(ModSettings::lootRolls),
		Codec.intRange(0, 200).optionalFieldOf("open_cooldown", DEFAULT.openCooldown).forGetter(ModSettings::openCooldown)
	).apply(instance, ModSettings::new));

	public static ModSettings get(MinecraftServer server) {
		return server.overworld().getAttachedOrCreate(ModAttachments.SETTINGS);
	}

	public static void set(MinecraftServer server, ModSettings settings) {
		server.overworld().setAttached(ModAttachments.SETTINGS, settings);
	}

	public ModSettings withExplosionsBreakBlocks(boolean value) {
		return new ModSettings(value, this.announceFinds, this.lootRolls, this.openCooldown);
	}

	public ModSettings withAnnounceFinds(boolean value) {
		return new ModSettings(this.explosionsBreakBlocks, value, this.lootRolls, this.openCooldown);
	}

	public ModSettings withLootRolls(int value) {
		return new ModSettings(this.explosionsBreakBlocks, this.announceFinds, value, this.openCooldown);
	}

	public ModSettings withOpenCooldown(int value) {
		return new ModSettings(this.explosionsBreakBlocks, this.announceFinds, this.lootRolls, value);
	}
}
