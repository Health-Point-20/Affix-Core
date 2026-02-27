
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.yixi_xun.affix_core.items;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.yixi_xun.affix_core.AffixCoreMod;

public class AffixCoreModItems {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AffixCoreMod.MOD_ID);
	public static final RegistryObject<Item> RAFFLE_ITEM = ITEMS.register("raffle_item", RaffleItem::new);
	public static final RegistryObject<Item> RAFFLE_BLOCK = ITEMS.register("raffle_block", RaffleBlockItem::new);
}
