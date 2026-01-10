package net.yixi_xun.affix_core.affix;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

import static net.yixi_xun.affix_core.affix.AffixManager.getAffixes;

@Mod.EventBusSubscriber(modid = "affix_core", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AffixTrigger {
    /**
     * 攻击事件监听器
     */
    @SubscribeEvent
    public void onAttack(LivingDamageEvent event) {
        processAffixTrigger(event.getSource().getEntity(), "on_attack", event);
        processAffixTrigger(event.getEntity(), "on_hurt", event);
    }


    /**
     * 死亡事件监听器
     */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        processAffixTrigger(event.getEntity(), "on_death", event);
        processAffixTrigger(event.getEntity(), "on_kill", event);
    }

    /**
     * 获得药水效果事件监听器
     */
    @SubscribeEvent
    public void onPotionEffectAdd(MobEffectEvent.Applicable event) {
        processAffixTrigger(event.getEntity(), "on_effect_add", event);
    }

    /**
     * 装备修改事件监听器
     */
    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        processAffixTrigger(event.getEntity(), "on_equipment_change", event);
    }



    /**
     * 处理词缀触发
     */
    private static void processAffixTrigger(Entity entity, String trigger, Object event) {
        // 只处理玩家实体
        if (!(entity instanceof Player player)) {
            return;
        }

        // 遍历所有装备槽位
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack itemStack = player.getItemBySlot(slot);

            // 获取物品上的所有词缀
            List<Affix> affixes = getAffixes(itemStack);

            // 处理每个词缀
            for (Affix affix : affixes) {
                // 检查触发器是否匹配
                if (!trigger.equals(affix.trigger())) {
                    continue;
                }

                // 检查槽位是否允许触发
                if (!affix.canTriggerInSlot(slot)) {
                    continue;
                }

                // 创建词缀上下文
                AffixContext context = new AffixContext(
                        player.level(),
                        player,
                        itemStack,
                        affix,
                        trigger,
                        event
                );

                // 检查冷却
                if (affix.cooldown() > 0 && !context.isCooldownOver()) {
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
}
