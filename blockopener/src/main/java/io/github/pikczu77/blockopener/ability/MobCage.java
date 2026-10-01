package io.github.pikczu77.blockopener.ability;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Mob Cage: right-click any mob (yes, a Warden) to catch it, right-click again to let it out.
 * Released mobs are your allies until they die (see {@link Allies}).
 */
public final class MobCage {
	private static final Logger LOGGER = LogUtils.getLogger();
	/** Key of the caught mob's data inside the cage's {@code minecraft:custom_data}. */
	public static final String KEY = "blockopener_mob";

	public static boolean canCatch(Entity entity) {
		return entity instanceof Mob && !(entity instanceof EnderDragon) && entity.isAlive();
	}

	public static boolean isFull(ItemStack stack) {
		return captured(stack) != null;
	}

	public static @Nullable CompoundTag captured(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		return data.copyTag().getCompound(KEY).orElse(null);
	}

	/** The id of the caught mob's type, for the tooltip. */
	public static @Nullable String capturedType(ItemStack stack) {
		CompoundTag tag = captured(stack);
		return tag == null ? null : tag.getString("id").orElse(null);
	}

	public static boolean capture(ServerPlayer player, ItemStack cage, Mob mob) {
		if (isFull(cage) || !canCatch(mob)) {
			return false;
		}
		mob.ejectPassengers();
		mob.stopRiding();
		CompoundTag tag;
		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(mob.problemPath(), LOGGER)) {
			TagValueOutput output = TagValueOutput.createWithContext(reporter, mob.registryAccess());
			if (!mob.save(output)) {
				return false;
			}
			tag = output.buildResult();
		}
		// A fresh UUID on release, so a copied cage cannot clash with the original mob.
		tag.remove("UUID");
		CompoundTag data = new CompoundTag();
		data.put(KEY, tag);
		cage.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

		ServerLevel level = player.level();
		Vec3 at = mob.position().add(0.0, mob.getBbHeight() / 2, 0.0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 40, mob.getBbWidth() / 2, mob.getBbHeight() / 2, mob.getBbWidth() / 2, 0.05);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.2F, 0.8F);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 0.5F);
		mob.discard();
		return true;
	}

	public static boolean release(ServerPlayer player, ItemStack cage, Vec3 at) {
		CompoundTag tag = captured(cage);
		if (tag == null) {
			return false;
		}
		ServerLevel level = player.level();
		Entity entity = EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.LOAD, loaded -> {
			loaded.snapTo(at.x, at.y, at.z, player.getYRot() + 180.0F, 0.0F);
			return loaded;
		});
		if (entity == null) {
			return false;
		}
		entity.setDeltaMovement(Vec3.ZERO);
		entity.resetFallDistance();
		if (!level.tryAddFreshEntityWithPassengers(entity)) {
			return false;
		}
		cage.remove(DataComponents.CUSTOM_DATA);
		if (entity instanceof Mob mob) {
			mob.setPersistenceRequired();
			Allies.add(mob, player);
		}
		level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + entity.getBbHeight() / 2, at.z, 50, entity.getBbWidth() / 2, entity.getBbHeight() / 2, entity.getBbWidth() / 2, 0.3);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1.0F, 1.3F);
		return true;
	}

	private MobCage() {
	}
}
