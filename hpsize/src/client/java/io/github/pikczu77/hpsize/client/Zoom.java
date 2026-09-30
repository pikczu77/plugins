package io.github.pikczu77.hpsize.client;

import net.minecraft.client.Minecraft;

/**
 * Hold-to-zoom (great for filming giant mobs from far away). Scroll while zooming to change the zoom.
 */
public final class Zoom {
	private static boolean active;
	private static double factor = 4.0;
	private static double current = 1.0;
	private static long lastNanos;
	private static Boolean savedSmoothCamera;

	private Zoom() {
	}

	public static void setActive(Minecraft minecraft, boolean down) {
		if (down == active) {
			return;
		}

		active = down;

		if (down) {
			factor = ClientConfig.get().zoomFactor;

			if (ClientConfig.get().zoomCinematic) {
				savedSmoothCamera = minecraft.options.smoothCamera;
				minecraft.options.smoothCamera = true;
			}
		} else if (savedSmoothCamera != null) {
			minecraft.options.smoothCamera = savedSmoothCamera;
			savedSmoothCamera = null;
		}
	}

	public static boolean isActive() {
		return active;
	}

	/**
	 * Current (animated) zoom factor, 1.0 = no zoom.
	 */
	public static double currentFactor() {
		return current;
	}

	public static float modifyFov(float fov) {
		long now = System.nanoTime();
		double seconds = lastNanos == 0 ? 0.0 : Math.min(0.25, (now - lastNanos) / 1.0E9);
		lastNanos = now;

		double target = active ? factor : 1.0;
		current += (target - current) * (1.0 - Math.exp(-seconds * 14.0));

		if (Math.abs(target - current) < 0.001) {
			current = target;
		}

		return (float) (fov / current);
	}

	/**
	 * @return true if the scroll was used for zooming
	 */
	public static boolean onScroll(double amount) {
		if (!active || amount == 0) {
			return false;
		}

		factor = Math.max(1.1, Math.min(50.0, factor * (amount > 0 ? 1.2 : 1.0 / 1.2)));
		return true;
	}
}
