# 猎迹之眼实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 MysticArtifacts 中新增一个 Curios 饰品，客户端以高性能方式记录并渲染所有已加载实体最近 15 秒的脚印轨迹。

**Architecture:** 饰品只负责 Curios 装备状态；客户端 tick handler 负责从 `ClientLevel` 采样实体并写入按 UUID 分组的有界轨迹缓存；世界渲染 handler 负责将缓存中的轨迹点渲染为随时间淡出的左右脚印。轨迹不经过服务器、不持久化，并通过采样间隔、距离、位移阈值和实体上限控制性能。

**Tech Stack:** Minecraft 1.20.1、Forge 47.4.6、Curios 5.14.1+1.20.1 API、Java 17、Forge CLIENT 配置、`RenderLevelStageEvent`。

**Spec:** `docs/superpowers/specs/2026-09-13-trail-sight-design.md`

## Global Constraints

- 只修改 `E:\Server_mod\MysticArtifacts` 主项目；保留工作树中已有未提交改动。
- 不增加第三方依赖；复用现有 Curios API。
- 包路径只能使用 `com.uniye.mysticartifacts`，不得出现 `example`。
- 客户端类必须隔离，专用服务器不能加载 `net.minecraft.client` 类型。
- 默认轨迹寿命为 300 tick，采样间隔为 2 tick，最小位移为 0 格（记录所有非零水平位移），最大距离为 48 格，最大实体数为 256。
- 生产代码先有失败测试，再实现最小通过版本。

### Task 1: 轨迹纯逻辑缓存

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailSample.java`
- Create: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailBuffer.java`
- Create: `src/test/trail-sight-buffer-test.ps1`

**Interfaces:**
- `TrailSample(double x, double y, double z, double directionX, double directionZ, long tick)` 是不可变采样点。
- `TrailBuffer(int maxSamples, long retentionTicks)` 保存单实体采样点。
- `TrailBuffer.add(TrailSample sample)` 追加并清理旧点。
- `TrailBuffer.prune(long currentTick)` 删除超过保留时间的点。
- `TrailBuffer.samples()` 返回只读快照。

- [ ] **Step 1: Write the failing test**

在 PowerShell 测试中编译并运行一个只依赖 JDK 的 Java harness，直接调用 `TrailBuffer`，断言以下行为：

```java
TrailBuffer buffer = new TrailBuffer(2, 300);
buffer.add(new TrailSample(0, 0, 0, 1, 0, 0));
buffer.add(new TrailSample(1, 0, 0, 1, 0, 50));
buffer.add(new TrailSample(2, 0, 0, 1, 0, 101));
check(buffer.samples().size() == 2, "retains bounded samples");
buffer.prune(201);
check(buffer.samples().isEmpty(), "expires after retention ticks");
```

测试还要断言空队列、最大容量丢弃最旧点和只读快照。

- [ ] **Step 2: Run test to verify it fails**

Run: `powershell -ExecutionPolicy Bypass -File src/test/trail-sight-buffer-test.ps1`

Expected: FAIL，因为 `TrailSample.java` 和 `TrailBuffer.java` 尚不存在。

- [ ] **Step 3: Write minimal implementation**

使用 `ArrayDeque<TrailSample>` 保存点；`add` 先按新样本 tick 清理过期点，再追加并移除超过 `maxSamples` 的队首；`samples` 返回 `List.copyOf`，不暴露内部队列。

- [ ] **Step 4: Run test to verify it passes**

Run: `powershell -ExecutionPolicy Bypass -File src/test/trail-sight-buffer-test.ps1`

Expected: 输出 `trail sight buffer: PASS`，退出码为 0。

- [ ] **Step 5: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/client/trail/TrailSample.java src/main/java/com/uniye/mysticartifacts/client/trail/TrailBuffer.java src/test/trail-sight-buffer-test.ps1
git commit -m "test: add trail sight buffer coverage"
```

### Task 2: 饰品注册与客户端配置

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/item/impl/TrailSightItem.java`
- Create: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightClientConfig.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/init/ModItems.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/init/ModCreativeModTabs.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java`
- Modify: `src/main/resources/assets/mysticartifacts/lang/zh_cn.json`
- Modify: `src/main/resources/assets/mysticartifacts/lang/en_us.json`
- Create: `src/main/resources/assets/mysticartifacts/models/item/trail_sight.json`
- Create: `src/test/trail-sight-registration-contract-test.ps1`

**Interfaces:**
- `TrailSightItem.isWearing(LivingEntity)` 查询 Curios 是否装备该物品。
- `TrailSightClientConfig.RANGE`, `RETENTION_TICKS`, `SAMPLE_INTERVAL`, `MIN_STEP`, `MAX_ENTITIES` 提供客户端配置值；`RETENTION_TICKS` 默认 300，允许配置到 1200。

- [ ] **Step 1: Write the failing test**

契约测试读取源码和资源，断言物品注册名、`ICurioItem`、CLIENT 配置、创意标签、模型父级和中英文名称存在。

- [ ] **Step 2: Run test to verify it fails**

Run: `powershell -ExecutionPolicy Bypass -File src/test/trail-sight-registration-contract-test.ps1`

Expected: FAIL，报告缺少 `trail_sight` 注册与资源。

- [ ] **Step 3: Write minimal implementation**

注册 `TRAIL_SIGHT`，物品实现 `ICurioItem` 与 `canEquipFromUse`；配置注册为 `ModConfig.Type.CLIENT`；将物品加入现有创意标签；模型复用现有 `mysticartifacts:item/all_seeing_eye` 纹理，不新增依赖。

- [ ] **Step 4: Run test to verify it passes**

Run: `powershell -ExecutionPolicy Bypass -File src/test/trail-sight-registration-contract-test.ps1`

Expected: 输出 `trail sight registration contract: PASS`。

- [ ] **Step 5: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/item/impl/TrailSightItem.java src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightClientConfig.java src/main/java/com/uniye/mysticartifacts/init/ModItems.java src/main/java/com/uniye/mysticartifacts/init/ModCreativeModTabs.java src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java src/main/resources/assets/mysticartifacts/lang/zh_cn.json src/main/resources/assets/mysticartifacts/lang/en_us.json src/main/resources/assets/mysticartifacts/models/item/trail_sight.json src/test/trail-sight-registration-contract-test.ps1
git commit -m "feat: register trail sight curio"
```

### Task 3: 客户端采样与缓存管理

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightTracker.java`
- Create: `src/test/trail-sight-tracker-contract-test.ps1`

**Interfaces:**
- `TrailSightTracker.tick(ClientLevel level, LocalPlayer player)` 更新客户端缓存。
- `TrailSightTracker.clear()` 清空全部缓存。
- `TrailSightTracker.getTracks()` 返回当前轨迹的只读视图供渲染器使用。

- [ ] **Step 1: Write the failing test**

契约测试断言 tracker 使用 `ClientLevel`、客户端 tick、实体 UUID、采样间隔、位移阈值、距离限制、实体上限以及饰品卸下后的清理逻辑。

- [ ] **Step 2: Run test to verify it fails**

Run: `powershell -ExecutionPolicy Bypass -File src/test/trail-sight-tracker-contract-test.ps1`

Expected: FAIL，因为 tracker 文件不存在。

- [ ] **Step 3: Write minimal implementation**

使用 `Map<UUID, TrailBuffer>` 和 `Map<UUID, Vec3>` 保存轨迹与上次位置；只在 `ClientTickEvent.Phase.END` 且 `TrailSightItem.isWearing(player)` 时执行。遍历 `level.entitiesForRendering()`，按距离和上限筛选；根据实体速度或前后位置计算水平方向；只在水平位移非零时新增采样；每次 tick 删除过期和不存在实体。

- [ ] **Step 4: Run test to verify it passes**

Run: `powershell -ExecutionPolicy Bypass -File src/test/trail-sight-tracker-contract-test.ps1`

Expected: 输出 `trail sight tracker contract: PASS`。

- [ ] **Step 5: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightTracker.java src/test/trail-sight-tracker-contract-test.ps1
git commit -m "feat: track client entity trails"
```

### Task 4: 脚印世界渲染

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightRenderer.java`
- Create: `src/test/trail-sight-render-contract-test.ps1`

**Interfaces:**
- `TrailSightRenderer.render(RenderLevelStageEvent event)` 在指定世界渲染阶段绘制缓存轨迹。

- [ ] **Step 1: Write the failing test**

契约测试断言渲染器是客户端类、订阅 `RenderLevelStageEvent`、使用相机偏移、透明 render type、年龄淡出、左右脚印偏移，并且没有向服务器发送轨迹包。

- [ ] **Step 2: Run test to verify it fails**

Run: `powershell -ExecutionPolicy Bypass -File src/test/trail-sight-render-contract-test.ps1`

Expected: FAIL，因为渲染器文件不存在。

- [ ] **Step 3: Write minimal implementation**

仅处理 `AFTER_ENTITIES` 阶段；从事件获取 `PoseStack`、相机位置和 buffer；对每个样本按剩余寿命计算 alpha；使用水平法线生成左右脚印位置；通过 `VertexConsumer` 绘制两个小四边形；渲染完毕后结束 buffer，保证不修改后续 PoseStack 状态。

- [ ] **Step 4: Run test to verify it passes**

Run: `powershell -ExecutionPolicy Bypass -File src/test/trail-sight-render-contract-test.ps1`

Expected: 输出 `trail sight render contract: PASS`。

- [ ] **Step 5: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/client/trail/TrailSightRenderer.java src/test/trail-sight-render-contract-test.ps1
git commit -m "feat: render fading trail footprints"
```

### Task 5: 客户端事件接线与完整验证

**Files:**
- Modify: `src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java`
- Modify: `src/main/resources/assets/mysticartifacts/lang/zh_cn.json`
- Modify: `src/main/resources/assets/mysticartifacts/lang/en_us.json`

**Interfaces:**
- 客户端 tick 调用 `TrailSightTracker.tick`。
- 客户端世界渲染事件调用 `TrailSightRenderer.render`。

- [ ] **Step 1: Write the failing test**

扩展资源/接线契约测试，断言 `MysticArtifacts` 仅在 `Dist.CLIENT` 加载 tracker 与 renderer，且卸下饰品或世界为空时不执行客户端逻辑。

- [ ] **Step 2: Run test to verify it fails**

Run: `powershell -ExecutionPolicy Bypass -File src/test/trail-sight-wiring-contract-test.ps1`

Expected: FAIL，因为事件尚未接线。

- [ ] **Step 3: Write minimal implementation**

在 `@Mod.EventBusSubscriber(... value = Dist.CLIENT, bus = Bus.FORGE)` 的客户端 handler 中订阅 `ClientTickEvent` 与 `RenderLevelStageEvent`，分别调用 tracker 和 renderer；不要在主 mod 类的静态初始化中引用客户端类。

- [ ] **Step 4: Run test to verify it passes**

Run:

```text
powershell -ExecutionPolicy Bypass -File src/test/trail-sight-wiring-contract-test.ps1
powershell -ExecutionPolicy Bypass -File src/test/revolver-tests.ps1
./gradlew.bat compileJava
./gradlew.bat build
```

Expected: 所有契约测试退出码为 0，Gradle `compileJava` 和 `build` 退出码为 0。

- [ ] **Step 5: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java src/main/resources/assets/mysticartifacts/lang/zh_cn.json src/main/resources/assets/mysticartifacts/lang/en_us.json src/test/trail-sight-wiring-contract-test.ps1
git commit -m "feat: wire trail sight client events"
```

## Plan Self-Review

- 设计文档中的饰品注册、客户端采样、15 秒保留、范围限制、实体上限、左右脚印渲染、透明度衰减、客户端隔离和测试覆盖均有对应任务。
- 未使用第三方测试框架；纯逻辑测试只使用 JDK 和 PowerShell，符合依赖约束。
- 所有跨任务接口在任务定义中明确，`TrailBuffer` 只负责单实体数据，`TrailSightTracker` 负责实体生命周期，`TrailSightRenderer` 只负责绘制。
- 计划中无 `TBD`、`TODO` 或未定义的占位文件。
