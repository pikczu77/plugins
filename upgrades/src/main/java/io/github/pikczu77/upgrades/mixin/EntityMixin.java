package io.github.pikczu77.upgrades.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.ability.Enchanted;
import io.github.pikczu77.upgrades.ability.MiniShields;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/** Mini shields: projectiles bounce off the player, straight back at the shooter. */
	@Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
	private void upgrades$miniShields(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
		ProjectileDeflection deflection = MiniShields.deflection((Entity) (Object) this, projectile);

		if (deflection != null) {
			cir.setReturnValue(deflection);
		}
	}

	/** Enchanted body: mob drops come out several times. */
	@Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",
			at = @At("HEAD"))
	private void upgrades$moreDrops(ServerLevel level, ItemStack stack, Vec3 offset, CallbackInfoReturnable<ItemEntity> cir) {
		Enchanted.spawnExtraMobDrops((Entity) (Object) this, level, stack, offset);
	}
}
