package net.yixi_xun.affix_core.tooltip;

import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AttributeTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltips = event.getToolTip();
        TooltipFlag flag = event.getFlags();
        
        // 处理所有装备槽位的属性
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<Attribute, AttributeModifier> attributes = stack.getAttributeModifiers(slot);
            
            for (Map.Entry<Attribute, AttributeModifier> entry : attributes.entries()) {
                Attribute attribute = entry.getKey();
                AttributeModifier modifier = entry.getValue();

                if (AttributeFormatter.isPercentageAttribute(attribute)) {
                    removeOriginalAttributeLine(tooltips, attribute);
                    tooltips.add(AttributeFormatter.formatAttribute(attribute, modifier, flag));
                }
            }
        }
    }
    
    private static void removeOriginalAttributeLine(List<Component> tooltips, Attribute attribute) {
        String attributeKey = attribute.getDescriptionId();
        tooltips.removeIf(component -> {
            String text = component.getContents().toString();
            return text.contains(attributeKey);
        });
    }
}