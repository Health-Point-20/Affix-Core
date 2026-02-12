package net.yixi_xun.affix_core.affix;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.api.AffixEvent.CustomMessageEvent;
import net.yixi_xun.affix_core.curios.CuriosEventHandler;

import java.util.*;
import java.util.function.Consumer;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;
import static net.yixi_xun.affix_core.AffixCoreMod.MODID;
import static net.yixi_xun.affix_core.affix.AffixContext.createEntityData;
import static net.yixi_xun.affix_core.affix.AffixManager.getAffixes;
import static net.yixi_xun.affix_core.affix.AffixProcessor.handleItemRemoval;

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
            context.addVariable("target", createEntityData(target));
            context.addVariable("distance", context.getOwner().distanceTo(target));
        });
        processAffixTriggerWithVars(event.getEntity(), "on_hurt", event, (context) -> {
            context.addVariable("damage", event.getAmount());
            context.addVariable("damage_type", event.getSource().type().msgId());
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                context.addVariable("attacker", createEntityData(attacker));
                context.addVariable("distance", context.getOwner().distanceTo(attacker));
            } else {
                context.addVariable("attacker", createEntityData(context.getOwner())); // 默认为自身
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
            context.addVariable("target", target != null ? createEntityData(target) : createEntityData(context.getOwner()));
            context.addVariable("distance", context.getOwner().distanceTo(target != null ? target : context.getOwner()));
        });
        processAffixTriggerWithVars(event.getEntity(), "on_death", event, (context) -> {
            LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity ?
                    (LivingEntity) event.getSource().getEntity() : null;
            context.addVariable("damage_type", event.getSource().type().msgId());
            context.addVariable("killer", killer != null ? createEntityData(killer) : createEntityData(context.getOwner()));
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
                    handleItemRemoval(entity, from, affix);
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
                context.addVariable("target", createEntityData(target));
            }
        });}

    /**
     * 左键实体监听器
     */
    @SubscribeEvent
    public static void onLeftClickEntity(AttackEntityEvent event) {
        processAffixTriggerWithVars(event.getEntity(), "on_left_click_entity", event, (context) -> {
            if (event.getTarget() instanceof LivingEntity target) {
                context.addVariable("target", createEntityData(target));
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
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        try {
            // 预先构建词缀到槽位的映射，避免重复查询
            Map<Affix, ItemStack> affixLocationMap = new HashMap<>();
            List<Affix> validAffixes = new ArrayList<>();

            // 收集所有装备槽位上的词缀，并进行预过滤
            collectEquipmentAffixes(living, trigger, affixLocationMap, validAffixes);
            
            // 收集所有Curios槽位上的词缀
            CuriosEventHandler.getAllCuriosAffixes(living, affixLocationMap, validAffixes);

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
        Set<String> triggerSet = Collections.singleton(trigger);
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = living.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            
            List<Affix> slotAffixes = getAffixes(stack);
            for (Affix affix : slotAffixes) {
                if (affix != null && isTriggerMatch(affix.trigger(), triggerSet) && !affix.triggerInInvalidSlot(slot)) {
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
     * 处理单个词缀
     */
    private static void processSingleAffix(LivingEntity living, Affix affix, ItemStack itemStack, 
                                          String trigger, Event event, Consumer<AffixContext> eventVarSetter) {
        AffixProcessor.processSingleAffix(living, affix, itemStack, trigger, event, eventVarSetter, null);
    }
    
    /**
     * 检查触发器是否匹配
     * 
     * @param affixTrigger 词缀定义的触发器字符串
     * @param triggerSet 当前事件的触发器集合
     * @return 是否匹配
     */
    private static boolean isTriggerMatch(String affixTrigger, Set<String> triggerSet) {
        return AffixProcessor.isTriggerMatch(affixTrigger, triggerSet);
    }

}