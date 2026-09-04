# 武士刀开鞘与能量机制 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不添加 SlashBlade 依赖的前提下，为 MysticArtifacts 武士刀实现未开鞘/开鞘状态、能量循环、冲刺、独立刀光与自有贴图。

**Architecture:** 用物品栈 NBT 保存能量与开鞘截止刻；服务端在物品方法、格挡事件和网络包中验证全部战斗条件。新增两个 MysticArtifacts 刀光实体分别处理直线/扇形攻击和环形三脉冲攻击，客户端渲染器只绘制 MysticArtifacts 自有纹理。

**Tech Stack:** Minecraft 1.20.1、Forge 47.4.6、Java 17、现有 Forge SimpleChannel、EntityType、原版半透明实体渲染；不新增 Gradle 依赖。

**Spec:** `docs/superpowers/specs/2026-09-04-muramasa-sheath-energy-design.md`

## Global Constraints

- 目标版本固定为 Minecraft 1.20.1 / Forge 47.4.6。
- 不添加 SlashBlade_Resharped 或其他第三方依赖。
- 不修改 `E:/Server_mod/其他项目/SlashBlade_Resharped`。
- 不覆盖现有与武士刀无关的未提交改动。
- 所有技能伤害只能在逻辑服务器执行，并使用触发玩家作为伤害来源。
- 默认未开鞘贴图使用 MysticArtifacts 自有带鞘资源，开鞘贴图使用 MysticArtifacts 当前刀身资源。

## 文件结构

- `src/main/java/com/uniye/mysticartifacts/item/impl/KatanaState.java`：能量、截止刻和状态 NBT 的唯一读写入口。
- `src/main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java`：武士刀输入分流、普通攻击击退、冲刺和物品状态更新。
- `src/main/java/com/uniye/mysticartifacts/event/KatanaEvents.java`：格挡回能量与开鞘左键服务端拦截。
- `src/main/java/com/uniye/mysticartifacts/entity/KatanaSlashEntity.java`：直线/扇形刀光生命周期、命中集合和服务端伤害。
- `src/main/java/com/uniye/mysticartifacts/entity/KatanaCircleSlashEntity.java`：环形刀光三次脉冲和结束时关闭状态。
- `src/main/java/com/uniye/mysticartifacts/network/KatanaSwingPacket.java`：客户端空挥请求及服务端重新验证。
- `src/main/java/com/uniye/mysticartifacts/client/render/KatanaSlashRenderer.java`：仅客户端的刀光/环形刀光渲染。
- `src/main/java/com/uniye/mysticartifacts/init/ModEntities.java`、`NetworkHandler.java`、`MysticArtifacts.java`：实体、网络、客户端渲染注册。
- `src/main/resources/assets/mysticartifacts/models/item/katana*.json`：带鞘/开鞘模型切换。
- `src/main/resources/assets/mysticartifacts/textures/item/katana_sheathed.png`：16×16 带鞘物品贴图。
- `src/main/resources/assets/mysticartifacts/textures/entity/katana_slash.png`、`katana_circle_slash.png`：MysticArtifacts 刀光贴图。

### Task 1: 建立可测试的状态与能量核心

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/item/impl/KatanaState.java`
- Create: `src/test/java/com/uniye/mysticartifacts/item/impl/KatanaStateTest.java`

**Interfaces:**
- Produces `KatanaState.MAX_ENERGY`, `clampEnergy(int)`, `canDash(int, boolean)`, `consumeEnergy(int, int)`, `getEnergy(ItemStack)`, `setEnergy(ItemStack, int)`, `addEnergy(ItemStack, int)`, `isOpen(ItemStack, Level)`, `open(ItemStack, Level, int)`, `close(ItemStack)`, `canDash(ItemStack, Level)` and `consumeDash(ItemStack)`。

- [ ] **Step 1: Write the failing test**

```java
public final class KatanaStateTest {
    public static void main(String[] args) {
        require(KatanaState.clampEnergy(-4) == 0, "negative energy");
        require(KatanaState.clampEnergy(101) == 100, "energy cap");
        require(KatanaState.canDash(100, false), "full energy can dash");
        require(!KatanaState.canDash(99, false), "partial energy cannot dash");
        require(!KatanaState.canDash(100, true), "open katana cannot dash");
        require(KatanaState.consumeEnergy(100, 100) == 0, "dash consumes energy");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat compileTestJava`
Expected: FAIL because `KatanaState` and its primitive helper methods do not exist.

- [ ] **Step 3: Write the minimal implementation**

Implement the primitive helpers with `MAX_ENERGY = 100`, `DASH_COST = 100`, `HIT_ENERGY = 1`, `BLOCK_ENERGY = 10`, and make all ItemStack NBT access use the keys `KatanaEnergy`, `KatanaOpenUntil`, and `KatanaCircleAttackUntil`. The ItemStack wrappers delegate to these primitive helpers so the NBT layer and numeric rules remain separately testable.

- [ ] **Step 4: Run the test to verify it passes**

Run: `.\gradlew.bat compileJava compileTestJava`; then run `java -ea -cp "build/classes/java/main;build/classes/java/test" com.uniye.mysticartifacts.item.impl.KatanaStateTest`.
Expected: compilation succeeds and the process exits with code 0.

- [ ] **Step 5: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/item/impl/KatanaState.java src/test/java/com/uniye/mysticartifacts/item/impl/KatanaStateTest.java
git commit -m "feat: add katana state helpers"
```

### Task 2: 改造武士刀输入与普通战斗

**Files:**
- Modify: `src/main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/event/KatanaEvents.java`

**Interfaces:**
- Consumes `KatanaState` from Task 1 and existing `KatanaBlockEvent`。
- Produces `MuramasaItem.handleDash(Level, Player, InteractionHand)`、`MuramasaItem.triggerOpenSlash(Player)` and the energy/state behavior used by entities and packets。

- [ ] **Step 1: Write the failing contract checks**

Add a PowerShell contract test at `src/test/katana_contract_test.ps1` that asserts `MuramasaItem.java` no longer contains `applyDirectHealthCost`, contains `KatanaState.addEnergy`, and that `KatanaEvents.java` contains a block-energy increment of 10.

- [ ] **Step 2: Run the contract to verify it fails**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`.
Expected: FAIL against the current health-cost implementation.

- [ ] **Step 3: Implement the item state machine**

Keep the existing `startUsingItem` guard path for non-sneaking right-click while closed. Replace the sneaking right-click branch with `KatanaState.canDash`, `KatanaState.consumeDash`, the existing horizontal push, the existing step-height modifier, current configured dash multiplier, and `KatanaSlashEntity.createDash(...)`. Set the open deadline to `level.getGameTime() + 200` only after the dash is accepted. When open, route ordinary right-click to `KatanaCircleSlashEntity.create(...)` and set the circle deadline used by its third pulse. Remove both direct health-cost constants and `applyDirectHealthCost`.

- [ ] **Step 4: Implement closed-state melee behavior**

In `hurtEnemy`, call `super.hurtEnemy`, apply a strong horizontal knockback away from the player with vertical preservation, and add 1 energy only when the target remains a valid successful hit and the item is closed. Do not add energy when `KatanaState.isOpen` is true.

- [ ] **Step 5: Add closed-state expiration handling**

In `inventoryTick`, clear `KatanaOpenUntil` and `KatanaCircleAttackUntil` when their deadlines have passed, clamp energy, and retain the existing step-height cleanup without retaining `IaidoTicks` behavior.

- [ ] **Step 6: Add guard energy recovery**

In both projectile and living-attack successful block paths, call `KatanaState.addEnergy(stack, 10)` on the server before posting `KatanaBlockEvent`. Keep the existing perfect-block reflection, sound, cancellation, and durability behavior.

- [ ] **Step 7: Run the contract and compile**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`; then `.\gradlew.bat compileJava`.
Expected: contract PASS and Java compilation succeeds.

- [ ] **Step 8: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java src/main/java/com/uniye/mysticartifacts/event/KatanaEvents.java src/test/katana_contract_test.ps1
git commit -m "feat: add katana energy and sheath state"
```

### Task 3: 注册独立刀光实体并实现服务端伤害

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/entity/KatanaSlashEntity.java`
- Create: `src/main/java/com/uniye/mysticartifacts/entity/KatanaCircleSlashEntity.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/init/ModEntities.java`

**Interfaces:**
- `KatanaSlashEntity.createDash(ServerLevel, Player, ItemStack, Vec3, double)`。
- `KatanaSlashEntity.createOpenSlash(ServerLevel, Player, ItemStack)`。
- `KatanaCircleSlashEntity.create(ServerLevel, Player, ItemStack)`。
- Both entities use owner `Player`, sync only visual parameters needed by the renderer, and never load SlashBlade classes or resources.

- [ ] **Step 1: Write the failing registration/resource checks**

Extend `src/test/katana_contract_test.ps1` to require both entity class files, `KATANA_SLASH` and `KATANA_CIRCLE_SLASH` registrations, and `playerAttack` in the two entity classes.

- [ ] **Step 2: Run the checks to verify they fail**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`.
Expected: FAIL because the new entities and registrations do not exist.

- [ ] **Step 3: Implement `KatanaSlashEntity`**

Use a server-owned `Entity` with synced style, roll, base size, and lifetime. For dash style, position it along the dash vector and perform the accepted dash hitbox once. For open-slash style, use a short 10-tick lifetime, scan a forward AABB/arc on the server, and maintain a `Set<UUID>` so one target is hit once. Calculate damage as player attack damage ×1.0 for open slash and the existing configured ×10 dash multiplier. Call `target.hurt(owner.damageSources().playerAttack(owner), damage)` and apply enchantment bonus from the held MysticArtifacts item.

- [ ] **Step 4: Implement `KatanaCircleSlashEntity`**

Use a 9-tick lifetime with pulses at ticks 0, 3, and 6. At each pulse scan the owner-centered AABB expanded by 4 blocks, use a fresh per-pulse UUID set, and deal player attack damage ×0.75. Exclude the owner and dead/spectator entities. On tick 6, clear the matching katana stack's circle/open deadlines after applying damage.

- [ ] **Step 5: Register both EntityTypes**

Add `KATANA_SLASH` and `KATANA_CIRCLE_SLASH` to `ModEntities` with `MobCategory.MISC`, size `4.0F, 4.0F`, tracking range 64, and update interval 1; keep registration in the existing DeferredRegister.

- [ ] **Step 6: Run checks and compile**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`; then `.\gradlew.bat compileJava`.
Expected: contract PASS and Java compilation succeeds.

- [ ] **Step 7: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/entity/KatanaSlashEntity.java src/main/java/com/uniye/mysticartifacts/entity/KatanaCircleSlashEntity.java src/main/java/com/uniye/mysticartifacts/init/ModEntities.java src/test/katana_contract_test.ps1
git commit -m "feat: add server-side katana slash entities"
```

### Task 4: 接入开鞘左键网络请求与服务端验证

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/network/KatanaSwingPacket.java`
- Create: `src/main/java/com/uniye/mysticartifacts/client/event/KatanaClientHandler.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/network/NetworkHandler.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/event/KatanaEvents.java`

**Interfaces:**
- `KatanaSwingPacket.encode/decode/handle` with no client-provided damage or position.
- Server handler calls `MuramasaItem.triggerUnsheathedAttack` only after checking sender, main-hand item, open deadline, alive state, and item cooldown。

- [ ] **Step 1: Write the failing network contract**

Extend the PowerShell test to require `KatanaSwingPacket`, its registration in `NetworkHandler`, `enqueueWork`, and a server-side `getSender` check.

- [ ] **Step 2: Run the contract to verify it fails**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`.
Expected: FAIL because the packet does not exist.

- [ ] **Step 3: Implement the packet and registration**

Create a zero-payload packet. In `handle`, call `context.enqueueWork`, get the `ServerPlayer`, verify the main-hand `MuramasaItem` is open and not cooling down, then create an open slash and add a short cooldown. Register it after the existing message IDs without renumbering earlier messages.

- [ ] **Step 4: Send the request from the client**

Register a client-only handler for `PlayerInteractEvent.LeftClickEmpty` and the open-state entity attack path. Send `KatanaSwingPacket` only when the local player holds an open katana; never calculate damage on the client.

- [ ] **Step 5: Prevent double vanilla attacks**

In `KatanaEvents`, cancel `AttackEntityEvent` for an open katana and call the same `MuramasaItem.triggerOpenSlash(Player)` helper directly; leave closed-state ordinary attacks unchanged. The client packet is used only for empty swings, so an entity target creates exactly one slash entity.

- [ ] **Step 6: Run checks and compile**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`; then `.\gradlew.bat compileJava`.
Expected: contract PASS and Java compilation succeeds.

- [ ] **Step 7: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/network/KatanaSwingPacket.java src/main/java/com/uniye/mysticartifacts/client/event/KatanaClientHandler.java src/main/java/com/uniye/mysticartifacts/network/NetworkHandler.java src/main/java/com/uniye/mysticartifacts/event/KatanaEvents.java src/test/katana_contract_test.ps1
git commit -m "feat: sync katana unsheathed swings"
```

### Task 5: 实现 MysticArtifacts 自有客户端刀光渲染

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/client/render/KatanaSlashRenderer.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java`

**Interfaces:**
- Renderer resolves only `mysticartifacts:textures/entity/katana_slash.png` and `mysticartifacts:textures/entity/katana_circle_slash.png`。
- `MysticArtifacts.ClientModEvents.onClientSetup` registers both entity renderers and never references SlashBlade classes。

- [ ] **Step 1: Write the failing renderer contract**

Extend the PowerShell test to require the renderer class, both MysticArtifacts texture paths, and both `EntityRenderers.register` calls.

- [ ] **Step 2: Run the contract to verify it fails**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`.
Expected: FAIL because the renderer and resources do not exist.

- [ ] **Step 3: Implement the renderer**

Use `RenderType.entityTranslucent` and a `VertexConsumer` to draw a camera-facing translucent quad for dash/open slash styles. Draw a horizontal/vertical ring of quads for the circle style, interpolate entity position with partial ticks, apply synced roll/scale, and restore the `PoseStack` after each draw. Keep this class in the client package so the dedicated server never loads client-only types.

- [ ] **Step 4: Register the renderers**

Add `EntityRenderers.register(ModEntities.KATANA_SLASH.get(), KatanaSlashRenderer::new)` and the matching circle registration inside the existing client setup event.

- [ ] **Step 5: Run checks and compile**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`; then `.\gradlew.bat compileJava`.
Expected: contract PASS and Java compilation succeeds.

- [ ] **Step 6: Commit**

```text
git add src/main/java/com/uniye/mysticartifacts/client/render/KatanaSlashRenderer.java src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java src/test/katana_contract_test.ps1
git commit -m "feat: render mystic katana slash effects"
```

### Task 6: 制作并接入物品与刀光资源

**Files:**
- Create: `src/main/resources/assets/mysticartifacts/textures/item/katana_sheathed.png`
- Create: `src/main/resources/assets/mysticartifacts/textures/entity/katana_slash.png`
- Create: `src/main/resources/assets/mysticartifacts/textures/entity/katana_circle_slash.png`
- Modify: `src/main/resources/assets/mysticartifacts/models/item/katana.json`
- Create: `src/main/resources/assets/mysticartifacts/models/item/katana_sheathed.json`, `katana_open.json`
- Modify: `src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java`

- [ ] **Step 1: Create the item texture from the visible current MysticArtifacts katana**

Use the built-in image generation edit flow with the current `katana.png` as the edit target. Preserve 16×16 pixel-art composition, palette, diagonal orientation, handle and blade identity; add only a matching scabbard over the blade for the closed-state icon; no text, watermark, or SlashBlade material. Copy the validated output to `katana_sheathed.png`.

- [ ] **Step 2: Create MysticArtifacts slash textures**

Generate two transparent pixel-art effect textures: a bright red/white diagonal sword arc for `katana_slash.png`, and a red/white circular energy arc for `katana_circle_slash.png`. Use no SlashBlade texture or identifier, then copy the validated outputs into the MysticArtifacts entity texture directory.

- [ ] **Step 3: Add model variants and item property**

Make `katana.json` use the base `katana_sheathed` model and select `katana_open` when `mysticartifacts:open` is 1. Register the `open` property as `MuramasaItem.isOpen(stack, level)` and keep all model layer textures under the `mysticartifacts` namespace.

- [ ] **Step 4: Run resource and contract checks**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`; then `.\gradlew.bat processResources`.
Expected: contract PASS, all three PNGs exist, and resource processing succeeds.

- [ ] **Step 5: Commit**

```text
git add src/main/resources/assets/mysticartifacts src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java src/test/katana_contract_test.ps1
git commit -m "feat: add katana sheath and slash assets"
```

### Task 7: 集成验证与回归检查

**Files:**
- Modify only files identified by failed verification, limited to the Task 1–6 scope.

- [ ] **Step 1: Run static contracts**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File src/test/katana_contract_test.ps1`.
Expected: PASS with no SlashBlade resource path in MysticArtifacts katana code or resources.

- [ ] **Step 2: Build the mod**

Run: `.\gradlew.bat build`.
Expected: `build/libs/mysticartifacts-*.jar` is produced without compilation or resource errors.

- [ ] **Step 3: Run the dedicated server launch check**

Run: `.\gradlew.bat runServer` with the existing run configuration and stop after startup reaches the server-ready log.
Expected: no `net.minecraft.client` class-loading error and no missing entity registration error.

- [ ] **Step 4: Run the client smoke check**

Run: `.\gradlew.bat runClient`, give a test world a katana, and verify: closed icon has scabbard; closed left hit applies strong knockback and +1; guard gives +10; at 100 energy sneak-right dashes and opens; open left renders a slash and damages in range; open right renders a ring and damages on three pulses; the third pulse closes immediately; no health is removed by guard or dash.

- [ ] **Step 5: Review final diff and status**

Run: `git diff HEAD~7..HEAD --stat` and `git status --short`.
Expected: only the new design/plan commits and scoped implementation files are changed; existing unrelated user modifications remain present and uncommitted.

- [ ] **Step 6: Commit verification fixes if needed**

```text
git add -- src/main/java/com/uniye/mysticartifacts/item/impl/KatanaState.java src/main/java/com/uniye/mysticartifacts/item/impl/MuramasaItem.java src/main/java/com/uniye/mysticartifacts/event/KatanaEvents.java src/main/java/com/uniye/mysticartifacts/entity/KatanaSlashEntity.java src/main/java/com/uniye/mysticartifacts/entity/KatanaCircleSlashEntity.java src/main/java/com/uniye/mysticartifacts/init/ModEntities.java src/main/java/com/uniye/mysticartifacts/network/KatanaSwingPacket.java src/main/java/com/uniye/mysticartifacts/network/NetworkHandler.java src/main/java/com/uniye/mysticartifacts/client/event/KatanaClientHandler.java src/main/java/com/uniye/mysticartifacts/client/render/KatanaSlashRenderer.java src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java src/main/resources/assets/mysticartifacts src/test/java/com/uniye/mysticartifacts/item/impl/KatanaStateTest.java src/test/katana_contract_test.ps1
git commit -m "test: verify katana sheath combat loop"
```
