package io.github.pikczu77.blockopener.mixin;

import io.github.pikczu77.blockopener.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerMixin {
	/** The Glass Spyglass zooms and shows the scope overlay like a vanilla spyglass. */
	@Inject(method = "isScoping", at = @At("RETURN"), cancellable = true)
	private void blockopener$glassSpyglass(CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player) (Object) this;
		if (!cir.getReturnValueZ() && self.isUsingItem() && self.getUseItem().is(ModItems.GLASS_SPYGLASS)) {
			cir.setReturnValue(true);
		}
	}
}
