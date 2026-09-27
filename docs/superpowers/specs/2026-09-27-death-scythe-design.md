# 死神镰刀设计

日期：2026-09-27

状态：设计已获用户确认，等待文档审核后进入实现计划。

## 目标

新增 `mysticartifacts:death_scythe`，作为普通主手/副手武器使用。武器采用暗黑风格 Geo 模型，由 GPT-6 Astra 使用 Blockbench MCP 建模并输出可直接放入本模组资源目录的模型、贴图和动画文件。

死神镰刀本身不消耗耐久。玩家攻击生物后，镰刀记录最后一次攻击目标的 UUID，并显示持续 5 秒的能量条。能量未耗尽时右键可无距离限制地攻击该目标，右键冷却 1 秒，同时播放 Geo 斩击动画和客户端斩杀特效。

## 已确认的玩法规则

- 只记录一个目标 UUID，新的攻击会覆盖旧目标。
- 目标必须仍然存活并与玩家处于同一维度。
- 右键触发不限制距离，也不要求视线。
- 右键伤害使用玩家当前 `Attributes.ATTACK_DAMAGE` 数值。
- 右键不会消耗能量；在 5 秒倒计时结束前可每秒重复触发。
- 目标死亡、离开维度、UUID 无法解析或能量归零时，记录失效。
- 镰刀默认作为高伤害近战武器，基础伤害建议为 8.0；具体数值集中放入 Forge 配置，便于后续平衡。
- 右键成功触发后设置 20 tick 物品冷却。

## 状态与数据流

状态保存在镰刀 ItemStack 的服务端 NBT 中：

- `TargetUUID`：目标 UUID。
- `EnergyUntil`：服务端游戏时间戳，设置为当前时间加 100 tick。
- `SlashSequence`：每次成功右键递增，用于触发客户端斩击表现。

不保存每 tick 递减值，能量剩余量通过 `EnergyUntil - level.getGameTime()` 计算，避免背包 tick、跨手持槽或重复物品 tick 造成倒计时不一致。

镰刀重写物品耐久条接口：

- `isBarVisible`：存在有效目标且剩余能量大于 0 时显示。
- `getBarWidth`：将剩余 0～100 tick 映射到 0～13 格。
- `getBarColor`：使用暗红到紫红的能量颜色。
- 不设置真实耐久，不调用 `hurtAndBreak`。

攻击目标记录通过自定义物品的 `hurtEnemy` 服务端逻辑完成，避免客户端伪造 UUID。记录后立即写入 NBT，物品栈同步给客户端以更新能量条。

## 右键攻击流程

1. `DeathScytheItem.use` 只在服务端解析 `TargetUUID`。
2. 通过当前 `ServerLevel` 查找 UUID 对应实体。
3. 验证目标为存活的 `LivingEntity`，且仍在当前维度。
4. 读取玩家当前攻击伤害，使用玩家攻击伤害源对目标造成伤害。
5. 设置物品 20 tick 冷却并递增 `SlashSequence`。
6. 通过现有 `NetworkHandler` 向附近客户端发送斩击特效数据。
7. 触发 GeckoLib 的 `slash` 动画控制器。

无效目标不会造成伤害，也不会播放成功斩击特效；服务端会清理目标 UUID 和能量时间戳。

## Geo 模型与动画

由 GPT-6 Astra 的 Blockbench MCP 创建：

- `assets/mysticartifacts/geo/death_scythe.geo.json`
- `assets/mysticartifacts/animations/death_scythe.animation.json`
- `assets/mysticartifacts/textures/item/death_scythe.png`

模型风格：黑曜石色镰刃、暗红能量裂纹、骨质或金属化握柄、少量紫黑发光部件。模型需要兼容 GUI、第一人称、第三人称和地面展示。

动画至少包含：

- `animation.death_scythe.idle`：低幅度待机摆动。
- `animation.death_scythe.slash`：抬镰、横向斩击、回收三段动作，单次播放。

实现复用项目现有 GeckoLib `GeoItem` 模式：

- `DeathScytheItem implements GeoItem`。
- 使用 `GeoItem.registerSyncedAnimatable`。
- 增加可触发的 `slash` AnimationController。
- 使用独立 `DeathScytheRenderer extends GeoItemRenderer<DeathScytheItem>`。

## 斩击特效

服务端只发送目标位置、玩家位置、动画序列号和随机种子，不让客户端决定伤害或目标。

客户端维护短生命周期的斩击特效实例，使用现有自定义 RenderType/VertexConsumer 风格渲染：

- 由玩家位置指向目标位置的暗红主斩线。
- 叠加黑紫外圈、粒子残影和短暂发光边缘。
- 特效持续约 8～12 tick，使用 `partialTicks` 插值。
- 不创建服务端实体，避免额外实体同步和存档污染。
- 只在客户端渲染，服务端专注伤害和 NBT 状态。

## 注册与文件范围

预计新增或修改：

- `init/ModItems.java`：注册 `DEATH_SCYTHE`。
- `item/impl/DeathScytheItem.java`：攻击记录、能量条、右键攻击、GeoItem 动画。
- `client/render/DeathScytheRenderer.java`：Geo 模型、贴图和动画资源绑定。
- `client/ClientModEvents.java`：注册自定义物品渲染器。
- `network/NetworkHandler.java`：注册斩击特效客户端数据包。
- `network/DeathScytheSlashPacket.java`：传输斩击特效数据。
- `client/deathscythe/DeathScytheSlashRenderer.java`：客户端斩击特效。
- `Config.java`：基础伤害、能量持续时间、右键冷却和特效持续时间。
- `assets/mysticartifacts/geo/`、`animations/`、`textures/item/`：Astra 生成的资源。
- `assets/mysticartifacts/models/item/death_scythe.json`：物品模型入口。
- 中英文语言文件和必要的配方/创造模式标签。

不引入新的第三方依赖，复用项目已有 Forge、GeckoLib 和网络通道。

## 异常与安全边界

- 所有 UUID 查找、伤害、冷却和 NBT 写入在服务端完成。
- 客户端数据包只接受服务端发送，不接受客户端提交目标 UUID 或伤害值。
- 目标实体不是 `LivingEntity`、已死亡、离开维度或无法解析时立即清理记录。
- 玩家死亡、换维度和物品丢失不保留无效目标；ItemStack 本身的 NBT 随物品保存。
- 资源缺失时 GeoItemRenderer 使用安全的空/默认渲染路径，不能影响专用服务器启动。

## 测试与验证

实现前先为以下纯逻辑建立测试：

- 目标 UUID 覆盖规则。
- 100 tick 能量倒计时和耐久条映射。
- 无效目标清理规则。
- 20 tick 右键冷却规则。
- 斩击数据包字段范围和序列号递增。

实现后执行：

- 显式 Java 逻辑测试。
- `gradlew.bat compileJava`。
- `gradlew.bat build` 或项目允许的完整构建任务。
- `git diff --check`。
- 静态 JSON/Geo/动画资源检查。
- 游戏内验证攻击记录、5 秒能量条、右键攻击、动画、特效和跨维度失效。

静态编译通过不等于已经验证游戏内动画和渲染效果，后者必须单独实测。
