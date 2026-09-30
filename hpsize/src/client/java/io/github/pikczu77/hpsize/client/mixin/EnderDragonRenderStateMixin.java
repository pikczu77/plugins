package io.github.pikczu77.hpsize.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;

import io.github.pikczu77.hpsize.client.DragonScaleHolder;

@Mixin(EnderDragonRenderState.class)
public abstract class EnderDragonRenderStateMixin implements DragonScaleHolder {
	@Unique
	private float hpsize$scale = 1.0F;

	@Override
	public float hpsize$getScale() {
		return hpsize$scale;
	}

	@Override
	public void hpsize$setScale(float scale) {
		hpsize$scale = scale;
	}
}
