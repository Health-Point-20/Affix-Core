package net.yixi_xun.affix_core.tap;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.items.AffixCoreModItems;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AffixCoreModTap {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AffixCoreMod.MODID);
    public static final RegistryObject<CreativeModeTab> AFFIX_CORE_MOD_TAB = REGISTRY.register("affix_core",
            () -> CreativeModeTab.builder().title(Component.translatable("item_group.affix_core"))
                    .icon(() -> new ItemStack(AffixCoreModItems.RAFFLE_ITEM.get()))
                    .displayItems((parameters, tab) -> {
                        tab.accept(AffixCoreModItems.RAFFLE_ITEM.get());
                    }).build()
    );
}
