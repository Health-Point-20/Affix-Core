package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.yixi_xun.affix_core.affix.AffixContext;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;
import static net.yixi_xun.affix_core.affix.AffixContext.createItemData;
import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluateCondition;

/**
 * 物品操作类
 * 支持对物品进行增加/减少/设置数量或扔出物品操作
 * 操作对象可以是触发词缀的物品、目标实体的主手/副手物品、物品栏中的物品
 */
public class ItemOperation extends BaseOperation {
    
    /**
     * 操作模式枚举
     */
    public enum OperationMode {
        ADD("add"),           // 增加物品数量
        SUBTRACT("subtract"), // 减少物品数量
        SET("set"),           // 设置物品数量
        DROP("drop");        // 扔出物品
        
        private final String name;
        
        OperationMode(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public static OperationMode fromString(String name) {
            if (name == null || name.isEmpty()) {
                return ADD;
            }
            
            for (OperationMode mode : values()) {
                if (mode.name.equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return ADD;
        }
    }
    
    /**
     * 目标类型枚举
     */
    public enum TargetType {
        ITEM("item"),           // 触发词缀的物品
        MAIN_HAND("main_hand"),  // 主手物品
        OFF_HAND("off_hand"),    // 副手物品
        INVENTORY("inventory"); // 物品栏中的物品
        
        private final String name;
        
        TargetType(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public static TargetType fromString(String name) {
            if (name == null || name.isEmpty()) {
                return ITEM;
            }
            
            for (TargetType type : values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return ITEM;
        }
    }
    
    private final OperationMode operationMode;
    private final TargetType targetType;
    private final String amountExpression;
    private final String targetEntity;
    private final String itemFilter;
    
    /**
     * 构造函数
     * @param operationMode 操作模式 (ADD/SUBTRACT/SET/DROP)
     * @param targetType 目标类型 (ITEM/MAIN_HAND/OFF_HAND/INVENTORY)
     * @param amountExpression 数量表达式
     * @param targetEntity 目标实体标识符
     * @param itemFilter 物品栏筛选条件
     */
    public ItemOperation(String operationMode, String targetType, String amountExpression, 
                        String targetEntity, String itemFilter) {
        this.operationMode = OperationMode.fromString(operationMode);
        this.targetType = TargetType.fromString(targetType);
        this.amountExpression = amountExpression != null && !amountExpression.trim().isEmpty() ? amountExpression.trim() : "1";
        this.targetEntity = targetEntity != null ? targetEntity.trim() : "self";
        this.itemFilter = itemFilter != null ? itemFilter.trim() : "";
    }
    
    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            LOGGER.warn("物品操作执行失败：上下文为空");
            return;
        }
        
        try {
            int itemCount = calculateItemCount(context);
            String targetPath = buildTargetPath(targetEntity, targetType);
            performTargetOperation(context, targetPath, itemCount);
        } catch (Exception e) {
            LOGGER.error("执行物品操作时发生错误：operation={}, target={}", operationMode, targetType, e);
        }
    }
    
    /**
     * 计算物品数量
     */
    private int calculateItemCount(AffixContext context) {
        double amount = evaluateOrDefaultValue(amountExpression, context.getVariables(), 1.0);
        return Math.max(1, (int) Math.round(Math.abs(amount)));
    }
    
    /**
     * 构建目标物品路径字符串
     */
    private String buildTargetPath(String entityIdentifier, TargetType type) {
        return switch (type) {
            case ITEM -> entityIdentifier; // 只传实体标识符，默认为触发物品
            case MAIN_HAND -> entityIdentifier + ".mainhand";
            case OFF_HAND -> entityIdentifier + ".offhand";
            case INVENTORY -> entityIdentifier + ".inventory"; // inventory 特殊处理，需要索引
        };
    }
    
    /**
     * 执行目标物品操作
     */
    private void performTargetOperation(AffixContext context, String targetPath, int itemCount) {
        // 对于 INVENTORY 类型，需要遍历物品栏
        if (targetType == TargetType.INVENTORY) {
            performInventoryOperationWithFilter(context, targetPath, itemCount);
            return;
        }
        
        // 其他类型直接使用 getTargetItem 获取
        ItemStack targetItem = getTargetItem(context, targetPath);
        if (targetItem.isEmpty()) {
            LOGGER.debug("物品操作跳过：目标物品为空 (path={})", targetPath);
            return;
        }
        
        // 对于非 DROP 操作，直接修改物品
        // 对于 DROP 操作，需要先获取实体
        LivingEntity entity = getTargetEntity(context, targetEntity);
        performOperation(entity, targetItem, itemCount);
    }
    
    /**
     * 处理物品栏操作（带筛选条件）
     */
    private void performInventoryOperationWithFilter(AffixContext context, String basePath, int itemCount) {
        String[] parts = basePath.split("\\.");
        String entityIdentifier = parts[0];
        LivingEntity targetEntity = getTargetEntity(context, entityIdentifier);
        
        if (!(targetEntity instanceof Player player)) {
            LOGGER.debug("物品操作跳过：目标不是玩家实体");
            return;
        }
        
        Inventory inventory = player.getInventory();
        Map<String, Object> variables = context.getVariables();
        
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && isItemMatchingFilter(variables, stack, i)) {
                performSingleInventoryOperation(inventory, i, stack, targetEntity, itemCount);
                break;
            }
        }
    }
    
    /**
     * 检查物品是否匹配过滤条件
     * 处理 itemFilter 中可能存在的 Index 引用
     */
    private boolean isItemMatchingFilter(Map<String, Object> variables, ItemStack stack, int index) {
        if (itemFilter == null || itemFilter.isEmpty()) {
            return true;
        }
        
        try {
            Map<String, Object> itemData = createItemData(stack);
            variables.put("index", index);
            variables.put("item", itemData);
            return evaluateCondition(itemFilter, variables);
        } catch (Exception e) {
            LOGGER.error("物品筛选条件执行失败：filter={}, index={}", itemFilter, index, e);
            return false;
        }
    }
    
    /**
     * 执行单个物品栏格子的操作
     */
    private void performSingleInventoryOperation(Inventory inventory, int slotIndex, ItemStack stack, 
                                                  LivingEntity target, int itemCount) {
        switch (operationMode) {
            case ADD, SUBTRACT, SET -> {
                int newCount = switch (operationMode) {
                    case ADD -> Math.min(stack.getCount() + itemCount, stack.getMaxStackSize());
                    case SUBTRACT -> Math.max(0, stack.getCount() - itemCount);
                    case SET -> Math.max(0, Math.min(itemCount, stack.getMaxStackSize()));
                    default -> stack.getCount();
                };
                stack.setCount(newCount);
            }
            case DROP -> {
                ItemStack droppedStack = stack.copy();
                droppedStack.setCount(Math.min(droppedStack.getCount(), itemCount));
                inventory.removeItemNoUpdate(slotIndex);
                stack.shrink(droppedStack.getCount());
                if (stack.isEmpty()) {
                    inventory.setItem(slotIndex, ItemStack.EMPTY);
                }
                dropItemStack(target, droppedStack, droppedStack.getCount());
            }
        }
    }
    
    /**
     * 统一执行物品操作（核心方法）
     * @param entity 所属实体（DROP 操作需要）
     * @param stack 物品堆
     * @param count 操作数量
     */
    private void performOperation(@Nullable LivingEntity entity, ItemStack stack, int count) {
        int newCount = switch (operationMode) {
            case ADD -> Math.min(stack.getCount() + count, stack.getMaxStackSize());
            case SUBTRACT -> Math.max(0, stack.getCount() - count);
            case SET -> Math.max(0, Math.min(count, stack.getMaxStackSize()));
            case DROP -> 0;
        };
        
        if (operationMode == OperationMode.DROP && entity != null) {
            ItemStack dropStack = stack.copy();
            dropStack.setCount(Math.min(count, dropStack.getCount()));
            stack.setCount(0);
            dropItemStack(entity, dropStack, dropStack.getCount());
        } else {
            stack.setCount(newCount);
        }
    }
    
    /**
     * 扔出物品
     * 参考原版玩家扔物品的实现
     */
    private void dropItemStack(LivingEntity entity, ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            return;
        }
        
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            LOGGER.debug("物品投掷失败：当前世界不是服务器端");
            return;
        }
        
        try {
            ItemStack dropStack = stack.copy();
            dropStack.setCount(Math.min(count, dropStack.getCount()));
            
            double eyeY = entity.getEyeY() - 0.3F;
            ItemEntity itemEntity = new ItemEntity(serverLevel, entity.getX(), eyeY, entity.getZ(), dropStack);
            
            itemEntity.setPickUpDelay(40);
            itemEntity.setThrower(entity.getUUID());
            
            applyThrowVelocity(entity, level, itemEntity);
            
            serverLevel.addFreshEntity(itemEntity);
        } catch (Exception e) {
            LOGGER.error("投掷物品失败：entity={}, count={}", entity.getType(), count, e);
        }
    }
    
    /**
     * 应用投掷速度
     */
    private void applyThrowVelocity(LivingEntity entity, Level level, ItemEntity itemEntity) {
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        
        float sinYaw = Mth.sin(yaw * ((float)Math.PI / 180F));
        float cosYaw = Mth.cos(yaw * ((float)Math.PI / 180F));
        float sinPitch = Mth.sin(pitch * ((float)Math.PI / 180F));
        float cosPitch = Mth.cos(pitch * ((float)Math.PI / 180F));
        
        float randomAngle = level.random.nextFloat() * ((float)Math.PI * 2F);
        float randomSpeed = 0.02F * level.random.nextFloat();
        
        itemEntity.setDeltaMovement(
            (-sinYaw * cosPitch * 0.3F) + Math.cos(randomAngle) * randomSpeed,
            (-sinPitch * 0.3F + 0.1F + (level.random.nextFloat() - level.random.nextFloat()) * 0.1F),
            (cosYaw * cosPitch * 0.3F) + Math.sin(randomAngle) * randomSpeed
        );
    }
    
    @Override
    public void remove(AffixContext context) {
        // 物品操作是瞬时操作，不需要特殊的移除逻辑
    }
    
    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("OperationMode", operationMode.getName());
        nbt.putString("TargetType", targetType.getName());
        nbt.putString("AmountExpression", amountExpression);
        nbt.putString("TargetEntity", targetEntity);
        nbt.putString("ItemFilter", itemFilter);

        return nbt;
    }
    
    @Override
    public String getType() {
        return "item_operation";
    }
    
    /**
     * 工厂方法，从NBT创建ItemOperation
     */
    public static ItemOperation fromNBT(CompoundTag nbt) {
        String operationMode = getStringOrDefaultValue(nbt, "OperationMode", "subtract");
        String targetType = getStringOrDefaultValue(nbt, "TargetType", "item");
        String amountExpression = getStringOrDefaultValue(nbt, "AmountExpression", "1");
        String targetEntity = getStringOrDefaultValue(nbt, "TargetEntity", "self");
        String itemFilter = getStringOrDefaultValue(nbt, "ItemFilter", "");
        
        return new ItemOperation(operationMode, targetType, amountExpression, targetEntity, itemFilter);
    }
    
    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("item_operation", ItemOperation::fromNBT);
    }
}
