package io.github.pikczu77.blockopener.ability;

import net.minecraft.world.entity.Display;
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

	// Piston Launcher, slam, flight: no fall damage until the player lands (or until this game time).
	boolean launched;
	int launchedTicks;
	long noFallUntil;
}
