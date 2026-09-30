package io.github.pikczu77.upgrades.upgrade;

import java.util.List;

import net.minecraft.resources.Identifier;

/**
 * Something an advancement unlocks: one of the main upgrades from the video or a bonus.
 */
public interface Unlockable {
	String id();

	Identifier advancement();

	String displayName();

	int color();

	/** Description lines, {braces} mark the highlighted part. */
	List<String> lines();
}
