package net.yixi_xun.affix_core.affix;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;
import static net.yixi_xun.affix_core.affix.AffixManager.getAffixes;

/**
 * 词缀处理工具类
 * 提供通用的词缀执行逻辑，消除重复代码
 */
public class AffixProcessor {

    /**
     * 处理单个词缀的执行逻辑
     * 
     * @param entity 实体
     * @param affix 词缀
     * @param itemStack 物品栈
     * @param trigger 触发器
     * @param event 事件对象（可为null）
     * @param eventVarSetter 事件变量设置器（可为null）
     */
    public static void processSingleAffix(LivingEntity entity, Affix affix, ItemStack itemStack, 
                                         String trigger, Event event, 
                                         Consumer<AffixContext> eventVarSetter) {
        try {
            // 创建词缀上下文
            AffixContext context = new AffixContext(
                entity.level(),
                entity,
                itemStack,
                affix,
                trigger,
                event
            );

            // 设置事件特定变量
            if (eventVarSetter != null) {
                eventVarSetter.accept(context);
            }

            // 检查冷却
            if (affix.cooldown() > 0 && context.inCooldown()) {
                return;
            }

            // 检查条件
            if (!affix.checkCondition(context)) {
                return;
            }

            // 设置冷却
            if (affix.cooldown() > 0) {
                context.setCooldown(affix.cooldown());
            }

            // 执行操作
            affix.execute(context);

        } catch (Exception e) {
            LOGGER.error("处理词缀时发生错误: {}", affix, e);
        }
    }
    
    /**
     * 处理物品移除逻辑（专门用于 on_remove 事件）
     * 
     * @param entity 实体
     * @param itemStack 被移除的物品
     * @param affix 词缀
     */
    public static void handleItemRemoval(LivingEntity entity, ItemStack itemStack, Affix affix) {
        try {
            // 为 on_remove 事件创建基础的 AffixContext
            AffixContext context = new AffixContext(
                entity.level(), 
                entity, 
                itemStack, 
                affix, 
                "on_remove", 
                null
            );
            affix.remove(context);
        } catch (Exception e) {
            LOGGER.error("移除词缀效果时发生错误: {}", affix, e);
        }
    }
    
    /**
     * 检查触发器是否匹配
     * 
     * @param affixTrigger 词缀定义的触发器字符串
     * @param trigger 当前事件的触发器
     * @return 是否匹配
     */
    public static boolean isTriggerMatch(String affixTrigger, String trigger) {
        if (affixTrigger == null || affixTrigger.isEmpty()) {
            return false;
        }
        
        return Arrays.stream(affixTrigger.split(","))
            .map(String::trim)
            .anyMatch(trigger::equals);
    }

    /**
     * 处理词缀触发
     */
    public static void processAffixTrigger(Entity entity, String trigger, Event event) {
        processAffixTriggerWithVars(entity, trigger, event, (context) -> {});
    }

    /**
     * 处理词缀触发，带事件特定变量设置
     * 按照优先级顺序执行词缀
     *
     * @param entity 触发实体
     * @param trigger 触发器名称
     * @param event 事件对象
     * @param eventVarSetter 事件变量设置器
     */
    public static void processAffixTriggerWithVars(Entity entity, String trigger, Event event, Consumer<AffixContext> eventVarSetter) {
        // 只处理实体
        if (!(entity instanceof LivingEntity living)) return;

        // 只在服务端处理
        if (living.level().isClientSide()) return;

        try {
            // 预先构建词缀到槽位的映射，避免重复查询
            Map<Affix, ItemStack> affixLocationMap = new HashMap<>();
            List<Affix> validAffixes = new ArrayList<>();

            // 收集所有装备槽位上的词缀，并进行预过滤
            collectEquipmentAffixes(living, trigger, affixLocationMap, validAffixes);

            // 收集所有Curios槽位上的词缀
            getAllCuriosAffixes(living, trigger, affixLocationMap, validAffixes);

            if (validAffixes.isEmpty()) return;

            // 按优先级排序（数值越大优先级越高）
            validAffixes.sort(Comparator.comparingLong(Affix::priority).reversed());

            // 按优先级顺序处理每个词缀
            processAffixesInOrder(living, trigger, event, eventVarSetter, affixLocationMap, validAffixes);
        } catch (Exception e) {
            LOGGER.error("处理词缀触发时发生错误", e);
        }
    }

    /**
     * 收集装备槽位上的词缀
     */
    private static void collectEquipmentAffixes(LivingEntity living, String trigger,
                                               Map<Affix, ItemStack> affixLocationMap, List<Affix> validAffixes) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = living.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            List<Affix> slotAffixes = getAffixes(stack);
            for (Affix affix : slotAffixes) {
                if (affix != null && isTriggerMatch(affix.trigger(), trigger) && affix.canTriggerInSlot(slot.getName())) {
                    affixLocationMap.put(affix, stack);
                    validAffixes.add(affix);
                }
            }
        }
    }

    public static void getAllCuriosAffixes(LivingEntity entity, String trigger, Map<Affix, ItemStack> affixLocationMap, List<Affix> validAffixes) {
        if (entity == null) return;

        try {
            CuriosApi.getCuriosInventory(entity).ifPresent(inventory -> {
                for (Map.Entry<String, ICurioStacksHandler> entry : inventory.getCurios().entrySet()) {
                    collectAffixesFromHandler(entry.getValue(),  trigger, affixLocationMap, validAffixes);
                }
            });
        } catch (Exception e) {
            LOGGER.warn("获取所有Curios词缀时出错: {}", e.getMessage());
        }
    }

    private static void collectAffixesFromHandler(ICurioStacksHandler handler, String trigger, Map<Affix, ItemStack> affixLocationMap, List<Affix> validAffixes) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStacks().getStackInSlot(i);
            if (stack.isEmpty()) continue;

            List<Affix> slotAffixes = AffixManager.getAffixes(stack);
            if (slotAffixes.isEmpty()) continue;
            for (Affix affix : slotAffixes) {
                if (affix != null && isTriggerMatch(affix.trigger(), trigger) && affix.canTriggerInSlot(handler.getIdentifier())) {
                    affixLocationMap.put(affix, stack);
                    validAffixes.add(affix);
                }
            }
        }
    }

    /**
     * 按优先级顺序处理词缀
     */
    private static void processAffixesInOrder(LivingEntity living, String trigger, Event event,
                                             Consumer<AffixContext> eventVarSetter,
                                             Map<Affix, ItemStack> affixLocationMap, List<Affix> validAffixes) {
        for (Affix affix : validAffixes) {
            ItemStack foundStack = affixLocationMap.get(affix);
            if (foundStack == null) continue;

            processSingleAffix(living, affix, foundStack, trigger, event, eventVarSetter);
        }
    }

    /**
     * 处理单个物品的词缀
     */
    public static void processSingleItemAffix(Entity entity, String slot, ItemStack stack, String trigger, Event event, Consumer<AffixContext> eventVarSetter) {
        // 只处理实体
        if (!(entity instanceof LivingEntity living)) return;

        // 只在服务端处理
        if (living.level().isClientSide()) return;

        try {
            List<Affix> validAffixes = getAffixes(stack).stream()
                    .filter(affix -> affix != null && isTriggerMatch(affix.trigger(), trigger) && affix.canTriggerInSlot(slot))
                    .collect(Collectors.toList());

            if (validAffixes.isEmpty()) return;

            // 按优先级排序
            validAffixes.sort(Comparator.comparingLong(Affix::priority).reversed());

            // 按优先级顺序处理每个词缀
            for (Affix affix : validAffixes) {
                processSingleAffix(living, affix, stack, trigger, event, eventVarSetter);
            }
        } catch (Exception e) {
            LOGGER.error("处理词缀触发时发生错误", e);
        }
    }
}