
package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.AffixContext;

/**
 * 词缀操作接口，定义词缀触发时执行的操作
 */
public interface IOperation {

    /**
     * 执行操作
     * @param context 词缀执行上下文
     */
    void apply(AffixContext context);

    /**
     * 移除操作
     * @param context 词缀执行上下文
     */
    default void remove(AffixContext context) {
    }

    /**
     * 将操作序列化为NBT
     * @return 包含操作配置的CompoundTag
     */
    CompoundTag toNBT();

    /**
     * 获取操作类型标识符
     * @return 操作类型字符串
     */
    String getType();
}
