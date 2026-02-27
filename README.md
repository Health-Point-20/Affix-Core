# Affix-Core

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.13-orange.svg)](https://files.minecraftforge.net/)
[![Version](https://img.shields.io/badge/Version-v1.3-blue.svg)](https://github.com/yourusername/Affix-Core)
[![Curios API](https://img.shields.io/badge/Curios-API-orange.svg)](https://www.curseforge.com/minecraft/mc-mods/curios)

## 📋 项目概览

Affix Core 是一个Minecraft Forge模组，提供了一套完整的词缀系统。该系统允许通过为物品添加NBT数据来定义词缀，在各种游戏事件中触发丰富多样的效果。

### 🎯 核心特性
- **灵活的词缀系统**：支持多种触发器和复杂的条件表达式
- **丰富的操作类型**：涵盖伤害、治疗、属性、NBT等多种操作
- **Curios兼容**：支持Curios饰品槽位系统
- **高级Tooltip处理**：动态占位符、条件显示和彩色文本支持
- **抽奖系统**：创新的物品抽奖机制
- **表达式引擎**：支持的数学和逻辑表达式计算

## 🚀 主要功能

### 🔧 词缀系统
- 支持多种触发器（攻击、受伤、死亡、使用等）
- 灵活的条件表达式系统
- 基于UUID的冷却机制管理
- 槽位限制功能
- 优先级排序系统

### ⚡ 操作类型
- **DealDamageOperation**: 造成伤害（支持范围伤害）
- **PotionOperation**: 添加药水效果
- **AttributeOperation**: 属性修改（支持临时和永久修饰符）
- **HealthOperation**: 修改生命值/伤害吸收值
- **HealingOperation**: 治疗操作（直接治疗或修改治疗量）
- **CommandOperation**: 执行命令
- **NBTOperation**: NBT数据操作
- **ModifyDamageOperation**: 修改伤害
- **ModifyEffectOperation**: 修改获得的药水效果
- **ModifyDurationOperation**: 修改物品耐久度/最大耐久度
- **CancelEventOperation**: 取消事件
- **CustomMessageOperation**: 自定义消息显示
- **DisableAffixesOperation**: 禁用其他词缀

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

### 🎰 抽奖系统
- **容器模式**: 从指定容器中抽取物品
- **列表模式**: 从内置物品列表中抽取
- **概率控制**: 可自定义各槽位/物品的抽取概率
- **抽取设置**: 支持重复抽取、消耗物品、消耗自身等选项
- **可视化配置**: 通过命令系统直观配置抽奖参数

### 🔢 表达式计算引擎
- **数学运算**: +、-、*、/、^（幂运算）
- **逻辑运算**: &&（and）、||（or）、!（not）
- **比较运算**: ==、!=、<、>、<=、>=
- **内置函数**: min、max、abs、sqrt、log
- **字符串支持**: 字符串字面量及比较操作
- **变量系统**: 动态上下文变量访问

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
- `time`: 游戏时间
- `world_name`: 世界名称

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

### 🛡️ 特殊属性系统
- **闪避率 (Evasion)**: 控制受到攻击时的闪避概率
- **命中率 (Hit Rate)**: 提高攻击命中敌人的概率
- **最终命中率 (Final Hit Rate)**: 最终命中修正值
- **最终闪避率 (Final Evasion)**: 最终闪避修正值
- 属性自动格式化显示为百分比形式

### 🛠️ 配置管理

**配置文件位置**: `config/affix_core-common.toml`

**重要配置项**:
- **范围伤害参数**:
  - `maxAreaDamageRange`: 最大范围伤害半径（默认: 64.0）
  - `maxAreaDamageEntities`: 范围伤害最大实体数量（默认: 128）
- **抽奖系统参数**:
  - `showRaffleContainerPos`: 是否在tooltip中显示容器位置（默认: false）

## 🎮 使用方法

### 📜 命令系统

#### 词缀命令
```
/affix template <operationType> - 生成指定操作类型的样板词缀
```

**可用操作类型**:
- `deal_damage` - 造成伤害
- `add_potion` - 添加药水效果
- `modify_damage` - 修改伤害
- `attribute_modifier` - 属性修改
- `modify_effect` - 修改效果
- `nbt_operation` - NBT操作
- `cancel_event` - 取消事件
- `execute_command` - 执行命令
- `modify_duration` - 修改持续时间
- `health_operation` - 生命值操作
- `healing_operation` - 治疗操作
- `custom_message` - 自定义消息
- `disable_affixes` - 禁用词缀

#### 抽奖命令
```
/raffle set <players> draw_count <count> - 设置抽取次数
/raffle set <players> repeat <true/false> - 设置是否允许重复
/raffle set <players> probability <slot> <chance> - 设置槽位概率
/raffle set <players> consume_items <true/false> - 设置是否消耗物品
/raffle set <players> consume_self <true/false> - 设置是否消耗自身
/raffle copy_from <pos> - 从指定位置复制容器物品
/raffle clear items - 清空内置物品列表
/raffle clear container - 解绑容器
/raffle info - 显示当前抽奖信息
/raffle init - 初始化默认配置
```

#### 辅助命令
```
/repeat <times> <interval> run <command> - 重复执行命令
/wait <ticks> run <command> - 延迟执行命令
```

### 🔧 NBT编辑
推荐通过 `IBE Editor` 等模组直接编辑物品NBT来精确控制词缀配置。

### 🎯 触发器类型
- `on_attack`: 攻击时触发
- `on_hurt`: 受伤时触发
- `on_kill`: 击杀时触发
- `on_death`: 死亡时触发
- `on_tick`: 每tick触发
- `on_right_click`: 右击时触发
- `on_use_finish`: 使用完成时触发
- `on_use_tick`: 使用过程中触发

## ⚙️ 配置说明

本模组包含配置文件，可在 `config/affix_core-common.toml` 中找到。可调整的重要参数包括：

### 范围伤害配置
- `maxAreaDamageRange`: 最大范围伤害搜索半径（1.0-256.0，默认64.0）
- `maxAreaDamageEntities`: 范围伤害最大影响实体数（1-1024，默认128）

### 抽奖系统配置
- `showRaffleContainerPos`: 是否在tooltip中显示绑定容器的位置信息（默认false）

## 📄 许可证

本项目采用 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0) 许可证。

## 👥 贡献

欢迎提交Issue和Pull Request来帮助改进此项目！

## 👤 作者

- **Health_Point**

## 🆘 帮助与支持

如果您遇到问题，请在GitHub上提交Issue或访问我的 [Bilibili页面](https://space.bilibili.com/1424582807) 和 [QQ群](https://qm.qq.com/q/wshAOsGgSW) 获取更多信息。

## 🔄 更新日志

### v1.3 (当前版本)
- 新增抽奖系统，支持容器和列表两种模式
- 新增治疗操作类型（HealingOperation）
- 新增自定义消息操作类型（CustomMessageOperation）
- 新增禁用词缀操作类型（DisableAffixesOperation）
- 增强范围伤害功能，支持表达式配置
- 优化属性修饰符管理系统
- 改进Tooltip处理系统的性能
- 添加repeat和wait辅助命令
- 新增闪避率和命中率特殊属性
- 完善Curios API集成支持
