package io.github.pikczu77.upgrades.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

import io.github.pikczu77.upgrades.upgrade.Bonus;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

@Mixin(Animal.class)
public abstract class AnimalMixin {
	/** Two by Two bonus: breeding makes twins (the extra baby is added before vanilla adds its own). */
	@Inject(method = "spawnChildFromBreeding", at = @At("HEAD"))
	private void upgrades$twins(ServerLevel level, Animal partner, CallbackInfo ci) {
		Animal self = (Animal) (Object) this;
		ServerPlayer cause = self.getLoveCause() != null ? self.getLoveCause() : partner.getLoveCause();

		if (cause == null || !UpgradeManager.hasSpecial(cause, Bonus.Special.TWINS)) {
			return;
		}

		AgeableMob twin = self.getBreedOffspring(level, partner);

		if (twin != null) {
			twin.setBaby(true);
			twin.snapTo(self.getX(), self.getY(), self.getZ(), 0.0F, 0.0F);
			level.addFreshEntityWithPassengers(twin);
		}
	}
}
