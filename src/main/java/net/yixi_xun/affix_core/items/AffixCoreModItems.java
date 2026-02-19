
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
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, AffixCoreMod.MODID);
	public static final RegistryObject<Item> RAFFLE_ITEM = REGISTRY.register("raffle_item", RaffleItem::new);

}
