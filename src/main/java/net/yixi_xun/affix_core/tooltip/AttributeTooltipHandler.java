package net.yixi_xun.affix_core.tooltip;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.AFConfig;
import net.yixi_xun.affix_core.AffixCoreMod;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Consumer;

/**
     * 合并的属性修饰符工具提示，移植到1.20.1 Forge版本。
     */
@Mod.EventBusSubscriber(modid = AffixCoreMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class AttributeTooltipHandler {
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.ROOT));
    private static final UUID FAKE_MERGED_ID = UUID.nameUUIDFromBytes("affix_core:fake_merged_modifier".getBytes(StandardCharsets.UTF_8));

    static final ChatFormatting BASE_COLOR = ChatFormatting.DARK_GREEN;
    public static final int MERGE_BASE_MODIFIER_COLOR = 16758784; // Gold
    public static final int MERGED_MODIFIER_COLOR = 7699710; // Light Blue


    public static final Comparator<AttributeModifier> ATTRIBUTE_MODIFIER_COMPARATOR =
            Comparator.comparing(AttributeModifier::getOperation)
                    .thenComparing((AttributeModifier a) -> -Math.abs(a.getAmount()))
                    .thenComparing(AttributeModifier::getId);

    private static final Map<String, EquipmentSlotGroup> KEY_SLOT_MAP = Util.make(new HashMap<>(), map -> {
        map.put(Component.translatable("item.modifiers.mainhand").getString(), EquipmentSlotGroup.MAINHAND);
        map.put(Component.translatable("item.modifiers.offhand").getString(), EquipmentSlotGroup.OFFHAND);
        map.put(Component.translatable("item.modifiers.hand").getString(), EquipmentSlotGroup.HAND);
        map.put(Component.translatable("item.modifiers.head").getString(), EquipmentSlotGroup.HEAD);
        map.put(Component.translatable("item.modifiers.chest").getString(), EquipmentSlotGroup.CHEST);
        map.put(Component.translatable("item.modifiers.legs").getString(), EquipmentSlotGroup.LEGS);
        map.put(Component.translatable("item.modifiers.feet").getString(), EquipmentSlotGroup.FEET);
        map.put(Component.translatable("item.modifiers.armor").getString(), EquipmentSlotGroup.ARMOR);
    });


    private static final Set<ResourceLocation> BASE_ATTRIBUTE_IDS = Util.make(new HashSet<>(), set -> {
        set.add(ForgeRegistries.ATTRIBUTES.getKey(Attributes.ATTACK_DAMAGE));
        set.add(ForgeRegistries.ATTRIBUTES.getKey(Attributes.ATTACK_SPEED));
        set.add(ForgeMod.ENTITY_REACH.getId());
        set.remove(null);
    });

    private static final Map<Attribute, UUID> BASE_MODIFIER_IDS = Util.make(new HashMap<>(), map -> {
        map.put(Attributes.ATTACK_DAMAGE, UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF"));
        map.put(Attributes.ATTACK_SPEED, UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"));
    });

    public enum EquipmentSlotGroup {
        MAINHAND(EquipmentSlot.MAINHAND),
        OFFHAND(EquipmentSlot.OFFHAND),
        HAND(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND),
        HEAD(EquipmentSlot.HEAD),
        CHEST(EquipmentSlot.CHEST),
        LEGS(EquipmentSlot.LEGS),
        FEET(EquipmentSlot.FEET),
        ARMOR(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

        private final EquipmentSlot[] slots;

        EquipmentSlotGroup(EquipmentSlot... slots) {
            this.slots = slots;
        }

        public EquipmentSlot[] getSlots() {
            return slots;
        }

        public boolean contains(EquipmentSlot slot) {
            for (EquipmentSlot s : slots) {
                if (s == slot) return true;
            }
            return false;
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (AFConfig.ENABLE_ATTRIBUTE_TOOLTIP_MERGING.get()) {
            processTooltip(event.getItemStack(), event.getToolTip(), event.getEntity());
        }
    }

    private static Set<String> getModifierIdKeys(Multimap<Attribute, AttributeModifier> map) {
        Set<String> keys = new HashSet<>();
        map.forEach((attr, mod) -> {
            ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attr);
            if (attrId != null) {
                keys.add(attrId + ":" + mod.getId());
            }
        });
        return keys;
    }

    private static boolean containsExclusiveModifiers(Multimap<Attribute, AttributeModifier> source, Multimap<Attribute, AttributeModifier> target) {
        Set<String> sourceKeys = getModifierIdKeys(source);
        Set<String> targetKeys = getModifierIdKeys(target);
        return !targetKeys.containsAll(sourceKeys);
    }

    public static void processTooltip(ItemStack stack, List<Component> tooltip, @Nullable Player player) {
        List<AttributeSection> sections = findAttributeSections(tooltip);
        if (sections.isEmpty()) {
            return;
        }

        EquipmentSlotGroup initialPrimaryGroup = null;
        List<EquipmentSlotGroup> priorityOrder = List.of(
                EquipmentSlotGroup.HEAD, EquipmentSlotGroup.CHEST, EquipmentSlotGroup.LEGS, EquipmentSlotGroup.FEET,
                EquipmentSlotGroup.MAINHAND,
                EquipmentSlotGroup.HAND,
                EquipmentSlotGroup.ARMOR
        );

        for (EquipmentSlotGroup potentialPrimary : priorityOrder) {
            for (AttributeSection section : sections) {
                if (section.slot == potentialPrimary) {
                    initialPrimaryGroup = potentialPrimary;
                    break;
                }
            }
            if (initialPrimaryGroup != null) break;
        }

        if (initialPrimaryGroup == null) {
            return;
        }

        Multimap<Attribute, AttributeModifier> handMods = getSortedModifiers(stack, EquipmentSlotGroup.HAND);
        Multimap<Attribute, AttributeModifier> mainhandMods = getSortedModifiers(stack, EquipmentSlotGroup.MAINHAND);
        Multimap<Attribute, AttributeModifier> offhandMods = getSortedModifiers(stack, EquipmentSlotGroup.OFFHAND);

        EquipmentSlotGroup finalPrimaryGroup = initialPrimaryGroup;

        if (initialPrimaryGroup == EquipmentSlotGroup.HAND) {
            boolean mainhandHasExclusives = !mainhandMods.isEmpty() && containsExclusiveModifiers(mainhandMods, handMods);
            boolean offhandHasExclusives = !offhandMods.isEmpty() && containsExclusiveModifiers(offhandMods, handMods);

            if (mainhandHasExclusives) {
                finalPrimaryGroup = EquipmentSlotGroup.MAINHAND;
            } else if (offhandHasExclusives) {
                finalPrimaryGroup = EquipmentSlotGroup.OFFHAND;
            } else {
                Set<String> mainKeys = getModifierIdKeys(mainhandMods);
                Set<String> offKeys = getModifierIdKeys(offhandMods);
                if (!mainKeys.equals(offKeys)) {
                    if (!mainhandMods.isEmpty()) {
                        finalPrimaryGroup = EquipmentSlotGroup.MAINHAND;
                    } else if (!offhandMods.isEmpty()) {
                        finalPrimaryGroup = EquipmentSlotGroup.OFFHAND;
                    }
                }
            }
        }

        Multimap<Attribute, AttributeModifier> combinedModifiers = LinkedListMultimap.create();
        if (finalPrimaryGroup == EquipmentSlotGroup.HAND) {
            combinedModifiers.putAll(handMods);
            addNonDuplicateModifiers(combinedModifiers, mainhandMods);
        } else if (finalPrimaryGroup == EquipmentSlotGroup.MAINHAND) {
            combinedModifiers.putAll(mainhandMods);
            addNonDuplicateModifiers(combinedModifiers, handMods);
        } else if (finalPrimaryGroup == EquipmentSlotGroup.OFFHAND) {
            combinedModifiers.putAll(offhandMods);
            addNonDuplicateModifiers(combinedModifiers, handMods);
        } else {
            combinedModifiers.putAll(getSortedModifiers(stack, finalPrimaryGroup));
        }

        Set<EquipmentSlotGroup> additionalSlotGroups = new HashSet<>();
        if (finalPrimaryGroup == EquipmentSlotGroup.HEAD ||
                finalPrimaryGroup == EquipmentSlotGroup.CHEST ||
                finalPrimaryGroup == EquipmentSlotGroup.LEGS ||
                finalPrimaryGroup == EquipmentSlotGroup.FEET ) {
            additionalSlotGroups.add(EquipmentSlotGroup.ARMOR);
        }

        for(EquipmentSlotGroup additionalGroup : additionalSlotGroups) {
            Multimap<Attribute, AttributeModifier> additionalModifiers = getSortedModifiers(stack, additionalGroup);
            addNonDuplicateModifiers(combinedModifiers, additionalModifiers);
        }

        EquipmentSlotGroup groupForHeader = finalPrimaryGroup;

        if (combinedModifiers.isEmpty()) {
            return;
        }

        List<Component> newTooltip = new ArrayList<>();
        int currentOriginalIndex = 0;
        List<AttributeSection> sortedSections = new ArrayList<>(sections);
        sortedSections.sort(Comparator.comparingInt(s -> s.startIndex));
        AttributeSection firstSectionOverall = sortedSections.get(0);
        AttributeSection lastSectionOverall = sortedSections.get(sortedSections.size() - 1);
        int endOfLastSectionIndex = lastSectionOverall.startIndex + lastSectionOverall.lineCount;

        while (currentOriginalIndex < firstSectionOverall.startIndex) {
            newTooltip.add(tooltip.get(currentOriginalIndex++));
        }

        Component finalHeader = getHeaderForSlotGroup(groupForHeader);
        newTooltip.add(finalHeader);

        TooltipApplyResult applyResult = applyTextFor(newTooltip::add, combinedModifiers, player);

        // Skip original attribute lines
        currentOriginalIndex = endOfLastSectionIndex + 1;
        while (currentOriginalIndex < tooltip.size()) {
            newTooltip.add(tooltip.get(currentOriginalIndex++));
        }

        tooltip.clear();
        tooltip.addAll(newTooltip);
        new ProcessingResult(true, finalHeader, applyResult.needsShiftPrompt);
    }

    private static void addNonDuplicateModifiers(
            Multimap<Attribute, AttributeModifier> target,
            Multimap<Attribute, AttributeModifier> source) {

        Set<String> existingIds = getModifierIdKeys(target);

        source.forEach((attr, mod) -> {
            ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attr);
            if (attrId != null) {
                String key = attrId + ":" + mod.getId();
                if (!existingIds.contains(key)) {
                    target.put(attr, mod);
                }
            }
        });
    }

    private static Component getHeaderForSlotGroup(EquipmentSlotGroup group) {
        String groupName = group.name().toLowerCase(Locale.ROOT);
        String key = "item.modifiers." + groupName;
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    private static Multimap<Attribute, AttributeModifier> getSortedModifiers(ItemStack stack, EquipmentSlotGroup group) {
        Multimap<Attribute, AttributeModifier> map = LinkedListMultimap.create();
        for (EquipmentSlot slot : group.getSlots()) {
            map.putAll(stack.getAttributeModifiers(slot));
        }
        return map;
    }

    public static class TooltipApplyResult {
        boolean needsShiftPrompt = false;
        Set<Attribute> handledAttributes = new HashSet<>();
    }

    private static TooltipApplyResult applyTextFor(
            Consumer<Component> tooltip,
            Multimap<Attribute, AttributeModifier> modifierMap,
            @Nullable Player player) {

        TooltipApplyResult result = new TooltipApplyResult();
        if (modifierMap.isEmpty()) {
            return result;
        }

        Map<Attribute, BaseModifier> baseModifiers = new Reference2ReferenceLinkedOpenHashMap<>();
        Multimap<Attribute, AttributeModifier> remainingModifiers = LinkedListMultimap.create();

        separateBaseModifiers(modifierMap, baseModifiers, remainingModifiers);
        processBaseModifiers(tooltip, player, baseModifiers, result);
        processRemainingModifiers(tooltip, modifierMap, baseModifiers.keySet(), result);

        return result;
    }

    private static void separateBaseModifiers(
            Multimap<Attribute, AttributeModifier> modifierMap,
            Map<Attribute, BaseModifier> baseModifiersOutput,
            Multimap<Attribute, AttributeModifier> remainingModifiersOutput) {

        remainingModifiersOutput.putAll(modifierMap);
        var it = remainingModifiersOutput.entries().iterator();

        while (it.hasNext()) {
            var entry = it.next();
            Attribute attr = entry.getKey();
            AttributeModifier modifier = entry.getValue();

            if (isBaseModifier(attr, modifier)) {
                baseModifiersOutput.put(attr, new BaseModifier(modifier, new ArrayList<>()));
                it.remove();
            }
        }

        it = remainingModifiersOutput.entries().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Attribute attr = entry.getKey();
            AttributeModifier modifier = entry.getValue();
            BaseModifier base = baseModifiersOutput.get(attr);

            if (base != null && isBaseAttribute(attr)) {
                base.children.add(modifier);
                it.remove();
            }
        }
    }

    private static void processBaseModifiers(
            Consumer<Component> tooltip,
            @Nullable Player player,
            Map<Attribute, BaseModifier> baseModifiers,
            TooltipApplyResult result) {

        for (var entry : baseModifiers.entrySet()) {
            Attribute attr = entry.getKey();
            BaseModifier baseModifier = entry.getValue();

            double entityBase = player == null ? 0 : player.getAttributeBaseValue(attr);
            double baseValueFromModifier = baseModifier.base.getAmount();
            double rawBaseValue = baseValueFromModifier + entityBase;
            double finalValue = rawBaseValue;

            baseModifier.children.sort(ATTRIBUTE_MODIFIER_COMPARATOR);

            for (AttributeModifier childModifier : baseModifier.children) {
                finalValue = applyModifier(finalValue, rawBaseValue, childModifier);
            }

            boolean isMerged = !baseModifier.children.isEmpty();
            result.needsShiftPrompt |= isMerged;

            MutableComponent text = createBaseComponent(attr, finalValue);
            ChatFormatting color = isMerged ? null : BASE_COLOR;
            Integer intColor = isMerged ? MERGE_BASE_MODIFIER_COLOR : null;
            tooltip.accept(Component.literal(" ").append(text.withStyle(style -> {
                if (intColor != null) return style.withColor(intColor);
                return style.applyFormat(color);
            })));

            if (Screen.hasShiftDown() && isMerged) {
                text = createBaseComponent(attr, rawBaseValue);
                tooltip.accept(listHeader().append(text.withStyle(BASE_COLOR)));

                for (AttributeModifier modifier : baseModifier.children) {
                    tooltip.accept(listHeader().append(createModifierComponent(attr, modifier)));
                }
            }

            result.handledAttributes.add(attr);
        }
    }

    private static double applyModifier(double currentValue, double baseValue, AttributeModifier modifier) {
        return switch (modifier.getOperation()) {
            case ADDITION -> currentValue + modifier.getAmount();
            case MULTIPLY_BASE -> currentValue + modifier.getAmount() * baseValue;
            case MULTIPLY_TOTAL -> currentValue * (1.0 + modifier.getAmount());
        };
    }

    private static void processRemainingModifiers(
            Consumer<Component> tooltip,
            Multimap<Attribute, AttributeModifier> remainingModifiers,
            Set<Attribute> processedBaseAttributes,
            TooltipApplyResult result) {

        Map<Attribute, Collection<AttributeModifier>> sortedRemaining = new TreeMap<>(Comparator.comparing(
                ForgeRegistries.ATTRIBUTES::getKey,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        for (Attribute attr : remainingModifiers.keySet()) {
            if (!processedBaseAttributes.contains(attr)) {
                List<AttributeModifier> mods = new ArrayList<>(remainingModifiers.get(attr));
                mods.sort(ATTRIBUTE_MODIFIER_COMPARATOR);
                sortedRemaining.put(attr, mods);
            }
        }

        for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : sortedRemaining.entrySet()) {
            Attribute attr = entry.getKey();
            Collection<AttributeModifier> modifiers = entry.getValue();


            if (result.handledAttributes.contains(attr)) {
                continue;
            }
            if (modifiers.isEmpty()) continue;

            handleNonBaseMerging(attr, modifiers, tooltip, result);
            result.handledAttributes.add(attr);
        }
    }

    private static void handleNonBaseMerging(
            Attribute attr,
            Collection<AttributeModifier> modifiers,
            Consumer<Component> tooltip,
            TooltipApplyResult result) {

        Map<AttributeModifier.Operation, MergedModifierData> mergeData = new EnumMap<>(AttributeModifier.Operation.class);
        List<AttributeModifier> nonMergeable = new ArrayList<>();

        for (AttributeModifier modifier : modifiers) {
            if (modifier.getAmount() == 0) continue;

            boolean canMerge = modifier.getOperation() == AttributeModifier.Operation.ADDITION ||
                    modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE ||
                    modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL;

            if (isBaseModifier(attr, modifier)) {
                canMerge = false;
            }

            if (canMerge) {
                MergedModifierData data = mergeData.computeIfAbsent(modifier.getOperation(), op -> new MergedModifierData());
                if (!data.children.isEmpty()) {
                    data.isMerged = true;
                    result.needsShiftPrompt = true;
                }
                data.sum += modifier.getAmount();
                data.children.add(modifier);
            } else {
                nonMergeable.add(modifier);
            }
        }

        for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
            MergedModifierData data = mergeData.get(op);
            if (data == null || data.sum == 0) continue;

            AttributeModifier fakeModifier = new AttributeModifier(FAKE_MERGED_ID.toString(), data.sum, op);
            MutableComponent modComponent = createModifierComponent(attr, fakeModifier);

            if (data.isMerged) {
                tooltip.accept(modComponent.withStyle(style -> style.withColor(MERGED_MODIFIER_COLOR)));

                if (Screen.hasShiftDown()) {
                    data.children.sort(ATTRIBUTE_MODIFIER_COMPARATOR);
                    for (AttributeModifier mod : data.children) {
                        tooltip.accept(listHeader().append(createModifierComponent(attr, mod)));
                    }
                }
            } else if (!data.children.isEmpty()) {
                tooltip.accept(createModifierComponent(attr, data.children.get(0)));
            }
        }

        nonMergeable.sort(ATTRIBUTE_MODIFIER_COMPARATOR);
        for (AttributeModifier modifier : nonMergeable) {
            tooltip.accept(createModifierComponent(attr, modifier));
        }
    }

    private static MutableComponent createBaseComponent(Attribute attribute, double value) {
        return Component.translatable("attribute.modifier.equals.0",
                FORMAT.format(value),
                Component.translatable(attribute.getDescriptionId()));
    }

    public static MutableComponent createModifierComponent(Attribute attribute, AttributeModifier modifier) {
        double value = modifier.getAmount();
        boolean isPositive = value > 0;

        String key = isPositive ?
                "attribute.modifier.plus." + modifier.getOperation().toValue() :
                "attribute.modifier.take." + modifier.getOperation().toValue();
        String formattedValue = formatValue(attribute, value, modifier.getOperation());

        MutableComponent component = Component.translatable(key,
                formattedValue,
                Component.translatable(attribute.getDescriptionId()));

        if (!isBaseAttribute(attribute) && modifier.getId().equals(FAKE_MERGED_ID)) {
            return component.withStyle(style -> style.withColor(MERGED_MODIFIER_COLOR));
        }

        ChatFormatting color = ChatFormatting.WHITE;
        boolean handledByRule = false;

        if (!handledByRule) {
            color = isPositive ? ChatFormatting.BLUE : ChatFormatting.RED;
        }

        final ChatFormatting finalColor = color;
        return component.withStyle(style -> style.withColor(finalColor));
    }

    private static String formatValue(Attribute attribute, double value, AttributeModifier.Operation operation) {
        double absValue = Math.abs(value);

        if (operation == AttributeModifier.Operation.ADDITION) {
            if (attribute == Attributes.KNOCKBACK_RESISTANCE) {
                return FORMAT.format(absValue * 100) + "%" ;
            } else {
                return FORMAT.format(absValue);
            }
        } else {
            return FORMAT.format(absValue * 100);
        }
    }

    private static boolean isBaseAttribute(Attribute attribute) {
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return id != null && BASE_ATTRIBUTE_IDS.contains(id);
    }

    private static boolean isBaseModifier(Attribute attribute, AttributeModifier modifier) {
        UUID baseId = getBaseModifierId(attribute);
        return modifier.getId().equals(baseId);
    }

    @Nullable
    private static UUID getBaseModifierId(Attribute attribute) {
        return BASE_MODIFIER_IDS.get(attribute);
    }

    public static MutableComponent listHeader() {
        return Component.literal(" ┇ ").withStyle(ChatFormatting.GRAY);
    }

    private static List<AttributeSection> findAttributeSections(List<Component> tooltip) {
        List<AttributeSection> result = new ArrayList<>();
        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            EquipmentSlotGroup slot = getSlotFromText(line);

            if (slot != null) {
                int numLines = countAttributeLines(tooltip, i + 1);
                if (numLines > 0) {
                    result.add(new AttributeSection(i, numLines, slot));
                }
            }
        }
        return result;
    }

    public static EquipmentSlotGroup getSlotFromText(Component text) {
        String content = text.getString();
        return KEY_SLOT_MAP.get(content);
    }

    private static int countAttributeLines(List<Component> tooltip, int startIndex) {
        int count = 0;
        for (int i = startIndex; i < tooltip.size(); i++) {
            Component lineComp = tooltip.get(i);
            String line = lineComp.getString();

            if (line.isEmpty() || getSlotFromText(lineComp) != null) {
                break;
            }

            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                break;
            }
            char firstChar = trimmedLine.charAt(0);
            boolean looksLikeModifier = line.startsWith(" ") || firstChar == '+' || firstChar == '-' || Character.isDigit(firstChar) || firstChar == '(';

            if (!looksLikeModifier) {
                break;
            }

            count++;
        }
        return count;
    }

    public static class BaseModifier {
        final AttributeModifier base;
        final List<AttributeModifier> children;

        BaseModifier(AttributeModifier base, List<AttributeModifier> children) {
            this.base = base;
            this.children = children;
        }
    }

    private static class MergedModifierData {
        double sum = 0;
        boolean isMerged = false;
        List<AttributeModifier> children = new ArrayList<>();
    }

    private record AttributeSection(int startIndex, int lineCount, EquipmentSlotGroup slot) {
    }

    public record ProcessingResult(boolean modified, @Nullable Component finalHeader, boolean needsShiftPrompt) {
    }
}
