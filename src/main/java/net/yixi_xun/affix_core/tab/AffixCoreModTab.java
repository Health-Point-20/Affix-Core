package net.yixi_xun.affix_core.tab;

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
public class AffixCoreModTab {
    public static final DeferredRegister<CreativeModeTab> TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AffixCoreMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> AFFIX_CORE_MOD_TAB = TAB.register("affix_core",
            () -> CreativeModeTab.builder().title(Component.translatable("item_group.affix_core"))
                    .icon(() -> new ItemStack(AffixCoreModItems.RAFFLE_ITEM.get()))
                    .displayItems((parameters, tab) -> {
                        tab.accept(AffixCoreModItems.RAFFLE_ITEM.get());
                        tab.accept(AffixCoreModItems.RAFFLE_BLOCK.get());
                    })
                    .build()


    );
}
