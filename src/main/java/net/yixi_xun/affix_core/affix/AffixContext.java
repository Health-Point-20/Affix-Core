package net.yixi_xun.affix_core.affix;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AffixContext {
    private final Level world;
    private final LivingEntity owner; // 词缀物品的持有者
    private final ItemStack itemStack; // 带有词缀的物品
    private final int affixIndex; // 词缀在物品NBT列表中的索引
    private final Set<String> trigger; // 触发器类型

    // 事件特定信息
    private final Event event;

    // 变量映射，用于表达式计算
    private final Map<String, Object> variables = new HashMap<>();

    public AffixContext(Level world, LivingEntity owner, ItemStack itemStack, Affix affix, String trigger, Event event) {
        this.world = world;
        this.owner = owner;
        this.itemStack = itemStack;
        this.affixIndex = affix.index();
        this.trigger = Set.of(trigger.split(","));
        this.event = event;

        // 初始化基本变量
        variables.put("random", Math.random());
        variables.put("trigger_count", affix.triggerCount());

        // 初始化实体相关的复杂变量
        variables.put("self", createEntityData(owner));
    }

    // 创建实体数据映射
    public Map<String, Object> createEntityData(LivingEntity entity) {
        return createEntityDataStatic(entity);
    }
    
    /**
     * 实体数据创建方法，可供其他类使用
     */
    public static Map<String, Object> createEntityDataStatic(LivingEntity entity) {
        Map<String, Object> entityData = new HashMap<>();

        if (entity == null) {
            return entityData; // 返回空映射
        }

        // 基本状态
        entityData.put("health", entity.getHealth());
        entityData.put("max_health", entity.getMaxHealth());
        entityData.put("absorption", entity.getAbsorptionAmount());
        entityData.put("level", entity instanceof Player player ? player.experienceLevel : 0);
        entityData.put("x", entity.getX());
        entityData.put("y", entity.getY());
        entityData.put("z", entity.getZ());

        // 字符串信息
        entityData.put("name", entity.getName().getString());
        ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        entityData.put("type", entityKey != null ? entityKey.toString() : "unknown");
        entityData.put("uuid", entity.getStringUUID());

        // 布尔信息
        entityData.put("is_sprinting", entity.isSprinting() ? 1 : 0);
        entityData.put("is_sneaking", entity.isCrouching() ? 1 : 0);
        entityData.put("on_ground", entity.onGround() ? 1 : 0);
        entityData.put("is_swimming", entity.isSwimming() ? 1 : 0);

        // NBT数据
        CompoundTag nbt = entity.getPersistentData();
        entityData.put("nbt", parseNbtCompound(nbt));

        // 将实体对象存储起来，以便按需计算属性和效果
        entityData.put("entity_ref", entity);

        return entityData;
    }

    // 解析NBT复合标签
    private static Map<String, Object> parseNbtCompound(CompoundTag compound) {
        Map<String, Object> result = new HashMap<>();

        for (String key : compound.getAllKeys()) {
            Tag tag = compound.get(key);
            result.put(key, parseNbtTag(tag));
        }

        return result;
    }

    // 解析NBT标签
    private static Object parseNbtTag(Tag tag) {
        if (tag instanceof NumericTag numericTag) {
            return numericTag.getAsDouble();
        } else if (tag instanceof StringTag stringTag) {
            return stringTag.getAsString();
        } else if (tag instanceof CompoundTag compoundTag) {
            return parseNbtCompound(compoundTag);
        } else if (tag instanceof ListTag listTag) {
            // 对于列表，只取数值类型元素的平均值
            if (!listTag.isEmpty() && listTag.getElementType() == Tag.TAG_ANY_NUMERIC) {
                double sum = 0;
                for (int i = 0; i < listTag.size(); i++) {
                    sum += listTag.getDouble(i);
                }
                return sum / listTag.size();
            }
            return listTag.size(); // 返回列表大小
        } else {
            return tag.getAsString();
        }
    }

    /**
     * 添加自定义变量
     */
    public void addVariable(String name, Object value) {
        variables.put(name, value);
    }

    /**
     * 获取变量映射
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * 检查冷却是否结束
     */
    public boolean inCooldown() {
        return !AffixManager.isCooldownOver(itemStack, affixIndex, world);
    }

    /**
     * 设置冷却
     */
    public void setCooldown(Long cooldownTicks) {
        if (cooldownTicks > 0) {
            AffixManager.setCooldown(itemStack, affixIndex, cooldownTicks, world);
        }
    }

    // Getter方法
    public Level getWorld() {
        return world;
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getAffixIndex() {
        return affixIndex;
    }

    public Set<String> getTrigger() {
        return trigger;
    }

    public Event getEvent() {
        return event;
    }

    public LivingEntity getTarget() {
        if (trigger.contains("on_attack") && event instanceof LivingHurtEvent attackEvent)
            return attackEvent.getEntity();
        else if (trigger.contains("on_hurt") && event instanceof LivingHurtEvent hurtEvent)
            return hurtEvent.getSource().getEntity() instanceof LivingEntity target ? target : null;
        else if (trigger.contains("on_death") && event instanceof LivingDeathEvent deathEvent)
            return deathEvent.getSource().getEntity() instanceof LivingEntity killer ? killer : null;
        else if (trigger.contains("on_kill") && event instanceof LivingDeathEvent deathEvent)
            return deathEvent.getEntity();
        return owner;
    }
}