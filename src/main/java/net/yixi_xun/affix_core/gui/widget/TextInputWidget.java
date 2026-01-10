package net.yixi_xun.affix_core.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.yixi_xun.affix_core.gui.BaseWidget;
import net.yixi_xun.affix_core.gui.RenderUtils;

import java.util.function.Consumer;

public class TextInputWidget extends BaseWidget {
    private final EditBox editBox;
    private final String label;
    private Validator validator;
    private boolean isValid = true; // 新增：验证状态
    private String errorText = ""; // 新增：错误文本
    private Component placeholder; // 新增：占位符文本
    private int maxLength = -1; // 新增：最大长度限制
    private boolean showMaxLengthIndicator = false; // 新增：是否显示长度指示器

    // 新增：输入验证器接口
    public interface Validator {
        ValidationResult validate(String input);
    }

    // 新增：验证结果类
    public record ValidationResult(boolean isValid, String errorText) {

        public static ValidationResult valid() {
                return new ValidationResult(true, "");
            }

        public static ValidationResult invalid(String errorText) {
                return new ValidationResult(false, errorText);
            }
        }

    public TextInputWidget(int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);
        this.label = title.getString();
        this.editBox = new EditBox(font, x, y + 12, width, height, title);
        this.editBox.setBordered(false);
        this.editBox.setTextColor(0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 显示标签文本
        if (label != null && !label.isEmpty()) {
            guiGraphics.drawString(font, label, x, y, 0xFFFFFF, false);
        }
        
        // 更新EditBox的位置和大小，以防万一它们发生了变化
        editBox.setX(x);
        editBox.setY(y + 15); // 增加垂直间距
        editBox.setWidth(width);
        editBox.setHeight(height - 8); // 减少高度以适应标签
        
        // 绘制背景
        int borderColor = 0xFF555555;
        int bgColor;
        
        if (!isValid) {
            // 验证失败时显示红色背景
            bgColor = 0xFF330000;
            borderColor = 0xFFFF0000;
        } else if (isHovered(mouseX, mouseY) || editBox.isFocused()) {
            bgColor = 0xFF333333;
        } else {
            bgColor = 0xFF222222;
        }
        
        RenderUtils.drawBorderRect(guiGraphics, x, y + 15, width, height - 8, borderColor, bgColor);
        
        // 渲染 EditBox
        editBox.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 如果值为空且设置了占位符，绘制占位符
        if (editBox.getValue().isEmpty() && placeholder != null) {
            guiGraphics.drawString(font, placeholder, x + 4, y + 15 + (height - 8 - 8) / 2, 0x808080);
        }
        
        // 显示错误文本（如果存在）
        if (!isValid && !errorText.isEmpty()) {
            guiGraphics.drawString(font, errorText, x, y + height, 0xFF5555, false);
        }
        
        // 显示最大长度指示器（如果启用）
        if (showMaxLengthIndicator && maxLength > 0) {
            String lengthText = editBox.getValue().length() + "/" + maxLength;
            int textWidth = font.width(lengthText);
            guiGraphics.drawString(font, lengthText, x + width - textWidth, y + height, 0x808080, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return editBox.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // 检查是否超过最大长度限制
        if (maxLength > 0 && editBox.getValue().length() >= maxLength) {
            // 如果已经是最大长度，不允许继续输入字符
            return false;
        }

        boolean result = editBox.charTyped(codePoint, modifiers);
        validateInput(); // 每次字符输入后进行验证
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 处理Home/End键
        if (keyCode == 281 || keyCode == 282) { // GLFW_KEY_HOME or GLFW_KEY_END
            return editBox.keyPressed(keyCode, scanCode, modifiers);
        }
        
        boolean result = editBox.keyPressed(keyCode, scanCode, modifiers);
        validateInput(); // 每次按键后进行验证
        return result;
    }
    
    @Override
    public boolean hasFocus() {
        return editBox.isFocused();
    }
    
    @Override
    public boolean canTakeFocus() {
        return true; // TextInputWidget可以获取焦点
    }
    
    // 新增：设置验证器
    public void setValidator(Validator validator) {
        this.validator = validator;
        validateInput(); // 设置验证器后立即验证
    }
    
    // 新增：执行验证
    public void validateInput() {
        if (validator != null) {
            ValidationResult result = validator.validate(editBox.getValue());
            this.isValid = result.isValid;
            this.errorText = result.errorText;
        } else {
            this.isValid = true;
            this.errorText = "";
        }
    }
    
    // 新增：检查输入是否有效
    public boolean isValid() {
        return isValid;
    }
    
    // 新增：获取错误文本
    public String getErrorText() {
        return errorText;
    }
    
    // 新增：设置占位符文本
    public void setPlaceholder(Component placeholder) {
        this.placeholder = placeholder;
    }
    
    // 新增：设置最大长度
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
    
    // 新增：启用/禁用最大长度指示器
    public void setShowMaxLengthIndicator(boolean show) {
        this.showMaxLengthIndicator = show;
    }
    
    // 新增：静态工厂方法创建常见验证器
    public static Validator createIntegerValidator(int min, int max) {
        return input -> {
            if (input.isEmpty()) {
                return ValidationResult.invalid(Component.translatable("gui.affix_core.validation.required").getString());
            }
            
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    return ValidationResult.invalid(Component.translatable("gui.affix_core.validation.integer_range", min, max).getString());
                }
                return ValidationResult.valid();
            } catch (NumberFormatException e) {
                return ValidationResult.invalid(Component.translatable("gui.affix_core.validation.integer_format").getString());
            }
        };
    }
    
    public static Validator createDoubleValidator(double min, double max) {
        return input -> {
            if (input.isEmpty()) {
                return ValidationResult.invalid(Component.translatable("gui.affix_core.validation.required").getString());
            }
            
            try {
                double value = Double.parseDouble(input);
                if (value < min || value > max) {
                    return ValidationResult.invalid(Component.translatable("gui.affix_core.validation.double_range", min, max).getString());
                }
                return ValidationResult.valid();
            } catch (NumberFormatException e) {
                return ValidationResult.invalid(Component.translatable("gui.affix_core.validation.double_format").getString());
            }
        };
    }
    
    public static Validator createLengthValidator(int maxLength) {
        return input -> {
            if (input.length() > maxLength) {
                return ValidationResult.invalid(Component.translatable("gui.affix_core.validation.too_long", maxLength).getString());
            }
            return ValidationResult.valid();
        };
    }
    
    public static Validator createRequiredValidator() {
        return input -> {
            if (input.trim().isEmpty()) {
                return ValidationResult.invalid(Component.translatable("gui.affix_core.validation.required").getString());
            }
            return ValidationResult.valid();
        };
    }

    public String getValue() { return editBox.getValue(); }
    public void setValue(String value) { 
        editBox.setValue(value); 
        validateInput(); // 设置值后验证
    }
    public void setResponder(Consumer<String> responder) { editBox.setResponder(responder); }
    public void setFocused(boolean focused) { editBox.setFocused(focused); }
    public boolean isFocused() { return editBox.isFocused(); }
    public void setEditable(boolean editable) { editBox.setEditable(editable); }
}