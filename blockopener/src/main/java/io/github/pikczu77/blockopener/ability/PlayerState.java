package io.github.pikczu77.blockopener.ability;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Per-player runtime state of the abilities. Not saved: it only matters while the player is online. */
final class PlayerState {
	// Pumpkin Boots
	int sneakTicks;
	Display.@Nullable BlockDisplay pumpkin;
	boolean addedInvisibility;

	// Anvil Chestplate
	boolean prevShift;
	boolean slamming;
	long slamReadyAt;

	// Diamond Leggings
	boolean grantedFlight;
	long fireballReadyAt;

	// Sculk Helmet
	long sonicBoomReadyAt;

	// Weeping Totem: last place the player stood safely
	@Nullable Vec3 safePos;
	@Nullable ResourceKey<Level> safeLevel;

	// Slime Gloves bounce
	boolean wasOnGround = true;
	double airVy;

	// Storm Hammer: own lightning and fire do not hurt until this game time
	long stormSafeUntil;

	// Obsidian Shield dome
	long domeReadyAt;

	// Piston Launcher, slam, flight: no fall damage until the player lands (or until this game time).
	boolean launched;
	int launchedTicks;
	long noFallUntil;
}
