package io.github.pikczu77.upgrades.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;

import io.github.pikczu77.upgrades.ability.MiniShields;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
	/** A projectile bounced off mini shields now belongs to the shielded player. */
	@Inject(method = "deflect", at = @At("RETURN"))
	private void upgrades$takeOwnership(ProjectileDeflection deflection, @Nullable Entity deflector, @Nullable EntityReference<Entity> owner,
			boolean byAttack, CallbackInfoReturnable<Boolean> cir) {
		MiniShields.afterDeflect((Projectile) (Object) this, deflection);
	}
}
