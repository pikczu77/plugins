package io.github.pikczu77.upgrades.upgrade;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Upgrades for the advancements the video did not use, limited to the ones a normal run gets along the way (no
 * collecting every biome, cat or potion). Each gives a smaller power (a stat, a lasting effect or a special ability)
 * and shows the advancement's item somewhere on the body.
 */
public enum Bonus implements Unlockable {
	// Adventure.
	AVOID_VIBRATION("adventure/avoid_vibration", "Cichy Krok", 0x0E8A8A, Slot.LEGS, Items.SCULK_SENSOR,
			Effect.attr(Attributes.SNEAKING_SPEED, 0.35, false), "{Skradasz się} dużo szybciej"),
	SCULK_KILL("adventure/kill_mob_near_sculk_catalyst", "Rozrost", 0x0E8A8A, Slot.BACK, Items.SCULK_CATALYST,
			Effect.special(Special.XP_BONUS), "Zabite moby dają {dodatkowe doświadczenie}"),
	TRIALS_EDITION("adventure/minecraft_trials_edition", "Podwójny Skok", 0xB7C8F0, Slot.FEET, Items.RABBIT_FOOT,
			Effect.special(Special.DOUBLE_JUMP), "{Skok w powietrzu} — drugi skok"),
	OL_BETSY("adventure/ol_betsy", "Stara Betsy", 0xFFAA00, Slot.ARMS, Items.CROSSBOW,
			Effect.special(Special.MULTISHOT), "Łuk na czole strzela {trzema strzałami} naraz"),

	// The End.
	DRAGON_BREATH("end/dragon_breath", "Smoczy Oddech", 0xFF55FF, Slot.HEAD, Items.DRAGON_BREATH,
			Effect.special(Special.THORNS_WITHER), "Kto cię uderzy, {dostaje Obumarcie}"),
	END_GATEWAY("end/enter_end_gateway", "Ucieczka", 0x2F8F7F, Slot.ORBIT, Items.ENDER_PEARL,
			Effect.special(Special.ESCAPE), "Przy niskim zdrowiu {teleportujesz się} w bezpieczne miejsce"),
	FREE_THE_END("end/kill_dragon", "Pogromca Smoka", 0xAA00AA, Slot.HEAD, Items.DRAGON_HEAD,
			Effect.attr(Attributes.MAX_HEALTH, 4.0, false), "{+2 serca}"),

	// Husbandry.
	AXOLOTL_BUCKET("husbandry/axolotl_in_a_bucket", "Aksolotl", 0xFF9FC8, Slot.BELT, Items.AXOLOTL_BUCKET,
			Effect.potion(MobEffects.WATER_BREATHING), "{Oddychasz pod wodą}"),
	BREED_AN_ANIMAL("husbandry/breed_an_animal", "Hodowca", 0xE0C080, Slot.BELT, Items.WHEAT,
			Effect.attr(Attributes.MOVEMENT_EFFICIENCY, 0.5, false), "{Nie zwalniasz} na piasku dusz i miodzie"),
	FISHY_BUSINESS("husbandry/fishy_business", "Rybka", 0xFF9F7F, Slot.BELT, Items.SALMON,
			Effect.attr(Attributes.LUCK, 1.0, false), "{+1 szczęścia}"),
	BEST_FRIENDS("husbandry/tame_an_animal", "Najlepszy Przyjaciel", 0xFFFFFF, Slot.BELT, Items.NAME_TAG,
			Effect.special(Special.WOLF_BUDDY), "Masz {oswojonego wilka}, który zawsze wraca"),

	// Nether.
	LOCAL_BREWERY("nether/brew_potion", "Lokalny Browar", 0xFFAA00, Slot.BACK, Items.BREWING_STAND,
			Effect.attr(Attributes.ATTACK_DAMAGE, 1.0, false), "{+1 obrażeń} wręcz"),
	OH_SHINY("nether/distract_piglin", "Błyskotki", 0xFFD700, Slot.BELT, Items.GOLD_INGOT,
			Effect.special(Special.PIGLIN_CALM), "{Pigliny cię nie atakują}"),
	THOSE_WERE_THE_DAYS("nether/find_bastion", "Dawne Czasy", 0x4A4148, Slot.CHEST, Items.POLISHED_BLACKSTONE_BRICKS,
			Effect.attr(Attributes.ARMOR, 1.0, false), "{+1 pancerza}"),
	TERRIBLE_FORTRESS("nether/find_fortress", "Straszliwa Forteca", 0xFF7F00, Slot.HEAD, Items.MAGMA_CREAM,
			Effect.potion(MobEffects.FIRE_RESISTANCE), "{Odporność na ogień}"),
	SPOOKY_SKULL("nether/get_wither_skull", "Upiorna Czaszka", 0x4A4A4A, Slot.HEAD, Items.WITHER_SKELETON_SKULL,
			Effect.special(Special.WITHER_HIT), "Ciosy nakładają {Obumarcie}"),
	WAR_PIGS("nether/loot_bastion", "Świnie Wojenne", 0xFFD700, Slot.BELT, Items.GOLD_BLOCK,
			Effect.attr(Attributes.ARMOR_TOUGHNESS, 1.0, false), "{+1 wytrzymałości} pancerza"),
	CRYING_OBSIDIAN("nether/obtain_crying_obsidian", "Drugie Życie", 0xFFD700, Slot.POCKET, Items.TOTEM_OF_UNDYING,
			Effect.special(Special.TOTEM), "Raz na 10 minut {unikasz śmierci}"),
	RETURN_TO_SENDER("nether/return_to_sender", "Zwrot do Nadawcy", 0xFF7F00, Slot.ORBIT, Items.FIRE_CHARGE,
			Effect.special(Special.FIRE_HIT), "Ciosy {podpalają}");

	/** Where on the body the item of the bonus shows up. */
	public enum Slot {
		/** A crown of things around the head. */
		HEAD,
		/** Pinned to the chest. */
		CHEST,
		/** A fan behind the shoulders. */
		BACK,
		/** Hanging around the waist. */
		BELT,
		/** Sticking out of the arms. */
		ARMS,
		/** Around the shins. */
		LEGS,
		/** Slowly circling around the body. */
		ORBIT,
		/** One on each boot. */
		FEET,
		/** In the hip pocket (the second life totem, hidden while it recharges). */
		POCKET
	}

	/** Abilities that need their own code (see BonusAbilities). */
	public enum Special {
		MULTISHOT, XP_BONUS, THORNS_WITHER, ESCAPE, WOLF_BUDDY, PIGLIN_CALM, FIRE_HIT, WITHER_HIT, DOUBLE_JUMP, TOTEM
	}

	/** What the bonus does: an attribute modifier, a lasting potion effect or a special ability. */
	public record Effect(@Nullable Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation,
			@Nullable Holder<MobEffect> potion, @Nullable Special special) {
		static Effect attr(Holder<Attribute> attribute, double amount, boolean percent) {
			return new Effect(attribute, amount, percent ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE,
					null, null);
		}

		static Effect potion(Holder<MobEffect> potion) {
			return new Effect(null, 0.0, AttributeModifier.Operation.ADD_VALUE, potion, null);
		}

		static Effect special(Special special) {
			return new Effect(null, 0.0, AttributeModifier.Operation.ADD_VALUE, null, special);
		}
	}

	public static final List<Bonus> VALUES = List.of(values());

	private final Identifier advancement;
	private final String displayName;
	private final int color;
	private final List<String> lines;
	public final Slot slot;
	public final Item item;
	public final Effect effect;

	Bonus(String advancement, String displayName, int color, Slot slot, Item item, Effect effect, String line) {
		this.advancement = Identifier.withDefaultNamespace(advancement);
		this.displayName = displayName;
		this.color = color;
		this.slot = slot;
		this.item = item;
		this.effect = effect;
		this.lines = List.of(line);
	}

	@Override
	public String id() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public Identifier advancement() {
		return this.advancement;
	}

	@Override
	public String displayName() {
		return this.displayName;
	}

	@Override
	public int color() {
		return this.color;
	}

	@Override
	public List<String> lines() {
		return this.lines;
	}

	public boolean is(Special special) {
		return this.effect.special() == special;
	}

	public static @Nullable Bonus byId(String id) {
		for (Bonus bonus : VALUES) {
			if (bonus.id().equals(id.toLowerCase(Locale.ROOT))) {
				return bonus;
			}
		}

		return null;
	}

	public static @Nullable Bonus byAdvancement(Identifier advancement) {
		for (Bonus bonus : VALUES) {
			if (bonus.advancement.equals(advancement)) {
				return bonus;
			}
		}

		return null;
	}

	/** Bonus bits live in two longs (there are more than 64 bonuses). */
	public static boolean has(long[] bits, Bonus bonus) {
		return (bits[bonus.ordinal() >> 6] & (1L << (bonus.ordinal() & 63))) != 0L;
	}

	public static void set(long[] bits, Bonus bonus) {
		bits[bonus.ordinal() >> 6] |= 1L << (bonus.ordinal() & 63);
	}

	public static int count(long[] bits) {
		return Long.bitCount(bits[0]) + Long.bitCount(bits[1]);
	}

	/** Whether any unlocked bonus has the special ability. */
	public static boolean hasSpecial(long[] bits, Special special) {
		for (Bonus bonus : VALUES) {
			if (bonus.is(special) && has(bits, bonus)) {
				return true;
			}
		}

		return false;
	}

	public static long[] empty() {
		return new long[2];
	}
}
