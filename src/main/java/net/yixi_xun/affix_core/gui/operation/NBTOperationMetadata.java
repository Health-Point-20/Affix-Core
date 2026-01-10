package net.yixi_xun.affix_core.gui.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.operation.NBTOperation;
import net.yixi_xun.affix_core.gui.widget.TextInputWidget;

import java.util.Arrays;
import java.util.List;

public class NBTOperationMetadata implements OperationMetadata<NBTOperation> {
    @Override
    public String getOperationType() {
        return "nbt_modify";
    }

    @Override
    public String getDisplayName() {
        return "NBT修改";
    }

    @Override
    public List<InputFieldDefinition> getInputFields() {
        return Arrays.asList(
            new InputFieldDefinition("NBT路径", "NBT路径 (如: Affixes.custom_value 或 custom_value)", ""),
            new InputFieldDefinition("值表达式", "值表达式", "0"),
            new InputFieldDefinition("值类型", "值类型: number/string/boolean", "number"),
            new InputFieldDefinition("操作模式", "操作模式: add/modify/delete", "add"),
            new InputFieldDefinition("写入目标", "写入目标: item/owner/attacker/victim", "item")
        );
    }

    @Override
    public CompoundTag buildOperationNBT(List<TextInputWidget> inputs) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getOperationType());
        nbt.putString("NBTPath", !inputs.isEmpty() ? inputs.get(0).getValue() : "");
        nbt.putString("ValueExpression", inputs.size() > 1 ? inputs.get(1).getValue() : "0");
        nbt.putString("ValueType", inputs.size() > 2 ? inputs.get(2).getValue() : "number");
        nbt.putString("OperationMode", inputs.size() > 3 ? inputs.get(3).getValue() : "add");
        nbt.putString("Target", inputs.size() > 4 ? inputs.get(4).getValue() : "item");
        return nbt;
    }

    @Override
    public void populateInputs(NBTOperation operation, List<TextInputWidget> inputs) {
        if (!inputs.isEmpty()) {
            inputs.get(0).setValue(operation.toNBT().getString("NBTPath"));
        }
        if (inputs.size() > 1) {
            inputs.get(1).setValue(operation.toNBT().getString("ValueExpression"));
        }
        if (inputs.size() > 2) {
            inputs.get(2).setValue(operation.toNBT().getString("ValueType"));
        }
        if (inputs.size() > 3) {
            inputs.get(3).setValue(operation.toNBT().getString("OperationMode"));
        }
        if (inputs.size() > 4) {
            inputs.get(4).setValue(operation.toNBT().getString("Target"));
        }
    }

    @Override
    public Class<NBTOperation> getOperationClass() {
        return NBTOperation.class;
    }
}