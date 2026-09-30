package io.github.pikczu77.upgrades.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block display entities used for temporary visuals (the big nap bed, the pocket portals). They are tagged, and any
 * that survive a server restart are removed when they load again.
 */
public final class TempDisplays {
	private static final String TAG = "upgrades_temp";
	private static final Set<UUID> ACTIVE = new HashSet<>();

	private TempDisplays() {
	}

	/** An axis aligned box of the given block from (x0, y0, z0) to (x1, y1, z1). */
	public static Display.BlockDisplay box(ServerLevel level, BlockState state, double x0, double y0, double z0, double x1, double y1, double z1,
			boolean fullBright) {
		Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
		display.setPos(x0, y0, z0);
		display.setBlockState(state);
		display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
				new Vector3f((float) (x1 - x0), (float) (y1 - y0), (float) (z1 - z0)), new Quaternionf()));

		if (fullBright) {
			display.setBrightnessOverride(Brightness.FULL_BRIGHT);
		}

		display.addTag(TAG);
		level.addFreshEntity(display);
		ACTIVE.add(display.getUUID());
		return display;
	}

	public static void remove(ServerLevel level, UUID id) {
		ACTIVE.remove(id);
		Entity entity = level.getEntity(id);

		if (entity != null) {
			entity.discard();
		}
	}

	/** Removes displays left over from a previous session. */
	public static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity instanceof Display && entity.getTags().contains(TAG) && !ACTIVE.contains(entity.getUUID())) {
			entity.discard();
		}
	}

	public static void reset() {
		ACTIVE.clear();
	}
}
