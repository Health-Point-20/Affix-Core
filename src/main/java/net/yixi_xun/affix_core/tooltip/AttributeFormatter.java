package net.yixi_xun.affix_core.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.yixi_xun.affix_core.attributes.AffixCoreAttributes;

public class AttributeFormatter {

    public static MutableComponent formatAttribute(Attribute attribute, AttributeModifier modifier, TooltipFlag flag) {
        double value = modifier.getAmount();
        String attributeName = getLocalizedAttributeName(attribute);
        // 百分比属性增加百分比时，增加后缀以区分
        String suffix = modifier.getOperation() != AttributeModifier.Operation.ADDITION ? Component.translatable("attribute.modifier.potency").getString() : "";
        attributeName += suffix;
        return formatPercentage(value, attributeName, flag);
    }

    public static boolean isPercentageAttribute(Attribute attribute) {
        return attribute.equals(AffixCoreAttributes.EVASION.get()) || attribute.equals(AffixCoreAttributes.HIT_RATE.get())
                || attribute.equals(AffixCoreAttributes.FINAL_EVASION.get()) || attribute.equals(AffixCoreAttributes.FINAL_HIT_RATE.get());
    }

    private static String getLocalizedAttributeName(Attribute attribute) {
        // 获取本地化的属性名称
        return Component.translatable(attribute.getDescriptionId()).getString();
    }

    private static MutableComponent formatPercentage(double value, String name, TooltipFlag flag) {
        // 转换为百分比 (0.1 → 10%)
        double percentage = value * 100;
        String formattedValue = ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(percentage);

        MutableComponent comp;
        if (value > 0) {
            comp = Component.literal("+" + formattedValue + "% " + name).withStyle(ChatFormatting.BLUE);
        } else if (value < 0) {
            comp = Component.literal(formattedValue + "% " + name).withStyle(ChatFormatting.RED);
        } else {
            comp = Component.literal(formattedValue + "% " + name).withStyle(ChatFormatting.GRAY);
        }

        // 高级模式显示原始值
        if (flag.isAdvanced()) {
            if (value > 0) {
                comp.append(Component.literal(" [" + "+" + ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value) + "]").withStyle(ChatFormatting.GRAY));
            } else {
                comp.append(Component.literal(" [" + ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value) + "]").withStyle(ChatFormatting.GRAY));

            }
        }
        return comp;
    }
}