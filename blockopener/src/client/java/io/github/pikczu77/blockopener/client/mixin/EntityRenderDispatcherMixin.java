package io.github.pikczu77.blockopener.client.mixin;

import io.github.pikczu77.blockopener.registry.ModAttachments;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {
	/** A player in pumpkin form is only the pumpkin: hide the model, armor, held items and name tag. */
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void blockopener$hidePumpkinPlayers(E entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Player && Boolean.TRUE.equals(entity.getAttached(ModAttachments.PUMPKIN_FORM))) {
			cir.setReturnValue(false);
		}
	}
}
