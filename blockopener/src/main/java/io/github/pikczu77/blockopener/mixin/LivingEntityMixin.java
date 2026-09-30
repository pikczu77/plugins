package io.github.pikczu77.blockopener.mixin;

import io.github.pikczu77.blockopener.ability.SculkHelmet;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	/** Sculk Helmet wearers are immune to the Warden's Darkness. */
	@Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
	private void blockopener$darknessImmunity(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
		if (effect.is(MobEffects.DARKNESS) && SculkHelmet.isWearing((LivingEntity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
