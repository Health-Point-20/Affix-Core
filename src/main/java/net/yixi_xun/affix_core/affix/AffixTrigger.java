package net.yixi_xun.affix_core.affix;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.api.AffixEvent.CustomMessageEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static net.yixi_xun.affix_core.AffixCoreMod.MOD_ID;
import static net.yixi_xun.affix_core.affix.AffixContext.*;
import static net.yixi_xun.affix_core.affix.AffixManager.getAffixes;
import static net.yixi_xun.affix_core.affix.AffixProcessor.*;
import static net.yixi_xun.affix_core.affix.operation.VariableOperation.getEntityVariables;
import static net.yixi_xun.affix_core.affix.operation.VariableOperation.removeEntityVariables;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class AffixTrigger {
    private static final Map<Player, Vec3> lastPlayerPositions = new WeakHashMap<>();
    /**
     * 攻击事件监听器
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        LivingEntity target = event.getEntity();

        processAffixTriggerWithVars(event.getSource().getEntity(), "on_attack", event, (context) -> {
            addDamageVariables(context, event);
            context.addVariable("target", createEntityData(target));
            context.addVariable("distance", context.getOwner().distanceTo(target));
            if (attacker instanceof Player player) {
                context.addVariable("attack_cooldown", player.getAttackStrengthScale(0.5F));
            }
        });

        processAffixTriggerWithVars(event.getEntity(), "on_hurt", event, (context) -> {
            addDamageVariables(context, event);
            if (attacker != null) {
                context.addVariable("attacker", createEntityData(attacker));
                context.addVariable("distance", context.getOwner().distanceTo(attacker));
                if (attacker instanceof Player player) {
                    context.addVariable("attack_cooldown", player.getAttackStrengthScale(0.5F));
                }
            }
        });
    }

    // 添加伤害变量
    private static void addDamageVariables(AffixContext context, LivingHurtEvent event) {
        context.addVariable("damage", event.getAmount());
        context.addVariable("damage_type", event.getSource().type().msgId());
        context.addVariable("is_indirect", event.getSource().isIndirect() ? 1 : 0);
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

        handleEquipmentChange(event, entity, from, to, slot);
    }

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();
        String slot = event.getIdentifier();

        handleEquipmentChange(event, entity, from, to, slot);
    }

    private static void handleEquipmentChange(Event event, LivingEntity entity, ItemStack from, ItemStack to, String slot) {
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
                context.addVariable("slot", slot);
                context.addVariable("item", createItemData(to));
            });
            processAffixTriggerWithVars(entity, "on_any_equip", event, (context) -> {
                context.addVariable("slot", slot);
                context.addVariable("item", createItemData(to));
            });
        }

        if (!from.isEmpty()) {
            processSingleItemAffix(entity, slot, from, "on_unequip", event, (context) -> {
                context.addVariable("slot", slot);
                context.addVariable("item", createItemData(from));
            });
            processAffixTriggerWithVars(entity, "on_any_unequip", event, (context) -> {
                context.addVariable("slot", slot);
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
        processAffixTriggerWithVars(event.getEntity(), "on_right_click", event, (context ->
                context.addVariable("item", createItemData(event.getItemStack()))));
    }

    /**
     * 右键方块监听器
     */
    @SubscribeEvent
    public static void onInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        processAffixTriggerWithVars(event.getEntity(), "on_right_click_block", event, (context) ->
                    context.addVariable("block",createBlockData(event.getPos(), event.getLevel())));
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
        processAffixTriggerWithVars(event.getEntity(), "on_left_click_block", event, (context) ->
                context.addVariable("block", createBlockData(event.getPos(), event.getLevel())));
    }

    /**
     * 物品使用完成监听器
     */
    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        processSingleItemAffix(event.getEntity(), "", event.getItem(), "on_use_finish", event, (context) ->
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

    /**
     * 弹射物击中实体
     */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        HitResult rayTraceResult = event.getRayTraceResult();
        if (projectile.getOwner() instanceof LivingEntity owner) {
            var entity_vars = getEntityVariables(owner);

            if (rayTraceResult instanceof EntityHitResult hitResult) {
                if ((hitResult.getEntity() instanceof LivingEntity target)) {
                    if (entity_vars.containsKey("weapon")) {
                        ItemStack weapon = (ItemStack) entity_vars.get("weapon");
                        processSingleItemAffix(owner,"mainhand", weapon, "on_projectile_hit_entity", event, (context -> {
                            context.addVariable("target", createEntityData(target));
                            context.addVariable("weapon", createItemData(weapon));
                            addArrowVariables(context, projectile);
                        }));
                    }

                    if (entity_vars.containsKey("ammo")) {
                        ItemStack ammo = (ItemStack) entity_vars.get("ammo");
                        processSingleItemAffix(owner, "ammo", ammo, "on_projectile_hit_entity", event, (context -> {
                            context.addVariable("target", createEntityData(target));
                            context.addVariable("ammo", createItemData(ammo));
                            addArrowVariables(context, projectile);
                        }));
                    }
                }
            } else if (rayTraceResult instanceof BlockHitResult blockHitResult) {
                if (entity_vars.containsKey("weapon")) {
                    ItemStack weapon = (ItemStack) entity_vars.get("weapon");
                    processSingleItemAffix(owner,"mainhand", weapon, "on_projectile_hit_block", event, (context -> {
                        context.addVariable("block", createBlockData(blockHitResult.getBlockPos(), owner.level()));
                        context.addVariable("weapon", createItemData(weapon));
                        addArrowVariables(context, projectile);
                    }));
                }
                if (entity_vars.containsKey("ammo")) {
                    ItemStack ammo = (ItemStack) entity_vars.get("ammo");
                    processSingleItemAffix(owner, "ammo", ammo, "on_projectile_hit_block", event, (context -> {
                        context.addVariable("block", createBlockData(blockHitResult.getBlockPos(), owner.level()));
                        context.addVariable("ammo", createItemData(ammo));
                        addArrowVariables(context, projectile);
                    }));
                }
            }

            // 清理临时数据
            entity_vars.remove("weapon");
            entity_vars.remove("ammo");
            if (entity_vars.isEmpty()) {
                removeEntityVariables(owner);
            }
        }
    }

    private static void addArrowVariables(AffixContext context, Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow) {
            context.addVariable("arrow_damage", arrow.getBaseDamage());
            context.addVariable("arrow_speed", arrow.getDeltaMovement().length() * 20);
            context.addVariable("pierce_level", arrow.getPierceLevel());
        }
    }

    /**
     * 弹射物发射前监听器
     */
    @SubscribeEvent
    public static void onGetProjectile(LivingGetProjectileEvent event) {
        LivingEntity shooter = event.getEntity();
        ItemStack weapon = event.getProjectileWeaponItemStack();
        ItemStack ammo = event.getProjectileItemStack();

        if (shooter == null) return;

        var entity_vars = getEntityVariables(shooter);
        if (weapon != null) {
            entity_vars.put("weapon", weapon);
        }
        if (ammo != null) {
            entity_vars.put("ammo", ammo);
        }
    }

    /**
     * 玩家速度监听器
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        event.getServer().getPlayerList().getPlayers().forEach(player ->
                lastPlayerPositions.put(player, player.position()));
    }

    // 辅助方法：获取玩家移动向量
    public static Vec3 getPlayerMovement(Player player) {
        Vec3 lastPos = lastPlayerPositions.get(player);
        if (lastPos != null) {
            return lastPos.subtract(player.position());
        }
        return Vec3.ZERO;
    }
}