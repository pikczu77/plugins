package io.github.pikczu77.upgrades.client;

import java.util.ArrayDeque;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import io.github.pikczu77.upgrades.Upgrades;
import io.github.pikczu77.upgrades.net.Payloads;
import io.github.pikczu77.upgrades.upgrade.Bonus;
import io.github.pikczu77.upgrades.upgrade.Texts;
import io.github.pikczu77.upgrades.upgrade.Unlockable;
import io.github.pikczu77.upgrades.upgrade.Upgrade;

/**
 * The "NOWE ULEPSZENIE" banner that slides down from the top of the screen: icon of the new body part, its name, the
 * first line of the description and the collection counter. Several unlocks at once are shown one after another.
 */
public final class UnlockBanner {
	private static final int WIDTH = 300;
	private static final int MIN_WIDTH = 220;
	/** Room kept free on the right for the vanilla advancement toasts (160 wide). */
	private static final int TOAST_SPACE = 160 + 12;
	private static final int HEIGHT = 48;
	private static final int TOP = 8;
	private static final long SLIDE_IN = 350L;
	private static final long SLIDE_OUT = 450L;

	/** Solid colours: the banner must stay readable over a bright sky. */
	private static final int BACKGROUND = 0xFF14181C;
	private static final int EDGE = 0xFF3C4854;
	private static final int SHADOW = 0xFF07090B;
	private static final int SLOT = 0xFF262D34;
	private static final int KICKER = 0xFF7DFF7D;
	private static final int MUTED = 0xFFAAB4AD;
	private static final int BAR_BACK = 0xFF222A30;
	private static final int BAR_FILL = 0xFF56D364;

	private record Entry(Unlockable upgrade, int count, int total) {
	}

	private static final ArrayDeque<Entry> QUEUE = new ArrayDeque<>();
	private static @Nullable Entry current;
	private static long startedAt;

	private UnlockBanner() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.TITLE_AND_SUBTITLE, Upgrades.id("unlock_banner"),
				(graphics, deltaTracker) -> render(graphics));
	}

	public static void accept(Payloads.UnlockBanner payload) {
		Unlockable upgrade = Texts.byId(payload.upgrade());

		if (upgrade != null) {
			QUEUE.add(new Entry(upgrade, payload.count(), payload.total()));
		}
	}

	public static void clear() {
		QUEUE.clear();
		current = null;
	}

	/** Shorter banners when many unlocks are waiting (for example after /upgrades give @s all). */
	private static long duration() {
		return QUEUE.size() >= 3 ? 1200L : QUEUE.isEmpty() ? 3000L : 2200L;
	}

	private static void render(GuiGraphics graphics) {
		long now = Util.getMillis();

		if (current != null && now - startedAt >= duration()) {
			current = null;
		}

		if (current == null) {
			current = QUEUE.poll();
			startedAt = now;
		}

		if (current == null) {
			return;
		}

		draw(graphics, current, now - startedAt, duration());
	}

	private static void draw(GuiGraphics graphics, Entry entry, long age, long duration) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		Unlockable upgrade = entry.upgrade();
		int color = 0xFF000000 | upgrade.color();
		// Centred when there is room, otherwise pushed left so the advancement toasts on the right stay visible.
		int screen = graphics.guiWidth();
		int width = Math.max(Math.min(MIN_WIDTH, screen - 16), Math.min(WIDTH, screen - TOAST_SPACE - 8));
		int x = Math.max(6, Math.min((screen - width) / 2, screen - TOAST_SPACE - width));

		// Slide down, hold, slide back up.
		float shown;

		if (age < SLIDE_IN) {
			shown = easeOut(age / (float) SLIDE_IN);
		} else if (age > duration - SLIDE_OUT) {
			shown = 1.0F - easeIn((age - (duration - SLIDE_OUT)) / (float) SLIDE_OUT);
		} else {
			shown = 1.0F;
		}

		int y = Math.round(-HEIGHT - 4 + shown * (HEIGHT + 4 + TOP));

		// Body: shadow, edge, solid background.
		graphics.fill(x + 2, y + 2, x + width + 2, y + HEIGHT + 2, SHADOW);
		graphics.fill(x - 1, y - 1, x + width + 1, y + HEIGHT + 1, EDGE);
		graphics.fill(x, y, x + width, y + HEIGHT, BACKGROUND);

		// Icon slot, framed in the upgrade colour; the icon pops in.
		int slot = 38;
		int slotX = x + 5;
		int slotY = y + (HEIGHT - slot) / 2;
		graphics.fill(slotX - 1, slotY - 1, slotX + slot + 1, slotY + slot + 1, color);
		graphics.fill(slotX, slotY, slotX + slot, slotY + slot, SLOT);
		float pop = pop(age);
		graphics.pose().pushMatrix();
		graphics.pose().translate(slotX + slot / 2.0F, slotY + slot / 2.0F);
		graphics.pose().scale(2.0F * pop, 2.0F * pop);
		graphics.renderItem(icon(upgrade), -8, -8);
		graphics.pose().popMatrix();

		// Text column.
		int textX = slotX + slot + 7;
		int right = x + width - 6;
		boolean counting = age > 900L;
		int count = counting ? entry.count() : Math.max(0, entry.count() - 1);
		String counter = count + "/" + entry.total();
		graphics.drawString(font, Component.literal("NOWE ULEPSZENIE"), textX, y + 5, KICKER, false);
		graphics.drawString(font, counter, right - font.width(counter), y + 5, MUTED, false);

		graphics.pose().pushMatrix();
		graphics.pose().translate(textX, y + 15);
		graphics.pose().scale(1.5F, 1.5F);
		graphics.drawString(font, Component.literal(upgrade.displayName()), 0, 0, color, true);
		graphics.pose().popMatrix();

		// The description fits on one line, or goes on two smaller lines.
		Component description = Texts.description(upgrade, upgrade.lines().getFirst());
		int room = right - textX;

		if (font.width(description) <= room) {
			graphics.drawString(font, description, textX, y + 30, MUTED, false);
		} else {
			float small = 0.8F;
			List<FormattedCharSequence> lines = font.split(description, Math.round(room / small));
			graphics.pose().pushMatrix();
			graphics.pose().translate(textX, y + 28);
			graphics.pose().scale(small, small);

			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				graphics.drawString(font, lines.get(i), 0, i * 9, MUTED, false);
			}

			graphics.pose().popMatrix();
		}

		// Collection bar, filling up to the new count.
		int barY = y + HEIGHT - 7;
		float from = Math.max(0, entry.count() - 1) / (float) entry.total();
		float to = entry.count() / (float) entry.total();
		float fill = Mth.lerp(easeOut(Mth.clamp((age - 900L) / 500.0F, 0.0F, 1.0F)), from, to);
		graphics.fill(textX, barY, right, barY + 3, BAR_BACK);
		graphics.fill(textX, barY, textX + Math.round((right - textX) * fill), barY + 3, BAR_FILL);

		// A light sweep across the banner right after it lands.
		float sweep = (age - 380L) / 650.0F;

		if (sweep > 0.0F && sweep < 1.0F) {
			graphics.enableScissor(x, y, x + width, y + HEIGHT);
			int center = x + Math.round(sweep * (width + 60)) - 30;

			for (int i = -12; i <= 12; i++) {
				int alpha = Math.round(70 * (1.0F - Math.abs(i) / 12.0F));
				graphics.fill(center + i, y, center + i + 1, y + HEIGHT, ARGB.color(alpha, 255, 255, 255));
			}

			graphics.disableScissor();
		}
	}

	private static float easeOut(float t) {
		float inverse = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
		return 1.0F - inverse * inverse * inverse;
	}

	private static float easeIn(float t) {
		float clamped = Mth.clamp(t, 0.0F, 1.0F);
		return clamped * clamped * clamped;
	}

	/** 0.4 → 1.15 → 1.0 between 250 and 750 ms. */
	private static float pop(long age) {
		float t = Mth.clamp((age - 250L) / 500.0F, 0.0F, 1.0F);

		if (t < 0.6F) {
			return Mth.lerp(t / 0.6F, 0.4F, 1.15F);
		}

		return Mth.lerp((t - 0.6F) / 0.4F, 1.15F, 1.0F);
	}

	/** The body part of the upgrade: its item, or one of the mod's part models. */
	private static ItemStack icon(Unlockable upgrade) {
		if (upgrade instanceof Bonus bonus) {
			return new ItemStack(bonus.item);
		}

		return switch ((Upgrade) upgrade) {
			case FIST_PICKAXE -> item(Items.WOODEN_PICKAXE);
			case VEIN_MINER -> item(Items.STONE_PICKAXE);
			case GREEN_THUMB -> item(Items.WOODEN_HOE);
			case GOLEM_ARM -> model("golem_arm");
			case SWORD_BOOT -> item(Items.IRON_SWORD);
			case CLONE_1 -> model("mannequin_iron");
			case NAP -> model("back_bed");
			case DEAL_SNIFFER -> model("villager_nose");
			case TRIGGER_FINGER -> item(Items.BOW);
			case MINI_SHIELDS -> model("mini_shield");
			case VEIN_MINER_2 -> item(Items.IRON_PICKAXE);
			case VEIN_MINER_3 -> item(Items.DIAMOND_PICKAXE);
			case CLONE_2 -> model("mannequin_diamond");
			case HOT_HANDS -> item(Items.LAVA_BUCKET);
			case OBSIDIAN_HORN -> model("obsidian_horn");
			case ENCHANTED -> item(Items.ENCHANTED_BOOK);
			case PORTAL_GUN -> model("pocket_portal");
			case BLAZE_POWER -> item(Items.BLAZE_ROD);
			case VEIN_MINER_MAX -> item(Items.NETHERITE_PICKAXE);
			case CLONE_3 -> model("mannequin_netherite");
			case EYE_SPY -> item(Items.ENDER_EYE);
			case DRAGON_WING -> model("dragon_wing");
			case MULTIPLICITY -> item(Items.DRAGON_EGG);
			case FISHING_ROD -> item(Items.FISHING_ROD);
		};
	}

	private static ItemStack item(Item item) {
		return new ItemStack(item);
	}

	private static ItemStack model(String name) {
		ItemStack stack = new ItemStack(Items.STICK);
		stack.set(DataComponents.ITEM_MODEL, Upgrades.id(name));
		return stack;
	}
}
