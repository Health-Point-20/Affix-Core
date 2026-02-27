package net.yixi_xun.affix_core.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.affix.Affix;
import net.yixi_xun.affix_core.affix.AffixManager;
import net.yixi_xun.affix_core.affix.AffixProcessor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import top.theillusivec4.curios.api.event.CurioUnequipEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;

@Mod.EventBusSubscriber(modid = AffixCoreMod.MOD_ID)
public class CuriosEventHandler {

    /**
     * Curios装备事件监听器
     * 当物品被装备到Curios槽位时触发
     *
     * @param event CuriosEquipEvent事件
     */
    @SubscribeEvent
    public static void onCuriosEquip(CurioEquipEvent event) {
        LivingEntity entity = event.getEntity();
        String slotIdentifier = event.getSlotContext().identifier();
        ItemStack itemStack = event.getStack();

        try {
            processCuriosAffixTrigger(entity, "on_equip", slotIdentifier, itemStack);
        } catch (Exception e) {
            LOGGER.error("处理Curios装备事件时发生错误", e);
        }
    }

    /**
     * Curios卸下事件监听器
     * 当物品从Curios槽位中移除时触发
     *
     * @param event CuriosUnequipEvent事件
     */
    @SubscribeEvent
    public static void onCuriosUnequip(CurioUnequipEvent event) {
        LivingEntity entity = event.getEntity();
        String slotIdentifier = event.getSlotContext().identifier();
        ItemStack itemStack = event.getStack();

        try {
            handleItemRemoval(entity, itemStack);
            processCuriosAffixTrigger(entity, "on_unequip", slotIdentifier, itemStack);
        } catch (Exception e) {
            LOGGER.error("处理Curios卸下事件时发生错误", e);
        }
    }

    private static void handleItemRemoval(LivingEntity entity, ItemStack itemStack) {
        List<Affix> affixes = AffixManager.getAffixes(itemStack);
        if (affixes.isEmpty()) return;
        for (Affix affix : affixes) {
            AffixProcessor.handleItemRemoval(entity, itemStack, affix);
        }
    }

    private static void processCuriosAffixTrigger(LivingEntity entity, String trigger, String slotIdentifier, ItemStack itemStack) {
        List<Affix> affixes = AffixManager.getAffixes(itemStack);
        if (affixes.isEmpty()) return;

        Set<String> triggerSet = Set.of(trigger);
        for (Affix affix : affixes) {
            if (affix == null) continue;

            // 使用 AffixProcessor 统一检查逻辑
            if (!AffixProcessor.isTriggerMatch(affix.trigger(), triggerSet)) continue;
            if (!affix.canTriggerInSlot(null, slotIdentifier)) continue;

            // 复用 AffixProcessor 的处理逻辑，传入 slot 变量
            AffixProcessor.processSingleAffix(entity, affix, itemStack, trigger, null,
                    context -> {
                        context.addVariable("slot", null);
                        context.addVariable("curios_slot", slotIdentifier);
                    },
                    null);
        }
    }

    public static void getAllCuriosAffixes(LivingEntity entity, Map<Affix, ItemStack> affixLocationMap, List<Affix> validAffixes) {
        if (entity == null) return;

        try {
            CuriosApi.getCuriosInventory(entity).ifPresent(inventory -> {
                for (Map.Entry<String, ICurioStacksHandler> entry : inventory.getCurios().entrySet()) {
                    collectAffixesFromHandler(entry.getValue(), affixLocationMap, validAffixes);
                }
            });
        } catch (Exception e) {
            LOGGER.warn("获取所有Curios词缀时出错: {}", e.getMessage());
        }
    }

    private static void collectAffixesFromHandler(ICurioStacksHandler handler, Map<Affix, ItemStack> affixLocationMap, List<Affix> validAffixes) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStacks().getStackInSlot(i);
            if (stack.isEmpty()) continue;

            List<Affix> slotAffixes = AffixManager.getAffixes(stack);
            for (Affix affix : slotAffixes) {
                if (affix != null) {
                    affixLocationMap.put(affix, stack);
                    validAffixes.add(affix);
                }
            }
        }
    }
}
