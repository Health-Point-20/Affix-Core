package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.api.ExpressionHelper;

import java.util.Map;
import java.util.UUID;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;

/**
 * 操作基类，提供通用的操作模板和工具方法
 * 用于减少重复代码，统一操作行为
 */
public abstract class BaseOperation implements IOperation {
    
    /**
     * 安全地获取目标实体
     * @param context 词缀上下文
     * @param target 目标标识符 ("self", "target" 等)
     * @return 目标实体，如果无法获取则返回null
     */
    protected static LivingEntity getTargetEntity(AffixContext context, String target) {
        if (context == null) {
            return null;
        }

       LivingEntity targetEntity = switch (target.toLowerCase()) {
            case "self","owner" -> context.getOwner();
            case "target" -> context.getTarget();
            default -> {
                // 从UUID获取实体
                if (context.getWorld() instanceof ServerLevel serverLevel) {
                    if (serverLevel.getEntity(UUID.fromString(target)) instanceof LivingEntity livingEntity) {
                        yield livingEntity;
                    }
                }
                // 无法获取目标实体，返回自身
                yield context.getOwner();
            }
        };

        if (isInValidEntity(targetEntity)) {
            return context.getOwner();
        }

        return targetEntity;
    }

    /**
     * 安全地获取目标物品
     * 
     * <p>支持的目标格式：</p>
     * <ul>
     *   <li>{@code "self"} - 触发词缀的物品</li>
     *   <li>{@code "owner.mainhand"} - 自身主手物品</li>
     *   <li>{@code "owner.inventory.0"} - 自身物品栏第 0 格物品</li>
     *   <li>{@code "owner.equipment.head"} - 自身头盔槽装备</li>
     * </ul>
     * 
     * @param context 词缀上下文
     * @param targetPath 目标路径字符串（格式：{@code "entity.subject.index"}）
     * @return 目标物品，如果无法获取则返回 {@link ItemStack#EMPTY}
     */
    protected static ItemStack getTargetItem(AffixContext context, String targetPath) {
        if (context == null || targetPath == null || targetPath.trim().isEmpty()) {
            LOGGER.warn("获取目标物品失败：上下文或路径为空");
            return ItemStack.EMPTY;
        }
            
        try {
            String[] parts = targetPath.trim().split("\\.");
                
            if (parts.length < 1) {
                return ItemStack.EMPTY;
            }
                
            // 第一部分：实体标识符（self/target/UUID）
            String entityIdentifier = parts[0];
            LivingEntity targetEntity = getTargetEntity(context, entityIdentifier);
                
            if (isInValidEntity(targetEntity)) {
                return ItemStack.EMPTY;
            }
                
            // 如果没有指定第二部分，默认返回触发物品
            if (parts.length == 1) {
                return context.getItemStack();
            }
                
            // 第二部分：物品类型（mainhand/offhand/inventory/equipment）
            String itemType = parts[1].toLowerCase();
                
            return switch (itemType) {
                case "self" -> context.getItemStack();
                case "mainhand", "hand" -> targetEntity.getMainHandItem();
                case "offhand" -> targetEntity.getOffhandItem();
                        
                case "inventory" -> {
                    if (parts.length < 3) {
                        LOGGER.error("物品栏索引缺失：{}", targetPath);
                        yield ItemStack.EMPTY;
                    }
                    if (targetEntity instanceof Player player) {
                        try {
                            int index = Integer.parseInt(parts[2]);
                            if (index >= 0 && index < player.getInventory().getContainerSize()) {
                                yield player.getInventory().getItem(index);
                            } else {
                                LOGGER.error("物品栏索引超出范围：{} (目标当前可用范围：0-{})", index, player.getInventory().getContainerSize() - 1);
                                yield ItemStack.EMPTY;
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.error("无效的物品栏索引：{}", parts[2]);
                            yield ItemStack.EMPTY;
                        }
                    } else {
                        LOGGER.error("目标不是玩家实体，无法访问物品栏：{}", targetEntity.getType());
                        yield ItemStack.EMPTY;
                    }
                }
                        
                case "equipment" -> {
                    if (parts.length < 3) {
                        LOGGER.error("装备槽位名称缺失：{}", targetPath);
                        yield ItemStack.EMPTY;
                    }
                    try {
                        EquipmentSlot slot = EquipmentSlot.byName(parts[2]);
                        yield targetEntity.getItemBySlot(slot);
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("装备槽位解析失败：{}", parts[2]);
                        yield ItemStack.EMPTY;
                    }
                }
                        
                default -> {
                    LOGGER.error("未知的物品类型：{} (完整路径：{})", itemType, targetPath);
                    yield ItemStack.EMPTY;
                }
            };
        } catch (Exception e) {
            LOGGER.error("获取目标物品时发生异常：path={}", targetPath, e);
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * 验证实体是否有效
     * @param entity 待验证的实体
     * @return 实体是否有效
     */
    protected static boolean isInValidEntity(LivingEntity entity) {
        return entity == null || !entity.isAlive();
    }
    
    /**
     * 安全地计算表达式值
     * @param expression 表达式字符串
     * @param vars 上下文变量
     * @param defaultValue 默认值
     * @return 计算结果
     */
    protected static double evaluateOrDefaultValue(String expression, Map<String, Object> vars, double defaultValue) {
        if (expression == null || expression.trim().isEmpty()) {
            return defaultValue;
        }
        return ExpressionHelper.evaluate(expression, vars);
    }
    
    /**
     * 标准化的数学运算
     */
    public enum MathOperation {
        ADD("add"),
        SUBTRACT("subtract"), 
        MULTIPLY("multiply"),
        SET("set");
        
        private final String name;
        
        MathOperation(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public static MathOperation fromString(String name) {
            if (name == null || name.isEmpty()) {
                return ADD;
            }
            
            for (MathOperation op : values()) {
                if (op.name.equalsIgnoreCase(name)) {
                    return op;
                }
            }
            return ADD;
        }
        
        /**
         * 执行数学运算
         * @param currentValue 当前值
         * @param modifier 修饰符
         * @return 运算结果
         */
        public double apply(double currentValue, double modifier) {
            return switch (this) {
                case ADD -> currentValue + modifier;
                case SUBTRACT -> currentValue - modifier;
                case MULTIPLY -> currentValue * modifier;
                case SET -> modifier;
            };
        }
        
        /**
         * 执行数学运算
         */
        public float apply(float currentValue, float modifier) {
            return (float) apply(currentValue, (double) modifier);
        }
    }
    

    
    /**
     * 从 NBT 获取字符串值的简便方法
     */
    protected static String getStringOrDefaultValue(CompoundTag nbt, String key, String defaultValue) {
        return nbt.contains(key) ? nbt.getString(key) : defaultValue;
    }

}