package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.api.ExpressionHelper;

import java.util.Map;
import java.util.UUID;

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
     * 从NBT获取字符串值的简便方法
     */
    protected static String getString(CompoundTag nbt, String key, String defaultValue) {
        return nbt.contains(key) ? nbt.getString(key) : defaultValue;
    }

}