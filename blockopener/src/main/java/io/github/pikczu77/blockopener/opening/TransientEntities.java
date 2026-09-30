package io.github.pikczu77.blockopener.opening;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Display entities used for effects (opening animation, pumpkin disguise) only live while the server
 * is running. Any that got saved anyway (crash, server stop mid-effect) are removed when they load.
 */
public final class TransientEntities {
	public static final String TAG = "blockopener_transient";

	private static final Set<UUID> LIVE = new HashSet<>();

	/** Call before adding the entity to the level. */
	public static <T extends Entity> T spawn(ServerLevel level, T entity) {
		entity.addTag(TAG);
		LIVE.add(entity.getUUID());
		level.addFreshEntity(entity);
		return entity;
	}

	public static void remove(Entity entity) {
		LIVE.remove(entity.getUUID());
		entity.discard();
	}

	static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity.getTags().contains(TAG) && !LIVE.contains(entity.getUUID())) {
			entity.discard();
		}
	}

	static void clear() {
		LIVE.clear();
	}

	private TransientEntities() {
	}
}
