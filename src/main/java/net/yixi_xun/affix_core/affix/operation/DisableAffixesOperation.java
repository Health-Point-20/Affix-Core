package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.api.AffixEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class DisableAffixesOperation extends BaseOperation {
    // 使用线程安全的集合避免并发问题
    private static final Map<String, Set<String>> APPLIED_DISABLES = new ConcurrentHashMap<>();
    
    private static final String DISABLE_AFFIXES_TAG = "DisableAffixes";
    private static final String ALL_OPERATIONS_KEY = "all";
    
    private final long priority;
    private final String disableOperations;
    private final String target;

    public DisableAffixesOperation(Long priority, String disableOperations, String target) {
        this.priority = priority != null ? priority : 0L;
        this.disableOperations = disableOperations != null ? disableOperations : "all";
        this.target = target != null ? target : "target";
    }

    @SubscribeEvent
    public static void onAffixExecute(AffixEvent.AffixExecuteEvent event) {
        // 空值检查
        if (event.getAffix() == null || event.getContext() == null || event.getContext().getOwner() == null) {
            return;
        }
        
        String operationType = event.getAffix().operation().getType();
        long affixPriority = event.getAffix().priority();
        CompoundTag disabled = event.getContext().getOwner().getPersistentData().getCompound(DISABLE_AFFIXES_TAG);
        
        // 检查特定操作类型是否被禁用
        if (shouldCancelExecution(disabled, operationType, affixPriority)) {
            event.setCanceled(true);
            return;
        }
        
        // 检查是否禁用所有操作
        if (shouldCancelExecution(disabled, ALL_OPERATIONS_KEY, affixPriority)) {
            event.setCanceled(true);
        }
    }
    
    /**
     * 判断是否应该取消词缀执行
     * @param disabled 禁用配置标签
     * @param key 检查的键名
     * @param affixPriority 词缀优先级
     * @return 是否应该取消执行
     */
    private static boolean shouldCancelExecution(CompoundTag disabled, String key, long affixPriority) {
        if (!disabled.contains(key)) {
            return false;
        }
        
        long disabledPriority = disabled.getLong(key);
        boolean shouldCancel = disabledPriority >= affixPriority;
        
        if (shouldCancel) {
            AffixCoreMod.LOGGER.debug("Affix execution canceled due to disabled operation: {} with priority: {} >= affix priority: {}", 
                key, disabledPriority, affixPriority);
        }
        
        return shouldCancel;
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        LivingEntity target = getTargetEntity(context, this.target);
        if (isInValidEntity(target)) {
            return;
        }
        
        try {
            CompoundTag data = target.getPersistentData();
            CompoundTag disabled = data.getCompound(DISABLE_AFFIXES_TAG);
            
            String[] operations = processDisableOperations(disabled);
            data.put(DISABLE_AFFIXES_TAG, disabled);
            
            String key = generateKey(context, target);
            recordAppliedDisables(key, operations);
        } catch (Exception e) {
            AffixCoreMod.LOGGER.error("应用禁用词缀操作时发生错误", e);
        }
    }

    /**
     * 处理禁用操作配置
     */
    private String[] processDisableOperations(CompoundTag disabled) {
        String[] operations;
        if ("all".equals(disableOperations)) {
            operations = new String[]{ALL_OPERATIONS_KEY};
            disabled.putLong(ALL_OPERATIONS_KEY, priority);
        } else {
            operations = disableOperations.split(",");
            for (String operation : operations) {
                String trimmedOp = operation.trim();
                if (!trimmedOp.isEmpty()) {
                    disabled.putLong(trimmedOp, priority);
                }
            }
        }
        return operations;
    }

    /**
     * 记录已应用的禁用操作
     */
    private void recordAppliedDisables(String key, String[] operations) {
        Set<String> disabledOps = APPLIED_DISABLES.computeIfAbsent(key, 
            k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        for (String operation : operations) {
            String trimmedOp = operation.trim();
            if (!trimmedOp.isEmpty()) {
                disabledOps.add(trimmedOp);
            }
        }
    }

    @Override
    public void remove(AffixContext context) {
        LivingEntity target = getTargetEntity(context, this.target);
        if (target == null) return;
        
        // 获取之前应用的禁用操作记录
        String key = generateKey(context, target);
        Set<String> disabledOps = APPLIED_DISABLES.get(key);
        if (disabledOps == null || disabledOps.isEmpty()) {
            return;
        }
        
        CompoundTag data = target.getPersistentData();
        CompoundTag disabled = data.getCompound(DISABLE_AFFIXES_TAG);
        
        // 移除对应的禁用记录（只有优先级匹配时才移除）
        for (String operation : new HashSet<>(disabledOps)) {
            if (disabled.contains(operation)) {
                long storedPriority = disabled.getLong(operation);
                // 只有当存储的优先级等于当前操作的优先级时才移除
                if (storedPriority == priority) {
                    disabled.remove(operation);
                    disabledOps.remove(operation);
                }
            }
        }
        
        // 如果DisableAffixes标签为空，则完全移除
        if (disabled.getAllKeys().isEmpty()) {
            data.remove(DISABLE_AFFIXES_TAG);
        } else {
            data.put(DISABLE_AFFIXES_TAG, disabled);
        }
        
        // 如果该键下的禁用操作集合为空，则完全移除该键
        if (disabledOps.isEmpty()) {
            APPLIED_DISABLES.remove(key);
        }
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Target", target);
        nbt.putLong("Priority", priority);
        nbt.putString("DisableOperations", disableOperations);
        return nbt;
    }

    @Override
    public String getType() {
        return "disable_affixes";
    }

    /**
     * 生成用于标识此操作应用的唯一键
     */
    private String generateKey(AffixContext context, LivingEntity target) {
        return target.getStringUUID() + "_" + context.getAffix().uuid();
    }

    /**
     * 工厂方法，从NBT创建DisableAffixesOperation
     */
    public static DisableAffixesOperation fromNBT(CompoundTag nbt) {
        Long priority = nbt.contains("Priority") ? nbt.getLong("Priority") : 0L;
        String disableOperations = nbt.contains("DisableOperations") ? nbt.getString("DisableOperations") : "all";
        String target = nbt.contains("Target") ? nbt.getString("Target") : "target";

        return new DisableAffixesOperation(priority, disableOperations, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("disable_affixes", DisableAffixesOperation::fromNBT);
    }
}