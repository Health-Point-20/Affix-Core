package net.yixi_xun.affix_core.gui.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.operation.CommandOperation;
import net.yixi_xun.affix_core.gui.widget.TextInputWidget;

import java.util.Arrays;
import java.util.List;

public class CommandOperationMetadata implements OperationMetadata<CommandOperation> {
    @Override
    public String getOperationType() {
        return "command_execute";
    }

    @Override
    public String getDisplayName() {
        return "执行命令";
    }

    @Override
    public List<InputFieldDefinition> getInputFields() {
        return Arrays.asList(
            new InputFieldDefinition("命令表达式", "要执行的命令 (如: say Hello 或 give @p diamond 1)", "say Hello"),
            new InputFieldDefinition("执行目标", "执行者: server/owner/attacker (默认: server)", "server"),
            new InputFieldDefinition("执行方式", "执行方式: server/player (默认: server)", "server")
        );
    }

    @Override
    public CompoundTag buildOperationNBT(List<TextInputWidget> inputs) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getOperationType());
        nbt.putString("CommandExpression", !inputs.isEmpty() ? inputs.get(0).getValue() : "say Hello");
        nbt.putString("Target", inputs.size() > 1 ? inputs.get(1).getValue() : "server");
        nbt.putString("Executor", inputs.size() > 2 ? inputs.get(2).getValue() : "server");
        return nbt;
    }

    @Override
    public void populateInputs(CommandOperation operation, List<TextInputWidget> inputs) {
        if (!inputs.isEmpty()) {
            inputs.get(0).setValue(operation.toNBT().getString("CommandExpression"));
        }
        if (inputs.size() > 1) {
            inputs.get(1).setValue(operation.toNBT().getString("Target"));
        }
        if (inputs.size() > 2) {
            inputs.get(2).setValue(operation.toNBT().getString("Executor"));
        }
    }

    @Override
    public Class<CommandOperation> getOperationClass() {
        return CommandOperation.class;
    }
}