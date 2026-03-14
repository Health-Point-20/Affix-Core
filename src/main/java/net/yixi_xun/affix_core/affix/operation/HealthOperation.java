package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.yixi_xun.affix_core.affix.AffixContext;

/**
 * 生命值操作，包括设置生命值、设置伤害吸收值等
 */
public class HealthOperation extends BaseOperation {

    public enum Mode {
        HEALTH("health"),      // 设置生命值
        ABSORPTION("absorption"); // 设置伤害吸收值

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Mode fromString(String name) {
            if (name == null || name.isEmpty()) {
                return HEALTH;
            }
            
            for (Mode mode : values()) {
                if (mode.name.equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return HEALTH;
        }
    }

    private final Mode mode;
    private final MathOperation operation;
    private final String amountExpression;
    private final String target;

    public HealthOperation(Mode mode, MathOperation operation, String amountExpression, String target) {
        this.mode = mode != null ? mode : Mode.HEALTH;
        this.operation = operation != null ? operation : MathOperation.SET;
        this.amountExpression = amountExpression != null ? amountExpression : "0";
        this.target = target != null ? target : "self";
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        LivingEntity targetEntity = getTargetEntity(context, target);
        if (isInValidEntity(targetEntity)) {
            return;
        }

        double computedAmount = evaluateOrDefaultValue(amountExpression, context.getVariables(), 0.0);

        switch (mode) {
            case HEALTH -> handleSetHealth(targetEntity, computedAmount);
            case ABSORPTION -> handleSetAbsorption(targetEntity, computedAmount);
        }
    }

    /**
     * 处理设置生命值
     */
    private void handleSetHealth(LivingEntity entity, double amount) {
        float currentHealth = entity.getHealth();
        float newHealth = HandleOperation((float) amount, currentHealth);
        entity.setHealth(newHealth);
    }

    private float HandleOperation(float amount, float currentHealth) {
        return switch (operation) {
            case ADD -> currentHealth + amount;
            case SUBTRACT -> Math.max(0.0f, currentHealth - amount);
            case MULTIPLY -> operation.apply(currentHealth, amount);
            case SET -> amount;
        };
    }

    /**
     * 处理设置伤害吸收值
     */
    private void handleSetAbsorption(LivingEntity entity, double amount) {
        float currentAbsorption = entity.getAbsorptionAmount();
        float newAbsorption = HandleOperation((float) amount, currentAbsorption);
        entity.setAbsorptionAmount(newAbsorption);
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
        String modeStr = getStringOrDefaultValue(nbt, "Mode", "health");
        Mode mode = Mode.fromString(modeStr);

        String operationStr = getStringOrDefaultValue(nbt, "Operation", "set");
        MathOperation operation = MathOperation.fromString(operationStr);

        String amountExpression = getStringOrDefaultValue(nbt, "AmountExpression", "0");
        String target = getStringOrDefaultValue(nbt, "Target", "");

        return new HealthOperation(mode, operation, amountExpression, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("health_operation", HealthOperation::fromNBT);
    }
}