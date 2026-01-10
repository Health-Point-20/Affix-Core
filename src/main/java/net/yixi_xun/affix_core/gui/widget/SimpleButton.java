package net.yixi_xun.affix_core.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.yixi_xun.affix_core.gui.BaseWidget;
import net.yixi_xun.affix_core.gui.RenderUtils;

import java.util.function.Consumer;

public class SimpleButton extends BaseWidget {
    private final Consumer<SimpleButton> onPress;
    private final ResourceLocation texture;
    
    // 构造函数
    public SimpleButton(int x, int y, int width, int height, Component message, Consumer<SimpleButton> onPress) {
        this(x, y, width, height, message, onPress, null);
    }

    // 带纹理的构造函数
    public SimpleButton(int x, int y, int width, int height, Component message, Consumer<SimpleButton> onPress, ResourceLocation texture) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.texture = texture;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHovered(mouseX, mouseY);
        int borderColor = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
        int backgroundColor = hovered ? 0xFF444444 : 0xFF222222;

        if (texture != null) {
            // 如果有纹理，绘制纹理 (这里简单处理，实际可能需要更复杂的UV逻辑)
            RenderUtils.drawTexture(guiGraphics, texture, x, y, width, height);
        } else {
            // 默认绘制圆角矩形风格的背景
            RenderUtils.drawBorderRect(guiGraphics, x, y, width, height, borderColor, backgroundColor);
        }

        // 绘制文字 (居中)
        int textColor = active ? 0xFFFFFF : 0xA0A0A0; // 禁用时文字变暗
        guiGraphics.drawCenteredString(font, message.getString(), x + width / 2, y + (height - 8) / 2, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0) { // 0 = 左键
            if (active) {
                onPress.accept(this);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean hasFocus() {
        return false; // 按钮通常不会有输入焦点
    }
}