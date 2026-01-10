package net.yixi_xun.affix_core;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AffixCoreMod.MODID)
public class AffixCoreMod {

    public static final String MODID = "affix_core";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AffixCoreMod(IEventBus modEventBus, ModLoadingContext modLoadingContext) {

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        modLoadingContext.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 初始化词缀系统
        net.yixi_xun.affix_core.affix.AffixManager.init();
        LOGGER.info("Affix Core system initialized");
    }

    private static final Queue<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ArrayDeque<>();

    public static void queueServerWork(int tick, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            if (tick > 0 && action != null) {
                workQueue.add(new AbstractMap.SimpleEntry<>(action, tick));
            }
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 使用迭代器安全地修改队列
            Iterator<AbstractMap.SimpleEntry<Runnable, Integer>> iterator = workQueue.iterator();
            while (iterator.hasNext()) {
                AbstractMap.SimpleEntry<Runnable, Integer> task = iterator.next();
                
                // 减少剩余tick数
                int remainingTicks = task.getValue() - 1;
                task.setValue(remainingTicks);
                
                // 如果任务已到期，则执行并移除
                if (remainingTicks <= 0) {
                    iterator.remove();
                    task.getKey().run();
                }
            }
        }
    }
}