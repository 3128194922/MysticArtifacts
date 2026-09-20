# 旗枪 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with review checkpoints.

**Goal:** 在 MysticArtifacts 的 Forge 1.20.1 中新增旗枪，服务端对玩家周围半径 5 格的其他生物造成范围伤害，并通过客户端网络事件渲染 5 条红色发光旗帜轨迹。

**Architecture:** 旗枪是独立的 `SwordItem`，避免复用现有长矛的蓄力/投掷逻辑。Forge `AttackEntityEvent` 只在服务端做范围伤害并发送短生命周期的中心点与朝向；客户端状态容器保存事件，`RenderLevelStageEvent.AFTER_ENTITIES` 使用专用发光 `RenderType` 绘制五条圆周旗帜带，不生成实体。

**Tech Stack:** Minecraft 1.20.1、Forge 47.4.6、Java 17 source compatibility、Forge SimpleChannel、Forge client rendering API、PowerShell contract tests。

**Spec:** `docs/superpowers/specs/2026-09-20-flag-spear-design.md`

## Global Constraints

- 只修改 `E:\Server_mod\MysticArtifacts` 主项目，保留工作区已有无关改动。
- 不引入新的第三方依赖；复用现有长矛贴图。
- 所有伤害逻辑只在服务端执行；客户端网络包只更新渲染状态。
- 包路径使用 `com.uniye.mysticartifacts`，不出现 `example`。
- 固定范围半径 5 格、固定 5 条轨迹、固定红色发光效果。
- 不注册轨迹实体，不改变现有长矛、闪电瓶、武士刀和幽匿共生体行为。
- 每个实现任务都先写失败测试并确认失败，再写最小生产代码。

---

### Task 1: 建立旗枪契约与轨迹数学测试

**Files:**
- Create: `src/test/flag-spear-contract-test.ps1`
- Create: `src/test/flag-spear-profile-test.ps1`
- Create: `src/test/java/com/uniye/mysticartifacts/flag/FlagSpearTrailProfileTest.java`

**Interfaces:**
- Consumes: 当前项目文件结构和即将实现的 `FlagSpearTrailProfile` 常量/方法约定。
- Produces: 可独立运行的静态契约检查，以及用于验证五相位、半径和波形参数的 Java 测试。

- [ ] **Step 1: Write the failing PowerShell contract test**

  检查以下尚未存在的契约：

  ```powershell
  $requiredFiles = @(
      'main/java/com/uniye/mysticartifacts/item/impl/FlagSpearItem.java',
      'main/java/com/uniye/mysticartifacts/event/FlagSpearEvents.java',
      'main/java/com/uniye/mysticartifacts/network/FlagSpearTrailPacket.java',
      'main/java/com/uniye/mysticartifacts/client/flag/FlagSpearClientState.java',
      'main/java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderer.java',
      'main/java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderTypes.java',
      'main/resources/assets/mysticartifacts/models/item/flag_spear.json'
  )
  foreach ($relativePath in $requiredFiles) {
      if (-not (Test-Path (Join-Path $projectRoot $relativePath))) {
          throw "Missing flag spear file: $relativePath"
      }
  }
  ```

  同时检查 `ModItems.FLAG_SPEAR`、创造模式物品栏、`AttackEntityEvent`、`inflate(5.0D)`、`AFTER_ENTITIES`、`TRAIL_COUNT = 5`、红色颜色常量、网络注册和中英文语言键。

- [ ] **Step 2: Write the failing profile test**

  使用项目已有的无框架 `main` 测试风格，先约定生产类 API：

  ```java
  public static void main(String[] args) {
      requireEquals(5, FlagSpearTrailProfile.TRAIL_COUNT);
      requireEquals(24, FlagSpearTrailProfile.SAMPLES_PER_TRAIL);
      requireClose(5.0D, FlagSpearTrailProfile.radiusAt(0), 1.0E-6D);
      requireClose(5.0D, FlagSpearTrailProfile.radiusAt(23), 1.0E-6D);
      requireClose(0.0D, FlagSpearTrailProfile.phaseFor(0), 1.0E-6D);
      requireClose(Math.PI * 8.0D / 5.0D, FlagSpearTrailProfile.phaseFor(4), 1.0E-6D);
  }
  ```

  新增 PowerShell 包装脚本 `src/test/flag-spear-profile-test.ps1`，按项目现有纯 Java 测试模式编译 profile 和测试类：

  ```powershell
  $outputDir = Join-Path $projectRoot 'build/flag-spear-profile-test'
  & 'C:/Program Files/Java/jdk-17/bin/javac.exe' -encoding UTF-8 -d $outputDir $source $test
  & 'C:/Program Files/Java/jdk-17/bin/java.exe' -cp $outputDir com.uniye.mysticartifacts.flag.FlagSpearTrailProfileTest
  ```

- [ ] **Step 3: Run tests to verify RED**

  运行 PowerShell 契约测试和 profile 测试。预期失败原因是旗枪生产文件和轨迹 profile 尚未创建，而不是测试脚本语法错误。

- [ ] **Step 4: Commit tests only**

  ```powershell
  git add -- src/test/flag-spear-contract-test.ps1 src/test/flag-spear-profile-test.ps1 src/test/java/com/uniye/mysticartifacts/flag/FlagSpearTrailProfileTest.java
  git commit -m "test: define flag spear contracts"
  ```

### Task 2: 注册旗枪及基础资源

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/item/impl/FlagSpearItem.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/init/ModItems.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/init/ModCreativeModTabs.java`
- Create: `src/main/resources/assets/mysticartifacts/models/item/flag_spear.json`
- Modify: `src/main/resources/assets/mysticartifacts/lang/zh_cn.json`
- Modify: `src/main/resources/assets/mysticartifacts/lang/en_us.json`
- Test: `src/test/flag-spear-contract-test.ps1`

**Interfaces:**
- Consumes: `Tiers.DIAMOND`、现有 `ModItems`/创造模式物品栏注册模式。
- Produces: `ModItems.FLAG_SPEAR`，类型为 `RegistryObject<Item>`，以及可加载的旗枪模型和语言键。

- [ ] **Step 1: Add the smallest item registration**

  在 `ModItems` 使用现有注册方式添加：

  ```java
  public static final RegistryObject<Item> FLAG_SPEAR = ITEMS.register("flag_spear",
          () -> new FlagSpearItem(Tiers.DIAMOND, 3, -2.8F, new Item.Properties().stacksTo(1)));
  ```

  `FlagSpearItem` 初始只继承 `SwordItem` 并提供对应构造函数，不复制 `SpearItem` 的蓄力逻辑。

- [ ] **Step 2: Add creative tab, model, and translations**

  创造模式物品栏加入 `ModItems.FLAG_SPEAR.get()`。模型复用现有长矛贴图：

  ```json
  {
    "parent": "minecraft:item/handheld",
    "textures": { "layer0": "mysticartifacts:item/spear" }
  }
  ```

  添加 `item.mysticartifacts.flag_spear` 的中文名“旗枪”和英文名“Flag Spear”。

- [ ] **Step 3: Run resource contract and compile**

  ```powershell
  powershell -ExecutionPolicy Bypass -File .\src\test\flag-spear-contract-test.ps1
  .\gradlew.bat compileJava --no-daemon --offline --console=plain --max-workers=1
  ```

  预期当前契约中与行为、网络和渲染相关的断言仍失败，但注册、资源和编译错误应被及时暴露。

- [ ] **Step 4: Commit registration**

  ```powershell
  git add -- src/main/java/com/uniye/mysticartifacts/item/impl/FlagSpearItem.java src/main/java/com/uniye/mysticartifacts/init/ModItems.java src/main/java/com/uniye/mysticartifacts/init/ModCreativeModTabs.java src/main/resources/assets/mysticartifacts/models/item/flag_spear.json src/main/resources/assets/mysticartifacts/lang/zh_cn.json src/main/resources/assets/mysticartifacts/lang/en_us.json
  git commit -m "feat: register flag spear"
  ```

### Task 3: 实现服务端范围伤害与网络包

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/event/FlagSpearEvents.java`
- Create: `src/main/java/com/uniye/mysticartifacts/network/FlagSpearTrailPacket.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/network/NetworkHandler.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java`
- Test: `src/test/flag-spear-contract-test.ps1`

**Interfaces:**
- Consumes: `ModItems.FLAG_SPEAR`、`AttackEntityEvent`、`ServerLevel`、现有 `ClientPacketHandler` 模式。
- Produces: `FlagSpearTrailPacket.send(ServerLevel, Vec3, float)`，以及仅服务端执行的半径 5 范围伤害。

- [ ] **Step 1: Extend the contract for server behavior and packet shape**

  契约必须确认：

  ```text
  event.getEntity() instanceof ServerPlayer
  event.getTarget() instanceof LivingEntity
  player.getMainHandItem().is(ModItems.FLAG_SPEAR.get())
  player.getBoundingBox().inflate(5.0D)
  candidate != primaryTarget
  player.level().damageSources().playerAttack(player)
  FlagSpearTrailPacket.send(...)
  PacketDistributor.NEAR
  NetworkDirection.PLAY_TO_CLIENT
  ```

  包解码限制单个消息为有限坐标/朝向值，拒绝非有限数值；本包不接受客户端发送方向。

- [ ] **Step 2: Run the contract to confirm RED**

  ```powershell
  powershell -ExecutionPolicy Bypass -File .\src\test\flag-spear-contract-test.ps1
  ```

  预期失败在服务端事件或网络文件的缺失/缺少断言处。

- [ ] **Step 3: Implement server-only damage**

  `FlagSpearEvents` 订阅 Forge `AttackEntityEvent`，按设计执行过滤、AABB 查询和平方距离检查。范围目标不包括攻击主目标；对每个可伤害的 `LivingEntity` 使用 `playerAttack(player)`，伤害数值取玩家当前 `Attributes.ATTACK_DAMAGE`，并补充 `EnchantmentHelper.getDamageBonus`。不取消原事件，不在客户端运行。

- [ ] **Step 4: Implement the S→C packet**

  `FlagSpearTrailPacket` 写入 `double x/y/z` 与 `float yaw`，在 `handle` 中通过 `enqueueWork` 和 `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` 调用客户端处理器；`send` 使用中心点所在维度的 `PacketDistributor.NEAR`，半径 96 格。注册为当前 `NetworkHandler` 的最后一个 PLAY_TO_CLIENT 消息。

- [ ] **Step 5: Run contract and compile**

  ```powershell
  powershell -ExecutionPolicy Bypass -File .\src\test\flag-spear-contract-test.ps1
  .\gradlew.bat compileJava --no-daemon --offline --console=plain --max-workers=1
  ```

- [ ] **Step 6: Commit server behavior**

  ```powershell
  git add -- src/main/java/com/uniye/mysticartifacts/event/FlagSpearEvents.java src/main/java/com/uniye/mysticartifacts/network/FlagSpearTrailPacket.java src/main/java/com/uniye/mysticartifacts/network/NetworkHandler.java src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java src/test/flag-spear-contract-test.ps1
  git commit -m "feat: add flag spear area attack"
  ```

### Task 4: 实现客户端轨迹 profile、状态和发光渲染

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/client/flag/FlagSpearTrailProfile.java`
- Create: `src/main/java/com/uniye/mysticartifacts/client/flag/FlagSpearClientState.java`
- Create: `src/main/java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderTypes.java`
- Create: `src/main/java/com/uniye/mysticartifacts/client/flag/FlagSpearRenderer.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java`
- Test: `src/test/java/com/uniye/mysticartifacts/flag/FlagSpearTrailProfileTest.java`
- Test: `src/test/flag-spear-profile-test.ps1`
- Test: `src/test/flag-spear-contract-test.ps1`

**Interfaces:**
- Consumes: `FlagSpearTrailPacket` 的中心坐标/朝向，`RenderLevelStageEvent.AFTER_ENTITIES`。
- Produces: `FlagSpearTrailProfile.TRAIL_COUNT = 5`、`SAMPLES_PER_TRAIL = 24`、`radiusAt(int)`、`phaseFor(int)`；以及可清理、限长、10 tick 过期的客户端状态。

- [ ] **Step 1: Implement the profile and run the Java test**

  profile 固定五条轨迹和 24 个采样点，`radiusAt` 返回 5.0，`phaseFor(index)` 返回 `2π * index / 5`，并提供波形偏移的确定性函数供渲染器使用。先确保 Task 1 的 Java 测试由生产 profile 变为 GREEN。

- [ ] **Step 2: Implement bounded client state**

  状态事件保存 `Vec3 center`、`float yaw`、`long tick`，生命周期 10 tick，最大缓存 32；客户端 tick 清理过期数据和无世界状态。`accept` 丢弃非有限坐标/朝向，`snapshot` 返回副本，防止渲染阶段修改缓存。

- [ ] **Step 3: Implement client-safe packet routing**

  在 `ClientPacketHandler` 增加 `handleFlagSpearTrail(Vec3 center, float yaw)`，调用 `FlagSpearClientState.accept`。公共包类仍只通过 `DistExecutor` 间接引用客户端处理器。

- [ ] **Step 4: Implement the emissive render type**

  `FlagSpearRenderTypes.luminous()` 使用 `DefaultVertexFormat.POSITION_COLOR`，关闭纹理和光照，启用透明混合，关闭背面剔除，不写深度，并使用始终通过的深度测试；渲染输出固定为红色。

- [ ] **Step 5: Implement five ribbon trails**

  `FlagSpearRenderer` 监听 `AFTER_ENTITIES`，相机平移后为每个状态事件绘制五条圆周轨迹。每条轨迹按 profile 的 24 个采样点生成相邻四边形，使用相位、垂直偏移、宽度和正弦波形变化；先绘制宽的低 alpha 外层，再绘制窄的高 alpha 内层。轨迹中心使用 packet 中心，水平朝向用于旋转整个图案，年龄用于透明度衰减。

- [ ] **Step 6: Run all contract/profile checks and compile**

  ```powershell
  powershell -ExecutionPolicy Bypass -File .\src\test\flag-spear-contract-test.ps1
  .\gradlew.bat compileJava --no-daemon --offline --console=plain --max-workers=1
  powershell -ExecutionPolicy Bypass -File .\src\test\flag-spear-profile-test.ps1
  ```

- [ ] **Step 7: Commit client rendering**

  ```powershell
  git add -- src/main/java/com/uniye/mysticartifacts/client/flag src/main/java/com/uniye/mysticartifacts/client/network/ClientPacketHandler.java src/test/java/com/uniye/mysticartifacts/flag/FlagSpearTrailProfileTest.java src/test/flag-spear-contract-test.ps1
  git commit -m "feat: render flag spear trails"
  ```

### Task 5: 最终验证与工作区审计

**Files:**
- Test: `src/test/flag-spear-contract-test.ps1`
- Inspect: all files changed by this feature only

**Interfaces:**
- Consumes: Tasks 1–4 的注册、服务端、网络和客户端渲染实现。
- Produces: 通过的契约/编译证据，以及不包含无关改动的最终变更清单。

- [ ] **Step 1: Run the focused contract test**

  ```powershell
  powershell -ExecutionPolicy Bypass -File .\src\test\flag-spear-contract-test.ps1
  ```

- [ ] **Step 2: Run the profile test and compile**

  ```powershell
  .\gradlew.bat compileJava --no-daemon --offline --console=plain --max-workers=1
  powershell -ExecutionPolicy Bypass -File .\src\test\flag-spear-profile-test.ps1
  ```

- [ ] **Step 3: Run diff checks**

  ```powershell
  git diff --check HEAD~4..HEAD
  git status --short
  ```

  确认旗枪提交只包含计划中的文件；工作区原有的其他修改保持未改写。

- [ ] **Step 4: Perform optional client verification**

  若本地运行环境允许，启动客户端进入世界，用旗枪攻击实体，检查第三人称和第一人称下的 5 条红色轨迹、半径范围、实体遮挡和重复伤害；否则明确保留“静态/编译已验证，游戏内观感未验证”的结论。

- [ ] **Step 5: Final commit/checkpoint**

  若前述检查全部通过，保留前面四个小提交并记录最终验证结果；不提交与旗枪无关的既有工作区文件。
