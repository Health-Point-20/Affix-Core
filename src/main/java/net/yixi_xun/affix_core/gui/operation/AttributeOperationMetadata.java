package net.yixi_xun.affix_core.gui.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.operation.AttributeOperation;
import net.yixi_xun.affix_core.gui.widget.TextInputWidget;

import java.util.Arrays;
import java.util.List;

public class AttributeOperationMetadata implements OperationMetadata<AttributeOperation> {
    @Override
    public String getOperationType() {
        return "attribute_modifier";
    }

    @Override
    public String getDisplayName() {
        return "属性修改";
    }

    @Override
    public List<InputFieldDefinition> getInputFields() {
        return Arrays.asList(
            new InputFieldDefinition("属性ID", "属性ID (如: generic.attack_damage)", "generic.attack_damage"),
            new InputFieldDefinition("数值表达式", "数值表达式", "0"),
            new InputFieldDefinition("操作类型", "操作类型: 0=add, 1=multiply, 2=multiply_base", "0"),
            new InputFieldDefinition("是否永久", "true/false", "true"),
            new InputFieldDefinition("持续时间表达式", "非永久时有效", "0"),
            new InputFieldDefinition("目标", "目标: self/target", "self")
        );
    }

    @Override
    public CompoundTag buildOperationNBT(List<TextInputWidget> inputs) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getOperationType());
        nbt.putString("Attribute", !inputs.isEmpty() ? inputs.get(0).getValue() : "generic.attack_damage");
        nbt.putString("AmountExpression", inputs.size() > 1 ? inputs.get(1).getValue() : "0");
        nbt.putInt("Operation", inputs.size() > 2 ? Integer.parseInt(inputs.get(2).getValue()) : 0);
        nbt.putBoolean("IsPermanent", inputs.size() <= 3 || Boolean.parseBoolean(inputs.get(3).getValue()));
        nbt.putString("DurationExpression", inputs.size() > 4 ? inputs.get(4).getValue() : "0");
        nbt.putString("Target", inputs.size() > 5 ? inputs.get(5).getValue() : "self");
        return nbt;
    }

    @Override
    public void populateInputs(AttributeOperation operation, List<TextInputWidget> inputs) {
        if (!inputs.isEmpty()) {
            inputs.get(0).setValue(operation.toNBT().getString("Attribute"));
        }
        if (inputs.size() > 1) {
            inputs.get(1).setValue(operation.toNBT().getString("AmountExpression"));
        }
        if (inputs.size() > 2) {
            inputs.get(2).setValue(String.valueOf(operation.toNBT().getInt("Operation")));
        }
        if (inputs.size() > 3) {
            inputs.get(3).setValue(String.valueOf(operation.toNBT().getBoolean("IsPermanent")));
        }
        if (inputs.size() > 4) {
            inputs.get(4).setValue(operation.toNBT().getString("DurationExpression"));
        }
        if (inputs.size() > 5) {
            inputs.get(5).setValue(operation.toNBT().getString("Target"));
        }
    }

    @Override
    public Class<AttributeOperation> getOperationClass() {
        return AttributeOperation.class;
    }
}