package io.github.pikczu77.upgrades.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;

import io.github.pikczu77.upgrades.ability.BonusAbilities;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Shadow
	public abstract Input getLastClientInput();

	/** Sees every jump key press as it arrives, even a very short one (for the double jump). */
	@Inject(method = "setLastClientInput", at = @At("HEAD"))
	private void upgrades$onInput(Input input, CallbackInfo ci) {
		if (input.jump() && !this.getLastClientInput().jump()) {
			BonusAbilities.onJumpPressed((ServerPlayer) (Object) this);
		}
	}
}
