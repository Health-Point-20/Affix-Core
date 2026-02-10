package net.yixi_xun.affix_core;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// Config class for the mod
@Mod.EventBusSubscriber(modid = AffixCoreMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AFConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();


    // 范围伤害相关配置
    public static final ForgeConfigSpec.DoubleValue MAX_AREA_DAMAGE_RANGE;
    public static final ForgeConfigSpec.IntValue MAX_AREA_DAMAGE_ENTITIES;
    
    static {
        BUILDER.push("area_damage");
        MAX_AREA_DAMAGE_RANGE = BUILDER
            .comment("Maximum search range for area damage operations to prevent performance issues (default: 64.0)")
            .defineInRange("maxAreaDamageRange", 64.0, 1.0, 256.0);
        MAX_AREA_DAMAGE_ENTITIES = BUILDER
            .comment("Maximum number of entities that can be affected by area damage to prevent performance issues (default: 64)")
            .defineInRange("maxAreaDamageEntities", 128, 1, 1024);
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}