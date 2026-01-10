package net.yixi_xun.affix_core.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.yixi_xun.affix_core.mixin.LivingEntityMixin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HurtManager {

    private static final Map<LivingEntity,Boolean> damagingEntity = new ConcurrentHashMap<>();

    public static void extraHurt(LivingEntity target, DamageSource source, float damage) {
        LivingEntityMixin targetMixin = (LivingEntityMixin) target;
        int origInvulnerableTime = target.invulnerableTime;
        float origHurt = targetMixin.getLastHurt();

        if (damagingEntity.containsKey(target)) {return;}

        target.invulnerableTime = 0;
        damagingEntity.put(target, true);
        target.hurt(source, damage);
        damagingEntity.remove(target);

        targetMixin.setLastHurt(origHurt);
        target.invulnerableTime = origInvulnerableTime;
    }
}