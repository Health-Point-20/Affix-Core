package net.yixi_xun.affix_core.affix;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;

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
     * @param additionalVariables 额外变量设置器（可为null）
     */
    public static void processSingleAffix(LivingEntity entity, Affix affix, ItemStack itemStack, 
                                         String trigger, Event event, 
                                         Consumer<AffixContext> eventVarSetter,
                                         Consumer<AffixContext> additionalVariables) {
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
            
            // 设置额外变量
            if (additionalVariables != null) {
                additionalVariables.accept(context);
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
     * @param triggerSet 当前事件的触发器集合
     * @return 是否匹配
     */
    public static boolean isTriggerMatch(String affixTrigger, Set<String> triggerSet) {
        if (affixTrigger == null || affixTrigger.isEmpty()) {
            return false;
        }
        
        return Arrays.stream(affixTrigger.split(","))
            .map(String::trim)
            .anyMatch(triggerSet::contains);
    }

}