package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.*;

import static net.yixi_xun.affix_core.AffixCoreMod.queueServerWork;

/**
 * 属性操作，用于修改实体的属性
 */
public class AttributeOperation extends BaseOperation {
    private static final Map<AttributeModifier, Long> MODIFIERS = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Set<AttributeModifier>> APPLIED_MODIFIERS = Collections.synchronizedMap(new HashMap<>());

    private final ResourceLocation attributeId;
    private final String amountExpression;
    private final AttributeModifier.Operation operation;
    private final String name;
    private final boolean isPermanent;
    private final String durationExpression;
    private final String target;
    private final boolean shouldRemove;

    public AttributeOperation(ResourceLocation attributeId, String amountExpression, 
                             AttributeModifier.Operation operation, String name, 
                             boolean isPermanent, String durationExpression, 
                             String target, boolean shouldRemove) {
        this.attributeId = attributeId != null ? attributeId : ResourceLocation.tryParse("generic.attack_damage");
        this.amountExpression = amountExpression != null ? amountExpression : "0";
        this.operation = operation != null ? operation : AttributeModifier.Operation.ADDITION;
        this.name = name != null && !name.isEmpty() ? name : "Affix Attribute Modifier";
        this.isPermanent = isPermanent;
        this.durationExpression = durationExpression != null ? durationExpression : "100";
        this.target = target != null ? target : "self";
        this.shouldRemove = shouldRemove;
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            AffixCoreMod.LOGGER.warn("无效的属性ID: {}", attributeId);
            return;
        }

        double computedAmount = evaluateOrDefaultValue(amountExpression, context.getVariables(), 0.0);
        UUID uuid = generateUUID(context);
        AttributeModifier modifier = new AttributeModifier(uuid, name, computedAmount, operation);

        var targetEntity = getTargetEntity(context, target);
        if (isInValidEntity(targetEntity)) {
            return;
        }

        var attributeInstance = targetEntity.getAttribute(attribute);
        if (attributeInstance != null && attributeInstance.getModifier(uuid) == null) {
            attributeInstance.addTransientModifier(modifier);
            
            String key = generateKey(context);
            synchronized (APPLIED_MODIFIERS) {
                APPLIED_MODIFIERS.computeIfAbsent(key, k -> Collections.synchronizedSet(new HashSet<>())).add(modifier);
            }

            if (!isPermanent) {
                scheduleRemoval(context, modifier, attributeInstance, key);
            }
        }
    }

    /**
     * 生成修饰符UUID
     */
    private UUID generateUUID(AffixContext context) {
        String seed = name + attributeId.toString() + amountExpression + context.getAffix().uuid();
        return UUID.nameUUIDFromBytes(seed.getBytes());
    }

    /**
     * 安排修饰符移除
     */
    private void scheduleRemoval(AffixContext context, AttributeModifier modifier, 
                               AttributeInstance attributeInstance, String key) {
        int duration = Math.max(1, (int) evaluateOrDefaultValue(durationExpression, context.getVariables(), 100));
        MODIFIERS.put(modifier, context.getWorld().getGameTime() + duration);
        
        queueServerWork(duration, () -> {
            try {
                if (MODIFIERS.containsKey(modifier)) {
                    MODIFIERS.remove(modifier);
                    attributeInstance.removeModifier(modifier);
                    removeFromTracking(key, modifier);
                }
            } catch (Exception e) {
                AffixCoreMod.LOGGER.warn("移除属性修饰符时发生错误", e);
            }
        });
    }

    /**
     * 从跟踪集合中移除修饰符
     */
    private void removeFromTracking(String key, AttributeModifier modifier) {
        synchronized (APPLIED_MODIFIERS) {
            Set<AttributeModifier> trackedModifiers = APPLIED_MODIFIERS.get(key);
            if (trackedModifiers != null) {
                trackedModifiers.remove(modifier);
                if (trackedModifiers.isEmpty()) {
                    APPLIED_MODIFIERS.remove(key);
                }
            }
        }
    }

    /**
     * 移除此操作应用的所有属性修饰符
     */
    public void remove(AffixContext context) {
        if (!shouldRemove) {
            return; // 如果不应移除，则直接返回
        }
        String key = generateKey(context);
        Set<AttributeModifier> modifiers = APPLIED_MODIFIERS.get(key);
        if (modifiers == null || modifiers.isEmpty()) {
            return; // 如果没有应用的修饰符，则直接返回
        }
        modifiers = new HashSet<>(modifiers);

        if (!modifiers.isEmpty()) {
            // 获取目标实体和属性
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
            if (attribute == null) {
                return;
            }

            // 使用与应用时相同的目标实体
            var targetEntity = getTargetEntity(context, target);
            if (targetEntity == null) {
                return;
            }

            var attributeInstance = targetEntity.getAttribute(attribute);
            if (attributeInstance != null) {
                // 移除所有相关的修饰符
                for (AttributeModifier modifier : modifiers) {
                    attributeInstance.removeModifier(modifier);

                    // 从全局修饰符映射中也移除（如果存在）
                    MODIFIERS.remove(modifier);
                }
            }

            // 清空跟踪集合
            APPLIED_MODIFIERS.remove(key);
        }
    }

    /**
     * 生成用于标识此操作应用的唯一键
     */
    private String generateKey(AffixContext context) {
        return context.getOwner().getStringUUID() + "_" +
                attributeId.toString() + "_" +
                name + "_" +
                context.getAffix().uuid();
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Attribute", attributeId.toString());
        nbt.putString("AmountExpression", amountExpression);
        nbt.putInt("Operation", operation.toValue());
        if (name != null) nbt.putString("Name", name);
        nbt.putString("Target", target);
        nbt.putBoolean("IsPermanent", isPermanent);
        nbt.putString("DurationExpression", durationExpression);
        nbt.putBoolean("ShouldRemove", shouldRemove);
        return nbt;
    }

    @Override
    public String getType() {
        return "attribute_modifier";
    }

    /**
     * 工厂方法，从NBT创建AttributeOperation
     */
    public static AttributeOperation fromNBT(CompoundTag nbt) {
        String attributeStr = nbt.contains("Attribute") ? nbt.getString("Attribute") : "generic.attack_damage";
        ResourceLocation attributeId = ResourceLocation.tryParse(attributeStr);

        String amountExpression = nbt.contains("AmountExpression") ? nbt.getString("AmountExpression") : "0";
        int operationInt = nbt.contains("Operation") ? nbt.getInt("Operation") : 0;
        AttributeModifier.Operation operation = AttributeModifier.Operation.fromValue(operationInt);

        String name = nbt.contains("Name") ? nbt.getString("Name") : "Affix Attribute Modifier";
        boolean isPermanent = nbt.contains("IsPermanent") && nbt.getBoolean("IsPermanent");
        String durationExpression = nbt.contains("DurationExpression") ? nbt.getString("DurationExpression") : "100";
        String target = nbt.contains("Target") ? nbt.getString("Target") : "self";
        boolean shouldRemove = !nbt.contains("ShouldRemove") || nbt.getBoolean("ShouldRemove");

        return new AttributeOperation(attributeId, amountExpression, operation, name, isPermanent, durationExpression, target, shouldRemove);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("attribute_modifier", AttributeOperation::fromNBT);
    }
}
