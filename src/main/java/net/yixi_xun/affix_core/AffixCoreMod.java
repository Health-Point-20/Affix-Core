package net.yixi_xun.affix_core;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.yixi_xun.affix_core.affix.AffixManager;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AffixCoreMod.MODID)
public class AffixCoreMod {

    public static final String MODID = "affix_core";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public AffixCoreMod() {
        MinecraftForge.EVENT_BUS.register(this);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        AffixManager.init();
        modLoadingContext.registerConfig(ModConfig.Type.COMMON, AFConfig.SPEC);

    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // 注册客户端事件处理器
        }
    }
}