# 旗枪 Blockbench Java 模型

模型由 Blockbench MCP 建模并使用 `modded_entity` Java Class 导出，目标为 Forge 1.20.1 / Mojang-Parchment 映射。保留原生 `EntityModel<T>`、`ModelPart`、`createBodyLayer()`、旋转子节点与 `renderToBuffer()`；仅补充包名、imports 和资源标识。

## 文件

- `src/main/java/com/uniye/mysticartifacts/client/model/FlagSpearModel.java`：可编译模型类。
- `src/main/resources/assets/mysticartifacts/textures/item/flag_spear_model.png`：64×64 单张材质图。
- `docs/flag_spear/FlagSpearModel.bbmodel`：可继续编辑的 Blockbench 源模型，内嵌材质。
- `docs/flag_spear/preview.png`：Blockbench 三维预览截图。
- `docs/flag_spear/item-display-template.json`：客户端接入时可参考的物品显示变换，当前不替换运行资源。

## 造型与纹理

34 个立方体；长枪杆、阶梯收尖枪刃、五段波折红旗、金色包边、菱形徽记和双流苏。旗面有厚度，可从两侧观看。

原 `mysticartifacts:item/spear` 是 16×16 透明物品图标，直接作为长杆和旗面 box UV 会出现透明缺口。专用图集复用了该图的棕色枪杆、青色枪尖像素色板，左下角保留原图像素，并增加红色织物和金色区；原 PNG 未修改。运行时只需要 `FlagSpearModel.TEXTURE`，无需其他材质或第三方库。

## 留给主代理的客户端接入

1. 在客户端 MOD 总线 `EntityRenderersEvent.RegisterLayerDefinitions` 中调用 `event.registerLayerDefinition(FlagSpearModel.LAYER_LOCATION, FlagSpearModel::createBodyLayer)`。
2. 客户端烘焙：`new FlagSpearModel<Entity>(Minecraft.getInstance().getEntityModels().bakeLayer(FlagSpearModel.LAYER_LOCATION))`。资源重载后重新获取烘焙模型，避免缓存旧 ModelPart。
3. 在 BEWLR 的 `renderByItem` 中调用 `model.renderToBuffer(...)`，材质用 `RenderType.entityCutoutNoCull(FlagSpearModel.TEXTURE)`，传入当前光照、覆盖值及 RGBA=1；如需附魔闪光，使用物品渲染器的 foil buffer。
4. 原生 Java 坐标 Y 向下，模型握持中心为原点（Blockbench 的 `[0,24,0]`）。渲染时先 `translate(0.5,0.5,0.5)`，再 `scale(-1,-1,1)`，再绘制；几何由 ModelPart 自动按 1/16 换算，无需额外除以 16。不要再次套用实体的 `translate(0,-1.5,0)`。
5. Java 类不包含物品栏/手持显示变换。显示模板采用 `builtin/entity` 和八种 `display` 预设，交给原版物品模型应用。BEWLR 不应再次应用同一套 display 变换。GUI 预设缩小并居中完整模型；手持预设以握柄为中心。
6. 由主代理接入 `IClientItemExtensions#getCustomRenderer`，并在准备就绪后替换运行时 `models/item/flag_spear.json`。本次未改 `FlagSpearItem`、客户端事件注册或该运行资源，当前游戏中仍使用旧物品外观。

接口依据：https://docs.minecraftforge.net/en/1.20.x/items/bewlr/

## 验证边界

已通过 `compileJava --no-daemon --offline --console=plain --max-workers=1`（BUILD SUCCESSFUL）。Blockbench 检查确认 34 个立方体 UV 均在 64×64 图集范围内；源工程含 1 张内嵌材质；显示模板含 8 个 display 场景。未修改旗枪行为、网络包、轨迹或物品类。

Blockbench 预览用于检查完整轮廓、旗面双面结构和材质。物品栏、第一/第三人称预设仍需在客户端渲染接入后进游戏校准；本交付不宣称游戏内显示已验证。
