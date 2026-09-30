package io.github.pikczu77.hpsize.client;

/**
 * Carries the Ender Dragon's scale in its render state (vanilla does not draw the dragon scaled).
 */
public interface DragonScaleHolder {
	float hpsize$getScale();

	void hpsize$setScale(float scale);
}
