package net.yixi_xun.affix_core.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.affix.Affix;
import net.yixi_xun.affix_core.gui.BaseScreen;
import net.yixi_xun.affix_core.gui.BaseWidget;
import net.yixi_xun.affix_core.gui.RenderUtils;
import net.yixi_xun.affix_core.gui.operation.OperationMetadata;
import net.yixi_xun.affix_core.gui.operation.OperationMetadataManager;
import net.yixi_xun.affix_core.gui.widget.CycleButton;
import net.yixi_xun.affix_core.gui.widget.SimpleButton;
import net.yixi_xun.affix_core.gui.widget.TextInputWidget;
import net.yixi_xun.affix_core.network.NetworkManager;
import net.yixi_xun.affix_core.network.UpdateAffixC2SPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.yixi_xun.affix_core.affix.operation.OperationManager.getFactoryMap;

public class AffixEditorScreen extends BaseScreen {
    private final Affix originalAffix;
    private final Consumer<CompoundTag> saveCallback;
    private final int slotIndex;
    private TextInputWidget triggerInput;
    private List<TextInputWidget> inputWidgets; // 替换原来的特定输入框变量
    private SimpleButton saveButton; // 保存按钮引用
    private ToastWidget toastWidget; // 新增：用于显示toast消息

    // 简单的状态记录
    private String currentOperationType;
    private boolean hasUnsavedChanges = false; // 新增：跟踪是否有未保存的更改
    private CompoundTag initialNbt; // 新增：初始NBT快照，用于脏检查

    // 新增：Toast消息显示组件
    private class ToastWidget extends BaseWidget {
        private String toastMessage = "";
        private long toastStartTime = 0;
        private static final long TOAST_DURATION = 3000; // 3秒
        
        public ToastWidget() {
            super(0, 0, 200, 20, Component.empty());
        }
        
        public void showToast(String message) {
            this.toastMessage = message;
            this.toastStartTime = System.currentTimeMillis();
        }
        
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (!toastMessage.isEmpty() && (System.currentTimeMillis() - toastStartTime < TOAST_DURATION)) {
                int toastX = (width - 200) / 2;
                int toastY = 30;
                
                // 绘制toast背景
                RenderUtils.drawBorderRect(guiGraphics, toastX, toastY, 200, 20, 0xFF555555, 0xAA000000);
                guiGraphics.drawCenteredString(font, toastMessage, toastX + 100, toastY + 5, 0xFFFFFF);
            }
        }
    }

    public AffixEditorScreen(Affix affix, int slotIndex, Consumer<CompoundTag> saveCallback) {
        super(Component.translatable("gui.affix_core.affix_editor.title"));
        this.originalAffix = affix;
        this.slotIndex = slotIndex;
        this.saveCallback = saveCallback;
    }

    @Override
    public void init() {
        super.init();
        clearWidgets();
        layout(); // 调用布局方法
    }
    
    @Override
    protected void layout() {
        int centerX = width / 2;
        int startY = (int) (height * 0.05); // 5%高度作为起始位置

        // 标题
        addWidget(new BaseWidget((int)(centerX - width * 0.15), (int) (height * 0.02), (int) (width * 0.3), 20, Component.literal(Component.translatable("gui.affix_core.affix_editor.title").getString())) {
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.drawCenteredString(font, message.getString(), centerX, (int) (height * 0.025), 0xFFFF00);
            }
        });

        // 1. 触发器编辑
        int inputWidth = Math.min(400, (int) (width * 0.4)); // 最大宽度不超过400px或屏幕宽度的40%
        addWidget(new TextInputWidget(centerX - inputWidth / 2, startY, inputWidth, 24, Component.translatable("gui.affix_core.affix_editor.trigger")));
        triggerInput = (TextInputWidget) widgets.get(widgets.size() - 1);
        if (originalAffix != null) {
            triggerInput.setValue(originalAffix.trigger());
        }
        // 设置触发器验证器
        triggerInput.setValidator(TextInputWidget.createLengthValidator(64));
        triggerInput.setPlaceholder(Component.translatable("gui.affix_core.placeholder.trigger")); // 新增：占位符
        triggerInput.setResponder(s -> updateUnsavedChanges()); // 新增：跟踪更改

        // 2. 操作类型选择 (使用循环按钮)
        startY += 35;
        List<String> operationTypes = new ArrayList<>(getFactoryMap().keySet());
        CycleButton operationTypeButton = new CycleButton(centerX - inputWidth / 2, startY, inputWidth, 20, operationTypes, this::confirmRebuildInputs);
        addWidget(operationTypeButton);
        
        // 如果有原始操作，设置初始值
        if (originalAffix != null) {
            operationTypeButton.setCurrentOption(originalAffix.operation().getType());
            this.currentOperationType = originalAffix.operation().getType();
        } else {
            this.currentOperationType = operationTypeButton.getCurrentOption();
        }

        // 3. 初始化特定字段
        String initialType = originalAffix != null ? originalAffix.operation().getType() : operationTypeButton.getCurrentOption();
        rebuildInputs(initialType);

        // 4. 保存按钮
        saveButton = new SimpleButton(centerX - 50, (int) (height * 0.9), 100, 20, Component.translatable("gui.affix_core.button.save"), btn -> {
            if (validateAndSave()) {
                onClose();
            }
        });
        addWidget(saveButton);
        
        // 初始验证
        validateInputs();
        
        // 创建初始NBT快照
        createInitialNbtSnapshot();
        
        // 新增：Toast消息显示组件
        toastWidget = new ToastWidget();
        addWidget(toastWidget);
    }

    // 新增：创建初始NBT快照
    private void createInitialNbtSnapshot() {
        initialNbt = new CompoundTag();
        if (originalAffix != null) {
            initialNbt.putString("Trigger", originalAffix.trigger());
            CompoundTag opTag = new CompoundTag();
            opTag.putString("Type", originalAffix.operation().getType());
            initialNbt.put("Operation", opTag);
        }
    }

    // 新增：更新未保存更改标志
    private void updateUnsavedChanges() {
        hasUnsavedChanges = !isCurrentStateEqualToInitial();
    }

    // 新增：检查当前状态是否等于初始状态
    private boolean isCurrentStateEqualToInitial() {
        if (initialNbt == null) return false;
        
        CompoundTag currentNbt = getCurrentStateAsNbt();
        return initialNbt.equals(currentNbt);
    }

    // 新增：获取当前状态为NBT
    private CompoundTag getCurrentStateAsNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Trigger", triggerInput != null ? triggerInput.getValue() : "");
        
        CompoundTag opTag = new CompoundTag();
        opTag.putString("Type", currentOperationType != null ? currentOperationType : "");
        nbt.put("Operation", opTag);
        
        return nbt;
    }

    // 新增：确认重建输入（切换操作类型前的确认）
    private void confirmRebuildInputs(String newType) {
        if (hasUnsavedChanges) {
            // 如果有未保存的更改，询问用户是否继续
            // 在实际应用中，这里应该显示一个确认对话框
            // 为了演示，我们暂时直接切换类型，但会显示一个toast提醒
            toastWidget.showToast(Component.translatable("gui.affix_core.toast.changes_will_be_lost").getString());
        }
        rebuildInputs(newType);
    }

    // 新增：验证所有输入并保存
    private boolean validateAndSave() {
        if (!validateInputs()) {
            // 显示错误信息
            toastWidget.showToast(Component.translatable("gui.affix_core.error.validation_failed").getString());
            return false; // 验证失败，不保存
        }
        
        if (!saveAffix()) { // 修改：保存方法返回布尔值
            return false;
        }
        
        return true;
    }
    
    // 新增：验证所有输入
    private boolean validateInputs() {
        boolean allValid = true;
        
        // 验证触发器
        if (triggerInput != null) {
            triggerInput.validateInput();
            if (!triggerInput.isValid()) {
                allValid = false;
            }
        }
        
        // 验证操作参数
        if (inputWidgets != null) {
            for (TextInputWidget widget : inputWidgets) {
                widget.validateInput();
                if (!widget.isValid()) {
                    allValid = false;
                }
            }
        }
        
        // 更新保存按钮状态
        if (saveButton != null) {
            saveButton.setActive(allValid);
        }
        
        return allValid;
    }

    private void rebuildInputs(String type) {
        this.currentOperationType = type;

        // 移除之前的输入组件（保留标题、触发器输入、操作类型按钮和保存按钮）
        // 保留前3个组件（标题、触发器输入、操作类型按钮）和最后一个组件（保存按钮）
        List<BaseWidget> preservedWidgets = new ArrayList<>();
        for (int i = 0; i < Math.min(3, widgets.size()); i++) {
            preservedWidgets.add(widgets.get(i));
        }
        if (!widgets.isEmpty()) {
            preservedWidgets.add(widgets.get(widgets.size() - 1)); // 保存按钮
        }
        
        widgets.clear();
        widgets.addAll(preservedWidgets);

        int startY = (int) (height * 0.2); // 20%高度开始放置输入框
        int centerX = width / 2;
        int inputWidth = Math.min(400, (int) (width * 0.4)); // 最大宽度不超过400px或屏幕宽度的40%

        // 使用元数据系统处理操作类型
        OperationMetadata<?> metadata = OperationMetadataManager.getMetadata(type);
        if (metadata != null) {
            inputWidgets = new ArrayList<>();
            
            // 根据元数据生成输入框
            int currentIndex = 0;
            for (var fieldDef : metadata.getInputFields()) {
                addWidget(new TextInputWidget(centerX - inputWidth / 2, startY + currentIndex * 40, inputWidth, 24, 
                    Component.literal(fieldDef.label)));
                TextInputWidget widget = (TextInputWidget) widgets.get(widgets.size() - 1);
                widget.setValue(fieldDef.defaultValue);
                
                // 设置占位符
                widget.setPlaceholder(Component.translatable("gui.affix_core.placeholder.field", fieldDef.label));
                
                // 根据字段类型设置验证器（这里可以根据元数据中的字段信息设置适当的验证器）
                // 暂时使用长度验证器作为示例
                widget.setValidator(TextInputWidget.createLengthValidator(256));
                
                // 添加响应器来跟踪更改
                widget.setResponder(s -> updateUnsavedChanges());
                
                inputWidgets.add(widget);
                currentIndex++;
            }
            
            // 如果有原始操作，填充输入框
            if (originalAffix != null && originalAffix.operation().getType().equals(type)) {
                // 使用元数据系统来填充输入框
                metadata.populateInputsFromIOperation(originalAffix.operation(), inputWidgets);
            }
        } else {
            // 处理未知操作类型
            addRenderableText(centerX - inputWidth / 2, startY, Component.translatable("gui.affix_core.unknown_operation", type));
        }
        
        // 重新验证输入
        validateInputs();
    }

    // 辅助：添加不可编辑的提示文本
    private void addRenderableText(int x, int y, Component textComponent) {
        addWidget(new BaseWidget(x, y, 150, 20, textComponent) {
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.drawString(font, message.getString(), x, y, 0xAAAAAA);
            }
        });
    }

    private boolean saveAffix() {
        // 这里构建新的 NBT
        CompoundTag newNbt = new CompoundTag();
        newNbt.putString("Trigger", triggerInput.getValue());
        
        CompoundTag opTag;
        
        // 使用元数据系统构建操作NBT
        OperationMetadata<?> metadata = OperationMetadataManager.getMetadata(currentOperationType);
        if (metadata != null) {
            // 添加try-catch处理构建NBT过程中的异常
            try {
                opTag = metadata.buildOperationNBT(inputWidgets != null ? inputWidgets : new ArrayList<>());
            } catch (Exception e) {
                // 如果构建NBT失败，显示错误信息
                System.err.println("Failed to build operation NBT: " + e.getMessage());
                
                // 在UI上显示错误
                toastWidget.showToast(Component.translatable("gui.affix_core.error.failed_to_build_nbt").getString());
                
                return false; // 保存失败
            }
        } else {
            // 退回到基本处理
            opTag = new CompoundTag();
            opTag.putString("Type", currentOperationType);
            if (inputWidgets != null && !inputWidgets.isEmpty()) {
                opTag.putString("Amount", inputWidgets.get(0).getValue());
            }
        }
        
        newNbt.put("Operation", opTag);
        
        // 如果有有效的槽位索引，使用网络包发送到服务器
        if (slotIndex != -1 && minecraft != null && minecraft.player != null) {
            // 添加物品校验信息到NBT
            var itemInSlot = minecraft.player.getInventory().getItem(slotIndex);
            if (!itemInSlot.isEmpty()) {
                newNbt.putString("ExpectedItemId", ForgeRegistries.ITEMS.getKey(itemInSlot.getItem()).toString());
                newNbt.putInt("ExpectedItemCount", itemInSlot.getCount());
            }
            
            NetworkManager.CHANNEL.sendToServer(new UpdateAffixC2SPacket(slotIndex, newNbt, true));
            // 关闭屏幕
            minecraft.setScreen(null);
        } else {
            saveCallback.accept(newNbt);
        }
        
        // 重置未保存更改标志
        hasUnsavedChanges = false;
        
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
       if (keyCode == 256 /* Escape key */) { // Esc键
           if (hasUnsavedChanges) {
               // 如果有未保存的更改，询问用户是否放弃
               // 在实际实现中，这里应该显示一个确认对话框
               // 为了演示，我们暂时直接关闭
               toastWidget.showToast(Component.translatable("gui.affix_core.toast.unsaved_changes_discarded").getString());
           }
           onClose();
            return true;
        }
        
        // 检查Ctrl+S (保存)
        if (keyCode == 33 && (modifiers & 2) != 0) { // S键 + Control
            validateAndSave();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    // 新增：处理关闭事件，如果有未保存的更改则提示
    @Override
    public void onClose() {
        if (hasUnsavedChanges) {
            // 在实际实现中，这里应该显示一个确认对话框
            // 为了演示，我们暂时直接关闭
            // 可以考虑使用Minecraft的内置确认GUI
        }
        super.onClose();
    }
}