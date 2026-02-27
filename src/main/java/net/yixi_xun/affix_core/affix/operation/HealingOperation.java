package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.yixi_xun.affix_core.affix.AffixContext;

/**
 * 治疗操作，用于修改治疗量或直接治疗实体
 */
public class HealingOperation extends BaseOperation {

    public enum Mode {
        MODIFY("modify"),         // 修改治疗量（只在实体被治疗时生效）
        HEAL("heal");             // 直接治疗实体

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Mode fromString(String name) {
            if (name == null || name.isEmpty()) {
                return MODIFY;
            }
            
            for (Mode mode : values()) {
                if (mode.name.equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return MODIFY;
        }
    }

    private final Mode mode;
    private final MathOperation operation;
    private final String amountExpression;
    private final String target;

    public HealingOperation(Mode mode, MathOperation operation, String amountExpression, String target) {
        this.mode = mode != null ? mode : Mode.MODIFY;
        this.operation = operation != null ? operation : MathOperation.ADD;
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
            case MODIFY:
                if (context.getEvent() instanceof LivingHealEvent event) {
                    float originalHealAmount = event.getAmount();
                    float modifiedHealAmount = operation.apply(originalHealAmount, (float) computedAmount);
                    event.setAmount(modifiedHealAmount);
                }
                break;
            case HEAL:
                targetEntity.heal((float) computedAmount);
                break;
        }
    }

    @Override
    public void remove(AffixContext context) {
        // HealingOperation是瞬时操作，不需要移除逻辑
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
        return "healing_operation";
    }

    /**
     * 工厂方法，从NBT创建HealingOperation
     */
    public static HealingOperation fromNBT(CompoundTag nbt) {
        String modeStr = getString(nbt, "Mode", "heal");
        Mode mode = Mode.fromString(modeStr);

        String operationStr = getString(nbt, "Operation", "add");
        MathOperation operation = MathOperation.fromString(operationStr);

        String amountExpression = getString(nbt, "AmountExpression", "0");
        String target = getString(nbt, "Target", "self");

        return new HealingOperation(mode, operation, amountExpression, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("healing_operation", HealingOperation::fromNBT);
    }
}
