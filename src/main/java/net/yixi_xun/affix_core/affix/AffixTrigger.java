package net.yixi_xun.affix_core.affix;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.api.AffixEvent.CustomMessageEvent;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static net.yixi_xun.affix_core.AffixCoreMod.MODID;
import static net.yixi_xun.affix_core.affix.AffixManager.getAffixes;

@Mod.EventBusSubscriber(modid = MODID)
public class AffixTrigger {
    /**
     * 攻击事件监听器
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        processAffixTriggerWithVars(event.getSource().getEntity(), "on_attack", event, (context) -> {
            LivingEntity target = event.getEntity();
            context.addVariable("damage", event.getAmount());
            context.addVariable("damage_type", event.getSource().type().msgId());
            context.addVariable("target", context.createEntityData(target));
            context.addVariable("distance", context.getOwner().distanceTo(target));
        });
        processAffixTriggerWithVars(event.getEntity(), "on_hurt", event, (context) -> {
            context.addVariable("damage", event.getAmount());
            context.addVariable("damage_type", event.getSource().type().msgId());
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                context.addVariable("attacker", context.createEntityData(attacker));
                context.addVariable("distance", context.getOwner().distanceTo(attacker));
            } else {
                context.addVariable("attacker", context.createEntityData(context.getOwner())); // 默认为自身
                context.addVariable("distance", 0);
            }
        });
    }

    /**
     * 死亡事件监听器
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        processAffixTriggerWithVars(event.getSource().getEntity(), "on_kill", event, (context) -> {
            LivingEntity target = event.getEntity();
            context.addVariable("target", target != null ? context.createEntityData(target) : context.createEntityData(context.getOwner()));
            context.addVariable("distance", context.getOwner().distanceTo(target != null ? target : context.getOwner()));
        });
        processAffixTriggerWithVars(event.getEntity(), "on_death", event, (context) -> {
            LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity ?
                    (LivingEntity) event.getSource().getEntity() : null;
            context.addVariable("damage_type", event.getSource().type().msgId());
            context.addVariable("killer", killer != null ? context.createEntityData(killer) : context.createEntityData(context.getOwner()));
            context.addVariable("distance", killer != null ? context.getOwner().distanceTo(killer) : 0);
        });
    }

    /**
     * 获得药水效果事件监听器
     */
    @SubscribeEvent
    public static void onEffectAdd(MobEffectEvent.Applicable event) {
        processAffixTriggerWithVars(event.getEntity(), "on_effect_add", event, (context) -> {
            context.addVariable("duration", event.getEffectInstance().getDuration());
            context.addVariable("amplifier", event.getEffectInstance().getAmplifier());
        });
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
                if (affix != null) {
                    // 为 on_remove 事件创建基础的 AffixContext
                    AffixContext context = new AffixContext(entity.level(), entity, from, affix, "on_remove", null);
                    affix.remove(context);
                }
            }
        }

        // 如果新装备有词缀，触发 on_equip 事件
        if (!to.isEmpty()) {
            processAffixTriggerWithVars(entity, "on_equip", event, (context) -> context.addVariable("slot", event.getSlot().getName()));}

        if (!from.isEmpty()) {
            processAffixTriggerWithVars(entity, "on_unequip", event, (context) -> context.addVariable("slot", event.getSlot().getName()));
        }
    }

    /**
     * tick监听器
     */
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        processAffixTrigger(event.getEntity(), "on_tick", event);
    }

    /**
     *  右键监听器
     */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickItem event) {
        processAffixTrigger(event.getEntity(), "on_right_click", event);
        // 右键的目标为方块
        BlockState block = event.getLevel().getBlockState(event.getPos());
        if (!block.isAir()) {
            processAffixTriggerWithVars(event.getEntity(), "on_right_click_block", event, (context) -> {
                context.addVariable("block_name", block.getBlock().getName());
                context.addVariable("block_id", block.getBlock().getDescriptionId());
                context.addVariable("x", event.getPos().getX());
                context.addVariable("y", event.getPos().getY());
                context.addVariable("z", event.getPos().getZ());
            });
        }
    }

    /**
     * 右键实体监听器
     */
    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        processAffixTriggerWithVars(event.getEntity(), "on_right_click_entity", event, (context) -> {
            if (event.getTarget() instanceof LivingEntity target) {
                context.addVariable("target", context.createEntityData(target));
            }
        });}

    /**
     * 左键实体监听器
     */
    @SubscribeEvent
    public static void onLeftClickEntity(AttackEntityEvent event) {
        processAffixTriggerWithVars(event.getEntity(), "on_left_click_entity", event, (context) -> {
            if (event.getTarget() instanceof LivingEntity target) {
                context.addVariable("target", context.createEntityData(target));
            }
        });
    }

    /**
     * 左键方块监听器
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        processAffixTriggerWithVars(event.getEntity(), "on_left_click_block", event, (context) -> {
            context.addVariable("block_name", event.getLevel().getBlockState(event.getPos()).getBlock().getName());
            context.addVariable("block_id", event.getLevel().getBlockState(event.getPos()).getBlock().getDescriptionId());
            context.addVariable("x", event.getPos().getX());
            context.addVariable("y", event.getPos().getY());
            context.addVariable("z", event.getPos().getZ());
        });
    }

    /**
     * 左键空处监听器
     */
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        processAffixTrigger(event.getEntity(), "on_left_click_empty", event);
    }

    /**
     * 物品使用完成监听器
     */
    @SubscribeEvent
    public static void onUse(LivingEntityUseItemEvent.Finish event) {
        processAffixTrigger(event.getEntity(), "on_use_finish", event);
    }

    /**
     * 持续物品使用监听器
     */
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        processAffixTriggerWithVars(event.getEntity(), "on_use_tick", event, (context) ->
                context.addVariable("duration", event.getDuration()));
    }

    /**
     * 物品使用开始监听器
     */
    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        processAffixTrigger(event.getEntity(), "on_use_start", event);
    }

    /**
     * 物品丢弃监听器
     */
    @SubscribeEvent
    public static void onDrop(ItemTossEvent event) {
        processAffixTriggerWithVars(event.getEntity(), "on_drop", event, (context ->
                context.addVariable("item.name", event.getEntity().getItem().getDescriptionId())));
    }

    /**
     * 自定义消息监听器
     */
    @SubscribeEvent
    public static void onCustomMessage(CustomMessageEvent event) {
        processAffixTriggerWithVars(event.getEntity(), "on_custom_message", event, (context ->
                context.addVariable("message", event.getMessage())));
    }

    /**
     * 处理词缀触发
     */
    private static void processAffixTrigger(Entity entity, String trigger, Event event) {
        processAffixTriggerWithVars(entity, trigger, event, (context) -> {});
    }

    /**
     * 处理词缀触发，带事件特定变量设置
     */
    private static void processAffixTriggerWithVars(Entity entity, String trigger, Event event, Consumer<AffixContext> eventVarSetter) {
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
                if (affix == null) {
                    continue; // 跳过null的affix
                }
                
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

                // 设置事件特定变量
                eventVarSetter.accept(context);

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
            if (affix == null) {
                continue; // 跳过null的affix
            }
            
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