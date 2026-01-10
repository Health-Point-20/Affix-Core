package net.yixi_xun.affix_core.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class BaseWidget {
    protected int x, y, width, height;
    protected boolean visible = true;
    protected boolean active = true;
    protected Component message;
    protected Font font;
    protected BaseScreen screen; // 新增：对所属屏幕的引用

    public BaseWidget(int x, int y, int width, int height, Component message) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message;
        font = Minecraft.getInstance().font;
    }

    // 新增：设置所属屏幕
    public void setScreen(BaseScreen screen) {
        this.screen = screen;
    }

    // 渲染方法
    public abstract void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    // 鼠标点击事件，返回true表示事件已被消费，停止传递
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    
    // 鼠标释放事件
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    
    // 鼠标拖动事件
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { return false; }

    // 鼠标滚轮事件
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) { return false; }

    // 键盘输入事件
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    // 字符输入事件
    public boolean charTyped(char codePoint, int modifiers) { return false; }
    
    // 是否有焦点
    public boolean hasFocus() { 
        return screen != null && this == screen.getFocusedWidget(); 
    }
    
    // 新增：是否可以获取焦点
    public boolean canTakeFocus() { return false; }
    
    // 新增：获得焦点时的回调
    public void onFocusGained() {}
    
    // 新增：失去焦点时的回调
    public void onFocusLost() {}

    // 辅助方法：判断鼠标是否在组件内
    public boolean isHovered(double mouseX, double mouseY) {
        return visible && active && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    // Getters and Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isVisible() { return visible; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public void setActive(boolean active) { this.active = active; }
}