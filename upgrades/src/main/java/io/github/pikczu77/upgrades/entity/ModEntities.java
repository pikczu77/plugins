package io.github.pikczu77.upgrades.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import io.github.pikczu77.upgrades.Upgrades;

public final class ModEntities {
	public static final EntityType<CombatClone> COMBAT_CLONE = register("combat_clone",
			EntityType.Builder.of(CombatClone::new, MobCategory.MISC).sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(10));
	public static final EntityType<ThrownObsidian> THROWN_OBSIDIAN = register("thrown_obsidian",
			EntityType.Builder.<ThrownObsidian>of(ThrownObsidian::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.5F)
					.clientTrackingRange(6).updateInterval(10));
	public static final EntityType<HomingFireball> HOMING_FIREBALL = register("homing_fireball",
			EntityType.Builder.<HomingFireball>of(HomingFireball::new, MobCategory.MISC).noLootTable().sized(0.6F, 0.6F)
					.clientTrackingRange(6).updateInterval(2));

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Upgrades.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(COMBAT_CLONE, CombatClone.createAttributes());
	}
}
