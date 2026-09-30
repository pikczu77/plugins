package io.github.pikczu77.upgrades.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * Completing an advancement grants the matching upgrade (announced after the vanilla chat message).
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	@Unique
	private boolean upgrades$completed;

	@Inject(method = "award", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
	private void upgrades$onCompleted(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
		UpgradeManager.onAdvancementDone(this.player, advancement);
		this.upgrades$completed = true;
	}

	@Inject(method = "award", at = @At("RETURN"))
	private void upgrades$afterAward(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
		if (this.upgrades$completed) {
			this.upgrades$completed = false;
			UpgradeManager.afterAward(this.player);
		}
	}

	@Inject(method = "revoke", at = @At("RETURN"))
	private void upgrades$afterRevoke(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && Upgrade.byAdvancement(advancement.id()) != null) {
			UpgradeManager.afterRevoke(this.player);
		}
	}
}
