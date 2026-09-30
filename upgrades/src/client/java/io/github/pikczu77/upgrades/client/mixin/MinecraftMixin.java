package io.github.pikczu77.upgrades.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import io.github.pikczu77.upgrades.client.PowerInput;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	public @Nullable LocalPlayer player;

	@Shadow
	public @Nullable MultiPlayerGameMode gameMode;

	@Shadow
	public @Nullable HitResult hitResult;

	/**
	 * The end of the method is only reached when nothing used the click (no block, entity or item reacted), which is
	 * exactly when an empty hand should fire a power.
	 */
	@Inject(method = "startUseItem", at = @At("TAIL"))
	private void upgrades$emptyHandPower(CallbackInfo ci) {
		if (this.player == null || this.gameMode == null || this.gameMode.isDestroying() || this.player.isHandsBusy()) {
			return;
		}

		int target = this.hitResult instanceof EntityHitResult entityHit ? entityHit.getEntity().getId() : -1;
		PowerInput.emptyHandClick(this.player, target);
	}
}
