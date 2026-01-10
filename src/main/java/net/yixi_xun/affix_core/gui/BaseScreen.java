package net.yixi_xun.affix_core.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseScreen extends Screen {
    protected final List<BaseWidget> widgets = new ArrayList<>();
    
    // 新增：焦点管理
    protected BaseWidget focusedWidget = null;

    public BaseScreen(Component title) {
        super(title);
    }

    // 添加组件
    protected <T extends BaseWidget> T addWidget(T widget) {
        widget.setScreen(this); // 设置组件的屏幕引用
        widgets.add(widget);
        return widget;
    }

    // 清除组件
    protected void clearWidgets() {
        for (BaseWidget widget : widgets) {
            widget.setScreen(null); // 移除组件的屏幕引用
        }
        widgets.clear();
        focusedWidget = null;
    }

    // 新增：设置焦点组件
    public void setFocusedWidget(BaseWidget widget) {
        if (this.focusedWidget != null && this.focusedWidget != widget) {
            this.focusedWidget.onFocusLost();
        }
        
        this.focusedWidget = widget;
        
        if (widget != null) {
            widget.onFocusGained();
        }
    }

    // 新增：获取焦点组件
    public BaseWidget getFocusedWidget() {
        return focusedWidget;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 渲染背景 (通常由子类实现或默认为半透明黑底)
        renderBackground(guiGraphics);
        
        // 2. 渲染所有组件
        for (BaseWidget widget : widgets) {
            if (widget.visible) {
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        
        // 3. 渲染提示层 (如物品栏Tooltip)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 倒序遍历，确保点击最上层的组件
        for (int i = widgets.size() - 1; i >= 0; i--) {
            BaseWidget widget = widgets.get(i);
            if (widget.visible && widget.mouseClicked(mouseX, mouseY, button)) {
                // 当组件被点击时，将其设置为焦点（如果它能获得焦点）
                if (widget.canTakeFocus()) {
                    setFocusedWidget(widget);
                }
                return true; // 事件被消费
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            BaseWidget widget = widgets.get(i);
            if (widget.visible && widget.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            BaseWidget widget = widgets.get(i);
            if (widget.visible && widget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            BaseWidget widget = widgets.get(i);
            if (widget.visible && widget.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 检查是否是Tab键，用于在组件间切换焦点
        if (keyCode == 258 /* GLFW_KEY_TAB */) { // TAB键
            cycleFocus(modifiers);
            return true;
        }
        
        // 检查是否是Enter键，如果焦点组件是TextInputWidget，则尝试触发保存
        if (keyCode == 257 && focusedWidget instanceof net.yixi_xun.affix_core.gui.widget.TextInputWidget) { // ENTER键
            // 在某些情况下，我们可能希望ENTER键触发保存操作
            // 这里可以留作子类扩展
        }
        
        // 首先检查当前焦点组件
        if (focusedWidget != null && focusedWidget.visible && focusedWidget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        // 然后检查所有可见组件
        for (BaseWidget widget : widgets) {
            if (widget.visible && widget != focusedWidget && widget.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // 新增：Tab键循环焦点
    private void cycleFocus(int modifiers) {
        if (widgets.isEmpty()) return;
        
        // 找到所有可获得焦点的组件
        List<BaseWidget> focusableWidgets = new ArrayList<>();
        for (BaseWidget widget : widgets) {
            if (widget.visible && widget.active && widget.canTakeFocus()) {
                focusableWidgets.add(widget);
            }
        }
        
        if (focusableWidgets.isEmpty()) return;
        
        int currentIndex = -1;
        if (focusedWidget != null) {
            currentIndex = focusableWidgets.indexOf(focusedWidget);
        }
        
        // 计算下一个焦点组件索引
        int nextIndex;
        if ((modifiers & 1) != 0) { // Shift键被按下，反向循环
            nextIndex = (currentIndex - 1 + focusableWidgets.size()) % focusableWidgets.size();
        } else { // 正向循环
            nextIndex = (currentIndex + 1) % focusableWidgets.size();
        }
        
        setFocusedWidget(focusableWidgets.get(nextIndex));
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // 优先检查焦点组件
        if (focusedWidget != null && focusedWidget.visible && focusedWidget.hasFocus() && focusedWidget.charTyped(codePoint, modifiers)) {
            return true;
        }
        
        // 如果焦点组件没有处理，则尝试其他组件（仅在它们可以无焦点接收字符时）
        for (BaseWidget widget : widgets) {
            if (widget.visible && widget != focusedWidget && widget.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    // 新增：响应式布局方法
    protected void layout() {
        // 子类可以重写此方法来实现响应式布局
        // 默认实现为空
    }
    
    @Override
    public void init() {
        super.init();
        // 初始化时调用布局方法
        layout();
    }
    
    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        // 重新调整大小时调用布局方法
        layout();
    }
    
    // 新增：暂停方法
    public void onPause() {
        // 子类可以重写此方法来处理屏幕暂停时的逻辑
    }
    
    // 新增：恢复方法
    public void onResume() {
        // 子类可以重写此方法来处理屏幕恢复时的逻辑
        layout(); // 恢复时重新布局
    }
    
    // 新增：销毁方法
    public void onDestroy() {
        // 子类可以重写此方法来处理屏幕销毁时的清理工作
        clearWidgets(); // 清理组件
    }
}