# Affix-Core

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.13-orange.svg)](https://files.minecraftforge.net/)

## 概述

Affix_Core 是一个Minecraft Forge模组，提供了一个的词缀系统，允许通过为物品添加NBT定义物品的词缀来在各种事件中触发各种效果。

## 功能

- 为物品添加多种词缀效果
- 修改物品耐久度/最大耐久等
- 冷却机制管理

## 使用方法

- 通过命令 `/affix` 访问词缀相关功能
  - `/affix template <operationType>` - 生成指定操作类型的样板词缀
  - 可用操作类型包括:
    - `deal_damage` - 造成伤害
    - `add_potion` - 添加药水效果
    - `modify_damage` - 修改伤害
    - `attribute` - 属性修改
    - `modify_effect` - 修改效果
    - `nbt_operation` - NBT操作
    - `cancel_event` - 取消事件
    - `execute_command` - 执行命令
    - `modify_duration` - 修改持续时间
    - `health_operation` - 生命值操作
- 通过 `IBE Editor` 之类的模组直接编辑物品NBT来精确控制词缀

## 配置

本模组包含配置文件，可在 `config/affix_core-common.toml` 中找到。您可以根据需要调整各项参数（虽然现在还没有...）。

## 许可证

本项目采用 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0) 许可证。

## 贡献

欢迎提交Issue和Pull Request来帮助改进此项目！

## 作者

- Health_Point

## 致谢

感谢所有测试人员和支持者对本项目的支持。

## 帮助

如果您遇到问题，请在GitHub上提交Issue或访问我们的 [Bilibili页面](https://space.bilibili.com/1424582807) 获取更多信息。