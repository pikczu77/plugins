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
 * Upgrades for every advancement the video did not use ("upgrade yourself infinitely"). Each one gives a smaller power
 * (a stat, a lasting effect or a special ability) and shows the advancement's item somewhere on the body.
 */
public enum Bonus implements Unlockable {
	// Adventure.
	ADVENTURING_TIME("adventure/adventuring_time", "Obieżyświat", 0x55FFFF, Slot.LEGS, Items.DIAMOND_BOOTS,
			Effect.attr(Attributes.MOVEMENT_SPEED, 0.15, true), "{+15% szybkości} chodzenia"),
	ARBALISTIC("adventure/arbalistic", "Arbalista", 0xFFAA00, Slot.BACK, Items.TIPPED_ARROW,
			Effect.special(Special.ARROW_POWER), "Twoje strzały zadają {+50% obrażeń}"),
	AVOID_VIBRATION("adventure/avoid_vibration", "Cichy Krok", 0x0E8A8A, Slot.LEGS, Items.SCULK_SENSOR,
			Effect.attr(Attributes.SNEAKING_SPEED, 0.35, false), "{Skradasz się} dużo szybciej"),
	BLOWBACK("adventure/blowback", "Podmuch", 0xB7C8F0, Slot.ORBIT, Items.WIND_CHARGE,
			Effect.attr(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0, false), "Wybuchy {cię nie odrzucają}"),
	BRUSH_ARMADILLO("adventure/brush_armadillo", "Łuska Pancernika", 0xC98C6B, Slot.CHEST, Items.ARMADILLO_SCUTE,
			Effect.attr(Attributes.ARMOR, 2.0, false), "{+2 pancerza}"),
	BULLSEYE("adventure/bullseye", "W Dziesiątkę", 0xFF5555, Slot.HEAD, Items.TARGET,
			Effect.special(Special.ARROW_STRAIGHT), "Twoje strzały {lecą prosto} (bez opadania)"),
	DECORATED_POT("adventure/craft_decorated_pot_using_only_sherds", "Garncarz", 0xC87850, Slot.BELT, Items.DECORATED_POT,
			Effect.attr(Attributes.LUCK, 1.0, false), "{+1 szczęścia} (lepsze łupy)"),
	CRAFTERS("adventure/crafters_crafting_crafters", "Automat", 0x9E9E9E, Slot.BACK, Items.CRAFTER,
			Effect.attr(Attributes.BLOCK_BREAK_SPEED, 0.1, true), "{+10% szybkości kopania}"),
	WORLD_HEIGHT_FALL("adventure/fall_from_world_height", "Twarde Lądowanie", 0x5599FF, Slot.LEGS, Items.WATER_BUCKET,
			Effect.attr(Attributes.SAFE_FALL_DISTANCE, 6.0, false), "Spadasz {6 bloków więcej} bez obrażeń"),
	HEART_TRANSPLANTER("adventure/heart_transplanter", "Drugie Serce", 0xFF7F3F, Slot.CHEST, Items.CREAKING_HEART,
			Effect.attr(Attributes.MAX_HEALTH, 4.0, false), "{+2 serca}"),
	HERO_OF_THE_VILLAGE("adventure/hero_of_the_village", "Bohater Wioski", 0x55FF55, Slot.BELT, Items.EMERALD_BLOCK,
			Effect.potion(MobEffects.HERO_OF_THE_VILLAGE), "Stały efekt {Bohatera Wioski} (tańszy handel)"),
	HONEY_SLIDE("adventure/honey_block_slide", "Lepkie Stopy", 0xFFB82E, Slot.LEGS, Items.HONEY_BLOCK,
			Effect.attr(Attributes.STEP_HEIGHT, 0.6, false), "{Wchodzisz na bloki} bez skakania"),
	KILL_ALL_MOBS("adventure/kill_all_mobs", "Łowca Wszystkiego", 0x55FFFF, Slot.BACK, Items.DIAMOND_SWORD,
			Effect.attr(Attributes.ATTACK_DAMAGE, 0.2, true), "{+20% obrażeń} wręcz"),
	SCULK_KILL("adventure/kill_mob_near_sculk_catalyst", "Rozrost", 0x0E8A8A, Slot.BACK, Items.SCULK_CATALYST,
			Effect.special(Special.XP_BONUS), "Zabite moby dają {dodatkowe doświadczenie}"),
	LIGHTEN_UP("adventure/lighten_up", "Świetliste Oczy", 0xE8A060, Slot.HEAD, Items.COPPER_BULB,
			Effect.potion(MobEffects.NIGHT_VISION), "Stałe {widzenie w ciemności}"),
	SURGE_PROTECTOR("adventure/lightning_rod_with_villager_no_fire", "Piorunochron", 0xE8A060, Slot.HEAD, Items.LIGHTNING_ROD,
			Effect.special(Special.LIGHTNING_IMMUNE), "{Pioruny cię nie ranią}"),
	TRIALS_EDITION("adventure/minecraft_trials_edition", "Próba Wiatru", 0xB7C8F0, Slot.LEGS, Items.CHISELED_TUFF,
			Effect.attr(Attributes.JUMP_STRENGTH, 0.1, false), "{Skaczesz wyżej}"),
	OL_BETSY("adventure/ol_betsy", "Stara Betsy", 0xFFAA00, Slot.ARMS, Items.CROSSBOW,
			Effect.special(Special.MULTISHOT), "Łuk na czole strzela {trzema strzałami} naraz"),
	OVEROVERKILL("adventure/overoverkill", "Miażdżący Cios", 0xAAAAAA, Slot.BACK, Items.MACE,
			Effect.special(Special.SMASH), "Cios {w locie} zadaje obrażenia jak buzdygan"),
	SOUND_OF_MUSIC("adventure/play_jukebox_in_meadows", "Dźwięk Muzyki", 0xFF55FF, Slot.BACK, Items.JUKEBOX,
			Effect.special(Special.REGEN_SLOW), "Powoli {odzyskujesz zdrowie} (przy muzyce)"),
	BOOKSHELF_POWER("adventure/read_power_of_chiseled_bookshelf", "Mól Książkowy", 0xC87850, Slot.BACK, Items.CHISELED_BOOKSHELF,
			Effect.attr(Attributes.BLOCK_INTERACTION_RANGE, 1.0, false), "{+1 zasięgu} do bloków"),
	REVAULTING("adventure/revaulting", "Skarbiec", 0x3F9F8F, Slot.BELT, Items.OMINOUS_TRIAL_KEY,
			Effect.attr(Attributes.LUCK, 2.0, false), "{+2 szczęścia}"),
	SALVAGE_SHERD("adventure/salvage_sherd", "Archeolog", 0xC87850, Slot.ARMS, Items.BRUSH,
			Effect.attr(Attributes.SUBMERGED_MINING_SPEED, 1.0, false), "Kopiesz {pod wodą} tak szybko jak na lądzie"),
	SNIPER_DUEL("adventure/sniper_duel", "Snajper", 0xFFFFFF, Slot.HEAD, Items.ARROW,
			Effect.special(Special.ARROW_SPEED), "Twoje strzały lecą {1,5× szybciej}"),
	SPEAR_MANY_MOBS("adventure/spear_many_mobs", "Włócznik", 0xD8D8D8, Slot.BACK, Items.IRON_SPEAR,
			Effect.attr(Attributes.ENTITY_INTERACTION_RANGE, 1.0, false), "{+1 zasięgu} ataku"),
	SPYGLASS_DRAGON("adventure/spyglass_at_dragon", "Sokole Oko", 0xAA00AA, Slot.HEAD, Items.SPYGLASS,
			Effect.special(Special.SPOT_MOBS), "Potwory w pobliżu {świecą}"),
	SPYGLASS_GHAST("adventure/spyglass_at_ghast", "Czy To Balon?", 0xF0F0F0, Slot.ORBIT, Items.STRING,
			Effect.attr(Attributes.KNOCKBACK_RESISTANCE, 0.3, false), "{+30% odporności} na odrzut"),
	SPYGLASS_PARROT("adventure/spyglass_at_parrot", "Czy To Ptak?", 0x55FF55, Slot.HEAD, Items.FEATHER,
			Effect.attr(Attributes.SAFE_FALL_DISTANCE, 4.0, false), "Spadasz {4 bloki więcej} bez obrażeń"),
	HIRED_HELP("adventure/summon_iron_golem", "Najemnik", 0xE08A2E, Slot.BELT, Items.CARVED_PUMPKIN,
			Effect.attr(Attributes.ATTACK_KNOCKBACK, 1.0, false), "Ciosy {mocniej odrzucają}"),
	THROW_TRIDENT("adventure/throw_trident", "Trójząb", 0x3FAFAF, Slot.BACK, Items.TRIDENT,
			Effect.attr(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.5, false), "{Szybciej pływasz}"),
	POSTMORTAL("adventure/totem_of_undying", "Drugie Życie", 0xFFD700, Slot.CHEST, Items.TOTEM_OF_UNDYING,
			Effect.special(Special.TOTEM), "Raz na 10 minut {unikasz śmierci}"),
	STAR_TRADER("adventure/trade_at_world_height", "Gwiezdny Handlarz", 0x55FF55, Slot.HEAD, Items.EMERALD,
			Effect.attr(Attributes.MAX_HEALTH, 2.0, false), "{+1 serce}"),
	SMITHING_STYLE("adventure/trim_with_all_exclusive_armor_patterns", "Styl Kowala", 0x7F7FFF, Slot.CHEST,
			Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, Effect.attr(Attributes.ARMOR_TOUGHNESS, 2.0, false), "{+2 wytrzymałości} pancerza"),
	NEW_LOOK("adventure/trim_with_any_armor_pattern", "Nowy Wygląd", 0xE0C080, Slot.CHEST, Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
			Effect.attr(Attributes.ARMOR, 1.0, false), "{+1 pancerza}"),
	TWO_BIRDS("adventure/two_birds_one_arrow", "Dwa Ptaki", 0xFFFF55, Slot.HEAD, Items.SPECTRAL_ARROW,
			Effect.special(Special.ARROW_PIERCE), "Twoje strzały {przebijają} moby"),
	UNDER_LOCK("adventure/under_lock_and_key", "Pod Kluczem", 0xE8A060, Slot.BELT, Items.TRIAL_KEY,
			Effect.attr(Attributes.BLOCK_BREAK_SPEED, 0.1, true), "{+10% szybkości kopania}"),
	LODESTONE("adventure/use_lodestone", "Magnes", 0xAAAAAA, Slot.CHEST, Items.LODESTONE,
			Effect.special(Special.MAGNET), "{Przyciągasz} przedmioty i doświadczenie"),
	VERY_FRIGHTENING("adventure/very_very_frightening", "Piorunujący Cios", 0xFFFF55, Slot.ORBIT, Items.GLOWSTONE_DUST,
			Effect.special(Special.LIGHTNING_HIT), "Ciosy czasem {przywołują piorun} (ciebie nie rani)"),
	VOLUNTARY_EXILE("adventure/voluntary_exile", "Wygnaniec", 0x7F7F7F, Slot.BELT, Items.OMINOUS_BOTTLE,
			Effect.attr(Attributes.ATTACK_DAMAGE, 1.0, false), "{+1 obrażeń} wręcz"),
	LIGHT_AS_RABBIT("adventure/walk_on_powder_snow_with_leather_boots", "Lekki Jak Królik", 0xC87850, Slot.LEGS, Items.LEATHER_BOOTS,
			Effect.attr(Attributes.JUMP_STRENGTH, 0.08, false), "{Skaczesz wyżej}"),
	WHO_NEEDS_ROCKETS("adventure/who_needs_rockets", "Podwójny Skok", 0xFF5555, Slot.LEGS, Items.FIREWORK_ROCKET,
			Effect.special(Special.DOUBLE_JUMP), "{Skok w powietrzu} (drugi skok)"),
	WHOS_THE_PILLAGER("adventure/whos_the_pillager_now", "Kto Tu Łupi?", 0x7F7F7F, Slot.ARMS, Items.IRON_AXE,
			Effect.attr(Attributes.ATTACK_SPEED, 0.15, true), "{+15% szybkości} ataku"),

	// The End.
	DRAGON_BREATH("end/dragon_breath", "Smoczy Oddech", 0xFF55FF, Slot.HEAD, Items.DRAGON_BREATH,
			Effect.special(Special.THORNS_WITHER), "Kto cię uderzy, {dostaje Obumarcie}"),
	ELYTRA("end/elytra", "Niebo To Granica", 0x9F9FBF, Slot.BACK, Items.ELYTRA,
			Effect.special(Special.NO_FALL), "{Brak obrażeń od upadku}"),
	END_GATEWAY("end/enter_end_gateway", "Ucieczka", 0x2F8F7F, Slot.ORBIT, Items.ENDER_PEARL,
			Effect.special(Special.ESCAPE), "Przy niskim zdrowiu {teleportujesz się} w bezpieczne miejsce"),
	END_CITY("end/find_end_city", "Miasto Kresu", 0xAA77AA, Slot.BACK, Items.PURPUR_BLOCK,
			Effect.attr(Attributes.ARMOR, 2.0, false), "{+2 pancerza}"),
	FREE_THE_END("end/kill_dragon", "Pogromca Smoka", 0xAA00AA, Slot.HEAD, Items.DRAGON_HEAD,
			Effect.attr(Attributes.MAX_HEALTH, 4.0, false), "{+2 serca}"),
	LEVITATE("end/levitate", "Lewitacja", 0xAA77AA, Slot.ORBIT, Items.SHULKER_SHELL,
			Effect.special(Special.SLOW_FALL_SNEAK), "{Kucnij w powietrzu} — powoli opadasz"),
	RESPAWN_DRAGON("end/respawn_dragon", "Znowu Koniec", 0xFF55FF, Slot.ORBIT, Items.END_CRYSTAL,
			Effect.attr(Attributes.ATTACK_DAMAGE, 2.0, false), "{+2 obrażeń} wręcz"),

	// Husbandry.
	BIRTHDAY_SONG("husbandry/allay_deliver_cake_to_note_block", "Urodziny", 0xFFFFFF, Slot.BELT, Items.CAKE,
			Effect.special(Special.SATIATED), "{Wolniej głodniejesz}"),
	ALLAY_FRIEND("husbandry/allay_deliver_item_to_player", "Przyjaciel Allaya", 0x55FFFF, Slot.ORBIT, Items.COOKIE,
			Effect.attr(Attributes.MOVEMENT_SPEED, 0.1, true), "{+10% szybkości} chodzenia"),
	AXOLOTL_BUCKET("husbandry/axolotl_in_a_bucket", "Aksolotl", 0xFF9FC8, Slot.BELT, Items.AXOLOTL_BUCKET,
			Effect.potion(MobEffects.WATER_BREATHING), "{Oddychasz pod wodą}"),
	BALANCED_DIET("husbandry/balanced_diet", "Zbilansowana Dieta", 0xFF5555, Slot.HEAD, Items.APPLE,
			Effect.special(Special.NO_HUNGER), "{Nie odczuwasz głodu}"),
	TWO_BY_TWO("husbandry/bred_all_animals", "Dwoje z Każdego", 0xFFD700, Slot.BELT, Items.GOLDEN_CARROT,
			Effect.special(Special.TWINS), "Zwierzęta, które rozmnażasz, mają {bliźniaki}"),
	BREED_AN_ANIMAL("husbandry/breed_an_animal", "Hodowca", 0xE0C080, Slot.BELT, Items.WHEAT,
			Effect.attr(Attributes.MOVEMENT_EFFICIENCY, 0.5, false), "{Nie zwalniasz} na piasku dusz i miodzie"),
	COMPLETE_CATALOGUE("husbandry/complete_catalogue", "Kocia Ekipa", 0xE0C080, Slot.BELT, Items.COD,
			Effect.special(Special.CREEPER_CALM), "{Creepery nie wybuchają} przy tobie"),
	LITTLE_SNIFFS("husbandry/feed_snifflet", "Mały Węszyciel", 0xC87850, Slot.HEAD, Items.TORCHFLOWER_SEEDS,
			Effect.special(Special.ORE_SENSE), "{Czujesz rudy} w pobliżu (świecą przez ściany)"),
	FISHY_BUSINESS("husbandry/fishy_business", "Rybka", 0xFF9F7F, Slot.BELT, Items.SALMON,
			Effect.attr(Attributes.LUCK, 1.0, false), "{+1 szczęścia}"),
	FROGLIGHTS("husbandry/froglights", "Żabie Moce", 0x9FFF9F, Slot.BELT, Items.VERDANT_FROGLIGHT,
			Effect.attr(Attributes.MAX_HEALTH, 2.0, false), "{+1 serce}"),
	HEALING_FRIENDSHIP("husbandry/kill_axolotl_target", "Moc Przyjaźni", 0xFF9FC8, Slot.BELT, Items.TROPICAL_FISH_BUCKET,
			Effect.special(Special.WATER_REGEN), "{Leczysz się} w wodzie"),
	FROG_SQUAD("husbandry/leash_all_frog_variants", "Żabia Ekipa", 0x9FFF9F, Slot.ARMS, Items.LEAD,
			Effect.attr(Attributes.ATTACK_SPEED, 0.1, true), "{+10% szybkości} ataku"),
	GLOW_AND_BEHOLD("husbandry/make_a_sign_glow", "Świecący Tusz", 0x3FFFBF, Slot.HEAD, Items.GLOW_INK_SAC,
			Effect.attr(Attributes.OXYGEN_BONUS, 3.0, false), "{Dłużej wytrzymujesz} pod wodą"),
	SERIOUS_DEDICATION("husbandry/obtain_netherite_hoe", "Poważne Oddanie", 0x55FF55, Slot.BACK, Items.NETHERITE_HOE,
			Effect.special(Special.GREEN_PLUS), "Zielona Rączka orze {5×5} i działa dalej"),
	SNIFFER_EGG("husbandry/obtain_sniffer_egg", "Ciekawy Zapach", 0xC0503F, Slot.BELT, Items.SNIFFER_EGG,
			Effect.attr(Attributes.LUCK, 1.0, false), "{+1 szczęścia}"),
	STAY_HYDRATED("husbandry/place_dried_ghast_in_water", "Nawodnij Się", 0x9FBFFF, Slot.BACK, Items.DRIED_GHAST,
			Effect.potion(MobEffects.DOLPHINS_GRACE), "Stała {Gracja Delfina}"),
	PLANTING_THE_PAST("husbandry/plant_any_sniffer_seed", "Sadząc Przeszłość", 0x3FBFBF, Slot.HEAD, Items.PITCHER_POD,
			Effect.attr(Attributes.MAX_HEALTH, 2.0, false), "{+1 serce}"),
	SHEAR_BRILLIANCE("husbandry/remove_wolf_armor", "Nożyce", 0xD8D8D8, Slot.ARMS, Items.SHEARS,
			Effect.attr(Attributes.BLOCK_BREAK_SPEED, 0.1, true), "{+10% szybkości kopania}"),
	GOOD_AS_NEW("husbandry/repair_wolf_armor", "Jak Nowy", 0xC98C6B, Slot.CHEST, Items.WOLF_ARMOR,
			Effect.special(Special.AUTO_REPAIR), "Narzędzia i zbroja {same się naprawiają}"),
	GOAT_BOAT("husbandry/ride_a_boat_with_a_goat", "Kozia Łódka", 0xC87850, Slot.BACK, Items.OAK_BOAT,
			Effect.attr(Attributes.SAFE_FALL_DISTANCE, 3.0, false), "Spadasz {3 bloki więcej} bez obrażeń"),
	BEE_OUR_GUEST("husbandry/safely_harvest_honey", "Żądło", 0xFFB82E, Slot.BELT, Items.HONEY_BOTTLE,
			Effect.special(Special.STING), "Ciosy {zatruwają}"),
	TOTAL_BEELOCATION("husbandry/silk_touch_nest", "Delikatne Dłonie", 0xFFB82E, Slot.BACK, Items.BEE_NEST,
			Effect.special(Special.SILK_FIST), "{Kucnij i kop pustą ręką} — jedwabny dotyk"),
	BUKKIT("husbandry/tadpole_in_a_bucket", "Kijanka", 0x9F7F5F, Slot.BELT, Items.TADPOLE_BUCKET,
			Effect.attr(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.3, false), "{Szybciej pływasz}"),
	BEST_FRIENDS("husbandry/tame_an_animal", "Najlepszy Przyjaciel", 0xFFFFFF, Slot.BELT, Items.NAME_TAG,
			Effect.special(Special.WOLF_BUDDY), "Masz {oswojonego wilka}, który zawsze wraca"),
	WAX_OFF("husbandry/wax_off", "Zdrapywacz", 0x9E9E9E, Slot.ARMS, Items.STONE_AXE,
			Effect.special(Special.FIST_AXE), "Pięści {rąbią jak siekiera}"),
	WAX_ON("husbandry/wax_on", "Woskowanie", 0xFFB82E, Slot.CHEST, Items.HONEYCOMB,
			Effect.attr(Attributes.KNOCKBACK_RESISTANCE, 0.2, false), "{+20% odporności} na odrzut"),
	WHOLE_PACK("husbandry/whole_pack", "Cała Wataha", 0xFFFFFF, Slot.BELT, Items.BONE,
			Effect.special(Special.WOLF_PACK), "Masz {3 wilki} zamiast jednego"),

	// Nether.
	ALL_EFFECTS("nether/all_effects", "Jak Tu Trafiliśmy?", 0xFFFFFF, Slot.BELT, Items.MILK_BUCKET,
			Effect.special(Special.CLEANSE), "{Złe efekty} od razu znikają"),
	FURIOUS_COCKTAIL("nether/all_potions", "Wściekły Koktajl", 0xFF55FF, Slot.BELT, Items.POTION,
			Effect.potion(MobEffects.HASTE), "Stały {Pośpiech}"),
	LOCAL_BREWERY("nether/brew_potion", "Lokalny Browar", 0xFFAA00, Slot.BACK, Items.BREWING_STAND,
			Effect.attr(Attributes.ATTACK_DAMAGE, 1.0, false), "{+1 obrażeń} wręcz"),
	RESPAWN_ANCHOR("nether/charge_respawn_anchor", "Prawie Dziewięć Żyć", 0xAA55FF, Slot.BACK, Items.RESPAWN_ANCHOR,
			Effect.attr(Attributes.MAX_HEALTH, 2.0, false), "{+1 serce}"),
	BRING_THE_BEACON("nether/create_beacon", "Latarnia", 0x55FFFF, Slot.HEAD, Items.BEACON,
			Effect.special(Special.BEACON_AURA), "Ty, twoje klony i wilki macie {Regenerację}"),
	BEACONATOR("nether/create_full_beacon", "Latarnik", 0x55FFFF, Slot.BACK, Items.DIAMOND_BLOCK,
			Effect.special(Special.BEACON_FULL), "Aura latarni daje też {Siłę}"),
	OH_SHINY("nether/distract_piglin", "Błyskotki", 0xFFD700, Slot.BELT, Items.GOLD_INGOT,
			Effect.special(Special.PIGLIN_CALM), "{Pigliny cię nie atakują}"),
	HOT_TOURIST("nether/explore_nether", "Gorąca Turystyka", 0x4A4148, Slot.LEGS, Items.NETHERITE_BOOTS,
			Effect.attr(Attributes.MOVEMENT_SPEED, 0.1, true), "{+10% szybkości} chodzenia"),
	SUBSPACE_BUBBLE("nether/fast_travel", "Podprzestrzenny Bąbel", 0xAA55FF, Slot.ORBIT, Items.MAP,
			Effect.attr(Attributes.MOVEMENT_SPEED, 0.1, true), "{+10% szybkości} chodzenia"),
	THOSE_WERE_THE_DAYS("nether/find_bastion", "Dawne Czasy", 0x4A4148, Slot.CHEST, Items.POLISHED_BLACKSTONE_BRICKS,
			Effect.attr(Attributes.ARMOR, 1.0, false), "{+1 pancerza}"),
	TERRIBLE_FORTRESS("nether/find_fortress", "Straszliwa Forteca", 0xFF7F00, Slot.HEAD, Items.MAGMA_CREAM,
			Effect.potion(MobEffects.FIRE_RESISTANCE), "{Odporność na ogień}"),
	SPOOKY_SKULL("nether/get_wither_skull", "Upiorna Czaszka", 0x4A4A4A, Slot.HEAD, Items.WITHER_SKELETON_SKULL,
			Effect.special(Special.WITHER_HIT), "Ciosy nakładają {Obumarcie}"),
	WAR_PIGS("nether/loot_bastion", "Świnie Wojenne", 0xFFD700, Slot.BELT, Items.GOLD_BLOCK,
			Effect.attr(Attributes.ARMOR_TOUGHNESS, 1.0, false), "{+1 wytrzymałości} pancerza"),
	CRYING_OBSIDIAN("nether/obtain_crying_obsidian", "Kto Kroi Cebulę?", 0xAA55FF, Slot.BACK, Items.CRYING_OBSIDIAN,
			Effect.attr(Attributes.MAX_HEALTH, 2.0, false), "{+1 serce}"),
	RETURN_TO_SENDER("nether/return_to_sender", "Zwrot do Nadawcy", 0xFF7F00, Slot.ORBIT, Items.FIRE_CHARGE,
			Effect.special(Special.FIRE_HIT), "Ciosy {podpalają}"),
	RIDE_STRIDER("nether/ride_strider", "Łódź z Nogami", 0x3FAF8F, Slot.ARMS, Items.WARPED_FUNGUS_ON_A_STICK,
			Effect.attr(Attributes.BURNING_TIME, -0.5, true), "{Krócej się palisz}"),
	FEELS_LIKE_HOME("nether/ride_strider_in_overworld_lava", "Jak w Domu", 0x3FAF8F, Slot.BELT, Items.WARPED_FUNGUS,
			Effect.attr(Attributes.MAX_HEALTH, 2.0, false), "{+1 serce}"),
	WITHERING_HEIGHTS("nether/summon_wither", "Dziki Tłum", 0xDDDDDD, Slot.HEAD, Items.NETHER_STAR,
			Effect.special(Special.WITHER_IMMUNE), "{Obumarcie cię nie rusza}"),
	UNEASY_ALLIANCE("nether/uneasy_alliance", "Niełatwy Sojusz", 0xF0F0F0, Slot.ORBIT, Items.GHAST_TEAR,
			Effect.special(Special.GHAST_CALM), "{Ghasty cię nie atakują}"),

	// Story.
	ZOMBIE_DOCTOR("story/cure_zombie_villager", "Doktor Zombie", 0xFFD700, Slot.BELT, Items.GOLDEN_APPLE,
			Effect.attr(Attributes.MAX_HEALTH, 4.0, false), "{+2 serca}");

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
		ORBIT
	}

	/** Abilities that need their own code (see BonusAbilities). */
	public enum Special {
		ARROW_POWER, ARROW_STRAIGHT, ARROW_SPEED, ARROW_PIERCE, MULTISHOT, XP_BONUS, LIGHTNING_IMMUNE, LIGHTNING_HIT, SMASH,
		REGEN_SLOW, WATER_REGEN, SPOT_MOBS, TOTEM, MAGNET, DOUBLE_JUMP, THORNS_WITHER, NO_FALL, ESCAPE, SLOW_FALL_SNEAK,
		SATIATED, NO_HUNGER, TWINS, CREEPER_CALM, ORE_SENSE, GREEN_PLUS, AUTO_REPAIR, STING, SILK_FIST, WOLF_BUDDY, WOLF_PACK,
		FIST_AXE, CLEANSE, BEACON_AURA, BEACON_FULL, PIGLIN_CALM, WITHER_HIT, FIRE_HIT, WITHER_IMMUNE, GHAST_CALM
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
