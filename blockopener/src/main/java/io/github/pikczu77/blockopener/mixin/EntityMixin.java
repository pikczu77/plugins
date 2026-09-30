package io.github.pikczu77.blockopener.mixin;

import io.github.pikczu77.blockopener.ability.SculkHelmet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityMixin {
	/** Sculk Helmet wearers are silent to sculk sensors and shriekers. */
	@Inject(method = "dampensVibrations", at = @At("HEAD"), cancellable = true)
	private void blockopener$sculkSilence(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof LivingEntity living && SculkHelmet.isWearing(living)) {
			cir.setReturnValue(true);
		}
	}
}
