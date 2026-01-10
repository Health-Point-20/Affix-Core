package net.yixi_xun.affix_core.affix;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;

import static net.yixi_xun.affix_core.AffixCoreMod.MODID;
import static net.yixi_xun.affix_core.affix.AffixManager.getAffixes;

@Mod.EventBusSubscriber(modid = MODID)
public class AffixTrigger {
    /**
     * 攻击事件监听器
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        processAffixTrigger(event.getSource().getEntity(), "on_attack", event);
        processAffixTrigger(event.getEntity(), "on_hurt", event);
    }


    /**
     * 死亡事件监听器
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        processAffixTrigger(event.getSource().getEntity(), "on_kill", event);
        processAffixTrigger(event.getEntity(), "on_death", event);
    }

    /**
     * 获得药水效果事件监听器
     */
    @SubscribeEvent
    public static void onEffectAdd(MobEffectEvent.Applicable event) {
        processAffixTrigger(event.getEntity(), "on_effect_add", event);
    }

    /**
     * 装备修改事件监听器
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();

        // 移除旧装备提供的词缀效果
        if (!from.isEmpty()) {
            List<Affix> affixes = getAffixes(from);
            if (affixes.isEmpty()) return;
            for (Affix affix : affixes) {
                affix.remove(new AffixContext(entity.level(), entity, from, affix, "on_remove", null));
            }
        }

        // 如果新装备有词缀，触发 on_equip 事件
        if (!to.isEmpty()) {
            processAffixTrigger(entity, "on_equip", event);
        }
    }

    /**
     * 处理词缀触发
     */
    private static void processAffixTrigger(Entity entity, String trigger, Event event) {
        // 只处理玩家实体
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        // 遍历所有装备槽位
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack itemStack = living.getItemBySlot(slot);

            // 获取物品上的所有词缀
            List<Affix> affixes = getAffixes(itemStack);

            if (affixes.isEmpty()) {
                continue;
            }

            // 处理每个词缀
            for (Affix affix : affixes) {
                Set<String> triggers = Set.of(affix.trigger().split(","));
                // 检查触发器是否匹配
                if (!triggers.contains(trigger)) {
                    continue;
                }

                // 检查槽位是否允许触发
                if (affix.triggerInInvalidSlot(slot)) {
                    continue;
                }

                // 创建词缀上下文
                AffixContext context = new AffixContext(
                        living.level(),
                        living,
                        itemStack,
                        affix,
                        trigger,
                        event
                );

                // 检查冷却
                if (affix.cooldown() > 0 && context.inCooldown()) {
                    continue;
                }

                // 检查条件
                if (!affix.checkCondition(context)) {
                    continue;
                }

                // 执行操作
                affix.execute(context);

                // 设置冷却
                if (affix.cooldown() > 0) {
                    context.setCooldown(affix.cooldown());
                }
            }
        }
    }
    
    /**
     * 手动触发词缀
     */
    public static void trigger(ItemStack itemStack, LivingEntity entity, Level level, EquipmentSlot slot, String trigger) {
        // 获取物品上的所有词缀
        List<Affix> affixes = getAffixes(itemStack);

        // 处理每个词缀
        for (Affix affix : affixes) {
            // 检查触发器是否匹配
            if (!trigger.equals(affix.trigger())) {
                continue;
            }

            // 检查槽位是否允许触发
            if (affix.triggerInInvalidSlot(slot)) {
                continue;
            }

            // 创建词缀上下文
            AffixContext context = new AffixContext(
                    level,
                    entity,
                    itemStack,
                    affix,
                    trigger,
                    null
            );

            // 检查冷却
            if (affix.cooldown() > 0 && context.inCooldown()) {
                continue;
            }

            // 检查条件
            if (!affix.checkCondition(context)) {
                continue;
            }

            // 执行操作
            affix.execute(context);

            // 设置冷却
            if (affix.cooldown() > 0) {
                context.setCooldown(affix.cooldown());
            }
        }
    }
}