# 属性工具提示合并功能

## 功能描述

此功能实现了类似于 Fabric 模组 DynamicTooltips 的属性工具提示合并功能。它可以将物品的多个属性修饰符合并显示，提高工具提示的可读性和用户体验。

## 主要特性

1. **属性合并显示** - 将同一属性的多个修饰符合并显示
2. **智能分组** - 按装备槽位智能分组显示属性
3. **颜色编码** - 使用不同颜色区分属性变化（增加/减少）
4. **格式化输出** - 提供美观的属性显示格式
5. **配置控制** - 可通过配置文件启用/禁用此功能

## 技术实现

- **事件处理** - 使用 Forge 的 [ItemTooltipEvent](file:///D:/Minecraft/Forge/MinecraftForge/src/main/java/net/minecraftforge/event/entity/player/ItemTooltipEvent.java#L14-L14) 事件拦截物品工具提示
- **属性合并算法** - 实现了高效的属性修饰符合并算法
- **装备槽位检测** - 使用 Minecraft 的 [EquipmentSlot](file:///D:/Minecraft/Forge/MinecraftForge/src/main/java/net/minecraft/world/entity/EquipmentSlot.java#L14-L14) 而非 EquipmentSlotGroup
- **注册表访问** - 使用 ForgeRegistries 访问属性注册表
- **资源位置** - 使用 ResourceLocation.tryParse 而非已废弃的 parse 方法

## 配置选项

在模组配置文件中可以找到以下选项：

```toml
[tooltip]
# 启用属性工具提示合并以改善可读性（默认：true）
enableAttributeTooltipMerging = true
```

## 使用场景

- 当玩家鼠标悬停在带有多个属性修饰符的物品上时
- 在背包界面中查看物品详细信息时
- 适用于武器、护甲和其他带有属性修饰符的物品

## 注意事项

- 此功能仅在客户端运行
- 依赖于 Minecraft Forge 的客户端事件系统
- 与其他修改工具提示的模组可能存在兼容性问题
- 使用了正确的 Forge API 和现代 Minecraft 开发实践