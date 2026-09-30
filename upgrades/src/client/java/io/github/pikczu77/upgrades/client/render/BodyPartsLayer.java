package io.github.pikczu77.upgrades.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.pikczu77.upgrades.Upgrades;
import io.github.pikczu77.upgrades.client.ClientUpgrades;
import io.github.pikczu77.upgrades.upgrade.Upgrade;

/**
 * Draws the upgrades growing out of the player: the pickaxe chain on the shoulder, the hoe on the head, the golem arm
 * through the chest (with the clone mannequins and the lava bucket on it), the sword in the boot, the bed on the back,
 * the villager nose, the forehead bow, the mini shields, the obsidian horn, the pocket portal, the orbiting blaze rods,
 * the ender eye, the dragon wings and the dragon egg. Once the body is enchanted, everything glints.
 *
 * <p>Every part is positioned in a "friendly" frame of its body part: +X = the player's right, +Y = up, -Z = forward,
 * in blocks, starting at the pivot of the part (neck for the head and body, hip for the legs).
 */
public class BodyPartsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Item[] PICKAXES = {Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE};
	private static final Upgrade[] PICKAXE_UPGRADES = {Upgrade.FIST_PICKAXE, Upgrade.VEIN_MINER, Upgrade.VEIN_MINER_2, Upgrade.VEIN_MINER_3,
			Upgrade.VEIN_MINER_MAX};
	private static final Upgrade[] MANNEQUIN_UPGRADES = {Upgrade.CLONE_1, Upgrade.CLONE_2, Upgrade.CLONE_3};
	private static final String[] MANNEQUINS = {"mannequin_iron", "mannequin_diamond", "mannequin_netherite"};

	public BodyPartsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		long mask = ClientUpgrades.mask(state.id);

		if (mask == 0L || state.isInvisible || state.isSpectator) {
			return;
		}

		Parts parts = new Parts(pose, collector, light, state, Upgrade.has(mask, Upgrade.ENCHANTED));
		PlayerModel model = this.getParentModel();

		// Body.
		parts.begin(model.body);
		this.pickaxeChain(parts, mask);
		this.golemArm(parts, mask);
		this.backAndHips(parts, mask);
		this.wings(parts, mask, state);
		this.blazeRods(parts, mask, state);
		parts.end();

		// Head.
		parts.begin(model.head);
		this.head(parts, mask);
		parts.end();

		// Right leg.
		if (Upgrade.has(mask, Upgrade.SWORD_BOOT)) {
			parts.begin(model.rightLeg);
			parts.push(0.22, -0.66, -0.22);
			parts.rotY(40.0F);
			parts.rotZ(-45.0F);
			parts.item(Items.IRON_SWORD, 0.75F);
			parts.pop();
			parts.end();
		}
	}

	/** Pickaxes sticking out sideways from the right shoulder, each tier attached to the end of the last one. */
	private void pickaxeChain(Parts parts, long mask) {
		int index = 0;

		for (int i = 0; i < PICKAXES.length; i++) {
			if (!Upgrade.has(mask, PICKAXE_UPGRADES[i])) {
				continue;
			}

			parts.push(0.45 + index * 0.85, -0.1, 0.02);
			parts.rotZ(-45.0F);
			parts.item(PICKAXES[i], 0.8F);
			parts.pop();
			index++;
		}
	}

	/** The iron golem arm through the chest, with the lava bucket in its fist and the clone mannequins stacked on it. */
	private void golemArm(Parts parts, long mask) {
		boolean arm = Upgrade.has(mask, Upgrade.GOLEM_ARM);
		float length = 1.2F;

		parts.push(-0.06, -0.28, 0.35);
		parts.rotY(12.0F);
		parts.rotX(-10.0F);

		if (arm) {
			parts.push(0.0, 0.0, 0.0);
			parts.rotX(90.0F);
			parts.model("golem_arm", length, 0.5, 1.0, 0.5);
			parts.pop();
		}

		if (Upgrade.has(mask, Upgrade.HOT_HANDS)) {
			parts.push(0.0, -0.05, arm ? -length - 0.12 : -0.4);
			parts.item(Items.LAVA_BUCKET, 0.55F);
			parts.pop();
		}

		double height = 0.12;

		for (int i = 0; i < MANNEQUIN_UPGRADES.length; i++) {
			if (!Upgrade.has(mask, MANNEQUIN_UPGRADES[i])) {
				continue;
			}

			// Standing on the arm, the next one on the shoulders of the previous one.
			parts.push(0.0, height, arm ? -0.62 : -0.2);
			parts.rotZ(-6.0F);
			parts.model(MANNEQUINS[i], 0.75F, 0.5, 0.0, 0.5);
			parts.pop();
			height += 0.7;
		}

		parts.pop();
	}

	private void backAndHips(Parts parts, long mask) {
		if (Upgrade.has(mask, Upgrade.NAP)) {
			// Flat on the back like a shell, pillow up.
			parts.push(0.0, -0.38, 0.13);
			parts.model("back_bed", 0.8F, 0.5, 0.5, 0.0);
			parts.pop();
		}

		if (Upgrade.has(mask, Upgrade.MINI_SHIELDS)) {
			for (int side = -1; side <= 1; side += 2) {
				// Like pauldrons: facing up, outwards and a bit forwards, so they are seen from the front too.
				parts.push(side * 0.42, 0.08, -0.02);
				parts.rotY(side * 120.0F);
				parts.rotX(-45.0F);
				parts.model("mini_shield", 0.8F, 0.5, 0.5, 0.5);
				parts.pop();
			}
		}

		if (Upgrade.has(mask, Upgrade.PORTAL_GUN)) {
			// In the front pocket.
			parts.push(0.12, -0.6, -0.14);
			parts.model("pocket_portal", 0.4F, 0.5, 0.5, 0.5);
			parts.pop();
		}

		if (Upgrade.has(mask, Upgrade.MULTIPLICITY)) {
			parts.push(-0.3, 0.14, 0.0);
			parts.item(Items.DRAGON_EGG, 0.32F);
			parts.pop();
		}
	}

	private void wings(Parts parts, long mask, AvatarRenderState state) {
		if (!Upgrade.has(mask, Upgrade.DRAGON_WING)) {
			return;
		}

		float speed = state.isFallFlying ? 0.6F : 0.12F;
		float flap = Mth.sin(state.ageInTicks * speed) * 14.0F;

		for (int side = -1; side <= 1; side += 2) {
			parts.push(side * 0.1, -0.08, 0.2);
			parts.rotY(side * -25.0F);
			parts.rotZ(side * (32.0F + flap));

			if (side < 0) {
				// Mirror the right wing into a left one.
				parts.rotY(180.0F);
			}

			parts.model("dragon_wing", 1.5F, 0.0, 0.85, 0.5);
			parts.pop();
		}
	}

	/** Blaze rods circling around the waist, like a blaze. */
	private void blazeRods(Parts parts, long mask, AvatarRenderState state) {
		if (!Upgrade.has(mask, Upgrade.BLAZE_POWER)) {
			return;
		}

		int rods = 8;

		for (int i = 0; i < rods; i++) {
			float angle = state.ageInTicks * 3.0F + i * (360.0F / rods);
			double radians = Math.toRadians(angle);
			double bob = Mth.sin(state.ageInTicks * 0.1F + i) * 0.08;
			parts.push(Math.cos(radians) * 0.75, -0.5 + bob + (i % 2) * 0.18, Math.sin(radians) * 0.75);
			parts.rotY(-angle);
			parts.rotZ(45.0F);
			parts.item(Items.BLAZE_ROD, 0.6F);
			parts.pop();
		}
	}

	private void head(Parts parts, long mask) {
		if (Upgrade.has(mask, Upgrade.GREEN_THUMB)) {
			// A hoe standing on top of the head, blade up.
			parts.push(0.0, 0.8, 0.0);
			parts.rotZ(45.0F);
			parts.item(Items.WOODEN_HOE, 0.7F);
			parts.pop();
		}

		if (Upgrade.has(mask, Upgrade.TRIGGER_FINGER)) {
			// The bow on the forehead, standing up and pointing forward.
			parts.push(0.0, 0.62, -0.28);
			parts.rotY(90.0F);
			parts.rotX(-20.0F);
			parts.rotZ(45.0F);
			parts.item(Items.BOW, 0.75F);
			parts.pop();
		}

		if (Upgrade.has(mask, Upgrade.DEAL_SNIFFER)) {
			parts.push(0.0, 0.06, -0.25);
			parts.rotY(180.0F);
			parts.model("villager_nose", 1.0F, 0.5, 0.0, 0.0);
			parts.pop();
		}

		if (Upgrade.has(mask, Upgrade.OBSIDIAN_HORN)) {
			parts.push(-0.18, 0.44, -0.02);
			parts.rotZ(28.0F);
			parts.rotX(-10.0F);
			parts.model("obsidian_horn", 1.0F, 0.5, 0.0, 0.5);
			parts.pop();
		}

		if (Upgrade.has(mask, Upgrade.EYE_SPY)) {
			// Instead of the left eye.
			parts.push(-0.13, 0.24, -0.27);
			parts.item(Items.ENDER_EYE, 0.32F);
			parts.pop();
		}
	}

	/** Helper that keeps track of the pose and submits items. */
	private final class Parts {
		private final PoseStack pose;
		private final SubmitNodeCollector collector;
		private final int light;
		private final AvatarRenderState state;
		private final boolean glint;

		Parts(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, boolean glint) {
			this.pose = pose;
			this.collector = collector;
			this.light = light;
			this.state = state;
			this.glint = glint;
		}

		void begin(ModelPart part) {
			this.pose.pushPose();
			BodyPartsLayer.this.getParentModel().root().translateAndRotate(this.pose);
			part.translateAndRotate(this.pose);
			// Model space is upside down and mirrored: turn it so +Y is up and +X is the player's right.
			this.pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
		}

		void end() {
			this.pose.popPose();
		}

		void push(double x, double y, double z) {
			this.pose.pushPose();
			this.pose.translate(x, y, z);
		}

		void pop() {
			this.pose.popPose();
		}

		void rotX(float degrees) {
			this.pose.mulPose(Axis.XP.rotationDegrees(degrees));
		}

		void rotY(float degrees) {
			this.pose.mulPose(Axis.YP.rotationDegrees(degrees));
		}

		void rotZ(float degrees) {
			this.pose.mulPose(Axis.ZP.rotationDegrees(degrees));
		}

		/** A vanilla item, centered. */
		void item(Item item, float scale) {
			this.submit(new ItemStack(item), scale, 0.5, 0.5, 0.5);
		}

		/** One of the mod's body part models; (ax, ay, az) is the anchor inside the 1×1×1 model box. */
		void model(String name, float scale, double ax, double ay, double az) {
			ItemStack stack = new ItemStack(Items.STICK);
			stack.set(DataComponents.ITEM_MODEL, Upgrades.id(name));
			this.submit(stack, scale, ax, ay, az);
		}

		private void submit(ItemStack stack, float scale, double ax, double ay, double az) {
			if (this.glint) {
				stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			}

			// Submitted parts are drawn later in the frame, so every part gets its own render state.
			ItemStackRenderState render = new ItemStackRenderState();
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.getItemModelResolver().updateForTopItem(render, stack, ItemDisplayContext.NONE, minecraft.level, null, this.state.id);
			this.pose.pushPose();
			this.pose.scale(scale, scale, scale);
			// The item renderer already centres the 1×1×1 model box, move the anchor to the origin instead.
			this.pose.translate(0.5 - ax, 0.5 - ay, 0.5 - az);
			render.submit(this.pose, this.collector, this.light, OverlayTexture.NO_OVERLAY, this.state.outlineColor);
			this.pose.popPose();
		}
	}
}
