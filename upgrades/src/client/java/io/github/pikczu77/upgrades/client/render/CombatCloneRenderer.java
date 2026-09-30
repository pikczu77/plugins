package io.github.pikczu77.upgrades.client.render;

import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import io.github.pikczu77.upgrades.entity.CombatClone;

/**
 * Draws a combat clone as a copy of its owner: the player model with the owner's own skin (slim or wide arms).
 */
public class CombatCloneRenderer extends HumanoidMobRenderer<CombatClone, AvatarRenderState, PlayerModel> {
	private final PlayerModel wide;
	private final PlayerModel slim;

	public CombatCloneRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
		this.wide = this.model;
		this.slim = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new AvatarRenderState();
	}

	@Override
	public void extractRenderState(CombatClone clone, AvatarRenderState state, float partialTick) {
		super.extractRenderState(clone, state, partialTick);
		state.skin = skin(clone.ownerUuid());
		state.id = clone.getId();
	}

	private static PlayerSkin skin(UUID owner) {
		if (owner == null) {
			return DefaultPlayerSkin.getDefaultSkin();
		}

		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		PlayerInfo info = connection == null ? null : connection.getPlayerInfo(owner);
		return info != null ? info.getSkin() : DefaultPlayerSkin.get(owner);
	}

	@Override
	public void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		// Pick the arm width of the owner's skin for this clone.
		this.model = state.skin.model() == PlayerModelType.SLIM ? this.slim : this.wide;
		super.submit(state, pose, collector, camera);
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return state.skin.body().texturePath();
	}

	@Override
	protected void scale(AvatarRenderState state, PoseStack pose) {
		// Same size as a player (the player renderer shrinks the model slightly).
		pose.scale(0.9375F, 0.9375F, 0.9375F);
	}
}
