package net.yixi_xun.affix_core.gui.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.operation.PotionOperation;
import net.yixi_xun.affix_core.gui.widget.TextInputWidget;

import java.util.Arrays;
import java.util.List;

public class PotionOperationMetadata implements OperationMetadata<PotionOperation> {
    @Override
    public String getOperationType() {
        return "add_potion";
    }

    @Override
    public String getDisplayName() {
        return "药水效果";
    }

    @Override
    public List<InputFieldDefinition> getInputFields() {
        return Arrays.asList(
            new InputFieldDefinition("药水ID", "药水效果ID (如: minecraft:speed)", "minecraft:speed"),
            new InputFieldDefinition("持续时间表达式", "持续时间表达式", "100"),
            new InputFieldDefinition("等级表达式", "等级表达式", "0"),
            new InputFieldDefinition("目标", "目标: self/target", "target")
        );
    }

    @Override
    public CompoundTag buildOperationNBT(List<TextInputWidget> inputs) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getOperationType());
        nbt.putString("Effect", inputs.size() > 0 ? inputs.get(0).getValue() : "minecraft:speed");
        nbt.putString("DurationExpression", inputs.size() > 1 ? inputs.get(1).getValue() : "100");
        nbt.putString("AmplifierExpression", inputs.size() > 2 ? inputs.get(2).getValue() : "0");
        nbt.putString("Target", inputs.size() > 3 ? inputs.get(3).getValue() : "target");
        return nbt;
    }

    @Override
    public void populateInputs(PotionOperation operation, List<TextInputWidget> inputs) {
        if (inputs.size() > 0) {
            inputs.get(0).setValue(operation.toNBT().getString("Effect"));
        }
        if (inputs.size() > 1) {
            inputs.get(1).setValue(operation.toNBT().getString("DurationExpression"));
        }
        if (inputs.size() > 2) {
            inputs.get(2).setValue(operation.toNBT().getString("AmplifierExpression"));
        }
        if (inputs.size() > 3) {
            inputs.get(3).setValue(operation.toNBT().getString("Target"));
        }
    }

    @Override
    public Class<PotionOperation> getOperationClass() {
        return PotionOperation.class;
    }
}