# 幽匿共生体 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `trail_sight` Curios 饰品改造成隐藏实体、监听原版震动、累积暴露值并释放监守者音爆的幽匿共生体。

**Architecture:** 服务端为每个佩戴者创建基于 `VibrationSystem` 的动态震动监听器，使用原版传播、遮挡和事件筛选逻辑；传感器只把附近震动批量同步到佩戴者客户端，并在服务端维护有限的暴露状态。客户端通过实体渲染器代理统一隐藏实体和碰撞箱，通过独立队列渲染震动波纹。

现有脚印缓存、客户端实体采样和脚印渲染链路会被移除，避免新旧两套视觉效果同时运行；物品注册 ID 仍保持 `trail_sight`。

**Tech Stack:** Minecraft Java 1.20.1、Forge 47.4.6、Curios 5.14.1、现有 SimpleChannel 网络、Java 标准集合和 PowerShell/JDK 合约测试。

**Spec:** `docs/superpowers/specs/2026-09-14-sculk-symbiote-design.md`

## Global Constraints

- 保留物品注册 ID `trail_sight`，避免已有存档中的饰品失效。
- 不增加第三方依赖；复用现有 Curios API、Forge 网络和 Minecraft 原版 `VibrationSystem`。
- 包路径只能使用 `com.uniye.mysticartifacts`，不得出现 `example`。
- 客户端类必须隔离，专用服务器不能加载 `net.minecraft.client` 类型。
- 默认震动监听半径为 16 格，暴露值每次增加 10，满值 100，音爆冷却 40 tick。
- 当前环境无法访问指定 KubeJS 素材目录，资源替换任务必须先检查路径并记录实际结果，不得伪造素材来源。

---

### Task 1: 服务端配置与暴露值纯逻辑

**Files:**
- Modify: `src/main/java/com/uniye/mysticartifacts/Config.java`
- Create: `src/main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteExposure.java`
- Test: `src/test/sculk-symbiote-exposure-test.ps1`

**Interfaces:**
- `SculkSymbioteExposure.add(int amount, int maximum)` 返回达到满值后的新值并封顶。
- `SculkSymbioteExposure.reset()` 清零。
- `SculkSymbioteExposure.isFull(int maximum)` 判断是否满值。
- `Config.SCULK_VIBRATION_RADIUS`、`SCULK_EXPOSURE_PER_EVENT`、`SCULK_EXPOSURE_MAX`、`SCULK_SONIC_BOOM_COOLDOWN`、`SCULK_MAX_TRACKED_SOURCES` 提供服务端配置。

- [ ] **Step 1: Write the failing test**

在 PowerShell 测试脚本中编译 `SculkSymbioteExposure.java` 的 JDK harness，断言 `0 + 10 = 10`、`95 + 10` 封顶为 `100`、`isFull(100)` 正确，并检查 `Config.java` 包含五个配置字段。

- [ ] **Step 2: Run test to verify it fails**

运行 `& .\src\test\sculk-symbiote-exposure-test.ps1`，预期因纯逻辑类和配置字段不存在而失败。

- [ ] **Step 3: Write minimal implementation**

在 `Config.java` 的现有 Forge 配置 builder 中加入：监听半径 `16, 1, 64`，每次增加 `10, 1, 100`，满值 `100, 1, 1000`，冷却 `40, 0, 1200`，目标上限 `128, 1, 512`；纯逻辑类只保存一个整数并实现上述三个方法。

- [ ] **Step 4: Run test to verify it passes**

重新运行 `& .\src\test\sculk-symbiote-exposure-test.ps1`，预期输出 `sculk symbiote exposure: PASS`。

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/uniye/mysticartifacts/Config.java src/main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteExposure.java src/test/sculk-symbiote-exposure-test.ps1
git commit -m "feat: add sculk symbiote exposure state"
```

### Task 2: 原版 VibrationSystem 服务端传感器

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteServerHandler.java`
- Create: `src/main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteSensor.java`
- Test: `src/test/sculk-symbiote-vibration-contract-test.ps1`

**Interfaces:**
- `SculkSymbioteServerHandler.onPlayerTick(PlayerTickEvent)` 管理佩戴者传感器生命周期。
- `SculkSymbioteSensor implements VibrationSystem, VibrationSystem.User`。
- `SculkSymbioteSensor.tick()` 调用 `VibrationSystem.Ticker.tick` 并移动动态监听器。
- `SculkSymbioteSensor.onReceiveVibration(...)` 接收事件位置、事件类型和来源实体。

- [ ] **Step 1: Write the failing test**

契约脚本检查服务端源文件包含 `VibrationSystem`、`VibrationSystem.User`、`DynamicGameEventListener`、`VibrationSystem.Ticker.tick`、`GameEventTags.VIBRATIONS`、`onReceiveVibration`、`isWearing` 和 `remove`。

- [ ] **Step 2: Run test to verify it fails**

运行 `& .\src\test\sculk-symbiote-vibration-contract-test.ps1`，预期因传感器文件不存在而失败。

- [ ] **Step 3: Write minimal implementation**

传感器持有 `ServerPlayer`、`VibrationSystem.Data`、`EntityPositionSource` 和 `DynamicGameEventListener<VibrationSystem.Listener>`；佩戴时 `add/move`，卸下或换维度时 `remove`。`canReceiveVibration` 排除佩戴者自身，`onReceiveVibration` 将位置加入批次队列，并仅对 `LivingEntity` 来源更新该玩家对应的暴露值。目标状态超过配置上限时按最近接收时间清理。

- [ ] **Step 4: Run test to verify it passes**

运行传感器契约测试，并执行已有轨迹/注册/接线契约测试，预期全部通过。

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteServerHandler.java src/main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteSensor.java src/test/sculk-symbiote-vibration-contract-test.ps1
git commit -m "feat: listen to vanilla vibrations for sculk symbiote"
```

### Task 3: 震动批次网络与原版音爆

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/network/SculkSymbioteVibrationPacket.java`
- Create: `src/main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteSonicBoom.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/network/NetworkHandler.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java`
- Test: `src/test/sculk-symbiote-network-contract-test.ps1`

**Interfaces:**
- `SculkSymbioteVibrationPacket(List<Marker>)` 服务端到客户端传输同一 tick 的位置、频率和来源 UUID。
- `SculkSymbioteVibrationPacket.handle(...)` 只通过 `DistExecutor` 调用客户端处理器。
- `SculkSymbioteSonicBoom.fire(ServerPlayer player, LivingEntity target)` 释放一次监守者音爆。

- [ ] **Step 1: Write the failing test**

检查数据包具有批次列表、数量上限、`PLAY_TO_CLIENT` 注册、客户端 `DistExecutor` 分流，以及音爆实现包含 `ParticleTypes.SONIC_BOOM`、`SoundEvents.WARDEN_SONIC_BOOM`、`damageSources().sonicBoom` 和 `setDeltaMovement/push`。

- [ ] **Step 2: Run test to verify it fails**

运行网络契约脚本，预期因数据包和音爆工具类不存在而失败。

- [ ] **Step 3: Write minimal implementation**

在 `NetworkHandler.register()` 末尾追加 packet ID，避免改变现有 ID。传感器在玩家 tick 末尾把当前 tick 的标记打包发送。音爆沿玩家眼睛到目标眼睛的方向发送原版音爆粒子，造成 10 点 sonic boom 伤害并应用监守者同款范围、方向和击退；完成后清零目标暴露值并设置服务端冷却。

- [ ] **Step 4: Run test to verify it passes**

运行网络契约、暴露值和已有网络相关测试，确认没有改变现有 packet 注册顺序。

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/uniye/mysticartifacts/network/SculkSymbioteVibrationPacket.java src/main/java/com/uniye/mysticartifacts/sculk/SculkSymbioteSonicBoom.java src/main/java/com/uniye/mysticartifacts/network/NetworkHandler.java src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java src/test/sculk-symbiote-network-contract-test.ps1
git commit -m "feat: sync sculk vibrations and fire sonic boom"
```

### Task 4: 客户端实体隐藏和碰撞箱状态

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteClientState.java`
- Create: `src/main/java/com/uniye/mysticartifacts/client/sculk/HiddenEntityRenderer.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightClientHandler.java`
- Delete: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightTracker.java`
- Delete: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightRenderer.java`
- Delete: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailBuffer.java`
- Delete: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailSample.java`
- Test: `src/test/sculk-symbiote-visibility-contract-test.ps1`

**Interfaces:**
- `SculkSymbioteClientState.setActive(boolean)` 安装/解除实体渲染代理，并保存原始阴影与碰撞箱开关。
- `SculkSymbioteClientState.isActive()` 返回当前佩戴显示状态。
- `SculkSymbioteClientState.acceptMarkers(...)` 接收网络批次。
- `HiddenEntityRenderer` 委托原始 renderer；激活时对实体模型、名称和阴影不调用原始 renderer。

- [ ] **Step 1: Write the failing test**

契约脚本检查客户端状态包含 `EntityRenderDispatcher`、`setRenderHitBoxes(false)`、`setRenderShadow(false)`、状态恢复、renderer map 代理和世界/退出清理。

- [ ] **Step 2: Run test to verify it fails**

运行可见性契约脚本，预期因新客户端状态和代理类不存在而失败。

- [ ] **Step 3: Write minimal implementation**

使用 `EntityRenderDispatcher.renderers` 与 `getSkinMap()` 安装一次代理；代理在激活状态直接跳过原始 renderer，在非激活状态正常委托。激活时保存 `shouldRenderHitBoxes()` 和阴影开关，设置为 false；状态关闭、注销和切换世界时恢复。只在状态变化时刷新 map，不在每个渲染帧重新包装。旧 `TrailSightTracker`、`TrailSightRenderer`、`TrailBuffer` 和 `TrailSample` 同步删除。

- [ ] **Step 4: Run test to verify it passes**

运行可见性契约和已有客户端接线测试。

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteClientState.java src/main/java/com/uniye/mysticartifacts/client/sculk/HiddenEntityRenderer.java src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightClientHandler.java src/test/sculk-symbiote-visibility-contract-test.ps1
git commit -m "feat: hide entities while sculk symbiote is worn"
```

### Task 5: 震动标记渲染与物品文案

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteRenderer.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java`
- Modify: `src/main/resources/assets/mysticartifacts/lang/zh_cn.json`
- Modify: `src/main/resources/assets/mysticartifacts/lang/en_us.json`
- Modify: `src/main/resources/assets/mysticartifacts/models/item/trail_sight.json`
- Test: `src/test/sculk-symbiote-render-contract-test.ps1`

**Interfaces:**
- `SculkSymbioteRenderer.render(RenderLevelStageEvent)` 绘制过期前的震动位置。
- `SculkSymbioteClientState.renderMarkers(...)` 提供有界标记快照。

- [ ] **Step 1: Write the failing test**

检查 renderer 使用 `AFTER_ENTITIES`、`SONIC_BOOM`/幽匿蓝色绘制、年龄淡出、标记上限和配置值；检查语言 JSON 含“幽匿共生体”。

- [ ] **Step 2: Run test to verify it fails**

运行渲染契约脚本，预期因新 renderer、文案和模型引用尚不存在而失败。

- [ ] **Step 3: Write minimal implementation**

使用有界 `ArrayDeque` 保存标记，按 `markerLifetime` 清理；在 `AFTER_ENTITIES` 绘制从事件位置向上扩散的半透明圆环/波纹，按年龄衰减 alpha。将中文名改为“幽匿共生体”，保留英文名 `Sculk Symbiote`。

- [ ] **Step 4: Run test to verify it passes**

运行渲染、资源 JSON 和所有既有 trail-sight 契约测试。

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/uniye/mysticartifacts/client/sculk/SculkSymbioteRenderer.java src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java src/main/resources/assets/mysticartifacts/lang/zh_cn.json src/main/resources/assets/mysticartifacts/lang/en_us.json src/main/resources/assets/mysticartifacts/models/item/trail_sight.json src/test/sculk-symbiote-render-contract-test.ps1
git commit -m "feat: render sculk vibration markers"
```

### Task 6: KubeJS 素材接入与最终验证

**Files:**
- Source check: `E:\MC\_UNSpro\开发环境\Client5.0\versions\1.20.1-Forge\_47.4.20\kubejs`
- Modify: `src/main/resources/assets/mysticartifacts/models/item/trail_sight.json`
- Add: `src/main/resources/assets/mysticartifacts/textures/item/sculk_symbiote.png` when source is accessible
- Test: `src/test/sculk-symbiote-final-contract-test.ps1`

**Interfaces:**
- 资源只从用户指定的 KubeJS 路径复制，目标资源引用使用 `mysticartifacts:item/sculk_symbiote`。

- [ ] **Step 1: Write the failing test**

最终契约检查资源路径存在或明确输出素材目录不可访问，并验证所有源码和资源路径不含 `example`。

- [ ] **Step 2: Run test to verify it fails**

运行最终契约脚本；当前已知源目录不存在，因此资源检查应输出明确的素材阻塞信息，而不是伪造 PASS。

- [ ] **Step 3: Copy only verified source assets**

当指定目录可访问时，只复制幽匿共生体对应的纹理/模型文件，转成 `src/main/resources/assets/mysticartifacts/` 结构；目录不可访问时保留已有可解析模型，并记录待用户提供素材，不使用未经授权的替代来源。

- [ ] **Step 4: Run final verification**

运行所有 `src/test/sculk-symbiote-*.ps1`、已有 trail-sight/revolver 测试、资源 JSON 解析、`git diff --check`，然后尝试 `./gradlew.bat compileJava`。若 Gradle 仍遇到环境权限错误，只报告该环境阻塞，不把它归因于源码。

- [ ] **Step 5: Commit verified resource changes**

```powershell
git add src/main/resources/assets/mysticartifacts src/test/sculk-symbiote-final-contract-test.ps1
git commit -m "feat: finish sculk symbiote assets and verification"
```

## Plan Self-Review

- 目标、震动监听、暴露值、音爆、隐藏渲染、网络批次、资源和测试均有独立任务。
- 服务端只引用世界/实体/网络类型，客户端渲染类集中在 `client` 包。
- 音爆使用原版粒子、声音、伤害源和击退参数，不新增第三方战斗库。
- 旧 `trail_sight` 注册 ID 保持不变，只有显示名称和行为变化。
- 指定素材目录不可访问时不会声称资源已完成，核心代码验证仍可独立进行。
