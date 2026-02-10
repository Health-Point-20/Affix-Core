package net.yixi_xun.affix_core.attributes;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
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
}
