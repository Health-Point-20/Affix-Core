package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义消息操作，用于发送自定义消息事件
 */
public class EntityVariableOperation extends BaseOperation {
    private final String target;
    private final String key;
    private final String value;

    private static final Map<UUID, Map<String, Object>> ENTITY_VARIABLES = new ConcurrentHashMap<>();

    public EntityVariableOperation(String key, String value, String target) {
        this.target = target != null ? target : "self";
        this.key = key;
        this.value = value;
    }

    public static Map<String, Object> getEntityVariables(LivingEntity entity) {
        return ENTITY_VARIABLES.get(entity.getUUID());
    }

    public static void clearEntityVariables(LivingEntity entity) {
        ENTITY_VARIABLES.remove(entity.getUUID());
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }

        LivingEntity target = getTargetEntity(context, this.target);
        Map<String, Object> current = ENTITY_VARIABLES.get(target.getUUID());

        if (current == null) current = new ConcurrentHashMap<>();

        if (value.startsWith("\"")) {
            current.put(key, value.substring(1, value.length() - 1));
        } else {
            var variables = context.getVariables();
            if (!variables.containsKey(key)) {
                variables.put(key, 0);
            }
            current.put(key, evaluateOrDefaultValue(value, variables, 0));
        }
        ENTITY_VARIABLES.put(target.getUUID(), current);
    }

    @Override
    public void remove(AffixContext context) {
        // 不需要移除
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Target", target);
        nbt.putString("Key", key);
        nbt.putString("Value", value);

        return nbt;
    }

    @Override
    public String getType() {
        return "entity_variable";
    }

    /**
     * 工厂方法，从NBT创建CustomMessageOperation
     */
    public static EntityVariableOperation fromNBT(CompoundTag nbt) {
        String target = nbt.contains("Target") ? nbt.getString("Target") : "self";
        String key = nbt.contains("Key") ? nbt.getString("Key") : "default_key";
        String value = nbt.contains("Value") ? nbt.getString("Value") : "0";

        return new EntityVariableOperation(key, value, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("entity_variable", EntityVariableOperation::fromNBT);
    }
}
