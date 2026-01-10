package net.yixi_xun.affix_core.gui.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.yixi_xun.affix_core.affix.operation.IOperation;
import net.yixi_xun.affix_core.gui.widget.TextInputWidget;

import java.util.List;

/**
 * 操作类型的元数据接口，定义了GUI如何处理不同操作类型
 */
public interface OperationMetadata<T extends IOperation> {
    /**
     * 获取操作类型的标识符
     */
    String getOperationType();

    /**
     * 获取操作类型显示的名称
     */
    String getDisplayName();

    /**
     * 获取需要的输入字段定义
     */
    List<InputFieldDefinition> getInputFields();

    /**
     * 从GUI输入中构建操作的NBT数据
     */
    CompoundTag buildOperationNBT(List<TextInputWidget> inputs);

    /**
     * 从操作的NBT数据填充GUI输入字段
     */
    void populateInputs(T operation, List<TextInputWidget> inputs);

    /**
     * 从IOperation实例填充GUI输入字段（通用方法）
     */
    default void populateInputsFromIOperation(IOperation operation, List<TextInputWidget> inputs) {
        if (operation.getClass().isAssignableFrom(getOperationClass())) {
            @SuppressWarnings("unchecked")
            T typedOperation = (T) operation;
            populateInputs(typedOperation, inputs);
        }
    }

    /**
     * 获取操作类型的实际类
     */
    Class<T> getOperationClass();

    /**
     * 输入字段定义
     */
    class InputFieldDefinition {
        public final String label;
        public final String placeholder;
        public final String defaultValue;

        public InputFieldDefinition(String label, String placeholder, String defaultValue) {
            this.label = label;
            this.placeholder = placeholder;
            this.defaultValue = defaultValue;
        }
    }
}