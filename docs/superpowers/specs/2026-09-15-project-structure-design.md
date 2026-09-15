# MysticArtifacts 项目结构渐进整理设计

## 目标

在不改变现有玩法、注册名、资源路径和第三方依赖的前提下，降低 Forge 1.20.1 模组入口类的职责密度，明确通用初始化、配置、可选集成和客户端初始化的边界。

## 当前问题

- `MysticArtifacts.java` 同时负责 DeferredRegister 注册、网络初始化、配置注册、Quark 可选集成、服务器事件和客户端渲染注册。
- 客户端渲染器、`ItemProperties` 和 GUI overlay 直接位于主模组类的嵌套客户端订阅器中，主入口因此直接引用 `net.minecraft.client` 类型。
- 通用注册表位于 `init`，左轮的菜单和声音注册表位于 `revolver`，入口类需要了解两套注册位置。
- 配置注册逻辑、可选 Quark 集成逻辑与模组启动编排耦合在一起。
- 当前工作区包含未提交的左轮、武士刀和测试改动，结构整理不能覆盖、删除或重命名这些改动。

## 设计边界

### 保持不变

- Minecraft 版本：1.20.1。
- Forge 版本：47.4.6。
- Java 包根：`com.uniye.mysticartifacts`。
- 所有注册名、资源路径、网络包协议和现有配置键。
- 当前已有的 GeckoLib、Curios 和可选 KubeJS 依赖。
- 现有功能类的包路径，除非为编译所需进行最小导入调整。
- 工作区已有未提交文件及其内容。

### 本次允许调整

- 新增少量启动编排类。
- 将主模组类中的客户端初始化代码移到独立客户端事件类。
- 将配置注册和 Quark 检测提取为独立入口。
- 对 `RevolverRegistries.java` 做仅限导入、格式和可读性的整理。
- 新增结构契约检查脚本。

## 目标结构

```text
com.uniye.mysticartifacts
├── MysticArtifacts.java       # 仅负责启动编排
├── init
│   ├── ModRegistries.java     # 统一注册入口
│   ├── ModItems.java          # 保留现有物品注册字段
│   ├── ModEntities.java       # 保留现有实体注册字段
│   ├── ModSounds.java         # 保留现有声音注册字段
│   └── ModCreativeModTabs.java
├── config
│   └── ModConfigs.java        # 统一注册配置
├── compat
│   └── QuarkCompat.java       # Quark 可选集成入口
└── client
    └── ClientModEvents.java   # 客户端渲染、GUI 与模型属性注册
```

`revolver/RevolverRegistries.java` 继续保留在左轮功能包中，由 `ModRegistries` 统一调用。这样既不改变左轮功能的现有边界，也避免主入口直接维护多处注册细节。

## 运行流程

### 通用启动

`MysticArtifacts` 构造函数只执行以下步骤：

1. 获取 mod event bus。
2. 调用 `ModRegistries.register(modEventBus)`。
3. 调用 `NetworkHandler.register()`。
4. 调用 `ModConfigs.register()`。
5. 调用 `QuarkCompat.register()`。

不再保留空的 `commonSetup`、`addCreative` 和 `onServerStarting` 回调，也不再让主入口直接引用客户端渲染 API。

### 注册入口

`ModRegistries.register(IEventBus)` 顺序调用：

1. `ModItems.register(eventBus)`。
2. `ModEntities.register(eventBus)`。
3. `ModSounds.register(eventBus)`。
4. `ModCreativeModTabs.register(eventBus)`。
5. `RevolverRegistries.MENUS.register(eventBus)`。
6. `RevolverRegistries.SOUNDS.register(eventBus)`。

所有现有 `RegistryObject` 字段和注册 ID 保持原样。

### 配置入口

`ModConfigs.register()` 统一调用现有的 `ModLoadingContext.registerConfig`：

- `Config.SPEC`：COMMON。
- `RevolverConfig.SPEC`：SERVER，保留 `mysticartifacts-revolver-server.toml` 文件名。
- `TrailSightClientConfig.SPEC`：CLIENT，保留 `mysticartifacts-trail-sight-client.toml` 文件名。

配置项名称、默认值、范围和加载事件不变。仅调整 `Config.SPEC` 的访问级别以支持独立注册入口。

### 客户端入口

`ClientModEvents` 使用现有 `@Mod.EventBusSubscriber(modid = MysticArtifacts.MODID, bus = Bus.MOD, value = Dist.CLIENT)` 订阅方式，承接：

- 求生玉 GUI overlay 注册。
- 现有实体渲染器注册。
- Curios 渲染器注册。
- 现有四个物品模型属性注册。

这些调用的执行时机、注册对象和回调逻辑保持不变。已有其他 `client` 包事件订阅器不移动、不合并。

### 可选集成

`QuarkCompat.register()` 只检查 `ModList.get().isLoaded("quark")`，已加载时注册现有 `CodexAnvilHandler`，未加载时不触碰 Quark 相关逻辑。不会引入 Quark 编译依赖。

## 失败处理与兼容性

- 如果结构契约检查发现注册名、资源路径或包路径变化，停止实现并修正，不继续执行构建。
- 如果编译失败，先区分结构改动导致的错误和现有未提交改动导致的错误；不覆盖用户文件。
- 不引入新的第三方库，不修改 `build.gradle` 的依赖集合。
- 不进行全量包路径迁移，避免破坏现有 KubeJS、Mixin、测试和资源引用。

## 验证方案

1. 结构契约检查：确认主入口不含 `net.minecraft.client` 引用，Java 包路径不含 `example`，关键注册名仍存在。
2. `gradlew.bat compileJava`：设置项目本地 Gradle 用户目录，避免使用无写权限的系统缓存路径。
3. 运行现有 Java 测试和契约脚本。
4. `git diff --check`：检查空白和补丁格式问题。
5. 检查 `git status`：确认只新增/修改设计范围内的结构文件，现有未提交功能文件保持原状。

## 完成标准

- 主入口不再直接导入 `net.minecraft.client`。
- 所有现有注册表仍被注册一次且注册名不变。
- 配置文件名、配置键和默认值不变。
- Quark 未安装时模组仍能正常加载，不出现硬依赖。
- Java 包路径不出现 `example`。
- 项目能够通过 `compileJava`，并且现有测试没有因结构整理产生回归。
