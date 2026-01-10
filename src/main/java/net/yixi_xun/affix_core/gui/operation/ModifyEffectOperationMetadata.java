package net.yixi_xun.affix_core.gui.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.operation.ModifyEffectOperation;
import net.yixi_xun.affix_core.gui.widget.TextInputWidget;

import java.util.Arrays;
import java.util.List;

public class ModifyEffectOperationMetadata implements OperationMetadata<ModifyEffectOperation> {
    @Override
    public String getOperationType() {
        return "on_effect_add";
    }

    @Override
    public String getDisplayName() {
        return "效果添加";
    }

    @Override
    public List<InputFieldDefinition> getInputFields() {
        return Arrays.asList(
            new InputFieldDefinition("持续时间表达式", "持续时间表达式", "100"),
            new InputFieldDefinition("等级表达式", "等级表达式", "0")
        );
    }

    @Override
    public CompoundTag buildOperationNBT(List<TextInputWidget> inputs) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getOperationType());
        nbt.putString("DurationExpression", inputs.size() > 0 ? inputs.get(0).getValue() : "100");
        nbt.putString("AmplifierExpression", inputs.size() > 1 ? inputs.get(1).getValue() : "0");
        return nbt;
    }

    @Override
    public void populateInputs(ModifyEffectOperation operation, List<TextInputWidget> inputs) {
        if (inputs.size() > 0) {
            inputs.get(0).setValue(operation.toNBT().getString("DurationExpression"));
        }
        if (inputs.size() > 1) {
            inputs.get(1).setValue(operation.toNBT().getString("AmplifierExpression"));
        }
    }

    @Override
    public Class<ModifyEffectOperation> getOperationClass() {
        return ModifyEffectOperation.class;
    }
}