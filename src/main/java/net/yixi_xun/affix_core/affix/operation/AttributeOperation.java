package net.yixi_xun.affix_core.affix.operation;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.*;

import static net.yixi_xun.affix_core.AffixCoreMod.queueServerWork;
import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;

/**
 * 属性操作，用于修改实体的属性
 */
public class AttributeOperation implements IOperation {
    public static final Map<AttributeModifier, Long> MODIFIERS = new HashMap<>();

    private final ResourceLocation attributeId;  // 属性ID
    private final String amountExpression;       // 属性值表达式
    private final AttributeModifier.Operation operation;  // 操作类型
    private final String name;                               // 属性修饰符名称
    private final boolean isPermanent;  // 是否永久生效
    private final String durationExpression;
    
    public AttributeOperation(ResourceLocation attributeId, String amountExpression, AttributeModifier.Operation operation, String name, boolean isPermanent, String durationExpression) {
        this.attributeId = attributeId;
        this.amountExpression = amountExpression;
        this.operation = operation;
        this.name = name;
        this.isPermanent = isPermanent;
        this.durationExpression = durationExpression;
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

        // 应用属性修饰符到实体
        var attributeInstance = context.getOwner().getAttribute(attribute);
        if (attributeInstance != null && attributeInstance.getModifier(uuid) == null) {
            attributeInstance.addTransientModifier(modifier);

            if (!isPermanent) {
                // 计算持续时间
                int duration = (int) evaluate(durationExpression, context.getVariables());
                MODIFIERS.put(modifier, context.getWorld().getGameTime() + duration);
                // 持续时间到后移除
                queueServerWork(duration, () -> {
                            if (MODIFIERS.get(modifier) <= context.getWorld().getGameTime()) {
                                MODIFIERS.remove(modifier);
                                attributeInstance.removeModifier(modifier);
                            }
                        }
                );
            }
        }
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Attribute", attributeId.toString());
        nbt.putString("AmountExpression", amountExpression);
        nbt.putInt("Operation", operation.toValue());
        if (name != null) nbt.putString("Name", name);
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
        String attributeStr = nbt.getString("Attribute");
        ResourceLocation attributeId = ResourceLocation.parse(attributeStr);

        String amountExpression = nbt.contains("AmountExpression") ? nbt.getString("AmountExpression") : "0";
        int operationInt = nbt.contains("Operation") ? nbt.getInt("Operation") : 0;
        AttributeModifier.Operation operation = AttributeModifier.Operation.fromValue(operationInt);
        
        String name = nbt.contains("Name") ? nbt.getString("Name") : "Affix Attribute Modifier";
        boolean isPermanent = nbt.contains("IsPermanent") && nbt.getBoolean("IsPermanent");
        String durationExpression = nbt.contains("DurationExpression") ? nbt.getString("DurationExpression") : "0";

        return new AttributeOperation(attributeId, amountExpression, operation, name, isPermanent, durationExpression);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("attribute_modifier", AttributeOperation::fromNBT);
    }
}
