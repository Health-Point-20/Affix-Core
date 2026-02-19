# Affix-Core

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.13-orange.svg)](https://files.minecraftforge.net/)
[![Version](https://img.shields.io/badge/Version-v1.1-blue.svg)](https://github.com/yourusername/Affix-Core)

## 概述

Affix_Core 是一个功能强大的Minecraft Forge模组，提供了一套完整的词缀系统。该系统允许通过为物品添加NBT数据来定义词缀，在各种游戏事件中触发丰富多样的效果。

## 版本特性

### Affix Core v1.1 更新亮点

**⚡ 新增功能**
- 新增repeat指令和wait指令
- 触发自定义消息事件和对应操作
- DealDamage操作新增范围攻击功能
- Potion操作新增效果叠加功能
- 新增属性闪避率/命中率以及最终命中率/最终闪避率

**🎨 界面增强**
- 新增Tooltip处理类，提供三大核心功能：
  1. 替换占位符为具体内容
  2. 条件控制文本显示
  3. 丰富的字体颜色处理功能

## 核心功能

### 🎯 词缀系统
- 支持多种触发器（攻击、受伤、死亡、使用等）
- 灵活的条件表达式系统
- 冷却机制管理
- 槽位限制功能

### ⚡ 操作类型
- **DealDamageOperation**: 造成伤害
- **PotionOperation**: 添加药水效果
- **AttributeOperation**: 属性修改
- **HealthOperation**: 修改生命值
- **CommandOperation**: 执行命令
- **NBTOperation**: NBT数据操作
- **ModifyDamageOperation**: 修改伤害
- **ModifyEffectOperation**: 修改获得的药水效果
- **ModifyDurationOperation**: 修改物品耐久度/最大耐久度
- **CancelEventOperation**: 取消事件

### 🔢 表达式计算引擎
- 支持数学运算（+、-、*、/、^）
- 逻辑运算（&&、||、!）
- 比较运算（==、!=、<、>、<=、>=）
- 内置函数（min、max、log）
- 字符串比较支持

### 📊 变量系统

变量系统分为两类：终端变量和中间变量。

#### 中间变量（Intermediate Variables）
中间变量是可以继续通过点号访问其属性的复合变量：

**实体相关变量**:
- `self`: 触发实体数据（可访问 `self.health`、`self.max_health` 等）
- `target`: 目标实体数据（可访问 `target.x`、`target.name` 等）
- `attacker`: 攻击者实体数据（on_hurt/on_attack触发器时存在）
- `killer`: 击杀者实体数据（on_killed/on_death触发器时存在）
- `owner`: 持有者引用

**其他复合变量**:
- `item`: 物品数据（可访问 `item.count`、`item.max_damage` 等）

#### 终端变量（Terminal Variables）
终端变量是不能再继续点号访问的基本属性变量：

**数值型终端变量**:
- `health`: 生命值
- `max_health`: 最大生命值
- `absorption`: 伤害吸收值
- `level`: 等级
- `x`, `y`, `z`: 坐标
- `damage`: 伤害值
- `distance`: 距离
- `duration`: 持续时间
- `amplifier`: 等级
- `random`: 随机数 [0,1)
- `trigger_count`: 触发次数

**字符串型终端变量**:
- `name`: 实体名称
- `type`: 实体类型ID
- `uuid`: 实体UUID
- `block_name`: 方块名称
- `block_id`: 方块ID
- `damage_type`: 伤害类型
- `slot`: 装备槽位

**布尔型终端变量**:
- `is_sprinting`: 是否疾跑 
- `is_sneaking`: 是否潜行 
- `on_ground`: 是否在地面 
- `is_swimming`: 是否游泳 

### 🎨 Tooltip处理系统

**核心功能**:
1. **占位符替换**: `${variable}` 格式动态内容替换
2. **条件显示**: `?{condition}text??{!condition}text` 格式条件文本
3. **颜色处理**: 丰富的字体颜色和样式支持

**颜色格式支持**:
- **单色**: `{c,red}` 或 `{c, #FFFFFF}`
- **循环色**: `{c, red-blue-yellow-green-#FFFFFF}`
- **渐变色**: `{c, #FFFFFF -> red -> #FFFFFF}`
- **样式组合**: `{c, red+bold}`

### 🛠️ 配置管理

**配置文件位置**: `config/affix_core-common.toml`

**重要配置项**:
- **范围伤害参数**:
  - `MAX_AREA_DAMAGE_RANGE`: 最大范围伤害半径
  - `MAX_AREA_DAMAGE_ENTITIES`: 范围伤害最大实体数量

## 使用方法

### 命令系统
通过命令 `/affix` 访问词缀相关功能：
```
/affix template <operationType> - 生成指定操作类型的样板词缀
```

**可用操作类型**:
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

### NBT编辑
推荐通过 `IBE Editor` 等模组直接编辑物品NBT来精确控制词缀配置。

### 触发器类型
- `on_attack`: 攻击时触发
- `on_hurt`: 受伤时触发
- `on_kill`: 击杀时触发
- `on_death`: 死亡时触发
- `on_tick`: 每tick触发
- `on_right_click`: 右击时触发
- `on_use_finish`: 使用完成时触发
- `on_use_tick`: 使用过程中触发
- 更多触发器详见模组文档

## 配置

本模组包含配置文件，可在 `config/affix_core-common.toml` 中找到。可调整的重要参数包括：

### 范围伤害配置
- `MAX_AREA_DAMAGE_RANGE`: 最大范围伤害半径
- `MAX_AREA_DAMAGE_ENTITIES`: 范围伤害最大实体数量

## 许可证

本项目采用 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0) 许可证。

## 贡献

欢迎提交Issue和Pull Request来帮助改进此项目！

## 作者

- Health_Point

## 帮助

如果您遇到问题，请在GitHub上提交Issue或访问我们的 [Bilibili页面](https://space.bilibili.com/1424582807) 获取更多信息。
