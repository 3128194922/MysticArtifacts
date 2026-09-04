# MysticArtifacts 功能调整设计

## 目标

在 `E:/Server_mod/MysticArtifacts` 内完成以下调整：

1. 保留长矛模型，将 `spear` 与 `griefer_spear` 的第三人称使用动作改为原版三叉戟动作；`griefer_spear` 的基础伤害为 2。
2. 删除 `mysticartifacts:nether_of_voice`，把其“按玩家视角持续修正抛射方向”的机制迁移给 `sword_swarm_charm` 召唤的剑实体。
3. 修复剑群周围剑渲染的卡顿，保持现有环绕视觉效果。
4. `ender_kunai` 投出后默认 5 秒（100 tick）消失，无论仍在飞行还是已经插入方块。
5. 移除旧灼烧器兼容，改为运行时兼容 ClangingHowl 的 `clanginghowl:flamethrower`；ClangingHowl 未安装时不加载相关逻辑。器灵从末影箱寻找并消耗 ClangingHowl 原本使用的 `clanginghowl:blaze_fuel_cylinder` 进行补充。
6. 只提供 `all_seeing_eye` 的优化方案，不在本次实现。

## 边界与兼容策略

- 只修改 MysticArtifacts；不修改 ClangingHowl。
- 不添加 ClangingHowl 编译依赖。通过 `ModList.get().isLoaded("clanginghowl")` 与物品注册表运行时查询实现软兼容。
- 兼容物品 ID 使用字符串常量：`clanginghowl:flamethrower`、`clanginghowl:blaze_fuel_cylinder`。
- 仅在 ClangingHowl 已加载时执行相关注册表查询和喷火器逻辑，避免缺少模组时类加载或注册表异常。
- 不改变长矛模型资源；移除此前误加的三叉戟模型引用，只调整使用动画/手臂姿势。

## 设计

### 长矛动作与伤害

- `SpearItem` 使用 `UseAnim.SPEAR`，并取消自定义弓箭姿势，使实体模型继续使用现有长矛模型，第三人称动作采用原版长矛/三叉戟式蓄力动作。
- `GrieferSpearItem` 继续复用 `SpearItem` 的动作实现，注册参数明确为基础攻击伤害 2。
- 物品模型 JSON 不引用原版三叉戟模型。

### Sword Swarm 视角追踪

- 删除 `NetherOfVoiceItem`、`NetherOfVoiceEntity`、对应注册与客户端渲染注册，以及不再使用的配置项和资源。
- `SwordPhantomEntity` 在服务端飞行期间读取拥有者的视角，将当前速度方向以平滑插值逐 tick 修正到视角方向，同时保持既有速度和命中逻辑。
- 只对仍在飞行、拥有 Player 且未命中的剑生效；实体命中后停止追踪（伤害被拒绝时仍按原逻辑反弹），停止状态随实体 NBT 保存；客户端不自行改变实体运动，避免服务端与客户端竞争。
- 初始生成偏移保留，确保剑从玩家周围而非身体中心生成。

### Sword Swarm 渲染卡顿

- 客户端缓存已解析的剑物品 ID 到 `ItemStack`，避免每帧重复注册表查询和创建对象。
- 渲染过程不调用会写入 NBT 的队列初始化/补全逻辑；队列在服务端装备 `curioTick` 的回满/恢复计时提前返回之前初始化，保留已有非空队列及攻击时的初始化路径。
- 每次渲染直接使用 Curios 已包含 partial tick 的 `ageInTicks` 作为唯一动画时间，身体朝向只插值一次，统一传给所有环绕剑。
- 保留现有层数、角速度和物品模型表现；将可复用的渲染参数放入客户端缓存/参数对象，降低每帧临时对象数量。

### 末影苦无生命周期

- 在 `EnderKunaiEntity` 增加投出后的生命周期计数，出生后累计 100 tick 即 `discard()`。
- 现有插地粒子、发光、追踪器清理和提示逻辑保持不变；超时统一沿用现有 `remove()` 清理路径。
- 计时不因插地而重置，保证“投出后 5 秒”语义一致。

### Artifact Spirit 与 ClangingHowl 喷火器

- 删除旧 `dungeonnowloading:scorcher`、`dungeonnowloading:soul_scorcher` 的 ID、分支与煤炭/木炭弹药判断。
- 将支持判断改为：只有 ClangingHowl 已加载且器灵绑定物品 ID 为 `clanginghowl:flamethrower` 时，才进入喷火器逻辑。
- 器灵维护与 ClangingHowl `IFuel` 一致的燃料容量：单个喷火器最多 1600 fuel；喷火期间每 20 tick 消耗 5 fuel，与原 `onUseTick` 的 `ticks % 20 == 0` 和 `IFuel` 默认每次消耗 5 一致。首个喷火 tick 扣料，此后每隔 20 tick 再扣料；火焰抛射体仍每 tick 生成。
- 器灵燃料不足时，从拥有者末影箱按槽位顺序寻找 `clanginghowl:blaze_fuel_cylinder`，消耗一个并补充 1600 fuel；未安装 ClangingHowl 或找不到燃料时安全跳过，不抛异常。
- 喷火攻击的现有启动、持续、过热、目标与火焰抛射体行为尽量保持，仅替换物品识别与燃料来源。

### all_seeing_eye 优化方案（本次只记录）

后续可按以下顺序实施：

1. 服务端把观察状态按观察者维护，并对请求包做频率限制、目标合法性检查和跨维度/死亡/登出清理。
2. 玩家列表采用缓存与增量更新，不在每次请求时重建完整列表。
3. 客户端缓存皮肤 `GameProfile` 和纹理位置，列表未变化时不重复查询；只绘制可见行并使用稳定的滚动边界。
4. 将每 tick 的全量观察者扫描改为事件驱动，必要时只处理状态发生变化的观察者。

## 验证

- 对修改后的 JSON 做解析检查，确认没有误引用三叉戟模型。
- 搜索源码确认 Nether of Voice 注册、旧灼烧器 ID 和直接编译依赖均已清除。
- 静态检查生命周期为 100 tick、ClangingHowl 软兼容检查、燃料 ID、1600 fuel 容量及每 20 tick 消耗 5 fuel 的节奏。
- 尝试运行项目现有 Gradle 构建/检查任务；若 Gradle wrapper 仍受网络下载限制，则记录为环境阻塞，不把未完成构建称为通过。
- 游戏内重点验证：长矛第三人称蓄力动作、剑群追踪与渲染平滑度、苦无 5 秒消失、ClangingHowl 存在/缺失两种启动场景、末影箱燃料消耗。
