package net.yixi_xun.affix_core.curios;

import net.minecraft.world.entity.EquipmentSlot;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Curios槽位类型枚举
 * 扩展现有的EquipmentSlot概念，支持Curios API的自定义槽位
 * 提供高效的槽位类型查找和转换功能
 */
public enum CuriosSlotType {
    // Vanilla装备槽位（用于兼容性）
    MAINHAND("mainhand", SlotCategory.VANILLA),
    OFFHAND("offhand", SlotCategory.VANILLA),
    HEAD("head", SlotCategory.VANILLA),
    CHEST("chest", SlotCategory.VANILLA),
    LEGS("legs", SlotCategory.VANILLA),
    FEET("feet", SlotCategory.VANILLA),
    
    // Curios标准槽位
    NECKLACE("necklace", SlotCategory.CURIOS),
    BACK("back", SlotCategory.CURIOS),
    BELT("belt", SlotCategory.CURIOS),
    HANDS("hands", SlotCategory.CURIOS),
    RING("ring", SlotCategory.CURIOS),
    CHARM("charm", SlotCategory.CURIOS),
    BRACELET("bracelet", SlotCategory.CURIOS),
    HEAD_CURIOS("head_curios", SlotCategory.CURIOS),
    BODY("body", SlotCategory.CURIOS),
    FEET_CURIOS("feet_curios", SlotCategory.CURIOS),
    QUIVER("quiver", SlotCategory.CURIOS),
    CURIO("curio", SlotCategory.CURIOS),
    
    // 任意槽位
    ANY("any", SlotCategory.ANY);
    
    private final String slotName;
    private final SlotCategory category;
    
    // 缓存槽位名称到枚举的映射
    private static final Map<String, CuriosSlotType> NAME_TO_TYPE_CACHE = new ConcurrentHashMap<>();
    
    // 缓存EquipmentSlot到CuriosSlotType的映射
    private static final Map<EquipmentSlot, CuriosSlotType> EQUIPMENT_SLOT_CACHE = new EnumMap<>(EquipmentSlot.class);
    
    static {
        // 初始化缓存
        for (CuriosSlotType type : values()) {
            NAME_TO_TYPE_CACHE.put(type.slotName.toLowerCase(), type);
        }
        
        // 初始化EquipmentSlot映射缓存
        EQUIPMENT_SLOT_CACHE.put(EquipmentSlot.MAINHAND, MAINHAND);
        EQUIPMENT_SLOT_CACHE.put(EquipmentSlot.OFFHAND, OFFHAND);
        EQUIPMENT_SLOT_CACHE.put(EquipmentSlot.HEAD, HEAD);
        EQUIPMENT_SLOT_CACHE.put(EquipmentSlot.CHEST, CHEST);
        EQUIPMENT_SLOT_CACHE.put(EquipmentSlot.LEGS, LEGS);
        EQUIPMENT_SLOT_CACHE.put(EquipmentSlot.FEET, FEET);
    }
    
    CuriosSlotType(String slotName, SlotCategory category) {
        this.slotName = slotName;
        this.category = category;
    }
    
    public String getSlotName() {
        return slotName;
    }
    
    public SlotCategory getCategory() {
        return category;
    }
    
    /**
     * 根据槽位名称获取对应的枚举值
     * 使用缓存提高查找性能
     * 
     * @param slotName 槽位名称
     * @return 对应的CuriosSlotType枚举值，如果找不到则返回ANY
     */
    public static CuriosSlotType fromString(String slotName) {
        if (slotName == null || slotName.isEmpty()) {
            return ANY;
        }
        
        // 先从缓存中查找
        CuriosSlotType cached = NAME_TO_TYPE_CACHE.get(slotName.toLowerCase());
        if (cached != null) {
            return cached;
        }
        
        // 缓存中未找到，返回默认值
        return ANY;
    }
    
    /**
     * 将Vanilla的EquipmentSlot转换为CuriosSlotType
     * 使用缓存提高转换性能
     * 
     * @param equipmentSlot 原始EquipmentSlot
     * @return 对应的CuriosSlotType枚举值
     */
    public static CuriosSlotType fromEquipmentSlot(EquipmentSlot equipmentSlot) {
        if (equipmentSlot == null) {
            return ANY;
        }
        
        // 从缓存中获取映射
        CuriosSlotType cached = EQUIPMENT_SLOT_CACHE.get(equipmentSlot);
        return cached != null ? cached : ANY;
    }

    /**
     * 判断是否为任意槽位
     * 
     * @return true如果是任意槽位，否则false
     */
    public boolean isAnySlot() {
        return category == SlotCategory.ANY;
    }

    /**
     * 槽位分类枚举
     * 定义不同类型的槽位分类
     */
    public enum SlotCategory {
        VANILLA,
        CURIOS,
        ANY
    }
}