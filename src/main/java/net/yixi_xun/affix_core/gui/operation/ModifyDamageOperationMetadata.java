package net.yixi_xun.affix_core.gui.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.operation.ModifyDamageOperation;
import net.yixi_xun.affix_core.gui.widget.TextInputWidget;

import java.util.Arrays;
import java.util.List;

public class ModifyDamageOperationMetadata implements OperationMetadata<ModifyDamageOperation> {
    @Override
    public String getOperationType() {
        return "modify_damage";
    }

    @Override
    public String getDisplayName() {
        return "伤害修改";
    }

    @Override
    public List<InputFieldDefinition> getInputFields() {
        return Arrays.asList(
            new InputFieldDefinition("伤害表达式", "伤害计算公式 (如: damage * 1.5)", "damage"),
            new InputFieldDefinition("操作类型", "add/multiply/set", "set")
        );
    }

    @Override
    public CompoundTag buildOperationNBT(List<TextInputWidget> inputs) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getOperationType());
        nbt.putString("Amount", !inputs.isEmpty() ? inputs.get(0).getValue() : "damage");
        nbt.putString("Operation", inputs.size() > 1 ? inputs.get(1).getValue() : "set");
        return nbt;
    }

    @Override
    public void populateInputs(ModifyDamageOperation operation, List<TextInputWidget> inputs) {
        if (!inputs.isEmpty()) {
            inputs.get(0).setValue(operation.toNBT().getString("Amount"));
        }
        if (inputs.size() > 1) {
            inputs.get(1).setValue(operation.toNBT().getString("Operation"));
        }
    }

    @Override
    public Class<ModifyDamageOperation> getOperationClass() {
        return ModifyDamageOperation.class;
    }
}