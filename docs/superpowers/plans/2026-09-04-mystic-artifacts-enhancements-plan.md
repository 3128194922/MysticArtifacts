# MysticArtifacts Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不引入 ClangingHowl 编译依赖的前提下，完成长矛动作、剑群抛射与渲染、末影苦无生命周期、器灵喷火器软兼容等调整。

**Architecture:** 保留现有物品、实体和渲染器结构，只在对应类中替换行为。ClangingHowl 通过 ModList 和注册表 ID 做运行时软兼容；剑群由服务端统一修正运动，客户端仅负责缓存和渲染。`all_seeing_eye` 只保留优化方案，不修改其代码。

**Tech Stack:** Minecraft 1.20.1 Forge、Java、Forge Registry/ModList、现有 Gradle Wrapper、PowerShell 静态契约检查。

**Spec:** `docs/superpowers/specs/2026-09-04-mystic-artifacts-enhancements-design.md`

## Global Constraints

- 只修改 `E:/Server_mod/MysticArtifacts`，不修改 `E:/Server_mod/其他项目/ClangingHowl`。
- 不添加 ClangingHowl 编译依赖；只使用 `ModList.get().isLoaded("clanginghowl")` 和运行时物品 ID。
- 保留长矛模型，不引用原版三叉戟模型。
- 旧 `dungeonnowloading` 灼烧器兼容和 `mysticartifacts:nether_of_voice` 必须从源码、注册和资源中移除。
- 必须保留工作区已有的 `AirburstArrowEntity.java`、`ExplodingArrowEntity.java`、`ScatterArrowDirection.java` 和 `kubejs_contract_test.ps1` 改动。

---

### Task 1: 长矛动作与 Nether of Voice 清理

**Files:**
- Modify: `src/main/java/com/uniye/mysticartifacts/item/impl/SpearItem.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/item/impl/GrieferSpearItem.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/item/impl/SpearItem.java` 中的 `SpearClient` 内部类（与上方文件合并修改）
- Modify: `src/main/java/com/uniye/mysticartifacts/init/ModItems.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/init/ModEntities.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/Config.java`
- Modify: `src/main/resources/assets/mysticartifacts/models/item/spear.json`
- Modify: `src/main/resources/assets/mysticartifacts/models/item/spear_using.json`
- Delete: `src/main/java/com/uniye/mysticartifacts/item/impl/NetherOfVoiceItem.java`
- Delete: `src/main/java/com/uniye/mysticartifacts/entity/NetherOfVoiceEntity.java`
- Delete: `src/main/resources/assets/mysticartifacts/models/item/nether_of_voice.json`
- Delete: `src/main/resources/assets/mysticartifacts/textures/item/nether_of_voice.png`

**Interfaces:**
- Produces: `SpearItem#getUseAnimation(ItemStack)` 返回 `UseAnim.SPEAR`；`GrieferSpearItem` 继续继承该动作，并由注册参数保持攻击修正值为 2。
- Removes: `ModItems.NETHER_OF_VOICE`、`ModEntities.NETHER_OF_VOICE` 及其客户端 renderer/config 引用。

- [ ] **Step 1: 写入静态失败检查**

  在验证命令中加入以下契约：

  ```powershell
  $source = Get-ChildItem -Recurse -File src/main | Get-Content -Raw
  if ($source -match 'NetherOfVoice|nether_of_voice') { throw 'Nether of Voice residual reference found' }
  if ((Get-Content src/main/java/com/uniye/mysticartifacts/item/impl/SpearItem.java -Raw) -notmatch 'UseAnim\.SPEAR') { throw 'Spear must use spear animation' }
  ```

- [ ] **Step 2: 运行失败检查并确认当前失败点**

  Run: `powershell -NoProfile -Command "$source = Get-ChildItem -Recurse -File src/main | Get-Content -Raw; if ($source -match 'NetherOfVoice|nether_of_voice') { throw 'Nether of Voice residual reference found' }; if ((Get-Content src/main/java/com/uniye/mysticartifacts/item/impl/SpearItem.java -Raw) -notmatch 'UseAnim\.SPEAR') { throw 'Spear must use spear animation' }"`

  Expected: 现状因 `SpearItem` 仍返回 `UseAnim.BOW` 且 Nether of Voice 引用仍存在而失败。

- [ ] **Step 3: 实现动作与删除注册**

  将 `SpearItem#getUseAnimation` 改为：

  ```java
  @Override
  public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
      return UseAnim.SPEAR;
  }
  ```

  移除 `SpearItem.SpearClient` 对 `SpearItem` 的 `BOW_AND_ARROW` 覆盖，让 Forge/原版 `UseAnim.SPEAR` 处理第三人称动作；删除两个 Nether 类、注册、renderer、配置字段/赋值和资源。检查 `griefer_spear` 注册仍调用 `new GrieferSpearItem(Tiers.IRON, 2, ...)`，不要把 `griefer_spear` 模型改成三叉戟。同步从 `spear.json` 与 `spear_using.json` 移除此前加入的 `minecraft:item/trident` display 覆盖，保留长矛原有 parent/layer0 和显示参数。

- [ ] **Step 4: 运行静态检查并确认通过**

  Expected: Nether 相关源码引用为 0，`SpearItem` 使用 `UseAnim.SPEAR`，`griefer_spear` 注册参数包含攻击修正值 `2`，长矛模型 JSON 不包含 `minecraft:item/trident`。

- [ ] **Step 5: 保存本任务变更**

  只暂存本任务修改的文件，确认不包含其他工作区改动后提交：

  ```powershell
  git add -- src/main/java/com/uniye/mysticartifacts/item/impl/SpearItem.java src/main/java/com/uniye/mysticartifacts/item/impl/GrieferSpearItem.java src/main/java/com/uniye/mysticartifacts/init/ModItems.java src/main/java/com/uniye/mysticartifacts/init/ModEntities.java src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java src/main/java/com/uniye/mysticartifacts/Config.java src/main/resources/assets/mysticartifacts/models/item/spear.json src/main/resources/assets/mysticartifacts/models/item/spear_using.json src/main/java/com/uniye/mysticartifacts/item/impl/NetherOfVoiceItem.java src/main/java/com/uniye/mysticartifacts/entity/NetherOfVoiceEntity.java src/main/resources/assets/mysticartifacts/models/item/nether_of_voice.json src/main/resources/assets/mysticartifacts/textures/item/nether_of_voice.png
  git commit -m "feat: update spear action and remove nether voice"
  ```

### Task 2: Sword Swarm 视角追踪与渲染缓存

**Files:**
- Modify: `src/main/java/com/uniye/mysticartifacts/entity/SwordPhantomEntity.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/network/SwordSwarmAttackPacket.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/client/render/SwordSwarmCharmRenderer.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/client/tuning/SwordSwarmRenderParams.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/item/impl/SwordSwarmCharm.java`

**Interfaces:**
- Consumes: `SwordSwarmAttackPacket` 当前生成偏移、`SwordSwarmCharm` 的显示队列、`SwordPhantomEntity` 的拥有者和显示物品。
- Produces: `SwordPhantomEntity#tick()` 服务端视角追踪；renderer 的客户端物品缓存和单次动画参数计算。

- [ ] **Step 1: 写入行为契约**

  使用源码检查确认新增运动逻辑必须满足：服务端执行、owner 为 `Player`、实体未命中/未停止、方向使用 `player.getLookAngle()`，并保留生成偏移。

- [ ] **Step 2: 实现服务端平滑追踪**

  在 `SwordPhantomEntity#tick()` 调用 `super.tick()` 后，仅在服务器、未进入地面/未移除、未停止追踪、拥有 Player 且速度非零时执行：

  ```java
  Vec3 current = getDeltaMovement();
  Vec3 target = player.getLookAngle().normalize();
  Vec3 direction = current.normalize().lerp(target, 0.15D).normalize();
  setDeltaMovement(direction.scale(current.length()));
  hasImpulse = true;
  ```

  追踪速率使用现有 Nether 机制的平滑值；不要在客户端重复改速度。在 `onHitEntity` 伤害判定之前设置停止追踪状态，保留伤害被拒绝时的反弹逻辑，并将停止状态写入/读取实体 NBT。

- [ ] **Step 3: 修复 renderer 的每帧开销**

  在 `SwordSwarmCharmRenderer` 中增加仅客户端使用的 `Map<ResourceLocation, ItemStack>` 缓存：首次解析注册表物品时创建 count 为 1 的副本，后续复用；未注册物品返回 `ItemStack.EMPTY`。渲染前只调用一次只读的 `SwordSwarmCharm.getDisplayQueue`。在服务端装备 `curioTick` 中、满储存/恢复计时的提前返回之前调用幂等的 `seedQueue`，使首次攻击前也有显示队列；保留现有攻击初始化路径。

  每次 `render` 只计算一次：

  ```java
  float animationTime = ageInTicks; // Curios 已包含 partialTicks
  float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
  ```

  内层所有剑使用这两个值，不再重复叠加 partial tick；移除未使用的 renderer/import，并复用 `PoseStack` 作用域。

- [ ] **Step 4: 运行客户端静态检查**

  Expected: renderer 不在每个剑槽位调用 `ForgeRegistries.ITEMS.getValue`，不在 render 路径调用 `getOrCreateTag`，动画时间只出现一次统一计算；剑群实体的视角追踪只出现在服务端分支。

- [ ] **Step 5: 保存本任务变更**

  ```powershell
  git add -- src/main/java/com/uniye/mysticartifacts/entity/SwordPhantomEntity.java src/main/java/com/uniye/mysticartifacts/network/SwordSwarmAttackPacket.java src/main/java/com/uniye/mysticartifacts/client/render/SwordSwarmCharmRenderer.java src/main/java/com/uniye/mysticartifacts/client/tuning/SwordSwarmRenderParams.java src/main/java/com/uniye/mysticartifacts/item/impl/SwordSwarmCharm.java
  git commit -m "fix: smooth sword swarm tracking and rendering"
  ```

### Task 3: Ender Kunai 5 秒生命周期

**Files:**
- Modify: `src/main/java/com/uniye/mysticartifacts/entity/EnderKunaiEntity.java`

**Interfaces:**
- Consumes: 现有 `groundTimer`、`timedOut` 和 `remove()` 清理路径。
- Produces: 投出后固定 100 tick 的生命周期。

- [ ] **Step 1: 写入生命周期契约**

  契约要求 `MAX_LIFETIME_TICKS = 100`，计数在飞行和插地状态都递增，不能因离开插地状态重置。

- [ ] **Step 2: 增加独立生命周期计数**

  在实体中新增 `lifetimeTicks`，每个服务端 tick 递增；达到 100 时设置 `timedOut = true` 并 `discard()`。保留 `groundTimer` 的插地表现计时，不把它复用为总生命周期。

- [ ] **Step 3: 验证并保存**

  Expected: 源码包含 100 tick 上限；没有仅在 `inGround` 分支才销毁的逻辑；现有追踪器清理仍由 `remove()` 执行。

  ```powershell
  $src = Get-Content src/main/java/com/uniye/mysticartifacts/entity/EnderKunaiEntity.java -Raw
  if ($src -notmatch 'MAX_LIFETIME_TICKS\s*=\s*100') { throw 'Kunai lifetime must be 100 ticks' }
  ```

  ```powershell
  git add -- src/main/java/com/uniye/mysticartifacts/entity/EnderKunaiEntity.java
  git commit -m "fix: expire thrown ender kunai after five seconds"
  ```

### Task 4: Artifact Spirit 的 ClangingHowl 喷火器软兼容

**Files:**
- Modify: `src/main/java/com/uniye/mysticartifacts/item/impl/ArtifactSpiritItem.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/entity/ArtifactSpiritEntity.java`

**Interfaces:**
- Consumes: `ArtifactSpiritItem#isSupportedWeapon`、`isAmmoForWeapon`、`ArtifactSpiritEntity` 现有喷火器状态机和末影箱扫描。
- Produces: 运行时可选的 `clanginghowl:flamethrower` 支持；末影箱消耗 `clanginghowl:blaze_fuel_cylinder`，单个补充 1600 fuel，喷火期间每 20 tick 消耗 5 fuel。

- [ ] **Step 1: 写入缺失模组和燃料契约**

  静态契约明确禁止 `dungeonnowloading`、`SCORCHER_ID` 和 `SOUL_SCORCHER_ID`，并要求出现 `ModList.get().isLoaded("clanginghowl")`、两个 ClangingHowl ID、1600 fuel 容量、每次消耗 5 fuel 及 20 tick 扣料间隔。

- [ ] **Step 2: 替换 ArtifactSpiritItem 识别逻辑**

  使用资源 ID 常量和 ModList guard：器灵绑定物品只有在 ClangingHowl 已加载且 ID 为 `clanginghowl:flamethrower` 时才被视为喷火器；删除旧煤炭/木炭弹药分支。非 ClangingHowl 场景不加载其类，不直接 import ClangingHowl 类。

- [ ] **Step 3: 替换 ArtifactSpiritEntity 燃料状态**

  将旧 `scorcherFuelTicks` 的简化煤炭计时改为 fuel 数值，设置最大值 1600。沿用 ClangingHowl `onUseTick` 的每 20 tick 消耗 5 fuel 节奏：现有喷火倒计时为 120，在剩余 120/100/80/60/40/20 tick 时各扣 5；现有 `FlameProjectileEntity` 仍每 tick 生成。仅在扣料时检查并补充燃料，避免已支付的 20 tick 期间提前补充或停火；启动前的燃料检查保留。燃料不足时从拥有者末影箱逐槽查找 `clanginghowl:blaze_fuel_cylinder`，消耗一个并增加 1600，最多不超过 1600。未安装模组、实体无 Player owner、末影箱不可用或扣料时没有燃料则安全跳过/结束当前喷火阶段，不抛异常。

- [ ] **Step 4: 保持 NBT 存档兼容**

  读取旧 `Scorcher*` 字段时不再进入旧灵魂喷火分支；写入新燃料字段使用独立键，例如 `ClangingHowlFuel`，并保留普通器灵其他 NBT 字段。旧存档出现旧字段时按空燃料处理，避免继续识别已删除的旧物品。

- [ ] **Step 5: 验证两种运行环境**

  静态检查确认 MysticArtifacts 的源码没有 ClangingHowl import 或 Gradle 依赖。运行现有项目测试/构建时分别检查：缺少 ClangingHowl 的类加载路径不访问其注册表；安装时绑定 `clanginghowl:flamethrower` 能识别并消耗 `clanginghowl:blaze_fuel_cylinder`。

- [ ] **Step 6: 保存本任务变更**

  ```powershell
  git add -- src/main/java/com/uniye/mysticartifacts/item/impl/ArtifactSpiritItem.java src/main/java/com/uniye/mysticartifacts/entity/ArtifactSpiritEntity.java
  git commit -m "feat: add optional ClangingHowl fuel compatibility"
  ```

### Task 5: 全量静态验证、构建尝试与交付说明

**Files:**
- Modify: `docs/superpowers/specs/2026-09-04-mystic-artifacts-enhancements-design.md`（只在验证结果与实现有偏差时更新）
- Modify: `src/test/kubejs_contract_test.ps1`（仅在已有契约测试确实需要新增断言时，保留其现有改动）

**Interfaces:**
- Consumes: Tasks 1-4 的源码和资源。
- Produces: 可复核的静态检查结果、构建结果和未完成项说明。

- [ ] **Step 1: 检查工作区范围**

  ```powershell
  git status --short
  git diff --check
  ```

  确认没有修改 `E:/Server_mod/其他项目/ClangingHowl`，并确认用户已有的四项工作区改动仍在。

- [ ] **Step 2: 检查 JSON 与残留引用**

  用 PowerShell `ConvertFrom-Json` 逐个解析 `src/main/resources/assets/mysticartifacts/models/item/*.json`；用 `rg` 检查以下模式没有不应存在的结果：

  ```text
  NetherOfVoice|nether_of_voice|dungeonnowloading|minecraft:item/trident
  ```

- [ ] **Step 3: 运行项目验证任务**

  ```powershell
  $env:GRADLE_USER_HOME = 'E:/Server_mod/MysticArtifacts/.gradle'
  .\gradlew.bat build
  ```

  如果 wrapper 仍因无法下载 Gradle 8.8 而失败，记录完整失败原因；只报告静态检查通过，不报告构建通过。

- [ ] **Step 4: 复核差异**

  ```powershell
  git diff --stat
  git diff --check
  git status --short
  ```

  逐项核对：长矛模型未替换、剑群逻辑没有客户端运动竞争、苦无是 100 tick、ClangingHowl 是软兼容、all_seeing_eye 仅有方案未被改动。

- [ ] **Step 5: 交付中文结果**

  报告已修改文件、验证通过项、构建是否受环境阻塞，以及 `all_seeing_eye` 的四点优化建议；同时输出项目要求的固定短语。
