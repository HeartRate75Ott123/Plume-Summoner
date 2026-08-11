# Plume Summoner

一个 1.21.1 NeoForge 召唤模组：**击杀生物解锁其召唤权，按 G 键打开召唤菜单，点击即可在面前召唤真实生物**。

基于 [Remorphed](https://github.com/ToCraft/Remorphed) 项目学到的 GUI 设计（滑动列表、排版、实体模型渲染、名称读取）重新实现，作者：**Plume Jade**，协议：**MIT**。

## 功能

- 击杀任意生物（怪物/动物/Boss）后永久解锁它的召唤权，存档数据保存在玩家 NBT 中
- 按 `G` 键打开召唤菜单（可在设置中改键）
- 滑动列表展示全部生物，支持关键词搜索，未解锁的条目灰显锁定
- 条目内直接渲染实体 3D 模型（自动缩放适配格子），悬停显示提示
- 点击已解锁条目，在当前玩家面前 3 格处召唤真实生物（含 AI）
- 每名玩家同屏同时最多召唤 10 个生物（可在配置中调整）

## 玩法流程

1. 击杀一只僵尸 → 屏幕提示"解锁召唤：僵尸"
2. 按 `G` 打开菜单 → 搜索"zombie"或滚动找到僵尸（模型已解锁，状态为"已解锁"）
3. 点击条目 → 僵尸在你面前被召唤出来

## 开发环境

- Minecraft 1.21.1 · NeoForge 21.1.238 · Parchment 2024.11.17
- Java 21（工具链自动指定）· Gradle wrapper + ModDevGradle 2.0.142

## 常用命令

```bash
# 启动客户端（开发环境）
./gradlew.bat runClient

# 构建正式 jar（输出到 build/libs/）
./gradlew.bat build
```

## 配置

配置文件生成于 `config/plume_summoner-server.toml`：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `maxSummonsPerPlayer` | 10 | 每名玩家同时召唤的生物数量上限 |

## 目录结构

```
src/main/java/plume/summoner/
├── PlumeSummoner.java          # @Mod 主类
├── config/SummonerConfig.java  # ModConfigSpec 配置
├── data/                       # 玩家击杀解锁数据接口
├── mixin/PlayerEntityMixin.java # NBT 持久化（read/addAdditionalSaveData 注入）
├── network/                    # CustomPacketPayload：C2S 召唤请求 + S2C 解锁同步
├── handler/                    # 击杀解锁事件 / 服务端召唤逻辑
├── client/                     # 按键绑定 + 客户端解锁缓存
└── screen/                     # SummonMenuScreen + widget（滑动列表/条目/搜索框/实体渲染缓存）
```

## 学习要点（对照 Remorphed 实现）

- **滑动列表**：`ContainerObjectSelectionList`（1.21.1 平滑滚动列表），条目渲染与点击在 `Entry#render` / `Entry#mouseClicked`
- **排版**：Screen 手写 y 坐标布局（标题 / 搜索框 / 列表 / 底部按钮）
- **模型导入**：不导入模型文件，直接用 `EntityType#create(Level)` 创建客户端实体实例，通过 `EntityRenderDispatcher` 手动渲染进 GUI（`prepare` + `render`，关闭阴影，缩放对齐格子，翻转 Z 轴面向观察者）
- **名称读取**：`EntityType#getDescription()` 获取本地化名称，搜索框用 `contains`（大小写不敏感）过滤