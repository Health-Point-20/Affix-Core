package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.Map;

import static net.yixi_xun.affix_core.affix.AffixContext.createItemData;
import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluateCondition;

/**
 * 物品操作类
 * 支持对物品进行增加/减少/设置数量或扔出物品操作
 * 操作对象可以是触发词缀的物品、目标实体的主手/副手物品、物品栏中的物品
 */
public class ItemOperation extends BaseOperation {
    
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
    private final String itemFilter; // 用于物品栏操作时筛选特定物品
    
    public ItemOperation(String operationMode, String targetType, String amountExpression, 
                        String targetEntity, String itemFilter) {
        this.operationMode = OperationMode.fromString(operationMode);
        this.targetType = TargetType.fromString(targetType);
        this.amountExpression = amountExpression != null ? amountExpression : "1";
        this.targetEntity = targetEntity != null ? targetEntity : "self";
        this.itemFilter = itemFilter != null ? itemFilter : "";
    }
    
    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        try {
            LivingEntity target = getTargetEntity(context, targetEntity);
            if (isInValidEntity(target)) {
                return;
            }
            
            double amount = evaluateOrDefaultValue(amountExpression, context.getVariables(), 1.0);
            int itemCount = Math.max(1, (int) Math.round(Math.abs(amount)));
            
            switch (targetType) {
                case ITEM -> handleItemTarget(context, target, itemCount);
                case MAIN_HAND -> handleMainHandTarget(target, itemCount);
                case OFF_HAND -> handleOffHandTarget(target, itemCount);
                case INVENTORY -> handleInventoryTarget(context, target, itemCount);
            }
        } catch (Exception e) {
            AffixCoreMod.LOGGER.error("执行物品操作时发生错误", e);
        }
    }
    
    /**
     * 处理触发词缀的物品操作
     */
    private void handleItemTarget(AffixContext context, LivingEntity target, int itemCount) {
        ItemStack itemStack = context.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }
        
        switch (operationMode) {
            case ADD, SUBTRACT, SET -> handleItemStackOperation(itemStack, itemCount);
            case DROP -> dropItemStack(target, itemStack, itemCount);
        }
    }
    
    /**
     * 处理手持物品操作（主手或副手）
     */
    private void handleHandTarget(LivingEntity target, InteractionHand hand, int itemCount) {
        ItemStack handItem = target.getItemInHand(hand);
        if (handItem.isEmpty()) {
            return;
        }
        
        switch (operationMode) {
            case ADD, SUBTRACT, SET -> handleItemStackOperation(handItem, itemCount);
            case DROP -> {
                target.setItemInHand(hand, ItemStack.EMPTY);
                dropItemStack(target, handItem, itemCount);
            }
        }
    }
    
    /**
     * 处理主手物品操作
     */
    private void handleMainHandTarget(LivingEntity target, int itemCount) {
        handleHandTarget(target, InteractionHand.MAIN_HAND, itemCount);
    }
    
    /**
     * 处理副手物品操作
     */
    private void handleOffHandTarget(LivingEntity target, int itemCount) {
        handleHandTarget(target, InteractionHand.OFF_HAND, itemCount);
    }
    
    /**
     * 处理物品栏操作
     */
    private void handleInventoryTarget(AffixContext context, LivingEntity target, int itemCount) {
        if (!(target instanceof Player player)) {
            return;
        }
        
        Inventory inventory = player.getInventory();
        processInventoryItems(context, inventory, target, itemCount);
    }
    
    /**
     * 检查物品是否匹配过滤条件
     * 处理itemFilter中可能存在的Index引用
     */
    private boolean isItemMatchingFilter(Map<String, Object> variables, ItemStack stack, int index) {
        if (itemFilter == null || itemFilter.isEmpty()) {
            return true;
        }
            
        // 创建物品数据并处理可能的Index引用
        Map<String, Object> itemData = createItemData(stack);
        variables.put("index", index);
            
        variables.put("item", itemData);
        return evaluateCondition(itemFilter, variables);
    }
    
    /**
     * 统一处理物品栏中的物品
     */
    private void processInventoryItems(AffixContext context, Inventory inventory, LivingEntity target, int itemCount) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            // 设置当前索引用于filter中的Index引用
            context.getVariables().put("index", i);
            if (!stack.isEmpty() && isItemMatchingFilter(context.getVariables(), stack, i)) {
                switch (operationMode) {
                    case ADD, SUBTRACT, SET -> handleItemStackOperation(stack, itemCount);
                    case DROP -> {
                        ItemStack droppedStack = stack.copy();
                        droppedStack.setCount(Math.min(droppedStack.getCount(), itemCount));
                        inventory.removeItemNoUpdate(i);
                        stack.shrink(droppedStack.getCount());
                        if (stack.isEmpty()) {
                            inventory.setItem(i, ItemStack.EMPTY);
                        }
                        dropItemStack(target, droppedStack, droppedStack.getCount());
                    }
                }
                break;
            }
        }
    }
    
    /**
     * 统一处理物品堆操作
     */
    private void handleItemStackOperation(ItemStack stack, int count) {
        int newCount = switch (operationMode) {
            case ADD -> Math.min(stack.getCount() + count, stack.getMaxStackSize());
            case SUBTRACT -> Math.max(0, stack.getCount() - count);
            case SET -> Math.max(0, Math.min(count, stack.getMaxStackSize()));
            default -> stack.getCount();
        };
        stack.setCount(newCount);
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
            return;
        }
            
        ItemStack dropStack = stack.copy();
        dropStack.setCount(Math.min(count, dropStack.getCount()));
            
        double eyeY = entity.getEyeY() - 0.3F;
        ItemEntity itemEntity = new ItemEntity(serverLevel, entity.getX(), eyeY, entity.getZ(), dropStack);
            
        itemEntity.setPickUpDelay(40);
        itemEntity.setThrower(entity.getUUID());
            
        // 参考原版实现的投掷逻辑
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
            
        float f = Mth.sin(yaw * ((float)Math.PI / 180F));
        float f1 = Mth.cos(yaw * ((float)Math.PI / 180F));
        float f2 = Mth.sin(pitch * ((float)Math.PI / 180F));
        float f3 = Mth.cos(pitch * ((float)Math.PI / 180F));
            
        float randomAngle = level.random.nextFloat() * ((float)Math.PI * 2F);
        float randomSpeed = 0.02F * level.random.nextFloat();
            
        itemEntity.setDeltaMovement(
            (-f * f3 * 0.3F) + Math.cos(randomAngle) * randomSpeed,
            (-f2 * 0.3F + 0.1F + (level.random.nextFloat() - level.random.nextFloat()) * 0.1F),
            (f1 * f3 * 0.3F) + Math.sin(randomAngle) * randomSpeed
        );
            
        serverLevel.addFreshEntity(itemEntity);
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
        String operationMode = getString(nbt, "OperationMode", "subtract");
        String targetType = getString(nbt, "TargetType", "item");
        String amountExpression = getString(nbt, "AmountExpression", "1");
        String targetEntity = getString(nbt, "TargetEntity", "self");
        String itemFilter = getString(nbt, "ItemFilter", "");
        
        return new ItemOperation(operationMode, targetType, amountExpression, targetEntity, itemFilter);
    }
    
    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("item_operation", ItemOperation::fromNBT);
    }
}
