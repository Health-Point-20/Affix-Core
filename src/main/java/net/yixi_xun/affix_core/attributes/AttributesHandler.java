package net.yixi_xun.affix_core.attributes;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;
import static net.yixi_xun.affix_core.attributes.AffixCoreAttributes.*;

@Mod.EventBusSubscriber
public class AttributesHandler {
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity entity ? entity : null;

        if (attacker == null || attacker.getAttribute(HIT_RATE.get()) == null || target.getAttribute(EVASION.get()) == null) return;
        try {
            double hit_rate = attacker.getAttributeValue(HIT_RATE.get());
            double evasion = target.getAttributeValue(EVASION.get());
            double final_hit_rate = attacker.getAttributeValue(FINAL_HIT_RATE.get());
            double final_evasion = target.getAttributeValue(FINAL_EVASION.get());

            double base_hit_rate = Math.max(hit_rate * (1 - evasion), 0);
            double actual_hit_rate = base_hit_rate + final_hit_rate - final_evasion;

            if (target.getRandom().nextDouble() > actual_hit_rate) {
                event.setCanceled(true);
                if (attacker instanceof Player player) {
                    player.displayClientMessage(Component.translatable("affix_core.hit_miss", target.getDisplayName()), true);
                }
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("计算攻击时出错: {}", e.getMessage());
        }

    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity entity ? entity : null;

        if (attacker != null && attacker.getAttribute(IGNORE_INVINCIBLE_TIME.get()) != null) {
            // 处理忽略无敌时间属性
            double ignoreInvincibleTime = attacker.getAttributeValue(IGNORE_INVINCIBLE_TIME.get());
            double ignoreRate = Math.max((1 - ignoreInvincibleTime), 0);
            target.invulnerableTime = (int) ((target.invulnerableTime - 10) * ignoreRate + 10);
        }


        // 处理物理伤害减免属性
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR) && target.getAttribute(PHYSICAL_DAMAGE_REDUCTION.get()) != null) {
            double physicalReduction = target.getAttributeValue(PHYSICAL_DAMAGE_REDUCTION.get());
            double reductionRate = Math.max((1 - physicalReduction), 0);
            event.setAmount((float)(event.getAmount() * reductionRate));
        }

        if (source.is(DamageTypes.MAGIC)) {
            // 处理魔法伤害减免属性
            if (target.getAttribute(MAGIC_DAMAGE_REDUCTION.get()) != null) {
                double magicReduction = target.getAttributeValue(MAGIC_DAMAGE_REDUCTION.get());
                double reductionRate = Math.max((1 - magicReduction), 0);
                event.setAmount((float) (event.getAmount() * reductionRate));
            }

            // 处理魔法护甲属性
            if (target.getAttribute(MAGIC_ARMOR.get()) != null) {
                float actualDamage = CombatRules.getDamageAfterAbsorb(event.getAmount(), (float) target.getAttributeValue(MAGIC_ARMOR.get()), Float.MAX_VALUE);
                event.setAmount(actualDamage);
            }
        }

        // 处理伤害减免属性
        if (target.getAttribute(DAMAGE_REDUCTION.get()) != null) {
        double damageReduction = target.getAttributeValue(DAMAGE_REDUCTION.get());
        double reductionRate = Math.max((1 - damageReduction), 0);
        event.setAmount((float)(event.getAmount() * reductionRate));
        }
    }
}
