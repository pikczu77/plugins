package io.github.pikczu77.upgrades.upgrade;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;

/**
 * Every body upgrade from the video, in the order they were unlocked. Each one is tied to a vanilla advancement:
 * completing the advancement grows the body part and unlocks the power. The set a player has is always derived from
 * their advancement progress, so nothing extra is stored in the world.
 *
 * <p>Description lines use {@code {braces}} for the highlighted part (drawn in the accent colour).
 */
public enum Upgrade {
	FIST_PICKAXE("story/mine_stone", "Pięści-Kilof", 0xC8A165, Group.NONE,
			"Twoje {pięści kopią jak kilof}! (drewniany)"),
	VEIN_MINER("story/upgrade_tools", "Vein Miner", 0x9E9E9E, Group.NONE,
			"{Vein Miner}: kopiesz od razu całą grupę takich samych bloków",
			"Pięści kopią teraz jak {kamienny kilof}"),
	GREEN_THUMB("husbandry/plant_seed", "Zielona Rączka", 0x55FF55, Group.CLICK,
			"{PPM pustą ręką} na ziemię — zaorujesz ją",
			"Uprawy wokół ciebie {rosną w mgnieniu oka}"),
	GOLEM_ARM("story/smelt_iron", "Ramię Golema", 0xD8D8D8, Group.NONE,
			"{Ciosy wręcz} zadają dodatkowe obrażenia i {podrzucają moby}"),
	SWORD_BOOT("adventure/kill_a_mob", "Miecz w Bucie", 0xFF5555, Group.NONE,
			"{Ataki wręcz} ranią wszystko dookoła (sweeping)",
			"{Sprint} w moby rani je i odrzuca"),
	CLONE_1("story/obtain_armor", "Klon Bojowy Lv. 1", 0xFFFF55, Group.NONE,
			"Gdy {potwór cię zrani}, pojawia się słaby klon, który odciąga wrogów"),
	NAP("adventure/sleep_in_bed", "Łóżko na Plecach", 0xFF6B6B, Group.SNEAK_CLICK,
			"{Kucnij + PPM pustą ręką} — ucinasz sobie drzemkę",
			"Przespana do końca drzemka {leczy do pełna}",
			"Łóżko jest {duże} — potrzebujesz trochę miejsca"),
	DEAL_SNIFFER("adventure/trade", "Nos Handlarza", 0x55FF55, Group.SNEAK_CLICK,
			"{Kucnij + PPM pustą ręką} — wywąchujesz pobliski handel",
			"Handlujesz {z mobami i z blokami}! Oferty są na krótko"),
	TRIGGER_FINGER("adventure/shoot_arrow", "Spust", 0xFFAA00, Group.CLICK,
			"{PPM pustą ręką} — łuk na czole strzela strzałą"),
	MINI_SHIELDS("story/deflect_arrow", "Mini Tarcze", 0xFFAA00, Group.NONE,
			"{Pociski} są blokowane automatycznie",
			"...i {odbijane} prosto w strzelca!"),
	VEIN_MINER_2("story/iron_tools", "Vein Miner+", 0xE0E0E0, Group.NONE,
			"{Większy zasięg} Vein Minera",
			"Pięści kopią teraz jak {żelazny kilof}"),
	VEIN_MINER_3("story/mine_diamond", "Vein Miner++", 0x55FFFF, Group.NONE,
			"{Dużo większy zasięg} Vein Minera",
			"Pięści kopią teraz jak {diamentowy kilof}"),
	CLONE_2("story/shiny_gear", "Klon Bojowy Lv. 2", 0x55FFFF, Group.NONE,
			"Gdy {potwór cię zrani}, pojawia się klon, który {walczy za ciebie}",
			"Klony nie mają zbroi i mają mało życia",
			"Po śmierci klon {dzieli się na dwa mniejsze}"),
	HOT_HANDS("story/lava_bucket", "Gorące Łapy", 0xFF7F00, Group.NONE,
			"{Ciosy wręcz} stawiają lawę pod nogami ofiary",
			"Lawa {znika po chwili}"),
	OBSIDIAN_HORN("story/form_obsidian", "Obsydianowy Róg", 0xAA55FF, Group.NONE,
			"{PPM obsydianem} — rzucasz nim z ogromną siłą",
			"Przy uderzeniu {wybucha i zalewa wszystko obsydianem}",
			"{Kucnij + PPM}, żeby postawić obsydian normalnie"),
	ENCHANTED("story/enchant_item", "Zaklęte Ciało", 0xFF55FF, Group.NONE,
			"Dropy z {mobów i bloków} są potężnie zaklęte (wielokrotne)"),
	PORTAL_GUN("story/enter_the_nether", "Portal w Kieszeni", 0x5555FF, Group.CLICK,
			"{PPM pustą ręką} — stawiasz portal tam, gdzie patrzysz",
			"Możesz mieć {2 portale} naraz",
			"Wejście w jeden {przenosi do drugiego}"),
	BLAZE_POWER("nether/obtain_blaze_rod", "Moc Blaze'a", 0xFFAA00, Group.NONE,
			"{PPM różdżką Blaze'a} — salwa kul ognia"),
	VEIN_MINER_MAX("nether/obtain_ancient_debris", "Vein Miner MAX", 0xAA0000, Group.NONE,
			"Zasięg Vein Minera: {EKSTREMALNY}",
			"Pięści kopią teraz jak {netherytowy kilof}"),
	CLONE_3("nether/netherite_armor", "Klon Bojowy Lv. 3", 0x8B6A8F, Group.NONE,
			"Gdy {potwór cię zrani}, pojawia się {potężny klon}",
			"Klony są {NIEŚMIERTELNE} i mają netherytowy miecz"),
	EYE_SPY("story/follow_ender_eye", "Oko Kresu", 0x00AA88, Group.NONE,
			"{Przytrzymaj kucanie} — ładujesz teleport",
			"Teleport jest losowy, z szansą na {portal do Kresu}"),
	DRAGON_WING("story/enter_the_end", "Skrzydła Smoka", 0xAA00AA, Group.CLICK,
			"{PPM pustą ręką} — samonaprowadzające kule ognia",
			"Kule {niszczą kryształy Kresu}",
			"Możesz {latać}!"),
	MULTIPLICITY("end/dragon_egg", "Następne Pokolenie", 0xFF55FF, Group.NONE,
			"Nowe pokolenie jest {w twoich rękach}...",
			"Dbaj o nie. {Bardzo} o nie dbaj.");

	/** Which input fires the power. Powers sharing an input can be cycled with a key (see {@code /upgrades powers}). */
	public enum Group {
		NONE,
		/** Right-click with an empty hand. */
		CLICK,
		/** Sneak + right-click with an empty hand. */
		SNEAK_CLICK
	}

	public static final List<Upgrade> VALUES = List.of(values());

	public final Identifier advancement;
	public final String displayName;
	public final int color;
	public final Group group;
	public final List<String> lines;

	Upgrade(String advancement, String displayName, int color, Group group, String... lines) {
		this.advancement = Identifier.withDefaultNamespace(advancement);
		this.displayName = displayName;
		this.color = color;
		this.group = group;
		this.lines = List.of(lines);
	}

	public String id() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public long bit() {
		return 1L << this.ordinal();
	}

	public static @Nullable Upgrade byId(String id) {
		for (Upgrade upgrade : VALUES) {
			if (upgrade.id().equals(id.toLowerCase(Locale.ROOT))) {
				return upgrade;
			}
		}

		return null;
	}

	public static @Nullable Upgrade byAdvancement(Identifier advancement) {
		for (Upgrade upgrade : VALUES) {
			if (upgrade.advancement.equals(advancement)) {
				return upgrade;
			}
		}

		return null;
	}

	public static long mask(Set<Upgrade> upgrades) {
		long mask = 0L;

		for (Upgrade upgrade : upgrades) {
			mask |= upgrade.bit();
		}

		return mask;
	}

	public static EnumSet<Upgrade> fromMask(long mask) {
		EnumSet<Upgrade> set = EnumSet.noneOf(Upgrade.class);

		for (Upgrade upgrade : VALUES) {
			if ((mask & upgrade.bit()) != 0L) {
				set.add(upgrade);
			}
		}

		return set;
	}

	public static boolean has(long mask, Upgrade upgrade) {
		return (mask & upgrade.bit()) != 0L;
	}

	/** Pickaxe tier the fists mine with: 0 none, 1 wood, 2 stone, 3 iron, 4 diamond, 5 netherite. */
	public static int fistTier(long mask) {
		if (!has(mask, FIST_PICKAXE)) {
			return 0;
		}

		if (has(mask, VEIN_MINER_MAX)) {
			return 5;
		}

		if (has(mask, VEIN_MINER_3)) {
			return 4;
		}

		if (has(mask, VEIN_MINER_2)) {
			return 3;
		}

		return has(mask, VEIN_MINER) ? 2 : 1;
	}

	/** 0 = no vein miner, 1-4 = the four vein miner levels from the video. */
	public static int veinLevel(long mask) {
		if (!has(mask, VEIN_MINER)) {
			return 0;
		}

		if (has(mask, VEIN_MINER_MAX)) {
			return 4;
		}

		if (has(mask, VEIN_MINER_3)) {
			return 3;
		}

		return has(mask, VEIN_MINER_2) ? 2 : 1;
	}

	/** 0 = no clones, otherwise the combat clone level (1-3). */
	public static int cloneLevel(long mask) {
		if (has(mask, CLONE_3)) {
			return 3;
		}

		if (has(mask, CLONE_2)) {
			return 2;
		}

		return has(mask, CLONE_1) ? 1 : 0;
	}
}
