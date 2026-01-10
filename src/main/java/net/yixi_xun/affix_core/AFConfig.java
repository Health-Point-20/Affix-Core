package net.yixi_xun.affix_core;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// Config class for the mod
@Mod.EventBusSubscriber(modid = AffixCoreMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AFConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_ATTRIBUTE_TOOLTIP_MERGING;
    
    static {
        BUILDER.push("tooltip");
        ENABLE_ATTRIBUTE_TOOLTIP_MERGING = BUILDER
            .comment("Enable merging of attribute tooltips for better readability (default: true)")
            .define("enableAttributeTooltipMerging", true);
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}