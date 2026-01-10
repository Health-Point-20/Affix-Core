package net.yixi_xun.affix_core.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.mixin.LivingEntityMixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class HurtManager {

    // 存储额外伤害信息的Map，键是目标实体，值是额外伤害数据
    private static final Map<LivingEntity, List<ExtraHurtData>> extraHurtQueue = new ConcurrentHashMap<>();

    private static final Map<LivingEntity, Long> lastDamagingTime = new ConcurrentHashMap<>();

    public record ExtraHurtData(DamageSource source, float damage) { }
    
    /**
     * 添加额外伤害到队列中，等待在onHurt事件中处理
     */
    public static void extraHurt(LivingEntity target, DamageSource source, float damage) {
        // 防止递归添加
        Long lastDamageTime = lastDamagingTime.get(target);
        if (lastDamageTime != null && target.level().getGameTime() == lastDamageTime) {
            return;
        }

        extraHurtQueue.computeIfAbsent(target, k -> new ArrayList<>()).add(new ExtraHurtData(source, damage));
    }
    
    /**
     * 处理单个实体的额外伤害
     */
    private static void processExtraHurt(LivingEntity target, ExtraHurtData data) {
        LivingEntityMixin targetMixin = (LivingEntityMixin) target;
        int origInvulnerableTime = target.invulnerableTime;
        float origHurt = targetMixin.getLastHurt();
        
        try {
            target.invulnerableTime = 0;
            lastDamagingTime.put(target, target.level().getGameTime());
            target.hurt(data.source(), data.damage());
        } finally {
            // 恢复原始状态
            targetMixin.setLastHurt(origHurt);
            target.invulnerableTime = origInvulnerableTime;
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        List<ExtraHurtData> extraHurtDataList = extraHurtQueue.get(target);

        if (extraHurtDataList == null) return;

        // 防止递归调用
        Long lastDamageTime = lastDamagingTime.get(target);
        if (lastDamageTime != null && target.level().getGameTime() == lastDamageTime) {
            return;
        }

        for (ExtraHurtData extraHurtData : extraHurtDataList) {
            if (extraHurtData != null) {
                // 处理额外伤害
                processExtraHurt(target, extraHurtData);
            }
        }
        // 清除此实体的额外伤害数据
        extraHurtQueue.remove(target);
    }
}