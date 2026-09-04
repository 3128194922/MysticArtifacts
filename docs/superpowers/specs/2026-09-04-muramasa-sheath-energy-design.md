# 武士刀开鞘与能量机制设计

## 目标

修改 MysticArtifacts 的 `katana`（`MuramasaItem`）为双状态武器：默认未开鞘，积攒能量后通过潜行右键冲刺进入 10 秒开鞘状态。实现不添加 SlashBlade_Resharped 依赖，不引用其 Java 类、实体、模型或纹理；只参考阎魔刀的攻击表现，并由 MysticArtifacts 自己提供刀光实体、渲染和资源。

## 状态机

物品 NBT 使用以下字段：

- `KatanaEnergy`：0 至 100 的整数，默认 0。
- `KatanaOpenUntil`：逻辑服务器游戏刻，0 表示未开鞘。
- `KatanaCircleAttackUntil`：环形攻击结束时间，用于第三个 AOE 脉冲后立即清除开鞘状态。

状态判断以 `KatanaOpenUntil > level.getGameTime()` 为准。物品在 `inventoryTick` 中清理过期状态，并将能量限制在合法范围。物品被丢弃、切换手持或重新加载时不额外创建全局状态；状态始终随物品栈保存。

## 输入与伤害

### 未开鞘左键

- 保留 SwordItem 的普通近战伤害。
- 对成功命中的目标施加强击退。
- 每次成功命中恢复 1 点能量。
- 不生成开鞘刀光，不消耗能量。

### 未开鞘右键

- 保留当前长按格挡、完美格挡、投射物处理、反弹和耐久消耗行为。
- 删除原有的直接生命值消耗。
- 每次成功格挡恢复 10 点能量，能量最多 100。

### 满能量潜行右键

- 仅当能量达到 100 且未处于冷却时触发。
- 消耗 100 点能量。
- 沿玩家水平视线方向冲刺，保留当前冲刺步高处理。
- 使用玩家攻击力 × `KatanaDashDamageMultiplier`（当前配置默认 10.0）造成冲刺伤害。
- 伤害来源使用玩家自身的 `playerAttack` DamageSource，并保留武器附魔伤害加成。
- 生成 MysticArtifacts 自有冲刺刀光效果。
- 成功后将 `KatanaOpenUntil` 设置为当前游戏刻 + 200。

### 开鞘左键

- 由服务端验证持有的仍是该武士刀且状态有效。
- 生成面向玩家视线的 MysticArtifacts 刀光实体，使用玩家攻击力 ×1.0 造成范围伤害。
- 刀光实体记录攻击者，并排除攻击者自身；同一实体生命周期内同一目标只命中一次。
- 不恢复能量。

### 开鞘右键

- 不再进入格挡使用状态。
- 生成以玩家为中心的环形刀光效果。
- 在半径 4 格范围内，于三个连续攻击时刻造成 AOE，每次伤害为玩家攻击力 ×0.75。
- 三段伤害均使用玩家自身 DamageSource；单个脉冲内目标只命中一次，允许目标被三个脉冲分别命中。
- 第三个脉冲完成后立即清除开鞘状态。
- 若开鞘计时先自然到期，也直接回到未开鞘。
- 不恢复能量。

## 独立刀光实现

新增 MysticArtifacts 自有刀光实体及渲染器，建议拆分为：

- `KatanaSlashEntity`：负责直线/扇形刀光的同步数据、生命周期、命中集合和服务端伤害。
- `KatanaCircleSlashEntity`：负责环形刀光的三段脉冲、每脉冲命中集合和结束回调。
- 客户端渲染器：使用 MysticArtifacts 的 `assets/mysticartifacts` 纹理，通过自定义半透明四边形/环形几何体绘制刀光。

实体的 owner 固定为触发技能的玩家。所有服务端伤害均通过 owner 的属性和 `playerAttack` DamageSource 计算，客户端不执行伤害。

## 网络与侧向

开鞘左键的空挥和无法由服务端自然捕获的挥刀输入通过现有 `NetworkHandler` 注册一个轻量请求包。包处理时切回服务器主线程，并重新验证玩家、手持物、状态、距离/冷却和触发条件。客户端渲染类放在 client 包内，公共代码不直接引用 `net.minecraft.client`，避免专用服务器加载失败。

## 资源

- 新增带剑鞘的未开鞘武士刀 16×16 贴图，基于 MysticArtifacts 当前武士刀风格制作。
- 保留当前刀身贴图作为开鞘显示贴图。
- 新增 MysticArtifacts 自有刀光/环形刀光纹理。
- 物品模型通过 `mysticartifacts:open` 属性在带鞘模型与开鞘模型之间切换。
- 不复制或引用 SlashBlade_Resharped 的材质文件。

## 预计修改范围

- `MuramasaItem`、`KatanaEvents`、`NetworkHandler`、`ModEntities` 与客户端事件/渲染注册。
- 新增武士刀网络包、刀光实体、实体渲染器及必要的辅助类。
- 更新武士刀物品模型、属性注册和 MysticArtifacts 纹理资源。
- 不修改 SlashBlade_Resharped，不调整 MysticArtifacts 的 Gradle 第三方依赖。

## 验证

1. 执行 MysticArtifacts 编译任务。
2. 确认专用服务器不加载客户端渲染类。
3. 验证能量：命中 +1、格挡 +10、上限 100、冲刺 -100。
4. 验证状态：默认未开鞘、冲刺开鞘 200 tick、开鞘右键三段 AOE 后立即关闭、自然到期关闭。
5. 验证所有刀光伤害来源为玩家，开鞘攻击不恢复能量。
6. 验证默认带鞘贴图与开鞘刀身贴图切换。

