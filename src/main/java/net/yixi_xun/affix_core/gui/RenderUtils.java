package net.yixi_xun.affix_core.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RenderUtils {
    private static final Font font = Minecraft.getInstance().font;

    // 绘制纯色矩形 (支持透明度)
    public static void drawRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + height, color);
    }

    // 绘制边框矩形
    public static void drawBorderRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int borderColor, int bgColor) {
        drawRect(guiGraphics, x, y, width, height, bgColor);
        drawRect(guiGraphics, x, y, width, 1, borderColor); // 上
        drawRect(guiGraphics, x, y + height - 1, width, 1, borderColor); // 下
        drawRect(guiGraphics, x, y, 1, height, borderColor); // 左
        drawRect(guiGraphics, x + width - 1, y, 1, height, borderColor); // 右
    }

    // 绘制纹理 (简单平铺)
    public static void drawTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height) {
        guiGraphics.blit(texture, x, y, 0, 0, width, height, width, height);
    }
    
    // 绘制纹理 (带UV裁剪)
    public static void drawTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        guiGraphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
    
    // 绘制九宫格拉伸纹理
    public static void drawNineSlice(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, int cornerSize) {
        // 四个角
        // 左上角
        guiGraphics.blit(texture, x, y, 0, 0, cornerSize, cornerSize, cornerSize * 3, cornerSize * 3);
        // 右上角
        guiGraphics.blit(texture, x + width - cornerSize, y, cornerSize * 2, 0, cornerSize, cornerSize, cornerSize * 3, cornerSize * 3);
        // 左下角
        guiGraphics.blit(texture, x, y + height - cornerSize, 0, cornerSize * 2, cornerSize, cornerSize, cornerSize * 3, cornerSize * 3);
        // 右下角
        guiGraphics.blit(texture, x + width - cornerSize, y + height - cornerSize, cornerSize * 2, cornerSize * 2, cornerSize, cornerSize, cornerSize * 3, cornerSize * 3);
        
        // 四条边
        // 顶边
        guiGraphics.blit(texture, x + cornerSize, y, cornerSize, 0, width - cornerSize * 2, cornerSize, cornerSize * 3, cornerSize * 3);
        // 底边
        guiGraphics.blit(texture, x + cornerSize, y + height - cornerSize, cornerSize, cornerSize * 2, width - cornerSize * 2, cornerSize, cornerSize * 3, cornerSize * 3);
        // 左边
        guiGraphics.blit(texture, x, y + cornerSize, 0, cornerSize, cornerSize, height - cornerSize * 2, cornerSize * 3, cornerSize * 3);
        // 右边
        guiGraphics.blit(texture, x + width - cornerSize, y + cornerSize, cornerSize * 2, cornerSize, cornerSize, height - cornerSize * 2, cornerSize * 3, cornerSize * 3);
        
        // 中心
        guiGraphics.blit(texture, x + cornerSize, y + cornerSize, cornerSize, cornerSize, width - cornerSize * 2, height - cornerSize * 2, cornerSize * 3, cornerSize * 3);
    }
    
    // 绘制垂直渐变矩形
    public static void drawVerticalGradient(GuiGraphics guiGraphics, int x, int y, int width, int height, int startColor, int endColor) {
        guiGraphics.fillGradient(x, y, x + width, y + height, startColor, endColor);
    }
    
    // 绘制水平渐变矩形
    public static void drawHorizontalGradient(GuiGraphics guiGraphics, int x, int y, int width, int height, int startColor, int endColor) {
        // 模拟水平渐变，通过多条垂直线段
        for (int i = 0; i < width; i++) {
            float ratio = (float) i / (width - 1);
            int color = interpolateColor(startColor, endColor, ratio);
            drawRect(guiGraphics, x + i, y, 1, height, color);
        }
    }
    
    // 颜色插值
    private static int interpolateColor(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        int a = (int) (a1 + (a2 - a1) * ratio);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    // 绘制圆角矩形
    public static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        // 绘制中心矩形
        drawRect(guiGraphics, x + radius, y + radius, width - radius * 2, height - radius * 2, color);
        // 绘制四条边
        drawRect(guiGraphics, x + radius, y, width - radius * 2, radius, color); // 上边
        drawRect(guiGraphics, x + radius, y + height - radius, width - radius * 2, radius, color); // 下边
        drawRect(guiGraphics, x, y + radius, radius, height - radius * 2, color); // 左边
        drawRect(guiGraphics, x + width - radius, y + radius, radius, height - radius * 2, color); // 右边
        
        // 绘制四个圆角（用小矩形近似）
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                if ((i - radius) * (i - radius) + (j - radius) * (j - radius) <= radius * radius) {
                    drawRect(guiGraphics, x + i, y + j, 1, 1, color); // 左上角
                    drawRect(guiGraphics, x + width - i - 1, y + j, 1, 1, color); // 右上角
                    drawRect(guiGraphics, x + i, y + height - j - 1, 1, 1, color); // 左下角
                    drawRect(guiGraphics, x + width - i - 1, y + height - j - 1, 1, 1, color); // 右下角
                }
            }
        }
    }
    
    // 文本截断：绘制超出最大宽度的部分用省略号替换
    public static void drawTruncatedString(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        String str = text.getString();
        String truncated = truncateString(guiGraphics, str, maxWidth);
        guiGraphics.drawString(font, truncated, x, y, color, false);
    }
    
    // 截断字符串
    private static String truncateString(GuiGraphics guiGraphics, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        
        if (ellipsisWidth >= maxWidth) {
            // 如果省略号本身就超过了最大宽度，尝试只显示部分字符
            for (int i = text.length(); i > 0; i--) {
                String substr = text.substring(0, i);
                if (font.width(substr) <= maxWidth) {
                    return substr;
                }
            }
            return ""; // 如果什么都放不下，返回空字符串
        }
        
        // 二分查找最适合的长度
        int left = 0;
        int right = text.length();
        
        while (left < right) {
            int mid = (left + right + 1) / 2;
            String substr = text.substring(0, mid) + ellipsis;
            
            if (font.width(substr) <= maxWidth) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }
        
        return left > 0 ? text.substring(0, left) + ellipsis : "";
    }
    
    // 多行文本居中绘制
    public static void drawMultiLineCentered(GuiGraphics guiGraphics, Component text, int x, int y, int width, int lineHeight, int color) {
        String[] lines = text.getString().split("\n");
        int totalHeight = lines.length * lineHeight;
        int currentY = y - totalHeight / 2; // 垂直居中
        
        for (String line : lines) {
            Component lineComponent = Component.literal(line);
            int lineWidth = font.width(lineComponent);
            int lineX = x + (width - lineWidth) / 2; // 水平居中
            guiGraphics.drawString(font, lineComponent, lineX, currentY, color, false);
            currentY += lineHeight;
        }
    }
    
    // 主题系统 - 预定义颜色
    public static class Theme {
        public static final int PRIMARY_COLOR = 0xFF4CAF50;
        public static final int SECONDARY_COLOR = 0xFF2196F3;
        public static final int ERROR_COLOR = 0xFFF44336;
        public static final int WARNING_COLOR = 0xFFFF9800;
        public static final int SUCCESS_COLOR = 0xFF4CAF50;
        public static final int BACKGROUND_DARK = 0xFF121212;
        public static final int BACKGROUND_MEDIUM = 0xFF1E1E1E;
        public static final int BACKGROUND_LIGHT = 0xFF2D2D2D;
        public static final int TEXT_PRIMARY = 0xFFFFFFFF;
        public static final int TEXT_SECONDARY = 0xFFBBBBBB;
        public static final int BORDER_COLOR = 0xFF555555;
    }

    // 样式系统 - 预定义样式
        public record Style(int backgroundColor, int borderColor, int textColor, int highlightColor) {

        // 默认样式
            public static final Style DEFAULT = new Style(Theme.BACKGROUND_MEDIUM, Theme.BORDER_COLOR, Theme.TEXT_PRIMARY, Theme.PRIMARY_COLOR);
            // 危险样式（用于删除按钮等）
            public static final Style DANGER = new Style(Theme.ERROR_COLOR, Theme.ERROR_COLOR, Theme.TEXT_PRIMARY, 0xFFFF8A80);
            // 成功样式
            public static final Style SUCCESS = new Style(Theme.SUCCESS_COLOR, Theme.SUCCESS_COLOR, Theme.TEXT_PRIMARY, 0xFFA5D6A7);
        }
}