package net.yixi_xun.affix_core.tooltip;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.api.ExpressionHelper;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tooltip处理器
 * 支持条件显示和文本格式化功能
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TooltipHandler {

    // 占位符模式: ${xxx}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    // 颜色和样式模式: {c,color} 或 {c,color+style1+style2}
    private static final Pattern COLOR_PATTERN = Pattern.compile("\\{c,([^}]+)}");

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltips = event.getToolTip();
        Player player = event.getEntity();
            
        if (player == null) return;
            
        // 创建并复用上下文变量
        Map<String, Object> context = createContextVariables(player, stack);
            
        // 批量处理所有tooltip组件
        tooltips.replaceAll(component -> processTooltipComponent(component, context));
    }
        
    /**
     * 处理单个tooltip组件
     */
    private static Component processTooltipComponent(Component component, Map<String, Object> context) {
        String originalText = component.getString();
            
        // 按顺序处理：占位符 → 条件 → 颜色
        String processedText = processPlaceholders(originalText, context);
        processedText = processConditions(processedText, context);
        MutableComponent colorResult = processColors(processedText);
            
        // 即使没有颜色处理，也要应用条件处理后的文本
        if (colorResult != null) {
            // 有颜色处理，保留原始组件的样式属性（除了颜色）
            return colorResult.setStyle(component.getStyle());
        } else {
            // 没有颜色处理，但仍需应用条件处理后的文本
            if (!processedText.equals(originalText)) {
                // 文本经过条件处理发生了变化，创建新的组件
                return Component.literal(processedText).setStyle(component.getStyle());
            } else {
                // 文本没有变化，返回原始组件
                return component.copy();
            }
        }
    }

    /**
     * 创建上下文变量映射
     */
    private static Map<String, Object> createContextVariables(Player player, ItemStack itemStack) {
        Map<String, Object> variables = new HashMap<>();
        
        // 基础变量
        variables.put("random", Math.random());

        // 按键状态变量
        variables.put("shift", isShiftPressed() ? 1.0 : 0.0);
        variables.put("ctrl", isCtrlPressed() ? 1.0 : 0.0);
        variables.put("alt", isAltPressed() ? 1.0 : 0.0);
        
        // 实体数据
        Map<String, Object> entityData = AffixContext.createEntityData(player);
        variables.put("owner", entityData);
        variables.put("self", entityData);
        
        // 物品数据
        variables.put("item", AffixContext.createItemData(itemStack));
        
        return variables;
    }

    /**
     * 处理占位符替换
     */
    private static String processPlaceholders(String text, Map<String, Object> contextVariables) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = getPlaceholderValue(placeholder, contextVariables);
            matcher.appendReplacement(result, replacement != null ? replacement : matcher.group(0));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 获取占位符的值
     */
    private static String getPlaceholderValue(String placeholder, Map<String, Object> contextVariables) {
        try {
            double result = ExpressionHelper.evaluate(placeholder, contextVariables);

            // 格式化输出
            if (Math.abs(result - Math.round(result)) < 0.001) {
                return String.valueOf((int) Math.round(result));
            } else {
                return String.format("%.2f", result);
            }
        } catch (Exception e) {
            return "${" + placeholder + "}";
        }
    }

    /**
     * 处理条件显示
     * 使用栈结构支持嵌套条件处理
     */
    private static String processConditions(String text, Map<String, Object> contextVariables) {
        StringBuilder result = new StringBuilder();
        Deque<Boolean> conditionStack = new ArrayDeque<>();
        conditionStack.push(true); // 默认全局条件为true
        
        int lastEnd = 0;
        int i = 0;
        
        while (i < text.length()) {
            // 查找条件开始标记 ?{
            int conditionStart = text.indexOf("?{", i);
            if (conditionStart == -1) {
                // 没有更多条件，添加剩余文本
                if (Boolean.TRUE.equals(conditionStack.peek())) {
                    result.append(text.substring(lastEnd));
                }
                break;
            }
            
            // 添加条件标记前的文本（如果当前条件满足）
            if (Boolean.TRUE.equals(conditionStack.peek()) && lastEnd < conditionStart) {
                result.append(text, lastEnd, conditionStart);
            }
            
            // 查找条件结束标记 }
            int conditionEnd = text.indexOf("}", conditionStart + 2);
            if (conditionEnd == -1) break;
            
            String condition = text.substring(conditionStart + 2, conditionEnd);
            
            // 查找内容结束位置（下一个标记开始或字符串结束）
            int contentStart = conditionEnd + 1;
            int nextConditionStart = findNextMarkerStart(text, contentStart);
            
            String content = text.substring(contentStart, nextConditionStart);
            
            // 评估条件
            boolean conditionResult = evaluateCondition(condition, contextVariables);
            
            // 根据父条件和当前条件决定是否显示内容
            boolean shouldShow = Boolean.TRUE.equals(conditionStack.peek()) && conditionResult;
            
            if (shouldShow) {
                result.append(content);
            }
            
            lastEnd = nextConditionStart;
            i = nextConditionStart;
        }
        
        return result.toString();
    }
    
    /**
     * 查找下一个标记的开始位置
     * 支持条件标记?{和颜色标记{c,
     */
    private static int findNextMarkerStart(String text, int startPos) {
        int nextCondition = text.indexOf("?{", startPos);
        int nextColor = text.indexOf("{c,", startPos);
        
        // 如果都没有找到，返回字符串末尾
        if (nextCondition == -1 && nextColor == -1) {
            return text.length();
        }
        
        // 如果只有一个找到，返回那个位置
        if (nextCondition == -1) {
            return nextColor;
        }
        if (nextColor == -1) {
            return nextCondition;
        }
        
        // 如果都找到了，返回较小的位置（更早出现的标记）
        return Math.min(nextCondition, nextColor);
    }

    /**
     * 评估条件
     */
    private static boolean evaluateCondition(String condition, Map<String, Object> contextVariables) {
        String trimmedCondition = condition.trim();

        // 处理否定条件（递归处理）
        if (trimmedCondition.startsWith("!")) {
            return !evaluateCondition(trimmedCondition.substring(1).trim(), contextVariables);
        }

        return ExpressionHelper.evaluateCondition(trimmedCondition, contextVariables);
    }

    /**
     * 检查Shift键是否被按下
     */
    private static boolean isShiftPressed() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
    }

    /**
     * 检查Ctrl键是否被按下
     */
    private static boolean isCtrlPressed() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, 341) || InputConstants.isKeyDown(window, 345);
    }

    /**
     * 检查Alt键是否被按下
     */
    private static boolean isAltPressed() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, 342) || InputConstants.isKeyDown(window, 346);
    }

    /**
     * 处理颜色格式
     * 支持多种颜色和样式格式
     */
    public static MutableComponent processColors(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
            
        Matcher matcher = COLOR_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
            
        MutableComponent result = Component.empty();
        int lastEnd = 0;
            
        do {
            // 添加颜色标记前的文本
            if (matcher.start() > lastEnd) {
                String beforeText = text.substring(lastEnd, matcher.start());
                result.append(Component.literal(beforeText));
            }
                
            String colorSpec = matcher.group(1);
                
            // 查找下一个颜色标记或文本结束
            int nextColorStart = text.indexOf("{c,", matcher.end());
            if (nextColorStart == -1) nextColorStart = text.length();
                
            String coloredText = text.substring(matcher.end(), nextColorStart);
                
            // 处理不同类型的颜色格式
            MutableComponent coloredComponent = createColoredComponent(coloredText, colorSpec);
            result.append(coloredComponent);
                
            lastEnd = nextColorStart;
        } while (matcher.find());
            
        // 添加剩余文本
        if (lastEnd < text.length()) {
            result.append(Component.literal(text.substring(lastEnd)));
        }
            
        return result;
    }
        
    /**
     * 创建带颜色的组件
     */
    private static MutableComponent createColoredComponent(String text, String colorSpec) {
        if (colorSpec.contains(":")) {
            // 逐字渐变: red:blue
            return handleCharacterGradient(text, colorSpec);
        } else if (colorSpec.contains("->")) {
            // 渐变色: red -> blue
            Style style = handleGradientColor(colorSpec).withItalic(false);
            return Component.literal(text).setStyle(style);
        } else if (colorSpec.contains("-")) {
            // 循环色: red-blue-yellow
            Style style = handleCycleColor(colorSpec).withItalic(false);
            return Component.literal(text).setStyle(style);
        } else {
            // 单色或带样式的颜色
            Style style = parseColorStyle(colorSpec);
            return Component.literal(text).setStyle(style);
        }
    }

    /**
     * 解析颜色和样式
     */
    private static Style parseColorStyle(String colorSpec) {
        Style style = Style.EMPTY;

        // 分离颜色和样式部分
        String[] parts = colorSpec.split("\\+");
        String colorPart = parts[0];
        String[] styleParts = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        // 处理颜色
        if (colorSpec.contains("->")) {
            // 渐变色: red -> #FFFFFF -> #000000
            style = handleGradientColor(colorPart).withItalic(false);
        }
        // 逐字渐变色: red:blue (第一个字红色到最后一字蓝色)
        // 这种格式需要在处理具体文本时才能确定,此处不处理
        else if (colorSpec.contains("-")) {
            // 循环色: red-blue-yellow
            style = handleCycleColor(colorPart).withItalic(false);
        } else {
            // 单色
            ChatFormatting colorFormatting = getColorFormatting(colorPart);
            if (colorFormatting != null) {
                style = style.applyFormat(colorFormatting).withItalic(false);
            } else {
                // 尝试解析十六进制颜色
                try {
                    int color = Integer.parseInt(colorPart.replace("#", ""), 16);
                    style = style.withColor(TextColor.fromRgb(color)).withItalic(false);
                } catch (NumberFormatException e) {
                    // 无效颜色，保持默认
                }
            }
        }

        // 处理样式
        for (String stylePart : styleParts) {
            ChatFormatting formatting = getStyleFormatting(stylePart.trim());
            if (formatting != null) {
                style = style.applyFormat(formatting);
            }
        }

        return style;
    }

    /**
     * 处理逐字渐变颜色
     * 格式: {c,red:blue} - 第一个字符红色，最后一个字符蓝色，中间平滑过渡
     */
    private static MutableComponent handleCharacterGradient(String text, String colorSpec) {
        String[] colors = colorSpec.split(":");
        if (colors.length != 2) {
            // 格式错误，返回原文本
            return Component.literal(text);
        }

        Color startColor = parseColor(colors[0].trim());
        Color endColor = parseColor(colors[1].trim());

        MutableComponent result = Component.literal("");
        int textLength = text.length();

        // 如果文本为空或只有一个字符，直接使用起始颜色
        if (textLength <= 1) {
            Style style = Style.EMPTY.withColor(TextColor.fromRgb(startColor.getRGB()));
            return Component.literal(text).setStyle(style);
        }

        // 为每个字符计算颜色
        for (int i = 0; i < textLength; i++) {
            float ratio = (float) i / (textLength - 1);
            Color charColor = interpolateColor(startColor, endColor, ratio);
            Style charStyle = Style.EMPTY.withColor(TextColor.fromRgb(charColor.getRGB()));

            result.append(Component.literal(String.valueOf(text.charAt(i))).setStyle(charStyle.withItalic(false)));
        }

        return result;
    }

    /**
     * 处理循环颜色
     */
    private static Style handleCycleColor(String colorSpec) {
        String[] colors = colorSpec.split("-");
        long tick = System.currentTimeMillis() / 250; // 每250ms切换一次
        int index = (int) (tick % colors.length);
        return parseColorStyle(colors[index]);
    }

    /**
     * 处理渐变色
     */
    private static Style handleGradientColor(String colorSpec) {
        String[] colors = colorSpec.split("\\s*->\\s*");
        if (colors.length < 2) return Style.EMPTY;

        long tick = System.currentTimeMillis() / 50; // 每50ms更新
        int totalSteps = 20 * (colors.length - 1); // 每段20步
        int currentStep = (int) (tick % totalSteps);

        int segment = currentStep / 20;
        int stepInSegment = currentStep % 20;

        if (segment >= colors.length - 1) segment = colors.length - 2;

        Color startColor = parseColor(colors[segment]);
        Color endColor = parseColor(colors[segment + 1]);

        float ratio = stepInSegment / 20.0f;
        Color interpolatedColor = interpolateColor(startColor, endColor, ratio);
        return Style.EMPTY.withColor(TextColor.fromRgb(interpolatedColor.getRGB()));
    }

    /**
     * 解析颜色
     */
    private static Color parseColor(String colorStr) {
        ChatFormatting formatting = getColorFormatting(colorStr);
        if (formatting != null) {
            return new Color(formatting.getColor() != null ? formatting.getColor() : 0xFFFFFF);
        }

        try {
            return new Color(Integer.parseInt(colorStr.replace("#", ""), 16));
        } catch (NumberFormatException e) {
            return Color.WHITE;
        }
    }

    /**
     * 获取颜色格式
     */
    private static ChatFormatting getColorFormatting(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "black" -> ChatFormatting.BLACK;
            case "dark_blue" -> ChatFormatting.DARK_BLUE;
            case "dark_green" -> ChatFormatting.DARK_GREEN;
            case "dark_aqua" -> ChatFormatting.DARK_AQUA;
            case "dark_red" -> ChatFormatting.DARK_RED;
            case "dark_purple" -> ChatFormatting.DARK_PURPLE;
            case "gold" -> ChatFormatting.GOLD;
            case "gray" -> ChatFormatting.GRAY;
            case "dark_gray" -> ChatFormatting.DARK_GRAY;
            case "blue" -> ChatFormatting.BLUE;
            case "green" -> ChatFormatting.GREEN;
            case "aqua" -> ChatFormatting.AQUA;
            case "red" -> ChatFormatting.RED;
            case "light_purple" -> ChatFormatting.LIGHT_PURPLE;
            case "yellow" -> ChatFormatting.YELLOW;
            case "white" -> ChatFormatting.WHITE;
            default -> null;
        };
    }

    /**
     * 颜色插值计算
     */
    private static Color interpolateColor(Color startColor, Color endColor, float ratio) {
        int r = (int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * ratio);
        int g = (int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * ratio);
        int b = (int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * ratio);
        return new Color(r, g, b);
    }

    /**
     * 获取样式格式
     */
    private static ChatFormatting getStyleFormatting(String styleName) {
        return switch (styleName.toLowerCase()) {
            case "bold", "b" -> ChatFormatting.BOLD;
            case "italic", "i" -> ChatFormatting.ITALIC;
            case "underline", "u" -> ChatFormatting.UNDERLINE;
            case "strikethrough", "s" -> ChatFormatting.STRIKETHROUGH;
            case "obfuscated", "o" -> ChatFormatting.OBFUSCATED;
            case "reset" -> ChatFormatting.RESET;
            default -> null;
        };
    }
}