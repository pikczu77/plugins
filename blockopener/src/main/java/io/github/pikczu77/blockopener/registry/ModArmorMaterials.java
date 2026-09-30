package io.github.pikczu77.blockopener.registry;

import io.github.pikczu77.blockopener.BlockOpener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * One material per custom armor piece, so each piece gets its own equipment asset (texture on the player).
 * Values follow the stats shown in the video tooltips.
 */
public final class ModArmorMaterials {
	public static final ArmorMaterial PUMPKIN = new ArmorMaterial(
		40, ArmorMaterials.makeDefense(3, 6, 8, 3, 11), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, 0.0F,
		ItemTags.REPAIRS_LEATHER_ARMOR, asset("pumpkin_boots")
	);
	public static final ArmorMaterial ANVIL = new ArmorMaterial(
		40, ArmorMaterials.makeDefense(3, 6, 8, 3, 11), 10, SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.1F,
		ItemTags.REPAIRS_IRON_ARMOR, asset("anvil_chestplate")
	);
	public static final ArmorMaterial DIAMOND_FLIGHT = new ArmorMaterial(
		40, ArmorMaterials.makeDefense(3, 6, 8, 3, 11), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.1F,
		ItemTags.REPAIRS_DIAMOND_ARMOR, asset("diamond_leggings")
	);
	public static final ArmorMaterial SCULK = new ArmorMaterial(
		40, ArmorMaterials.makeDefense(3, 6, 8, 3, 11), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F,
		ItemTags.REPAIRS_NETHERITE_ARMOR, asset("sculk_helmet")
	);

	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, BlockOpener.id(name));
	}

	private ModArmorMaterials() {
	}
}
