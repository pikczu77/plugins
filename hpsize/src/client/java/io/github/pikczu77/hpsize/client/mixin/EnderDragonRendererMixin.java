package io.github.pikczu77.hpsize.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

import io.github.pikczu77.hpsize.client.DragonScaleHolder;

/**
 * Vanilla draws the Ender Dragon without its scale attribute. This scales the model (and the death rays)
 * around the dragon's position, matching the scaled hitbox parts.
 */
@Mixin(EnderDragonRenderer.class)
public abstract class EnderDragonRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;F)V",
			at = @At("TAIL"))
	private void hpsize$extractScale(EnderDragon dragon, EnderDragonRenderState state, float partialTick, CallbackInfo ci) {
		((DragonScaleHolder) state).hpsize$setScale(dragon.getScale());
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER))
	private void hpsize$scaleModel(EnderDragonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		float scale = ((DragonScaleHolder) state).hpsize$getScale();

		if (scale != 1.0F) {
			poseStack.scale(scale, scale, scale);
		}
	}
}
