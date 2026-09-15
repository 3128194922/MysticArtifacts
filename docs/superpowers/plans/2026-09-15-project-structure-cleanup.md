# MysticArtifacts Project Structure Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变玩法、注册名、资源路径和依赖集合的前提下，降低主模组入口类的职责密度，并隔离通用、配置、可选集成和客户端初始化。

**Architecture:** 保留现有功能包和注册类，新增 `ModRegistries`、`ModConfigs`、`QuarkCompat` 三个启动编排入口，并把主类中的客户端订阅器提取到 `client/ClientModEvents`。所有现有注册对象仍由原类持有，主类只负责按固定顺序调用入口。

**Tech Stack:** Minecraft 1.20.1, Forge 47.4.6, Java 17, Gradle, PowerShell contract checks, existing JUnit setup.

**Spec:** `docs/superpowers/specs/2026-09-15-project-structure-design.md`

## Global Constraints

- 仅修改 `E:\Server_mod\MysticArtifacts` 主项目。
- 不新增第三方依赖，不修改现有 GeckoLib、Curios、KubeJS 依赖声明。
- 保持 Minecraft 1.20.1、Forge 47.4.6、Java 17。
- 保持 `com.uniye.mysticartifacts` 包根，Java 包路径不得出现 `example`。
- 保持所有注册名、资源路径、网络协议、配置键、配置默认值和配置文件名。
- 不覆盖、删除、重命名当前工作区中未提交的左轮、武士刀、测试或文档文件。
- 不进行全量功能包迁移；现有功能类只在编译需要时调整导入。
- 每次提交只暂存本任务明确列出的文件，不能使用全量 `git add`。

---

### Task 1: Add a structure contract test

**Files:**
- Create: `src/test/project-structure-contract-test.ps1`
- Reference: `src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java`
- Reference: `src/main/java/com/uniye/mysticartifacts/init/ModItems.java`
- Reference: `src/main/java/com/uniye/mysticartifacts/init/ModEntities.java`
- Reference: `src/main/java/com/uniye/mysticartifacts/init/ModSounds.java`
- Reference: `src/main/java/com/uniye/mysticartifacts/revolver/RevolverRegistries.java`

**Interfaces:**
- Produces: a PowerShell executable contract check that exits non-zero when the main class still contains client-only imports, required registration IDs disappear, or Java packages contain `example`.
- Consumes: the current repository root resolved from `$PSScriptRoot`.

- [ ] **Step 1: Write the failing contract test**

Create `src/test/project-structure-contract-test.ps1` with this exact behavior:

```powershell
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$mainPath = Join-Path $projectRoot 'src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java'
$mainText = Get-Content -Raw $mainPath

if ($mainText -match 'net\.minecraft\.client') {
    throw 'Main mod entry still references client-only classes.'
}

$requiredSources = @(
    'src/main/java/com/uniye/mysticartifacts/init/ModItems.java',
    'src/main/java/com/uniye/mysticartifacts/init/ModEntities.java',
    'src/main/java/com/uniye/mysticartifacts/init/ModSounds.java',
    'src/main/java/com/uniye/mysticartifacts/revolver/RevolverRegistries.java'
)

foreach ($relativePath in $requiredSources) {
    $absolutePath = Join-Path $projectRoot $relativePath
    if (-not (Test-Path -LiteralPath $absolutePath)) {
        throw "Required registration source is missing: $relativePath"
    }
}

$javaFiles = Get-ChildItem (Join-Path $projectRoot 'src/main/java') -Recurse -Filter '*.java'
$exampleMatches = $javaFiles | Select-String -Pattern 'package\s+[^;]*example' -CaseSensitive
if ($exampleMatches) {
    throw 'Java package path contains forbidden example token.'
}

$registeredIds = @('jiba_revolver', 'revolver_bullet', 'revolver_cylinder', 'mysticartifacts')
foreach ($id in $registeredIds) {
    $matches = $javaFiles | Select-String -Pattern [regex]::Escape($id) -CaseSensitive
    if (-not $matches) {
        throw "Required registration/resource id is missing: $id"
    }
}

Write-Output 'Project structure contract passed.'
```

- [ ] **Step 2: Run the contract to verify it fails before the refactor**

Run from `E:\Server_mod\MysticArtifacts`:

```powershell
& .\src\test\project-structure-contract-test.ps1
```

Expected: FAIL with `Main mod entry still references client-only classes.` because the current `MysticArtifacts.java` imports `net.minecraft.client` types.

- [ ] **Step 3: Commit the failing contract**

```powershell
git add -- src/test/project-structure-contract-test.ps1
git commit -m "test: add project structure contract"
```

### Task 2: Centralize common registration, configuration, and compatibility setup

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/init/ModRegistries.java`
- Create: `src/main/java/com/uniye/mysticartifacts/config/ModConfigs.java`
- Create: `src/main/java/com/uniye/mysticartifacts/compat/QuarkCompat.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/Config.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java`
- Review-only: `src/main/java/com/uniye/mysticartifacts/revolver/RevolverRegistries.java`

**Interfaces:**
- `ModRegistries.register(IEventBus eventBus)` calls the existing item, entity, sound, creative-tab, revolver menu, and revolver sound registration methods exactly once.
- `ModConfigs.register()` registers the existing COMMON, SERVER, and CLIENT specs with their current file names.
- `QuarkCompat.register()` performs the current Quark presence check and registers `CodexAnvilHandler` only when Quark is loaded.

- [ ] **Step 1: Add the registration facade**

Create `ModRegistries` with a private constructor and this method body order:

```java
public static void register(IEventBus eventBus) {
    ModItems.register(eventBus);
    ModEntities.register(eventBus);
    ModSounds.register(eventBus);
    ModCreativeModTabs.register(eventBus);
    RevolverRegistries.MENUS.register(eventBus);
    RevolverRegistries.SOUNDS.register(eventBus);
}
```

Use the existing `MysticArtifacts.MODID` indirectly through the existing registry classes; do not recreate any `DeferredRegister`.

- [ ] **Step 2: Expose the existing common config spec without changing values**

Change only `Config.SPEC` from package-private to `public static final`. Do not rename the field or alter any builder value, key, default, range, or `@SubscribeEvent` method.

- [ ] **Step 3: Add the configuration facade**

Create `ModConfigs` with this registration contract:

```java
public static void register() {
    ModLoadingContext context = ModLoadingContext.get();
    context.registerConfig(ModConfig.Type.SERVER,
            RevolverConfig.SPEC, "mysticartifacts-revolver-server.toml");
    context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    context.registerConfig(ModConfig.Type.CLIENT,
            TrailSightClientConfig.SPEC,
            "mysticartifacts-trail-sight-client.toml");
}
```

Keep the existing config registration types and file names. The extracted class may import `TrailSightClientConfig`; it must not import `net.minecraft.client.*`.

- [ ] **Step 4: Add the Quark compatibility facade**

Create `QuarkCompat` with a private constructor and this behavior:

```java
public static void register() {
    if (ModList.get().isLoaded("quark")) {
        MinecraftForge.EVENT_BUS.register(new CodexAnvilHandler());
    }
}
```

Do not add a Quark dependency or reference Quark classes directly.

- [ ] **Step 5: Replace duplicated orchestration in the main class**

In `MysticArtifacts`:

1. Replace the five direct registry calls with `ModRegistries.register(modEventBus)`.
2. Replace the three direct config registrations with `ModConfigs.register()`.
3. Replace the inline Quark check with `QuarkCompat.register()`.
4. Keep `NetworkHandler.register()` exactly once.
5. Remove the empty `commonSetup`, `addCreative`, and `onServerStarting` methods and their listeners/unused imports.
6. Do not yet remove client registration until Task 3 is complete.

- [ ] **Step 6: Run the compile-oriented static checks**

Run:

```powershell
& .\src\test\project-structure-contract-test.ps1
```

Expected: FAIL only because Task 3 has not yet removed client references from `MysticArtifacts.java`; no registration IDs should be reported missing.

- [ ] **Step 7: Commit the common setup extraction**

```powershell
git add -- `
  src/main/java/com/uniye/mysticartifacts/init/ModRegistries.java `
  src/main/java/com/uniye/mysticartifacts/config/ModConfigs.java `
  src/main/java/com/uniye/mysticartifacts/compat/QuarkCompat.java `
  src/main/java/com/uniye/mysticartifacts/Config.java `
  src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java
git commit -m "refactor: centralize mod startup registration"
```

### Task 3: Extract client-only initialization from the main mod class

**Files:**
- Create: `src/main/java/com/uniye/mysticartifacts/client/ClientModEvents.java`
- Modify: `src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java`

**Interfaces:**
- `ClientModEvents` is an automatically registered static client subscriber on the mod event bus.
- It owns the existing `RegisterGuiOverlaysEvent` handler and `FMLClientSetupEvent` handler.
- It may reference `net.minecraft.client.*`, renderer classes, Curios client APIs, and client item property APIs because it is guarded by `Dist.CLIENT`.

- [ ] **Step 1: Move the existing client subscriber without changing callback bodies**

Create `ClientModEvents` with:

```java
@Mod.EventBusSubscriber(
        modid = MysticArtifacts.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientModEvents {
    private ClientModEvents() {
    }
}
```

Move the existing nested `onRegisterGuiOverlays` and `onClientSetup` methods into this class. Preserve:

- overlay id `survival_jade_phantom`;
- `VanillaGuiOverlay.FOOD_LEVEL.id()` anchor;
- every `EntityRenderers.register` call and renderer factory;
- the Curios renderer registration;
- all four `ItemProperties.register` calls and their predicates;
- `event.enqueueWork` boundaries.

- [ ] **Step 2: Remove the nested client class and client imports from the main class**

Delete the nested `MysticArtifacts.ClientModEvents` class and remove its now-unused client-only imports. `MysticArtifacts.java` must contain no `net.minecraft.client` token and no `Dist.CLIENT` annotation.

- [ ] **Step 3: Run the structure contract**

Run:

```powershell
& .\src\test\project-structure-contract-test.ps1
```

Expected: PASS with `Project structure contract passed.`

- [ ] **Step 4: Commit the client boundary extraction**

```powershell
git add -- `
  src/main/java/com/uniye/mysticartifacts/client/ClientModEvents.java `
  src/main/java/com/uniye/mysticartifacts/MysticArtifacts.java
git commit -m "refactor: isolate client mod initialization"
```

### Task 4: Compile and run the existing test suite

**Files:**
- No source changes expected.
- Review: `git status --short`

**Interfaces:**
- Consumes: Tasks 1–3 source changes.
- Produces: verified compile/test results and a clean diff check for the implementation commits.

- [ ] **Step 1: Configure the project-local Gradle cache**

Run in PowerShell from `E:\Server_mod\MysticArtifacts`:

```powershell
$env:GRADLE_USER_HOME = 'E:\Server_mod\MysticArtifacts\.gradle-user'
```

- [ ] **Step 2: Compile the mod**

```powershell
& .\gradlew.bat compileJava --no-daemon
```

Expected: exit code `0` and no Java compilation errors.

- [ ] **Step 3: Run Java tests**

```powershell
& .\gradlew.bat test --no-daemon
```

Expected: exit code `0`. Existing tests must remain untouched and continue to run under their current configuration.

- [ ] **Step 4: Run the structure and existing PowerShell contract tests**

```powershell
& .\src\test\project-structure-contract-test.ps1
Get-ChildItem .\src\test -File -Filter '*contract-test.ps1' |
    Where-Object { $_.Name -ne 'project-structure-contract-test.ps1' } |
    ForEach-Object { & $_.FullName }
```

Expected: all scripts exit successfully. If an existing contract fails because of a pre-existing uncommitted feature, report the exact script and error without modifying that feature.

- [ ] **Step 5: Check patch quality and scope**

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; the implementation commits contain only the planned structure files; unrelated pre-existing untracked files remain uncommitted.

- [ ] **Step 6: Record verification outcome**

Report the exact commands and exit status. If Gradle cannot access the local cache or needs a dependency download, stop and report the environment blocker instead of changing dependency declarations.

## Self-Review Checklist

- [ ] Every requirement in `docs/superpowers/specs/2026-09-15-project-structure-design.md` is covered by Tasks 1–4.
- [ ] No task changes a resource ID, configuration key, network protocol, or dependency declaration.
- [ ] The contract test fails before the client extraction and passes after it.
- [ ] The plan contains no unresolved placeholder marker or undefined implementation detail.
- [ ] Existing uncommitted work is preserved and never staged by a broad path.
