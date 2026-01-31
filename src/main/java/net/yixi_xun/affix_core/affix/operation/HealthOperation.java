package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.api.ExpressionHelper;

/**
 * 生命值操作，包括治疗、设置生命值、设置伤害吸收值等
 */
public class HealthOperation implements IOperation {

    public enum Mode {
        HEAL("heal"),           // 治疗
        HEALTH("health"), // 设置生命值
        ABSORPTION("absorption"); // 设置伤害吸收值

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Mode fromString(String name) {
            for (Mode mode : Mode.values()) {
                if (mode.name.equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return HEAL; // 默认为治疗
        }
    }

    public enum Operation {
        ADD("add"),             // 加
        SUBTRACT("subtract"),   // 减
        MULTIPLY("multiply"),   // 乘
        SET("set");             // 设置

        private final String name;

        Operation(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Operation fromString(String name) {
            for (Operation op : Operation.values()) {
                if (op.name.equalsIgnoreCase(name)) {
                    return op;
                }
            }
            return ADD; // 默认为加
        }
    }

    private final Mode mode;                    // 模式：治疗、设置生命值、设置伤害吸收值
    private final Operation operation;          // 操作：加、减、乘、设置
    private final String amountExpression;      // 数值表达式
    private final String target;                // 目标

    public HealthOperation(Mode mode, Operation operation, String amountExpression, String target) {
        this.mode = mode;
        this.operation = operation;
        this.amountExpression = amountExpression;
        this.target = target;
    }

    @Override
    public void apply(AffixContext context) {
        // 根据target确定应用到哪个实体
        LivingEntity targetEntity = target.equals("self") ? context.getOwner() : context.getTarget();
        if (targetEntity == null) {
            return; // 如果为空，返回
        }

        // 计算数值
        double computedAmount = ExpressionHelper.evaluate(amountExpression, context.getVariables());

        switch (mode) {
            case HEAL:
                handleHeal(targetEntity, computedAmount);
                break;
            case HEALTH:
                handleSetHealth(targetEntity, computedAmount);
                break;
            case ABSORPTION:
                handleSetAbsorption(targetEntity, computedAmount);
                break;
        }
    }

    /**
     * 处理治疗
     */
    private void handleHeal(LivingEntity entity, double amount) {
       entity.heal((float) amount);
    }

    /**
     * 处理设置生命值
     */
    private void handleSetHealth(LivingEntity entity, double amount) {
        switch (operation) {
            case ADD:
                float newHealthAdd = entity.getHealth() + (float) amount;
                entity.setHealth(newHealthAdd);
                break;
            case SUBTRACT:
                float newHealthSubtract = entity.getHealth() - (float) amount;
                entity.setHealth(Math.max(0.0f, newHealthSubtract));
                break;
            case MULTIPLY:
                float currentHealth = entity.getHealth();
                float newHealthMultiply = calculateModifiedValue(currentHealth, (float) amount, operation);
                entity.setHealth(newHealthMultiply);
                break;
            case SET:
                entity.setHealth((float) amount);
                break;
        }
    }


    /**
     * 处理设置伤害吸收值
     */
    private void handleSetAbsorption(LivingEntity entity, double amount) {
        switch (operation) {
            case ADD:
                float absorptionAdd = entity.getAbsorptionAmount() + (float) amount;
                entity.setAbsorptionAmount(absorptionAdd);
                break;
            case SUBTRACT:
                float absorptionSubtract = Math.max(0.0f, entity.getAbsorptionAmount() - (float) amount);
                entity.setAbsorptionAmount(absorptionSubtract);
                break;
            case MULTIPLY:
                float currentAbsorption = entity.getAbsorptionAmount();
                float newAbsorption = calculateModifiedValue(currentAbsorption, (float) amount, operation);
                entity.setAbsorptionAmount(newAbsorption);
                break;
            case SET:
                entity.setAbsorptionAmount((float) amount);
                break;
        }
    }

    /**
     * 根据操作类型计算修改后的值
     */
    private float calculateModifiedValue(float currentValue, float modifier, Operation operation) {
        return switch (operation) {
            case ADD -> currentValue + modifier;
            case SUBTRACT -> currentValue - modifier;
            case MULTIPLY -> currentValue * (1.0f + modifier);
            case SET -> modifier;
        };
    }

    @Override
    public void remove(AffixContext context) {
        // HealthOperation是瞬时操作，不需要移除逻辑
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Mode", mode.getName());
        nbt.putString("Operation", operation.getName());
        nbt.putString("AmountExpression", amountExpression);
        nbt.putString("Target", target);
        return nbt;
    }

    @Override
    public String getType() {
        return "health_operation";
    }

    /**
     * 工厂方法，从NBT创建HealthOperation
     */
    public static HealthOperation fromNBT(CompoundTag nbt) {
        String modeStr = nbt.contains("Mode") ? nbt.getString("Mode") : "heal";
        Mode mode = Mode.fromString(modeStr);

        String operationStr = nbt.contains("Operation") ? nbt.getString("Operation") : "";
        Operation operation = Operation.fromString(operationStr);

        String amountExpression = nbt.contains("AmountExpression") ? nbt.getString("AmountExpression") : "0";
        String target = nbt.contains("Target") ? nbt.getString("Target") : "self";

        return new HealthOperation(mode, operation, amountExpression, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("health_operation", HealthOperation::fromNBT);
    }
}