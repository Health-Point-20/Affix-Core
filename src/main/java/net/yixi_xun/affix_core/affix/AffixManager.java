package net.yixi_xun.affix_core.affix;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.yixi_xun.affix_core.affix.operation.*;
import net.yixi_xun.affix_core.api.AffixEvent;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AffixManager {

    private static final String AFFIX_TAG_KEY = "Affixes";
    private static final String ITEM_UUID_NBT_KEY = "AffixItemUUID";
    
    // 使用物品的唯一ID作为Key，而不是ItemStack对象本身
    public static final Map<String, Map<String, Long>> affixCooldowns = new ConcurrentHashMap<>();
    
    // 缓存已解析的词缀列表，以避免重复的NBT序列化/反序列化
    private static final Map<String, WeakReference<List<Affix>>> affixCache = new ConcurrentHashMap<>();


    /**
     * 初始化词缀系统
     */
    public static void init() {
        // 注册内置操作类型
        DealDamageOperation.register();
        PotionOperation.register();
        ModifyDamageOperation.register();
        AttributeOperation.register();
        ModifyEffectOperation.register();
        NBTOperation.register();
        CancelEventOperation.register();
        CommandOperation.register();
        ModifyDurationOperation.register();
        net.yixi_xun.affix_core.affix.operation.HealthOperation.register();

        AffixEvent.RegisterOperationEvent event = new AffixEvent.RegisterOperationEvent();
        MinecraftForge.EVENT_BUS.post(event);
    }

    /**
     * 从物品中获取所有词缀
     */
    public static List<Affix> getAffixes(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasTag()) {
            return new ArrayList<>();
        }

        // 获取物品的唯一ID
        String itemId = getItemUniqueId(itemStack);
        
        // 尝试从缓存中获取
        WeakReference<List<Affix>> cachedRef = affixCache.get(itemId);
        List<Affix> cachedAffixes = cachedRef != null ? cachedRef.get() : null;
        
        // 检查缓存是否有效且NBT未发生变化
        if (cachedAffixes != null) {
            // 验证缓存是否仍然有效
            ListTag currentAffixList = itemStack.getOrCreateTag().getList(AFFIX_TAG_KEY, Tag.TAG_COMPOUND);
            if (currentAffixList.isEmpty()) return new ArrayList<>();
            if (isCacheValid(cachedAffixes, currentAffixList)) {
                return new ArrayList<>(cachedAffixes); // 返回副本以防止外部修改
            }
        }
        
        // 缓存无效或不存在，重新构建
        List<Affix> affixes = new ArrayList<>();
        ListTag affixList = itemStack.getOrCreateTag().getList(AFFIX_TAG_KEY, Tag.TAG_COMPOUND);

        for (int i = 0; i < affixList.size(); i++) {
            affixes.add(Affix.fromNBT(affixList.getCompound(i), i));
        }

        // 更新缓存
        affixCache.put(itemId, new WeakReference<>(affixes));
        
        return affixes;
    }
    
    /**
     * 验证缓存是否仍然有效
     */
    private static boolean isCacheValid(List<Affix> cachedAffixes, ListTag currentAffixList) {
        if (cachedAffixes.size() != currentAffixList.size()) {
            return false;
        }
        
        // 简单验证：检查每个词缀的哈希值是否一致
        for (int i = 0; i < cachedAffixes.size(); i++) {
            if (i >= currentAffixList.size()) {
                return false;
            }
            
            Affix cachedAffix = cachedAffixes.get(i);
            CompoundTag currentNBT = currentAffixList.getCompound(i);
            
            // 重建当前词缀并比较哈希值
            Affix currentAffix = Affix.fromNBT(currentNBT, i);

            if (currentAffix == null) return false;

            if (!Objects.equals(cachedAffix.trigger(), currentAffix.trigger()) ||
                    !Objects.equals(cachedAffix.operation().getType(), currentAffix.operation().getType())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 向物品添加词缀
     */
    public static void addAffix(ItemStack itemStack, Affix affix) {
        if (itemStack == null) {
            return;
        }

        ListTag affixList = itemStack.getOrCreateTag().getList(AFFIX_TAG_KEY, Tag.TAG_COMPOUND);
        affixList.add(affix.toNBT());
        itemStack.getOrCreateTag().put(AFFIX_TAG_KEY, affixList);
        
        // 添加词缀后，使缓存失效
        String itemId = getItemUniqueId(itemStack);
        affixCache.remove(itemId);
    }

    /**
     * 移除指定索引的词缀，并执行其移除逻辑
     */
    public static boolean removeAffix(ItemStack itemStack, int index, Level world, LivingEntity owner) {
        if (itemStack == null || !itemStack.hasTag()) {
            return false;
        }

        ListTag affixList = itemStack.getOrCreateTag().getList(AFFIX_TAG_KEY, Tag.TAG_COMPOUND);

        if (index < 0 || index >= affixList.size()) {
            return false;
        }

        Affix affixToRemove;
        try {
            affixToRemove = Affix.fromNBT(affixList.getCompound(index), index);
        } catch (Exception e) {
            // 如果词缀无法加载，记录错误并跳过移除逻辑
            net.yixi_xun.affix_core.AffixCoreMod.LOGGER.warn("Failed to load affix from NBT for removal at index {}: {}", index, e.getMessage());
            affixToRemove = null;
        }
        
        if (affixToRemove != null) {
            AffixContext context = new AffixContext(world, owner, itemStack, affixToRemove, "on_remove", null);
            affixToRemove.remove(context);
        }

        // 然后从NBT中移除该词缀
        affixList.remove(index);
        itemStack.getOrCreateTag().put(AFFIX_TAG_KEY, affixList);

        // 如果词缀列表为空，则移除整个标签
        if (affixList.isEmpty()) {
            itemStack.getOrCreateTag().remove(AFFIX_TAG_KEY);
            if (itemStack.getOrCreateTag().isEmpty()) {
                itemStack.setTag(null);
            }
        }
        
        // 移除词缀后，使缓存失效
        String itemId = getItemUniqueId(itemStack);
        affixCache.remove(itemId);

        return true;
    }

    /**
     * 清除物品上的所有词缀
     */
    public static void clearAffixes(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasTag()) {
            return;
        }

        itemStack.getOrCreateTag().remove(AFFIX_TAG_KEY);
        if (itemStack.getOrCreateTag().isEmpty()) {
            itemStack.setTag(null);
        }
        
        // 清除词缀后，使缓存失效
        String itemId = getItemUniqueId(itemStack);
        affixCache.remove(itemId);
    }
    
    /**
     * 获取物品的唯一ID（用于冷却追踪）
     */
    public static String getItemUniqueId(ItemStack itemStack) {
        CompoundTag nbt = itemStack.getOrCreateTag();
        
        // 只有当物品有词缀时才分配UUID，避免影响普通物品堆叠
        if (!nbt.contains(ITEM_UUID_NBT_KEY) && nbt.contains(AFFIX_TAG_KEY)) {
            nbt.putUUID(ITEM_UUID_NBT_KEY, UUID.randomUUID());
        }
        
        // 如果物品没有词缀，使用物品的基本信息生成一个稳定的ID
        if (!nbt.contains(ITEM_UUID_NBT_KEY)) {
            // 对于没有词缀的物品，使用物品类型和基本NBT信息生成ID，以保持堆叠兼容性
            String itemId = itemStack.getItem().getDescriptionId();
            if (itemStack.hasTag()) {
                // 如果有其他NBT但没有词缀，也加入NBT哈希以区分
                itemId += "_" + itemStack.getOrCreateTag().hashCode();
            }
            return itemId;
        }
        
        return nbt.getUUID(ITEM_UUID_NBT_KEY).toString();
    }
    
    /**
     * 检查冷却是否结束
     */
    public static boolean isCooldownOver(ItemStack itemStack, int affixIndex, Level world) {
        String itemId = getItemUniqueId(itemStack);
        Map<String, Long> cooldownMap = affixCooldowns.get(itemId);
        String key = "affix_cooldown_" + affixIndex;
        long currentTime = world.getGameTime(); // 使用世界游戏时间
        return cooldownMap == null || cooldownMap.getOrDefault(key, 0L) < currentTime;
    }

    /**
     * 设置冷却
     */
    public static void setCooldown(ItemStack itemStack, int affixIndex, long cooldownTicks, Level world) {
        if (cooldownTicks <= 0) return;
        
        String itemId = getItemUniqueId(itemStack);
        String key = "affix_cooldown_" + affixIndex;
        
        long currentTime = world.getGameTime(); 
        affixCooldowns.computeIfAbsent(itemId, k -> new ConcurrentHashMap<>())
            .put(key, currentTime + cooldownTicks);
    }
}