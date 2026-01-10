package net.yixi_xun.affix_core.tooltip;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
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
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.AFConfig;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Consumer;

import static net.yixi_xun.affix_core.AffixCoreMod.MODID;

/**
 * Forge版属性修改器工具提示处理器，来源于Fabric的DynamicTooltips模组
 * 用于合并和显示物品的属性修改器信息
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class AttributeTooltipHandler {

    private static final DecimalFormat FORMAT = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.ROOT));
    private static final ResourceLocation FAKE_MERGED_ID = ResourceLocation.fromNamespaceAndPath(MODID, "fake_merged_modifier");

    private static final UUID FAKE_MERGED_UUID = UUID.nameUUIDFromBytes(FAKE_MERGED_ID.toString().getBytes());

    // 基础属性ID集合，这些属性被视为"基础"修饰符
    private static final Set<ResourceLocation> BASE_ATTRIBUTE_IDS;
    static {
        Set<ResourceLocation> tempSet = new HashSet<>();
        addIfNotNull(tempSet, Attributes.ATTACK_DAMAGE);
        addIfNotNull(tempSet, Attributes.ATTACK_SPEED);
        tempSet.add(ForgeMod.ENTITY_REACH.getId());
        BASE_ATTRIBUTE_IDS = Collections.unmodifiableSet(tempSet);
    }

    // 辅助方法：减少重复代码
    private static void addIfNotNull(Set<ResourceLocation> set, Attribute attr) {
        ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attr);
        if (key != null) set.add(key);
    }

    // 基础修饰符ID映射
    private static final Map<ResourceLocation, ResourceLocation> BASE_MODIFIER_IDS;
    static {
        Map<ResourceLocation, ResourceLocation> tempMap = new HashMap<>();
        ResourceLocation attackDamageKey = ForgeRegistries.ATTRIBUTES.getKey(Attributes.ATTACK_DAMAGE);
        ResourceLocation attackSpeedKey = ForgeRegistries.ATTRIBUTES.getKey(Attributes.ATTACK_SPEED);
        
        if (attackDamageKey != null) {
            tempMap.put(attackDamageKey, ResourceLocation.fromNamespaceAndPath("minecraft", "base_attack_damage"));
        }
        if (attackSpeedKey != null) {
            tempMap.put(attackSpeedKey, ResourceLocation.fromNamespaceAndPath("minecraft", "base_attack_speed"));
        }
        BASE_MODIFIER_IDS = Collections.unmodifiableMap(tempMap);
    }

    private static final Map<String, EquipmentSlot> KEY_SLOT_MAP = new HashMap<>();
    static {
        putSlot("item.modifiers.mainhand", EquipmentSlot.MAINHAND);
        putSlot("item.modifiers.offhand", EquipmentSlot.OFFHAND);
        putSlot("item.modifiers.head", EquipmentSlot.HEAD);
        putSlot("item.modifiers.chest", EquipmentSlot.CHEST);
        putSlot("item.modifiers.legs", EquipmentSlot.LEGS);
        putSlot("item.modifiers.feet", EquipmentSlot.FEET);
    }

    private static void putSlot(String key, EquipmentSlot slot) {
        AttributeTooltipHandler.KEY_SLOT_MAP.put(Component.translatable(key).getString(), slot);
    }

    /**
     * 处理物品工具提示事件
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!AFConfig.ENABLE_ATTRIBUTE_TOOLTIP_MERGING.get()) return;
        
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        Player player = event.getEntity();

        List<AttributeSection> sections = findAttributeSections(tooltip);
        if (sections.isEmpty()) return;

        EquipmentSlot initialPrimaryGroup = determinePrimarySlot(stack, sections);
        if (initialPrimaryGroup == null) return;

        // 获取修饰符
        Multimap<Attribute, AttributeModifier> mainhandMods = getSortedModifiers(stack, EquipmentSlot.MAINHAND);
        Multimap<Attribute, AttributeModifier> offhandMods = getSortedModifiers(stack, EquipmentSlot.OFFHAND);

        Multimap<Attribute, AttributeModifier> combinedModifiers = combineModifiers(stack, initialPrimaryGroup, mainhandMods, offhandMods);
        handleArmorMerging(combinedModifiers, stack, initialPrimaryGroup);

        if (combinedModifiers.isEmpty()) return;

        applyNewTooltip(tooltip, sections, combinedModifiers, stack, player, initialPrimaryGroup);
    }

    /**
     * 确定主要装备槽位
     */
    private static EquipmentSlot determinePrimarySlot(ItemStack stack, List<AttributeSection> sections) {
        // 首先检查是否为护甲物品
        if (stack.getItem().canEquip(stack, EquipmentSlot.HEAD, null)) {
            return EquipmentSlot.HEAD;
        } else if (stack.getItem().canEquip(stack, EquipmentSlot.CHEST, null)) {
            return EquipmentSlot.CHEST;
        } else if (stack.getItem().canEquip(stack, EquipmentSlot.LEGS, null)) {
            return EquipmentSlot.LEGS;
        } else if (stack.getItem().canEquip(stack, EquipmentSlot.FEET, null)) {
            return EquipmentSlot.FEET;
        } else if (stack.getItem().canEquip(stack, EquipmentSlot.MAINHAND, null)) {
            return EquipmentSlot.MAINHAND;
        } else if (stack.getItem().canEquip(stack, EquipmentSlot.OFFHAND, null)) {
            return EquipmentSlot.OFFHAND;
        }
        
        // 如果不能装备，根据工具提示中出现的槽位决定
        List<EquipmentSlot> priorityOrder = Arrays.asList(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, // 特定护甲槽
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
        );

        for (EquipmentSlot potentialPrimary : priorityOrder) {
            for (AttributeSection section : sections) {
                if (section.slot == potentialPrimary) {
                    return potentialPrimary;
                }
            }
        }

        return null;
    }

    /**
     * 合并修饰符
     */
    private static Multimap<Attribute, AttributeModifier> combineModifiers(
            ItemStack stack,
            EquipmentSlot finalPrimaryGroup,
            Multimap<Attribute, AttributeModifier> mainhandMods,
            Multimap<Attribute, AttributeModifier> offhandMods) {

        Multimap<Attribute, AttributeModifier> combinedModifiers = ArrayListMultimap.create();
        
        if (finalPrimaryGroup == EquipmentSlot.MAINHAND) {
            combinedModifiers.putAll(mainhandMods);
        } else if (finalPrimaryGroup == EquipmentSlot.OFFHAND) {
            combinedModifiers.putAll(offhandMods);
        } else {
            // 对于护甲/身体槽位，使用它们自己的修饰符
            combinedModifiers.putAll(getSortedModifiers(stack, finalPrimaryGroup));
        }

        return combinedModifiers;
    }

    /**
     * 处理护甲合并
     */
    private static void handleArmorMerging(
            Multimap<Attribute, AttributeModifier> combinedModifiers,
            ItemStack stack,
            EquipmentSlot finalPrimaryGroup) {

        Set<EquipmentSlot> additionalSlotGroups = new HashSet<>();
        if (finalPrimaryGroup == EquipmentSlot.HEAD ||
            finalPrimaryGroup == EquipmentSlot.CHEST ||
            finalPrimaryGroup == EquipmentSlot.LEGS ||
            finalPrimaryGroup == EquipmentSlot.FEET) {
            // 添加所有护甲槽位
            additionalSlotGroups.add(EquipmentSlot.HEAD);
            additionalSlotGroups.add(EquipmentSlot.CHEST);
            additionalSlotGroups.add(EquipmentSlot.LEGS);
            additionalSlotGroups.add(EquipmentSlot.FEET);
        }

        for (EquipmentSlot additionalSlot : additionalSlotGroups) {
            Multimap<Attribute, AttributeModifier> additionalModifiers = getSortedModifiers(stack, additionalSlot);
            addNonDuplicateModifiers(combinedModifiers, additionalModifiers);
        }
    }

    /**
     * 应用新的工具提示
     */
    private static void applyNewTooltip(
            List<Component> tooltip,
            List<AttributeSection> sections,
            Multimap<Attribute, AttributeModifier> combinedModifiers,
            ItemStack stack,
            Player player,
            EquipmentSlot slotForHeader) {

        List<Component> newTooltip = new ArrayList<>();
        int currentOriginalIndex = 0;
        List<AttributeSection> sortedSections = new ArrayList<>(sections);
        sortedSections.sort(Comparator.comparingInt(s -> s.startIndex));
        AttributeSection firstSectionOverall = sortedSections.get(0);
        AttributeSection lastSectionOverall = sortedSections.get(sortedSections.size() - 1);
        int endOfLastSectionIndex = lastSectionOverall.startIndex + lastSectionOverall.lineCount;

        // 添加原始工具提示的前半部分
        while (currentOriginalIndex < firstSectionOverall.startIndex) {
            newTooltip.add(tooltip.get(currentOriginalIndex++));
        }

        // 添加头部
        Component finalHeader = getHeaderForSlot(slotForHeader);
        newTooltip.add(finalHeader);

        // 应用属性文本
        applyTextFor(stack, newTooltip::add, combinedModifiers, player);

        // 跳过原有的属性行
        currentOriginalIndex = endOfLastSectionIndex + 1;
        while (currentOriginalIndex < tooltip.size()) {
            newTooltip.add(tooltip.get(currentOriginalIndex++));
        }

        tooltip.clear();
        tooltip.addAll(newTooltip);
    }

    /**
     * 添加非重复修饰符
     */
    private static void addNonDuplicateModifiers(
            Multimap<Attribute, AttributeModifier> target,
            Multimap<Attribute, AttributeModifier> source) {

        Set<String> existingIds = new HashSet<>();
        target.entries().forEach(entry -> {
            ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            if (attrId != null) {
                existingIds.add(attrId + ":" + entry.getValue().getId());
            }
        });

        source.entries().forEach(entry -> {
            ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            if (attrId != null) {
                String key = attrId + ":" + entry.getValue().getId();
                if (!existingIds.contains(key)) {
                    target.put(entry.getKey(), entry.getValue());
                }
            }
        });
    }

    private static Component getHeaderForSlot(EquipmentSlot slot) {
        String key = switch (slot) {
            case MAINHAND -> "item.modifiers.mainhand";
            case OFFHAND -> "item.modifiers.offhand";
            case HEAD -> "item.modifiers.head";
            case CHEST -> "item.modifiers.chest";
            case LEGS -> "item.modifiers.legs";
            case FEET -> "item.modifiers.feet";
        };
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    /**
     * 获取排序后的修饰符
     */
    private static Multimap<Attribute, AttributeModifier> getSortedModifiers(ItemStack stack, EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> map = ArrayListMultimap.create();

        // 获取指定槽位的所有修饰符
        stack.getAttributeModifiers(slot).forEach((attribute, modifier) -> {
            if (attribute != null && modifier != null) {
                map.put(attribute, modifier);
            }
        });

        return map;
    }

    /**
     * 应用文本到工具提示
     */
    private static void applyTextFor(
            ItemStack stack,
            Consumer<Component> tooltip,
            Multimap<Attribute, AttributeModifier> modifierMap,
            Player player) {

        if (modifierMap.isEmpty()) {
            return;
        }

        // 分离基础修饰符和剩余修饰符
        Map<Attribute, BaseModifier> baseModifiers = new LinkedHashMap<>();
        Multimap<Attribute, AttributeModifier> remainingModifiers = ArrayListMultimap.create();

        separateBaseModifiers(modifierMap, baseModifiers, remainingModifiers);
        processBaseModifiers(stack, tooltip, player, baseModifiers);
        processRemainingModifiers(stack, tooltip, player, remainingModifiers, baseModifiers.keySet());
    }

    /**
     * 分离基础修饰符
     */
    private static void separateBaseModifiers(
            Multimap<Attribute, AttributeModifier> modifierMap,
            Map<Attribute, BaseModifier> baseModifiersOutput,
            Multimap<Attribute, AttributeModifier> remainingModifiersOutput) {

        remainingModifiersOutput.putAll(modifierMap);
        List<Map.Entry<Attribute, AttributeModifier>> entries = new ArrayList<>(remainingModifiersOutput.entries());
        remainingModifiersOutput.clear();

        for (Map.Entry<Attribute, AttributeModifier> entry : entries) {
            Attribute attr = entry.getKey();
            AttributeModifier modifier = entry.getValue();

            if (isBaseModifier(attr, modifier)) {
                baseModifiersOutput.put(attr, new BaseModifier(modifier, new ArrayList<>()));
            } else {
                remainingModifiersOutput.put(attr, modifier);
            }
        }

        // 再次遍历剩余修饰符，看是否有属于基础属性的子修饰符
        entries = new ArrayList<>(remainingModifiersOutput.entries());
        remainingModifiersOutput.clear();

        for (Map.Entry<Attribute, AttributeModifier> entry : entries) {
            Attribute attr = entry.getKey();
            AttributeModifier modifier = entry.getValue();
            BaseModifier base = baseModifiersOutput.get(attr);

            if (base != null && isBaseAttribute(attr)) {
                base.children.add(modifier);
            } else {
                remainingModifiersOutput.put(attr, modifier);
            }
        }
    }

    /**
     * 处理基础修饰符
     */
    private static void processBaseModifiers(
            ItemStack stack,
            Consumer<Component> tooltip,
            Player player,
            Map<Attribute, BaseModifier> baseModifiers) {

        // 使用标准排序器对修饰符进行排序
        Comparator<AttributeModifier> attributeModifierComparator = Comparator.comparing(AttributeModifier::getOperation)
                .thenComparing((AttributeModifier a) -> -Math.abs(a.getAmount()))
                .thenComparing(AttributeModifier::getId);

        for (Map.Entry<Attribute, BaseModifier> entry : baseModifiers.entrySet()) {
            Attribute attr = entry.getKey();
            BaseModifier baseModifier = entry.getValue();

            double entityBase = player != null ? player.getAttributeBaseValue(attr) : 0;
            double baseValueFromModifier = baseModifier.base.getAmount();
            double rawBaseValue = baseValueFromModifier + entityBase;
            double finalValue = rawBaseValue;

            // 按操作排序子修饰符以确保正确的计算顺序
            baseModifier.children.sort(attributeModifierComparator);

            for (AttributeModifier childModifier : baseModifier.children) {
                finalValue = applyModifier(finalValue, rawBaseValue, childModifier.getOperation(), childModifier.getAmount());
            }

            boolean isMerged = !baseModifier.children.isEmpty();

            MutableComponent text = createBaseComponent(attr, finalValue, entityBase, isMerged);
            ChatFormatting color = isMerged ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN;
            tooltip.accept(Component.literal(" ").append(text.withStyle(color)));

            if (isMerged) {
                // 显示详细视图
                text = createBaseComponent(attr, rawBaseValue, entityBase, false);
                tooltip.accept(listHeader().append(text.withStyle(ChatFormatting.DARK_GREEN)));

                for (AttributeModifier modifier : baseModifier.children) {
                    tooltip.accept(listHeader().append(createModifierComponent(attr, modifier)));
                }
            }
        }
    }

    /**
     * 处理剩余的修饰符
     */
    private static void processRemainingModifiers(
            ItemStack stack,
            Consumer<Component> tooltip,
            Player player,
            Multimap<Attribute, AttributeModifier> remainingModifiers,
            Set<Attribute> processedBaseAttributes) {

        // 按属性排序
        List<Map.Entry<Attribute, Collection<AttributeModifier>>> sortedEntries = remainingModifiers.asMap().entrySet().stream()
                .filter(entry -> {
                    Attribute attr = entry.getKey();
                    ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attr);
                    return attrId != null && !processedBaseAttributes.contains(attr);
                })
                .sorted(Comparator.comparing(entry -> ForgeRegistries.ATTRIBUTES.getKey(entry.getKey()),
                                             Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : sortedEntries) {
            Attribute attr = entry.getKey();
            Collection<AttributeModifier> modifiers = entry.getValue();

            if (modifiers.isEmpty()) continue;

            // 处理非基础合并
            handleNonBaseMerging(attr, modifiers, tooltip);
        }
    }

    /**
     * 处理非基础合并
     */
    private static void handleNonBaseMerging(
            Attribute attr,
            Collection<AttributeModifier> modifiers,
            Consumer<Component> tooltip) {

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

            AttributeModifier fakeModifier = new AttributeModifier(FAKE_MERGED_UUID, FAKE_MERGED_ID.getPath(), data.sum, op);
            MutableComponent modComponent = createModifierComponent(attr, fakeModifier);

            if (data.isMerged) {
                // 使用浅蓝色表示合并的非基础属性
                tooltip.accept(modComponent.withStyle(ChatFormatting.AQUA));

                data.children.sort(Comparator.comparing(AttributeModifier::getOperation)
                        .thenComparing(mod -> -Math.abs(mod.getAmount()))
                        .thenComparing(AttributeModifier::getId));
                for (AttributeModifier mod : data.children) {
                    tooltip.accept(listHeader().append(createModifierComponent(attr, mod)));
                }
            } else if (!data.children.isEmpty()) {
                tooltip.accept(createModifierComponent(attr, data.children.get(0)));
            }
        }

        nonMergeable.sort(Comparator.comparing(AttributeModifier::getOperation)
                .thenComparing(mod -> -Math.abs(mod.getAmount()))
                .thenComparing(AttributeModifier::getId));
        for (AttributeModifier modifier : nonMergeable) {
            tooltip.accept(createModifierComponent(attr, modifier));
        }
    }

    /**
     * 应用修饰符
     */
    private static double applyModifier(double currentValue, double baseValue, AttributeModifier.Operation operation, double amount) {
        return switch (operation) {
            case ADDITION -> currentValue + amount;
            case MULTIPLY_BASE -> currentValue + amount * baseValue;
            case MULTIPLY_TOTAL -> currentValue * (1.0 + amount);
        };
    }

    /**
     * 创建基础组件
     */
    private static MutableComponent createBaseComponent(Attribute attribute, double value, double entityBase, boolean merged) {
        return Component.translatable("attribute.modifier.equals.0",
                FORMAT.format(value),
                Component.translatable(attribute.getDescriptionId()));
    }

    /**
     * 优化4: 还原原版属性的颜色逻辑
     */
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

        // 检查是否为假合并的修饰符（浅蓝色）
        if (!isBaseAttribute(attribute) && modifier.getId().equals(FAKE_MERGED_UUID)) {
            return component.withStyle(ChatFormatting.AQUA);
        }

        ChatFormatting color = isPositive ? ChatFormatting.BLUE : ChatFormatting.RED;

        return component.withStyle(color);
    }

    /**
     * 格式化值
     */
    private static String formatValue(Attribute attribute, double value, AttributeModifier.Operation operation) {
        double absValue = Math.abs(value);

        if (operation == AttributeModifier.Operation.ADDITION) {
            // 特殊处理击退抗性（显示为百分比）
            if (attribute == Attributes.KNOCKBACK_RESISTANCE) {
                return FORMAT.format(absValue * 100) + "%";
            } else {
                return FORMAT.format(absValue);
            }
        } else {
            return FORMAT.format(absValue * 100) + "%";
        }
    }

    /**
     * 检查是否为基础属性
     */
    private static boolean isBaseAttribute(Attribute attribute) {
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return id != null && BASE_ATTRIBUTE_IDS.contains(id);
    }

    /**
     * 检查是否为基础修饰符
     */
    private static boolean isBaseModifier(Attribute attribute, AttributeModifier modifier) {
        ResourceLocation baseId = getBaseModifierId(attribute);
        return baseId != null && modifier.getId().equals(UUID.nameUUIDFromBytes(baseId.toString().getBytes()));
    }

    /**
     * 获取基础修饰符ID
     */
    private static ResourceLocation getBaseModifierId(Attribute attribute) {
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return id != null ? BASE_MODIFIER_IDS.get(id) : null;
    }

    /**
     * 列表头部
     */
    public static MutableComponent listHeader() {
        return Component.literal(" ┇ ").withStyle(ChatFormatting.GRAY);
    }

    /**
     * 查找属性部分
     */
    private static List<AttributeSection> findAttributeSections(List<Component> tooltip) {
        List<AttributeSection> result = new ArrayList<>();
        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            EquipmentSlot slot = getSlotFromText(line);

            if (slot != null) {
                int numLines = countAttributeLines(tooltip, i + 1);
                if (numLines > 0) {
                    result.add(new AttributeSection(i, numLines, slot));
                }
            }
        }
        return result;
    }

    /**
     * 从文本获取槽位
     */
    public static EquipmentSlot getSlotFromText(Component text) {
        String content = text.getString();
        return KEY_SLOT_MAP.get(content);
    }

    /**
     * 计算属性行数
     */
    private static int countAttributeLines(List<Component> tooltip, int startIndex) {
        int count = 0;
        for (int i = startIndex; i < tooltip.size(); i++) {
            Component lineComp = tooltip.get(i);
            String line = lineComp.getString();

            // 如果遇到空行或其他槽位标题则停止
            if (line.isEmpty() || getSlotFromText(lineComp) != null) {
                break;
            }

            // 检查行是否看起来像修饰符（以空格、+、-、数字或括号开头）
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                break;
            }
            char firstChar = trimmedLine.charAt(0);
            boolean looksLikeModifier = line.startsWith(" ") || firstChar == '+' || firstChar == '-' || 
                                       Character.isDigit(firstChar) || firstChar == '(';

            if (!looksLikeModifier) {
                break;
            }

            count++;
        }
        return count;
    }

    /**
     * 合并修饰符数据类
     */
    private static class MergedModifierData {
        double sum = 0;
        boolean isMerged = false;
        List<AttributeModifier> children = new ArrayList<>();
    }

    /**
         * 基础修饰符类
         */
        private record BaseModifier(AttributeModifier base, List<AttributeModifier> children) {
    }

    /**
         * 属性部分类
         */
        private record AttributeSection(int startIndex, int lineCount, EquipmentSlot slot) {
    }
}
