package net.yixi_xun.affix_core.affix.operation;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.*;

import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;
import static net.yixi_xun.affix_core.api.ServerWorkScheduler.queueServerWork;

/**
 * 属性操作，用于修改实体的属性
 */
public class AttributeOperation implements IOperation {
    public static final Map<AttributeModifier, Long> MODIFIERS = new HashMap<>();
    public static final Map<String, Set<AttributeModifier>> APPLIED_MODIFIERS = new HashMap<>();

    private final ResourceLocation attributeId;  // 属性ID
    private final String amountExpression;       // 属性值表达式
    private final AttributeModifier.Operation operation;  // 操作类型
    private final String name;                               // 属性修饰符名称
    private final boolean isPermanent;  // 是否永久生效
    private final String durationExpression;
    private final String target;  // 目标
    private final boolean shouldRemove;  // 是否启用移除功能

    public AttributeOperation(ResourceLocation attributeId, String amountExpression, AttributeModifier.Operation operation, String name, boolean isPermanent, String durationExpression, String target, boolean shouldRemove) {
        this.attributeId = attributeId;
        this.amountExpression = amountExpression;
        this.operation = operation;
        this.name = name;
        this.isPermanent = isPermanent;
        this.durationExpression = durationExpression;
        this.target = target;
        this.shouldRemove = shouldRemove;
    }

    @Override
    public void apply(AffixContext context) {
        // 获取属性
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            return;
        }

        // 计算属性值
        double computedAmount = evaluate(amountExpression, context.getVariables());

        // 创建UUID
        UUID uuid = UUID.nameUUIDFromBytes((name + attributeId.toString() + amountExpression + context.getAffixIndex()).getBytes());

        // 创建属性修饰符
        AttributeModifier modifier = new AttributeModifier(
                uuid,
                name,
                computedAmount,
                operation
        );

        // 根据target确定应用到哪个实体
        var targetEntity = target.equals("self") ? context.getOwner() : context.getTarget();

        // 应用属性修饰符到实体
        var attributeInstance = targetEntity.getAttribute(attribute);
        if (attributeInstance != null && attributeInstance.getModifier(uuid) == null) {
            attributeInstance.addTransientModifier(modifier);
            System.err.println("Applied attribute modifier: " + modifier + " Target" + targetEntity.getDisplayName());

            // 将修饰符添加到跟踪集合中，以便稍后可以移除
            String key = generateKey(context);
            APPLIED_MODIFIERS.computeIfAbsent(key, k -> new HashSet<>()).add(modifier);

            if (!isPermanent) {
                // 计算持续时间
                int duration = (int) evaluate(durationExpression, context.getVariables());
                System.err.println("Duration: " + duration);
                MODIFIERS.put(modifier, context.getWorld().getGameTime() + duration);
                // 持续时间到后移除
                queueServerWork(duration, () -> {
                            if (MODIFIERS.containsKey(modifier)) {
                                System.err.println("Removing attribute modifier: " + modifier.getName());
                                MODIFIERS.remove(modifier);
                                attributeInstance.removeModifier(modifier);

                                // 从跟踪集合中移除
                                Set<AttributeModifier> trackedModifiers = APPLIED_MODIFIERS.get(key);
                                if (trackedModifiers != null) {
                                    trackedModifiers.remove(modifier);
                                    // 如果集合为空，则完全移除该键
                                    if (trackedModifiers.isEmpty()) {
                                        APPLIED_MODIFIERS.remove(key);
                                    }
                                }
                            }
                        }
                );
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
            var targetEntity = target.equals("self") ? context.getOwner() : context.getTarget();
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
                context.getAffixIndex();
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
