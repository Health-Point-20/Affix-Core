package net.yixi_xun.affix_core.affix;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * 词缀NBT工具类，提供便捷的NBT操作方法
 */
public class AffixNBTUtils {

    private static final String AFFIX_TAG_KEY = "Affixes";

    /**
     * 检查物品是否有词缀
     */
    public static boolean hasAffixes(ItemStack itemStack) {
        return itemStack != null && 
               itemStack.hasTag() && 
               itemStack.getOrCreateTag().contains(AFFIX_TAG_KEY, Tag.TAG_LIST);
    }

    /**
     * 获取物品的词缀列表
     */
    public static ListTag getAffixList(ItemStack itemStack) {
        if (!hasAffixes(itemStack)) {
            return new ListTag();
        }
        return itemStack.getOrCreateTag().getList(AFFIX_TAG_KEY, Tag.TAG_COMPOUND);
    }

    /**
     * 设置物品的词缀列表
     */
    public static void setAffixList(ItemStack itemStack, ListTag affixList) {
        if (itemStack == null) {
            return;
        }

        if (!itemStack.hasTag()) {
            itemStack.setTag(new CompoundTag());
        }

        if (affixList.isEmpty()) {
            itemStack.getOrCreateTag().remove(AFFIX_TAG_KEY);
        } else {
            itemStack.getOrCreateTag().put(AFFIX_TAG_KEY, affixList);
        }
    }

    /**
     * 添加词缀到物品
     */
    public static void addAffix(ItemStack itemStack, CompoundTag affixNBT) {
        if (itemStack == null || affixNBT == null) {
            return;
        }

        ListTag affixList = getAffixList(itemStack);
        affixList.add(affixNBT);
        setAffixList(itemStack, affixList);
    }

    /**
     * 移除物品的指定索引词缀
     */
    public static boolean removeAffix(ItemStack itemStack, int index) {
        if (!hasAffixes(itemStack)) {
            return false;
        }

        ListTag affixList = getAffixList(itemStack);
        if (index < 0 || index >= affixList.size()) {
            return false;
        }

        affixList.remove(index);
        setAffixList(itemStack, affixList);
        return true;
    }

    /**
     * 移除物品的所有词缀
     */
    public static void clearAffixes(ItemStack itemStack) {
        if (hasAffixes(itemStack)) {
            itemStack.getOrCreateTag().remove(AFFIX_TAG_KEY);
        }
    }

    /**
     * 获取物品的词缀数量
     */
    public static int getAffixCount(ItemStack itemStack) {
        return hasAffixes(itemStack) ? getAffixList(itemStack).size() : 0;
    }

    /**
     * 创建一个基本的词缀NBT结构
     */
    public static CompoundTag createBasicAffix(String trigger, String operationType) {
        CompoundTag affixNBT = new CompoundTag();

        if (trigger != null && !trigger.isEmpty()) {
            affixNBT.putString("Trigger", trigger);
        }

        CompoundTag operationNBT = new CompoundTag();
        operationNBT.putString("Type", operationType);
        affixNBT.put("Operation", operationNBT);

        return affixNBT;
    }

    /**
     * 创建一个伤害词缀NBT
     */
    public static CompoundTag createDamageAffix(String trigger, float amount, String damageType) {
        CompoundTag affixNBT = createBasicAffix(trigger, "damage");
        CompoundTag operationNBT = affixNBT.getCompound("Operation");

        operationNBT.putFloat("Amount", amount);
        if (damageType != null && !damageType.isEmpty()) {
            operationNBT.putString("DamageType", damageType);
        }

        return affixNBT;
    }

    /**
     * 创建一个药水效果词缀NBT
     */
    public static CompoundTag createPotionAffix(String trigger, String effectId, int duration, int amplifier) {
        CompoundTag affixNBT = createBasicAffix(trigger, "potion");
        CompoundTag operationNBT = affixNBT.getCompound("Operation");

        operationNBT.putString("Effect", effectId);
        operationNBT.putInt("Duration", duration);
        operationNBT.putInt("Amplifier", amplifier);

        return affixNBT;
    }
}
