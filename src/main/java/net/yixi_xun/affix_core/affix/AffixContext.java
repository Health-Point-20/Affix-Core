package net.yixi_xun.affix_core.affix;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
    private final LivingEntity owner;
    private final ItemStack itemStack;
    private final Affix affix;
    private final Set<String> trigger;
    private final Event event;
    private final Map<String, Object> variables = new HashMap<>();

    public AffixContext(Level world, LivingEntity owner, ItemStack itemStack, Affix affix, String trigger, Event event) {
        this.world = world;
        this.owner = owner;
        this.itemStack = itemStack;
        this.affix = affix;
        this.trigger = Set.of(trigger.split(","));
        this.event = event;

        // 初始化基础变量
        variables.put("random", Math.random());
        variables.put("trigger_count", affix.triggerCount());
        variables.put("time", world.getGameTime());
        variables.put("world_name", world.dimension().location().toString());
    }

    // 初始化 self 变量
    private void ensureSelfInitialized() {
        if (!variables.containsKey("self")) {
            variables.put("self", createEntityData(owner));
        }
    }

    public static Map<String, Object> createEntityData(LivingEntity entity) {
        Map<String, Object> entityData = new HashMap<>();
        if (entity == null) return entityData;

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

        // 将 entity_ref 传入，供 ExpressionHelper 按需解析
        entityData.put("entity_ref", entity);

        return entityData;
    }

    public void addVariable(String name, Object value) {
        variables.put(name, value);
    }

    public Map<String, Object> getVariables() {
        ensureSelfInitialized(); // 确保使用前已初始化
        return variables;
    }

    public boolean inCooldown() {
        return !AffixManager.isCooldownOver(itemStack, affix, world);
    }

    public void setCooldown(Long cooldownTicks) {
        if (cooldownTicks > 0) {
            AffixManager.setCooldown(itemStack, affix, cooldownTicks, world);
        }
    }

    // Getter方法
    public Level getWorld() { return world; }
    public LivingEntity getOwner() { return owner; }
    public ItemStack getItemStack() { return itemStack; }
    public Affix getAffix() { return affix; }
    public Set<String> getTrigger() { return trigger; }
    public Event getEvent() { return event; }

    public LivingEntity getTarget() {
        // 事件类型检查
        if (event instanceof LivingHurtEvent hurtEvent) {
            if (trigger.contains("on_attack")) {
                LivingEntity target = hurtEvent.getEntity();
                return target != null && target.isAlive() ? target : owner;
            } else if (trigger.contains("on_hurt")) {
                Entity source = hurtEvent.getSource().getEntity();
                return source instanceof LivingEntity livingSource && livingSource.isAlive() ? livingSource : null;
            }
        } else if (event instanceof LivingDeathEvent deathEvent) {
            if (trigger.contains("on_kill")) {
                LivingEntity killed = deathEvent.getEntity();
                return killed != null ? killed : owner;
            } else if (trigger.contains("on_death")) {
                Entity killer = deathEvent.getSource().getEntity();
                return killer instanceof LivingEntity livingKiller && livingKiller.isAlive() ? livingKiller : null;
            }
        }

        // 自定义target变量处理
        Object targetVar = variables.get("target");
        if (targetVar instanceof Map<?,?> targetMap) {
            Object entityRef = targetMap.get("entity_ref");
            if (entityRef instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
                return livingEntity;
            }
        }

        return owner;
    }
}
