package io.github.pikczu77.blockopener.registry;

import io.github.pikczu77.blockopener.BlockOpener;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** Damage types are data driven, see {@code data/blockopener/damage_type}. */
public final class ModDamageTypes {
	public static final ResourceKey<DamageType> VOID_WATER = key("void_water");
	public static final ResourceKey<DamageType> MOSSPHERE = key("mossphere");

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, BlockOpener.id(name));
	}

	public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type));
	}

	public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), direct, causing);
	}

	private ModDamageTypes() {
	}
}
