package net.yixi_xun.affix_core.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.yixi_xun.affix_core.gui.BaseWidget;
import net.yixi_xun.affix_core.gui.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class ScrollablePanel extends BaseWidget {
    private final List<BaseWidget> children = new ArrayList<>();
    private int scrollOffset = 0;
    private int totalHeight = 0;
    private boolean isDragging = false; // 新增：是否正在拖拽
    private double lastMouseY = 0; // 新增：上次鼠标Y位置
    private int dragStartScroll = 0; // 新增：开始拖拽时的滚动位置
    private boolean enableSnapToRow = true; // 新增：是否启用行吸附功能

    public ScrollablePanel(int x, int y, int width, int height) {
        super(x, y, width, height, null);
    }

    public <T extends BaseWidget> void addChild(T widget) {
        children.add(widget);
        // 简单的布局：垂直排列
        if (children.size() > 1) {
            BaseWidget prev = children.get(children.size() - 2);
            widget.setY(prev.getY() + prev.getHeight() + 2); // 2px 间距
        } else {
            widget.setY(y + 2); // 第一个元素距离顶部2px
        }
        widget.setX(x + 4); // 左边距
        // 如果组件宽度没定义，默认填充
        if (widget.getWidth() == 0) widget.setWidth(width - 8);
        
        totalHeight = (widget.getY() + widget.getHeight()) - y;
    }

    public void clear() {
        children.clear();
        scrollOffset = 0;
        totalHeight = 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 绘制背景
        RenderUtils.drawRect(guiGraphics, x, y, width, height, 0xDD000000);

        guiGraphics.pose().pushPose();
        guiGraphics.enableScissor(x, y, x + width, y + height);

        for (BaseWidget child : children) {
            // 检查是否在可视区域内
            int renderY = child.getY() - y + scrollOffset;
            if (renderY + child.getHeight() >= y && renderY <= y + height) {
                // 临时修改 child 的 y 进行渲染
                int originalY = child.getY();
                child.setY(renderY);
                child.render(guiGraphics, mouseX, mouseY, partialTick);
                child.setY(originalY); // 恢复
            }
        }
        
        // 绘制滚动条
        if (totalHeight > height) {
            int barHeight = Math.max(10, (int) ((height / (double) totalHeight) * height));
            int barY = y + (int) ((-scrollOffset / (double) (totalHeight - height)) * (height - barHeight));
            RenderUtils.drawRect(guiGraphics, x + width - 5, barY, 4, barHeight, 0xFFFFFFFF);
        }
        
        guiGraphics.disableScissor();
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isHovered(mouseX, mouseY)) {
            int scrollSpeed = 20;
            int newScroll = scrollOffset + (int) (delta * scrollSpeed);
            
            // 限制滚动范围
            int maxScroll = Math.max(0, totalHeight - height);
            newScroll = Math.max(-maxScroll, Math.min(0, newScroll));
            
            // 如果启用了行吸附功能
            if (enableSnapToRow) {
                // 尝试吸附到最近的行
                scrollOffset = snapToNearestRow(newScroll);
            } else {
                scrollOffset = newScroll;
            }
            
            return true;
        }
        return false;
    }

    // 新增：启用/禁用行吸附功能
    public void setEnableSnapToRow(boolean enable) {
        this.enableSnapToRow = enable;
    }

    // 新增：计算滚动位置吸附到最近行
    private int snapToNearestRow(int proposedScroll) {
        // 简单的吸附算法：尝试吸附到组件边界
        if (children.isEmpty()) {
            return proposedScroll;
        }

        // 计算当前滚动位置下的可见区域
        int adjustedScroll = proposedScroll;
        int bestScroll = adjustedScroll;
        int minDistance = Integer.MAX_VALUE;

        // 检查每个子组件的边界是否接近屏幕边界
        for (BaseWidget child : children) {
            int childTop = child.getY() - y + adjustedScroll;
            int childBottom = childTop + child.getHeight();

            // 检查组件顶部是否接近屏幕顶部
            int topDistToTop = Math.abs(childTop);
            int topDistToBottom = Math.abs(childTop - height);
            int bottomDistToTop = Math.abs(childBottom);
            int bottomDistToBottom = Math.abs(childBottom - height);

            // 找到最小的距离并相应调整滚动位置
            int[] distances = {topDistToTop, topDistToBottom, bottomDistToTop, bottomDistToBottom};
            int[] candidateScrolls = {
                adjustedScroll - childTop,  // 吸附顶部到屏幕顶部
                adjustedScroll - (childTop - height), // 吸附顶部到屏幕底部
                adjustedScroll - childBottom, // 吸附底部到屏幕顶部
                adjustedScroll - (childBottom - height) // 吸附底部到屏幕底部
            };

            for (int i = 0; i < distances.length; i++) {
                if (distances[i] < minDistance && Math.abs(candidateScrolls[i]) <= Math.max(0, totalHeight - height)) {
                    minDistance = distances[i];
                    bestScroll = candidateScrolls[i];
                }
            }
        }

        // 如果最小距离很小（在吸附范围内），则使用吸附位置
        if (minDistance < 10) { // 10像素内的吸附阈值
            return bestScroll;
        }
        return adjustedScroll;
    }

    // 重写事件分发，考虑到滚动偏移
    private int getAdjustedMouseY(double mouseY) {
        return (int) mouseY - scrollOffset;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return false;
        
        // 检查是否点击滚动条区域
        if (totalHeight > height && mouseX >= x + width - 5 && mouseX <= x + width) {
            int barHeight = Math.max(10, (int) ((height / (double) totalHeight) * height));
            int barY = y + (int) ((-scrollOffset / (double) (totalHeight - height)) * (height - barHeight));
            
            if (mouseY >= barY && mouseY <= barY + barHeight) {
                // 开始拖拽滚动条
                isDragging = true;
                lastMouseY = mouseY;
                dragStartScroll = scrollOffset;
                return true;
            }
        }
        
        // 倒序遍历，优先处理上层组件
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseWidget child = children.get(i);
            int adjY = getAdjustedMouseY(mouseY);
            // 调整后的 hover 检查
            if (adjY >= child.getY() && adjY <= child.getY() + child.getHeight() && 
                mouseX >= child.getX() && mouseX <= child.getX() + child.getWidth()) {
                if (child.mouseClicked(mouseX, adjY, button)) return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging) {
            isDragging = false;
            return true;
        }
        
        if (!isHovered(mouseX, mouseY)) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseWidget child = children.get(i);
            int adjY = getAdjustedMouseY(mouseY);
            if (adjY >= child.getY() && adjY <= child.getY() + child.getHeight() && 
                mouseX >= child.getX() && mouseX <= child.getX() + child.getWidth()) {
                if (child.mouseReleased(mouseX, adjY, button)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            // 拖拽滚动条
            double scrollRatio = (mouseY - lastMouseY) / (double) height;
            int maxScroll = Math.max(0, totalHeight - height);
            int newScroll = (int) (dragStartScroll + scrollRatio * maxScroll);
            
            // 限制滚动范围
            scrollOffset = Math.max(-maxScroll, Math.min(0, newScroll));
            return true;
        }
        
        if (!isHovered(mouseX, mouseY)) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseWidget child = children.get(i);
            int adjY = getAdjustedMouseY(mouseY);
            if (adjY >= child.getY() && adjY <= child.getY() + child.getHeight() && 
                mouseX >= child.getX() && mouseX <= child.getX() + child.getWidth()) {
                if (child.mouseDragged(mouseX, adjY, button, dragX, dragY)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 将按键事件传递给子组件
        for (BaseWidget child : children) {
            if (child.hasFocus() && child.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // 将字符输入事件传递给子组件
        for (BaseWidget child : children) {
            if (child.hasFocus() && child.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean hasFocus() {
        // 检查是否有任何子组件有焦点
        for (BaseWidget child : children) {
            if (child.hasFocus()) {
                return true;
            }
        }
        return false;
    }
}