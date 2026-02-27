package net.yixi_xun.affix_core.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.*;

import static net.yixi_xun.affix_core.ACConfig.SHOW_RAFFLE_CONTAINER_POS;

/**
 * 抽奖物品数据管理器
 */
public class RaffleDataManager {
    
    // NBT标签常量
    public static final String TAG_CONTAINER_POS = "ContainerPos";
    public static final String TAG_PROBABILITIES = "Probabilities";
    public static final String TAG_DRAW_COUNT = "DrawCount";
    public static final String TAG_ALLOW_REPEAT = "AllowRepeat";
    public static final String TAG_CONSUME_ITEMS = "ConsumeItems";
    public static final String TAG_CONSUME_SELF = "ConsumeSelf";
    public static final String TAG_ITEMS_LIST = "ItemsList";
    public static final RandomSource random = RandomSource.create();

    public static IItemHandler getItemHandler(ItemStack stack, Level level) {
        return getItemHandler(stack.getOrCreateTag(), level);
    }

    public static IItemHandler getItemHandler(CompoundTag nbt, Level level) {
        BlockPos containerPos = getBoundContainerPos(nbt);

        if (containerPos == null) return null;

        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity == null) return null;

        Optional<IItemHandler> itemHandlerOpt = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        return itemHandlerOpt.orElse(null);

    }

    public static List<ItemStack> drawFromContainer(ItemStack raffleStack, Level level) {
        CompoundTag nbt = raffleStack.getOrCreateTag();

        // 检查是否需要消耗自身
        if (getConsumeSelf(raffleStack)) {
            raffleStack.shrink(1);
        }

        return drawFromContainer(nbt, level);
    }

    /**
     * 从容器中抽取物品
     */
    public static List<ItemStack> drawFromContainer(CompoundTag nbt, Level level) {
        List<ItemStack> results = new ArrayList<>();

        
        if (!nbt.contains(TAG_CONTAINER_POS)) {
            return results; // 未绑定容器
        }

        IItemHandler itemHandler = getItemHandler(nbt, level);
        if (itemHandler == null) {
            return results;
        }

        Map<Integer, Double> probabilities = getSlotProbabilities(nbt, itemHandler);
        int drawCount = getDrawCount(nbt);
        boolean allowRepeat = getAllowRepeat(nbt);
        boolean consumeItems = getConsumeItems(nbt);
        
        Set<Integer> drawnSlots = new HashSet<>();
        
        for (int i = 0; i < drawCount; i++) {
            Integer slot = selectRandomSlot(probabilities, drawnSlots);
            if (slot != null && slot < itemHandler.getSlots()) {
                ItemStack itemInSlot = itemHandler.getStackInSlot(slot);
                if (!itemInSlot.isEmpty()) {
                    results.add(itemInSlot.copy());
                    
                    // 消耗物品逻辑 - 以格为单位消耗整个槽位
                    if (consumeItems) {
                        itemHandler.extractItem(slot, itemInSlot.getCount(), false);
                        // 消耗后重新计算概率分布（避免下次抽到空槽位）
                        probabilities = getSlotProbabilities(nbt, itemHandler);
                    }
                    
                    if (!allowRepeat) {
                        drawnSlots.add(slot);
                        // 从概率映射中移除已抽取的槽位
                        probabilities.remove(slot);
                    }
                }
            }
        }
        
        return results;
    }

    public static List<ItemStack> drawFromItemList(ItemStack raffleStack) {
        CompoundTag nbt = raffleStack.getOrCreateTag();

        // 检查是否需要消耗自身
        if (getConsumeSelf(raffleStack)) {
            raffleStack.shrink(1);
        }

        return drawFromItemList(nbt);
    }

    /**
     * 从内置物品列表中抽取
     */
    public static List<ItemStack> drawFromItemList(CompoundTag nbt) {
        List<ItemStack> results = new ArrayList<>();
        
        if (!nbt.contains(TAG_ITEMS_LIST)) {
            return results;
        }
        
        ListTag itemsList = nbt.getList(TAG_ITEMS_LIST, Tag.TAG_COMPOUND);
        if (itemsList.isEmpty()) {
            return results;
        }
        
        Map<Integer, Double> probabilities = getItemProbabilities(nbt, itemsList.size());
        int drawCount = getDrawCount(nbt);
        boolean allowRepeat = getAllowRepeat(nbt);
        boolean consumeItems = getConsumeItems(nbt);
        
        Set<Integer> drawnIndices = new HashSet<>();
        List<Integer> indicesToRemove = new ArrayList<>();
        
        for (int i = 0; i < drawCount; i++) {
            Integer index = selectRandomSlot(probabilities, drawnIndices);
            if (index != null && index < itemsList.size()) {
                CompoundTag itemTag = itemsList.getCompound(index);
                ItemStack itemStack = ItemStack.of(itemTag);
                if (!itemStack.isEmpty()) {
                    results.add(itemStack);
                    
                    // 记录需要移除的索引
                    if (consumeItems) {
                        indicesToRemove.add(index);
                    }
                    
                    if (!allowRepeat) {
                        drawnIndices.add(index);
                        // 从概率映射中移除已抽取的索引
                        probabilities.remove(index);
                    }
                }
            }
        }
        
        // 如果需要消耗物品，则从列表中移除已抽取的物品
        if (consumeItems && !indicesToRemove.isEmpty()) {
            // 按降序排列索引，避免删除时影响前面元素的位置
            indicesToRemove.sort(Collections.reverseOrder());
            for (Integer index : indicesToRemove) {
                itemsList.remove(index.intValue());
            }
            // 更新NBT中的物品列表
            nbt.put(TAG_ITEMS_LIST, itemsList);
        }

        return results;
    }

    /**
     * 获取槽位概率分布
     */
    private static Map<Integer, Double> getSlotProbabilities(CompoundTag nbt, IItemHandler itemHandler) {
        Map<Integer, Double> probabilities = new HashMap<>();

        if (nbt.contains(TAG_PROBABILITIES)) {
            CompoundTag probTag = nbt.getCompound(TAG_PROBABILITIES);
            for (String key : probTag.getAllKeys()) {
                try {
                    int slot = Integer.parseInt(key);
                    double probability = probTag.getDouble(key);
                    if (slot >= 0 && slot < itemHandler.getSlots() && probability > 0) {
                        probabilities.put(slot, probability);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // 如果没有自定义概率，则平均分配给所有非空槽位
        if (probabilities.isEmpty()) {
            int totalSlots = itemHandler.getSlots();
            List<Integer> nonEmptySlots = new ArrayList<>();

            // 先收集所有非空槽位
            for (int i = 0; i < totalSlots; i++) {
                if (!itemHandler.getStackInSlot(i).isEmpty()) {
                    nonEmptySlots.add(i);
                }
            }

            // 如果有非空槽位，则平均分配概率
            if (!nonEmptySlots.isEmpty()) {
                double equalProb = 1.0 / nonEmptySlots.size();
                for (Integer slot : nonEmptySlots) {
                    probabilities.put(slot, equalProb);
                }
            }
            // 如果所有槽位都为空，则不添加任何概率
        } else {
            // 标准化概率总和为1
            normalizeProbabilities(probabilities);
        }

        return probabilities;
    }
    
    /**
     * 获取物品列表概率分布
     */
   private static Map<Integer, Double> getItemProbabilities(CompoundTag nbt, int itemCount) {
        Map<Integer, Double> probabilities = new HashMap<>();
        
        if (nbt != null && nbt.contains(TAG_PROBABILITIES)) {
            CompoundTag probTag = nbt.getCompound(TAG_PROBABILITIES);
            for (String key : probTag.getAllKeys()) {
                try {
                    int index = Integer.parseInt(key);
                    double probability = probTag.getDouble(key);
                    if (index >= 0 && index < itemCount && probability > 0) {
                        probabilities.put(index, probability);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // 如果没有自定义概率，则平均分配
        if (probabilities.isEmpty()) {
            double equalProb = 1.0 / itemCount;
            for (int i = 0; i < itemCount; i++) {
                probabilities.put(i, equalProb);
            }
        } else {
            // 标准化概率总和为1
            normalizeProbabilities(probabilities);
        }
        
        return probabilities;
    }

    /**
     * 标准化概率分布
     */
    private static void normalizeProbabilities(Map<Integer, Double> probabilities) {
        double sum = probabilities.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0 && Math.abs(sum - 1.0) > 0.001) {
            probabilities.replaceAll((k, v) -> v / sum);
        }
    }
    
    /**
     * 随机选择一个槽位
     */
    private static Integer selectRandomSlot(Map<Integer, Double> probabilities, Set<Integer> excluded) {
        if (probabilities.isEmpty()) {
            return null;
        }

        // 构建候选槽位列表（排除已抽取的槽位）
        List<Map.Entry<Integer, Double>> candidates = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : probabilities.entrySet()) {
            if (!excluded.contains(entry.getKey())) {
                candidates.add(entry);
            }
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        // 基于权重进行随机选择
        double totalWeight = candidates.stream()
            .mapToDouble(Map.Entry::getValue)
            .sum();
            
        if (totalWeight <= 0) {
            return null;
        }
        
        double randomValue = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;
        
        for (Map.Entry<Integer, Double> candidate : candidates) {
            cumulativeWeight += candidate.getValue();
            if (randomValue <= cumulativeWeight) {
                return candidate.getKey();
            }
        }
        
        // 如果因为浮点数精度问题没有选中，返回第一个候选
        return candidates.get(0).getKey();
    }
    
    // Getter和Setter方法
    public static int getDrawCount(ItemStack stack) {
        return getDrawCount(stack.getTag());
    }
    
    public static int getDrawCount(CompoundTag nbt) {
        return nbt != null ? Math.max(nbt.getInt(TAG_DRAW_COUNT), 1) : 1;
    }
    
    public static void setDrawCount(ItemStack stack, int count) {
        stack.getOrCreateTag().putInt(TAG_DRAW_COUNT, Math.max(1, count));
    }
    
    public static boolean getAllowRepeat(ItemStack stack) {
        return getAllowRepeat(stack.getTag());
    }
    
    public static boolean getAllowRepeat(CompoundTag nbt) {
        return nbt == null || nbt.getBoolean(TAG_ALLOW_REPEAT);
    }
    
    public static void setAllowRepeat(ItemStack stack, boolean allow) {
        stack.getOrCreateTag().putBoolean(TAG_ALLOW_REPEAT, allow);
    }
    
    public static boolean getConsumeItems(ItemStack stack) {
        return getConsumeItems(stack.getTag());
    }
    
    public static boolean getConsumeItems(CompoundTag nbt) {
        return nbt != null && nbt.getBoolean(TAG_CONSUME_ITEMS);
    }
    
    public static void setConsumeItems(ItemStack stack, boolean consume) {
        stack.getOrCreateTag().putBoolean(TAG_CONSUME_ITEMS, consume);
    }
    
    public static boolean getConsumeSelf(ItemStack stack) {
        return getConsumeSelf(stack.getTag());
    }
    
    public static boolean getConsumeSelf(CompoundTag nbt) {
        return nbt != null && nbt.getBoolean(TAG_CONSUME_SELF);
    }
    
    public static void setConsumeSelf(ItemStack stack, boolean consume) {
        stack.getOrCreateTag().putBoolean(TAG_CONSUME_SELF, consume);
    }
    
    public static void setSlotProbability(ItemStack stack, int slot, double probability) {
        CompoundTag nbt = stack.getOrCreateTag();
        CompoundTag probTag = nbt.getCompound(TAG_PROBABILITIES);
        if (probability <= 0) {
            probTag.remove(String.valueOf(slot));
        } else {
            probTag.putDouble(String.valueOf(slot), probability);
        }
        nbt.put(TAG_PROBABILITIES, probTag);
    }
    
    public static void setItemList(ItemStack stack, List<ItemStack> items) {
        CompoundTag nbt = stack.getOrCreateTag();
        ListTag itemsList = new ListTag();
        
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                itemsList.add(item.save(new CompoundTag()));
            }
        }
        
        nbt.put(TAG_ITEMS_LIST, itemsList);
    }
    
    /**
     * 清空内置物品列表
     */
    public static void clearItemList(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt != null) {
            nbt.remove(TAG_ITEMS_LIST);
            nbt.remove(TAG_PROBABILITIES); // 清除相关概率配置
        }
    }
    
    /**
     * 检查是否有有效的物品列表
     */
    public static boolean hasValidItemList(ItemStack stack) {
        return hasValidItemList(stack.getTag());
    }
    
    public static boolean hasValidItemList(CompoundTag nbt) {
        if (nbt == null || !nbt.contains(TAG_ITEMS_LIST)) {
            return false;
        }
        
        ListTag itemsList = nbt.getList(TAG_ITEMS_LIST, Tag.TAG_COMPOUND);
        return !itemsList.isEmpty();
    }
    
    /**
     * 获取绑定的容器位置
     * @param stack 抽奖物品
     * @return 容器位置，如果未绑定则返回null
     */
    public static BlockPos getBoundContainerPos(ItemStack stack) {
        return getBoundContainerPos(stack.getTag());
    }
    
    public static BlockPos getBoundContainerPos(CompoundTag nbt) {
        if (nbt == null || !nbt.contains(TAG_CONTAINER_POS)) {
            return null;
        }
        
        long posLong = nbt.getLong(TAG_CONTAINER_POS);
        return BlockPos.of(posLong);
    }
    
    /**
     * 设置绑定的容器位置
     * @param stack 抽奖物品
     * @param pos 容器位置
     */
    public static void setBoundContainerPos(ItemStack stack, BlockPos pos) {
        setBoundContainerPos(stack.getOrCreateTag(), pos);
    }

    public static void setBoundContainerPos(CompoundTag nbt, BlockPos pos) {
        if (pos != null) {
            nbt.putLong(TAG_CONTAINER_POS, pos.asLong());
        } else {
            unbindContainer(nbt);
        }
    }
    
    /**
     * 解绑容器
     */
    public static void unbindContainer(CompoundTag nbt) {
        if (nbt != null) {
            nbt.remove(TAG_CONTAINER_POS);
        }
    }

    public static void unbindContainer(ItemStack stack) {
        unbindContainer(stack.getTag());
    }
    
    /**
     * 初始化默认值
     * @param stack 抽奖物品
     * @return 是否成功初始化
     */
    public static boolean initializeDefaults(ItemStack stack) {
        if (!(stack.getItem() instanceof RaffleItem)) {
            return false;
        }
        
        // 设置默认值
        setDrawCount(stack, 1);              // 默认抽取1次
        setAllowRepeat(stack, true);         // 默认允许重复
        setConsumeItems(stack, false);    // 默认不消耗物品
        setConsumeSelf(stack, false);     // 默认不消耗自身
        
        // 清除现有配置
        clearItemList(stack);
        unbindContainer(stack);
        
        return true;
    }
    
    /**
     * 获取概率信息用于tooltip显示
     */
    public static List<Component> getProbabilityInfo(ItemStack stack, Level level) {
        List<Component> info = new ArrayList<>();
        CompoundTag nbt = stack.getOrCreateTag();

        info.add(Component.translatable("tooltip.raffle.probabilities").withStyle(ChatFormatting.GOLD));

        // 显示具体概率
        if (nbt.contains(TAG_ITEMS_LIST)) {
            // 物品列表模式
            info.add(Component.translatable("tooltip.raffle.item_list_mode").withStyle(ChatFormatting.YELLOW));
            ListTag itemsList = nbt.getList(TAG_ITEMS_LIST, Tag.TAG_COMPOUND);
            CompoundTag probTag = nbt.getCompound(TAG_PROBABILITIES);

            if (itemsList.isEmpty()) {
                info.add(Component.translatable("message.raffle.no_rewards").withStyle(ChatFormatting.RED));
            }

            for (int i = 0; i < itemsList.size(); i++) {
                CompoundTag itemTag = itemsList.getCompound(i);
                ItemStack itemStack = ItemStack.of(itemTag);
                if (!itemStack.isEmpty()) {
                    double prob = probTag.contains(String.valueOf(i)) ? 
                        probTag.getDouble(String.valueOf(i)) * 100 : (100.0 / itemsList.size());
                    info.add(Component.literal("  ").append(
                            itemStack.getDisplayName()).append(
                            Component.literal(" * " + itemStack.getCount() + String.format(": %.1f%%", prob))));
                }
            }
        } else if (nbt.contains(TAG_CONTAINER_POS)) {
            // 容器模式
            info.add(Component.translatable("tooltip.raffle.container_mode").withStyle(ChatFormatting.YELLOW));
            CompoundTag probTag = nbt.getCompound(TAG_PROBABILITIES);
            IItemHandler itemHandler = getItemHandler(stack, level);

            // 显示绑定的容器位置
            BlockPos containerPos = getBoundContainerPos(stack);
            if (SHOW_RAFFLE_CONTAINER_POS.get() && containerPos != null) {
                info.add(Component.translatable("tooltip.raffle.container_bound_at",
                                containerPos.getX(), containerPos.getY(), containerPos.getZ())
                        .withStyle(ChatFormatting.GRAY));
            }
            
            if (itemHandler == null) {
                info.add(Component.translatable("tooltip.raffle.container_not_available").withStyle(ChatFormatting.RED));
                return info;
            }
            
            List<ItemStack> items = new ArrayList<>();
            // 获取有效物品列表
            int slots = itemHandler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack itemInSlot = itemHandler.getStackInSlot(i);
                if (!itemInSlot.isEmpty()) {
                    items.add(itemInSlot);
                }
            }

            if (items.isEmpty()) {
                info.add(Component.translatable("message.raffle.no_rewards").withStyle(ChatFormatting.RED));
            }

            // 显示概率
            for (int i = 0; i < items.size(); i++) {
                ItemStack itemStack = items.get(i);
                double prob = probTag.contains(String.valueOf(i)) ?
                    probTag.getDouble(String.valueOf(i)) * 100 : (100.0 / items.size());
                info.add(Component.literal("  ").append(
                        itemStack.getDisplayName()).append(
                        Component.literal(" * " + itemStack.getCount() + String.format(": %.1f%%", prob))));
            }
        }
        
        return info;
    }
}