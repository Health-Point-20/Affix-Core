package net.yixi_xun.affix_core.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.yixi_xun.affix_core.gui.BaseWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CycleButton extends BaseWidget {
    private final List<String> options;
    private int currentIndex = 0;
    private final Consumer<String> onOptionChanged;

    public CycleButton(int x, int y, int width, int height, List<String> options, Consumer<String> onOptionChanged) {
        super(x, y, width, height, Component.literal(options.isEmpty() ? "" : options.get(0)));
        this.options = new ArrayList<>(options); // 创建副本以避免外部修改
        this.onOptionChanged = onOptionChanged;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHovered(mouseX, mouseY);
        int borderColor = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
        int backgroundColor = hovered ? 0xFF444444 : 0xFF222222;

        // 绘制按钮背景
        net.yixi_xun.affix_core.gui.RenderUtils.drawBorderRect(guiGraphics, x, y, width, height, borderColor, backgroundColor);

        // 绘制文字 (居中)
        String displayText = getCurrentOption();
        if (displayText.length() > 15) { // 如果文本过长，截断显示
            displayText = displayText.substring(0, 12) + "...";
        }
        int textColor = active ? 0xFFFFFF : 0xA0A0A0; // 禁用时文字变暗
        guiGraphics.drawCenteredString(font, displayText, x + width / 2, y + (height - 8) / 2, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0) { // 左键点击切换
            if (active && !options.isEmpty()) {
                currentIndex = (currentIndex + 1) % options.size();
                if (onOptionChanged != null) {
                    onOptionChanged.accept(getCurrentOption());
                }
                return true;
            }
        } else if (isHovered(mouseX, mouseY) && button == 1) { // 右键点击反向切换
            if (active && !options.isEmpty()) {
                currentIndex = (currentIndex - 1 + options.size()) % options.size();
                if (onOptionChanged != null) {
                    onOptionChanged.accept(getCurrentOption());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasFocus() {
        return false; // 循环按钮通常不会有输入焦点
    }

    public String getCurrentOption() {
        if (options.isEmpty()) {
            return "";
        }
        return options.get(currentIndex);
    }

    public void setCurrentOption(String option) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equals(option)) {
                currentIndex = i;
                break;
            }
        }
    }
}