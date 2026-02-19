package net.yixi_xun.affix_core.tooltip;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.yixi_xun.affix_core.api.ExpressionHelper.parseNbtTag;

/**
 * Tooltip处理器
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TooltipHandler {
    
    // 占位符模式: ${xxx}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    
    // 条件显示模式: ?{condition}text1??{!condition}text2
    private static final Pattern CONDITION_PATTERN = Pattern.compile("\\?\\{([^}]+)}(.*?)\\?\\{!([^}]+)}(.*?)(?=\\?\\{|$)");
    
    // 颜色和样式模式: {c,color} 或 {c,color+style1+style2}
    private static final Pattern COLOR_PATTERN = Pattern.compile("\\{c,([^}]+)}");

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltips = event.getToolTip();
        Player player = event.getEntity();
        
        if (player == null) return;
        
        // 创建轻量级上下文用于Tooltip处理
        Map<String, Object> contextVariables = createContextVariables(player, stack);
        
        // 处理每个tooltip行
        for (int i = 0; i < tooltips.size(); i++) {
            Component originalComponent = tooltips.get(i);
            String originalText = originalComponent.getString();
            
            // 处理占位符
            String processedText = processPlaceholders(originalText, contextVariables);
            
            // 处理条件显示
            processedText = processConditions(processedText, player);

            // 处理颜色
            MutableComponent finalComponent = processColors(processedText);

            // 只有包含颜色标记的文本才处理颜色，否则保留原有样式
            if (finalComponent == null) {
                finalComponent = originalComponent.copy();
            }
            tooltips.set(i, finalComponent);
        }
    }
    
    /**
     * 创建上下文变量映射（借鉴AffixContext的设计）
     */
    private static Map<String, Object> createContextVariables(Player player, ItemStack itemStack) {
        Map<String, Object> variables = new HashMap<>();
        
        // 初始化基本变量
        variables.put("random", Math.random());
        
        // 创建实体数据
        variables.put("owner", AffixContext.createEntityData(player));
        variables.put("self", AffixContext.createEntityData(player));
        
        // 创建物品数据
        variables.put("item", createItemData(itemStack));
        
        return variables;
    }
    
    /**
     * 创建物品数据映射
     */
    private static Map<String, Object> createItemData(ItemStack stack) {
        Map<String, Object> itemData = new HashMap<>();
        
        if (stack.isEmpty()) {
            return itemData;
        }
        
        itemData.put("count", stack.getCount());
        itemData.put("max_damage", stack.getMaxDamage());
        itemData.put("damage", stack.getDamageValue());
        itemData.put("name", stack.getHoverName().getString());
        
        // NBT数据
        CompoundTag nbt = stack.getTag();
        if (nbt != null) {
            itemData.put("nbt", parseNbtCompound(nbt));
        }
        
        return itemData;
    }
    
    /**
     * 解析NBT复合标签
     */
    private static Map<String, Object> parseNbtCompound(CompoundTag compound) {
        Map<String, Object> result = new HashMap<>();
        
        for (String key : compound.getAllKeys()) {
            Tag tag = compound.get(key);
            result.put(key, parseNbtTag(tag));
        }
        
        return result;
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
     * 获取占位符的值（使用ExpressionHelper统一处理）
     */
    private static String getPlaceholderValue(String placeholder, Map<String, Object> contextVariables) {
        try {
            // 使用ExpressionHelper计算表达式值
            double result = ExpressionHelper.evaluate(placeholder, contextVariables);
            
            // 格式化输出
            if (Math.abs(result - Math.round(result)) < 0.001) {
                return String.valueOf((int) Math.round(result));
            } else {
                return String.format("%.2f", result);
            }
        } catch (Exception e) {
            // 发生错误时返回原始占位符
            return "${" + placeholder + "}";
        }
    }
    
    /**
     * 处理条件显示
     */
    private static String processConditions(String text, Player player) {
        // 处理复合条件: ?{shift && ctrl}text1??{!shift}text2
        Matcher matcher = CONDITION_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String condition1 = matcher.group(1);
            String text1 = matcher.group(2);
            String condition2 = matcher.group(3);
            String text2 = matcher.group(4);
            
            String replacement;
            if (evaluateCondition(condition1, player)) {
                replacement = text1;
            } else if (evaluateCondition(condition2, player)) {
                replacement = text2;
            } else {
                replacement = "";
            }
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        // 处理简单的单条件
        return processSimpleConditions(result.toString(), player);
    }
    
    /**
     * 处理简单条件
     */
    private static String processSimpleConditions(String text, Player player) {
        // 匹配 ?{condition}text??{!condition}text 的模式
        Pattern simplePattern = Pattern.compile("\\?\\{([^}]+)}(.*?)(?=\\?\\{|$)");
        Matcher matcher = simplePattern.matcher(text);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String condition = matcher.group(1);
            String content = matcher.group(2);
            
            if (evaluateCondition(condition, player)) {
                matcher.appendReplacement(result, content);
            } else {
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 评估条件
     */
    private static boolean evaluateCondition(String condition, Player player) {
        // 处理按键条件
        if (condition.equalsIgnoreCase("shift")) {
            return isShiftPressed();
        } else if (condition.equalsIgnoreCase("ctrl") || condition.equalsIgnoreCase("control")) {
            return isCtrlPressed();
        } else if (condition.equalsIgnoreCase("alt")) {
            return isAltPressed();
        }
        
        // 处理组合条件
        if (condition.contains("&&")) {
            String[] parts = condition.split("&&");
            for (String part : parts) {
                if (!evaluateCondition(part.trim(), player)) {
                    return false;
                }
            }
            return true;
        } else if (condition.contains("||")) {
            String[] parts = condition.split("\\|");
            for (String part : parts) {
                if (evaluateCondition(part.trim(), player)) {
                    return true;
                }
            }
            return false;
        }
        
        // 处理否定条件
        if (condition.startsWith("!")) {
            return !evaluateCondition(condition.substring(1).trim(), player);
        }
        
        // 处理复杂的表达式条件
        Map<String, Object> variables = createContextVariables(player, player.getMainHandItem());
        return ExpressionHelper.evaluateCondition(condition, variables);
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
     */
    public static MutableComponent processColors(String text) {
        Matcher matcher = COLOR_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        
        MutableComponent result = Component.literal("");
        int lastEnd = 0;
        
        do {
            // 添加颜色标记前的文本
            if (matcher.start() > lastEnd) {
                String beforeText = text.substring(lastEnd, matcher.start());
                result.append(Component.literal(beforeText));
            }
            
            String colorSpec = matcher.group(1);
            Style style = parseColorStyle(colorSpec);
            
            // 找到下一个颜色标记或文本结束
            int nextColorStart = text.indexOf("{c,", matcher.end());
            if (nextColorStart == -1) nextColorStart = text.length();
            
            String coloredText = text.substring(matcher.end(), nextColorStart);
            result.append(Component.literal(coloredText).setStyle(style));
            
            lastEnd = nextColorStart;
        } while (matcher.find());
        
        // 添加剩余文本
        if (lastEnd < text.length()) {
            result.append(Component.literal(text.substring(lastEnd)));
        }
        
        return result;
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
        if (colorPart.contains("->")) {
            // 渐变色: red -> #FFFFFF -> #000000
            style = handleGradientColor(colorPart).withItalic(false);
        } else if (colorPart.contains("-")) {
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
        int r = (int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * ratio);
        int g = (int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * ratio);
        int b = (int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * ratio);
        
        return Style.EMPTY.withColor(TextColor.fromRgb(new Color(r, g, b).getRGB()));
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
     * 获取样式格式
     * 支持完整名称和缩写形式
     * - bold/b: 粗体
     * - italic/i: 斜体
     * - underline/u: 下划线
     * - strikethrough/s: 删除线
     * - obfuscated/o: 混淆效果
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