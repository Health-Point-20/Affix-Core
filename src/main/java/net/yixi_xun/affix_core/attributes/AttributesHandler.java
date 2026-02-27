package net.yixi_xun.affix_core.attributes;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AttributesHandler {
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity entity ? entity : null;

        if (attacker == null) return;
        double hit_rate = attacker.getAttributeValue(AffixCoreAttributes.HIT_RATE.get());
        double evasion = target.getAttributeValue(AffixCoreAttributes.EVASION.get());
        double final_hit_rate = attacker.getAttributeValue(AffixCoreAttributes.FINAL_HIT_RATE.get());
        double final_evasion = target.getAttributeValue(AffixCoreAttributes.FINAL_EVASION.get());

        double base_hit_rate = Math.max(hit_rate * (1 - evasion), 0);
        double actual_hit_rate = base_hit_rate + final_hit_rate - final_evasion;

        if (target.getRandom().nextDouble() > actual_hit_rate) {
            event.setCanceled(true);
            if (attacker instanceof Player player) {
                player.displayClientMessage(Component.translatable("affix_core.hit_miss", target.getDisplayName()), true);
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity entity ? entity : null;

        if (attacker != null) {
            // 处理忽略无敌时间属性
            double ignoreInvincibleTime = attacker.getAttributeValue(AffixCoreAttributes.IGNORE_INVINCIBLE_TIME.get());
            double ignoreRate = Math.max((1 - ignoreInvincibleTime), 0);
            target.invulnerableTime = (int) ((target.invulnerableTime - 10) * ignoreRate + 10);
        }


        // 处理物理伤害减免属性
        if (source.is(DamageTypes.GENERIC)) {
            double physicalReduction = target.getAttributeValue(AffixCoreAttributes.PHYSICAL_DAMAGE_REDUCTION.get());
            double reductionRate = Math.max((1 - physicalReduction), 0);
            event.setAmount((float)(event.getAmount() * reductionRate));
        }

        if (source.is(DamageTypes.MAGIC)) {
            // 处理魔法伤害减免属性
            double magicReduction = target.getAttributeValue(AffixCoreAttributes.MAGIC_DAMAGE_REDUCTION.get());
            double reductionRate = Math.max((1 - magicReduction), 0);
            event.setAmount((float)(event.getAmount() * reductionRate));

            // 处理魔法护甲属性
            float actualDamage = CombatRules.getDamageAfterAbsorb(event.getAmount(), (float) target.getAttributeValue(AffixCoreAttributes.MAGIC_ARMOR.get()), Float.MAX_VALUE);
            event.setAmount(actualDamage);
        }

        // 处理伤害减免属性
        double damageReduction = target.getAttributeValue(AffixCoreAttributes.DAMAGE_REDUCTION.get());
        double reductionRate = Math.max((1 - damageReduction), 0);
        event.setAmount((float)(event.getAmount() * reductionRate));
    }
}
