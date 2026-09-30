package io.github.pikczu77.hpsize.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;

/**
 * Lets the Ender Dragon use the scale attribute (vanilla always forces x1) and places its hitbox parts
 * (head, neck, body, tail, wings) as far from the dragon as its scale says, so they match the scaled model.
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
	@Inject(method = "sanitizeScale", at = @At("HEAD"), cancellable = true)
	private void hpsize$allowScale(float scale, CallbackInfoReturnable<Float> cir) {
		// The same limits as for every other living entity.
		cir.setReturnValue(Mth.clamp(scale, 0.0625F, 16.0F));
	}

	@Inject(method = "tickPart", at = @At("HEAD"), cancellable = true)
	private void hpsize$scalePart(EnderDragonPart part, double x, double y, double z, CallbackInfo ci) {
		EnderDragon dragon = (EnderDragon) (Object) this;
		float scale = dragon.getScale();
		EntityDimensions dimensions = part.getDimensions(part.getPose());

		if (Math.abs(part.getBbWidth() - dimensions.width()) > 1.0E-4F || Math.abs(part.getBbHeight() - dimensions.height()) > 1.0E-4F) {
			part.refreshDimensions();
		}

		part.setPos(dragon.getX() + x * scale, dragon.getY() + y * scale, dragon.getZ() + z * scale);
		ci.cancel();
	}
}
