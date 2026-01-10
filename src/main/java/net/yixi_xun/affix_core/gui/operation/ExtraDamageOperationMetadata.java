package net.yixi_xun.affix_core.gui.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.operation.DealDamageOperation;
import net.yixi_xun.affix_core.gui.widget.TextInputWidget;

import java.util.Arrays;
import java.util.List;

public class ExtraDamageOperationMetadata implements OperationMetadata<DealDamageOperation> {
    @Override
    public String getOperationType() {
        return "extra_damage";
    }

    @Override
    public String getDisplayName() {
        return "额外伤害";
    }

    @Override
    public List<InputFieldDefinition> getInputFields() {
        return Arrays.asList(
            new InputFieldDefinition("伤害数值表达式", "伤害数值 (如: 5.0)", "1.0"),
            new InputFieldDefinition("伤害类型", "伤害类型: magic/fire/freeze等", "generic")
        );
    }

    @Override
    public CompoundTag buildOperationNBT(List<TextInputWidget> inputs) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getOperationType());
        nbt.putString("Amount", !inputs.isEmpty() ? inputs.get(0).getValue() : "1.0");
        nbt.putString("DamageType", inputs.size() > 1 ? inputs.get(1).getValue() : "generic");
        return nbt;
    }

    @Override
    public void populateInputs(DealDamageOperation operation, List<TextInputWidget> inputs) {
        if (!inputs.isEmpty()) {
            inputs.get(0).setValue(operation.toNBT().getString("Amount"));
        }
        if (inputs.size() > 1) {
            inputs.get(1).setValue(operation.toNBT().getString("DamageType"));
        }
    }

    @Override
    public Class<DealDamageOperation> getOperationClass() {
        return DealDamageOperation.class;
    }
}