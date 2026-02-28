package net.yixi_xun.affix_core.affix;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.api.AffixEvent.CustomMessageEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.List;

import static net.yixi_xun.affix_core.AffixCoreMod.MOD_ID;
import static net.yixi_xun.affix_core.affix.AffixContext.createEntityData;
import static net.yixi_xun.affix_core.affix.AffixContext.createItemData;
import static net.yixi_xun.affix_core.affix.AffixManager.getAffixes;
import static net.yixi_xun.affix_core.affix.AffixProcessor.*;

@Mod.EventBusSubscriber(modid = MOD_ID)
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
            if (target instanceof Player player) {
                context.addVariable("attack_cooldown", player.getAttackStrengthScale(0.5F));
            }
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
        String slot = event.getSlot().getName();

        // 移除旧装备提供的词缀效果
        if (!from.isEmpty()) {
            List<Affix> affixes = getAffixes(from);
            if (affixes.isEmpty()) return;
            for (Affix affix : affixes) {
                handleItemRemoval(entity, from, affix);
            }
        }

        // 如果新装备有词缀，触发 on_equip 事件
        if (!to.isEmpty()) {
            processSingleItemAffix(entity, slot, to, "on_equip", event, (context) -> {
                context.addVariable("slot", event.getSlot().getName());
                context.addVariable("item", createItemData(to));
            });}

        if (!from.isEmpty()) {
            processSingleItemAffix(entity, slot, from, "on_unequip", event, (context) -> {
                context.addVariable("slot", event.getSlot().getName());
                context.addVariable("item", createItemData(from));
            });
        }
    }

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();
        String slot = event.getIdentifier();

        // 移除旧装备提供的词缀效果
        if (!from.isEmpty()) {
            List<Affix> affixes = getAffixes(from);
            if (affixes.isEmpty()) return;
            for (Affix affix : affixes) {
                handleItemRemoval(entity, from, affix);
            }
        }

        if (!to.isEmpty()) {
            processSingleItemAffix(entity, slot, to, "on_equip", event, (context) -> {
                context.addVariable("slot", event.getIdentifier());
                context.addVariable("item", createItemData(to));
            });}

        if (!from.isEmpty()) {
            processSingleItemAffix(entity, slot, from, "on_unequip", event, (context) -> {
                context.addVariable("slot", event.getIdentifier());
                context.addVariable("item", createItemData(from));
            });
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
    }

    /**
     * 右键方块监听器
     */
    @SubscribeEvent
    public static void onInteractBlock(PlayerInteractEvent.RightClickBlock event) {
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
     * 物品使用完成监听器
     */
    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        processAffixTriggerWithVars(event.getEntity(), "on_use_finish", event, (context) ->
                context.addVariable("item", createItemData(event.getItem())));
    }

    /**
     * 持续物品使用监听器
     */
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        processAffixTriggerWithVars(event.getEntity(), "on_use_tick", event, (context) -> {
            context.addVariable("duration", event.getDuration());
            context.addVariable("item", createItemData(event.getItem()));
        });
    }

    /**
     * 物品使用开始监听器
     */
    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        processAffixTriggerWithVars(event.getEntity(), "on_use_start", event, (context) ->
                context.addVariable("item", createItemData(event.getItem())));
    }

    /**
     * 物品丢弃监听器
     */
    @SubscribeEvent
    public static void onDrop(ItemTossEvent event) {
        processAffixTriggerWithVars(event.getEntity(), "on_drop", event, (context ->
                context.addVariable("item", createItemData(event.getEntity().getItem()))));
    }

    /**
     * 自定义消息监听器
     */
    @SubscribeEvent
    public static void onCustomMessage(CustomMessageEvent event) {
        processAffixTriggerWithVars(event.getEntity(), "on_custom_message", event, (context ->
                context.addVariable("message", event.getMessage())));
    }
}