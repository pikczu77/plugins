package io.github.pikczu77.hpsize.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import io.github.pikczu77.hpsize.client.Zoom;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void hpsize$zoomScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		// While zooming, the mouse wheel changes the zoom instead of the hotbar slot.
		if (Minecraft.getInstance().screen == null && Zoom.onScroll(yOffset)) {
			ci.cancel();
		}
	}
}
