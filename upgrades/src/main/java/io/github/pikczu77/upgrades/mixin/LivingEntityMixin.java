package io.github.pikczu77.upgrades.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import io.github.pikczu77.upgrades.ability.Enchanted;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
	private void upgrades$startLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
		Enchanted.startMobLoot((LivingEntity) (Object) this, source);
	}

	@Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
	private void upgrades$endLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
		Enchanted.endMobLoot((LivingEntity) (Object) this);
	}
}
