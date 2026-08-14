# AGENTS.md

本文件为在此仓库工作的 AI 代理（或协作者）提供上下文。请先完整阅读再动手。

## 项目是什么

**Creeper Healing** —— NeoForge 1.21.1 服务端模组：爆炸后自动恢复地形。
这是 [creeper-healing](https://github.com/ArkoSammy12/creeper-healing)（Fabric 版，作者 ArkoSammy12）的 **LGPL-2.1 移植版**，全部代码基于 NeoForge API 重写。

本地参考项目（工作区外，勿提交）：
- `D:/CODE/minecraft_mod/creeper-healing` —— fabric 原版源码（LGPL-2.1，**可以**作为功能基准参考）
- `D:/CODE/minecraft_mod/CreeperHeal` —— Lothrazar 系 NeoForge 移植（**自定义许可证，禁止照抄**，仅可参考其 NeoForge API 用法，如事件注册方式）

## 环境

| 项 | 值 |
|---|---|
| JDK | 21（本机：`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot`，不在 PATH，用全路径或设 `JAVA_HOME`） |
| Gradle | 9.2.1 wrapper（`gradlew.bat`） |
| 插件 | ModDevGradle 2.0.143（`net.neoforged.moddev`） |
| 目标 | Minecraft 1.21.1，NeoForge `neo_version=21.1.244`（编译用），运行时依赖 `neo_version_range=[21.1,)` |
| 配置缓存 | `org.gradle.configuration-cache=true` 已开启；改 `build.gradle`/模板后首次构建可能较慢 |

## 常用命令

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

.\.\gradlew.bat build                    # 完整构建（产物 build/libs/creeperhealing-<ver>.jar）
.\.\gradlew.bat compileJava              # 只编译，快速迭代
.\.\gradlew.bat runServer                # 启动开发服务器（run/ 目录，已配置 eula=true）
.\.\gradlew.bat build -Pmod_version=x    # 覆盖版本号（CI alpha 用）
```

端到端验证（服务器已开、`run/server.properties` 已开 RCON 25575/creepertest）：
1. `/creeper-healing heal_tnt_explosions true`
2. `/fill 0 64 0 2 66 2 minecraft:stone` + `/summon minecraft:tnt 1 65 1 {Fuse:1}`
3. 数秒后 `/execute if block 0 64 0 minecraft:stone run say HEALED_OK` → 日志应出现 HEALED_OK

## 源码结构（`src/main/java/com/huntersxy/creeperhealing/`）

| 包/类 | 职责 |
|---|---|
| `CreeperHealing.java` | `@Mod` 入口；注册 4 个事件处理器；`CreeperHealingConfig.init()` 必须最先调用 |
| `config/CreeperHealingConfig.java` | **自研** TOML 配置系统（`config/creeper-healing.toml`，格式与原版一致）。静态方法读写，`reload()`=默认值+文件覆盖，`save()` 重写全文件。命令修改自动落盘 |
| `handler/ExplosionEventHandler.java` | 爆炸捕获（Detonate）+ 掉落策略 + BlockDropsEvent 抑制 |
| `handler/WorldEventHandler.java` | 每维度 manager 生命周期（LevelEvent.Load/Unload）+ 每 tick 驱动（LevelTickEvent.Post） |
| `handler/PotionEventHandler.java` | 药水加速（ProjectileImpactEvent） |
| `explosions/` | `ExplosionEvent` 接口；`AbstractExplosionEvent`（逐块恢复主循环）；4 种模式；`ExplosionSourceType`（来源分类）；`ExplosionHealingMode`（枚举） |
| `blocks/` | `AffectedBlock` 接口 + `SingleAffectedBlock` / `DoubleAffectedBlock`（床/双植物整对恢复、双箱子联动） |
| `managers/` | `ExplosionManager`（**每 ServerLevel 一个**，含碰撞合并、药水加速入口、定时器刷新）；`ExplosionManagerRegistry` |
| `data/ExplosionHealingData.java` | SavedData 持久化（每维度 `data/creeperhealing_events.dat`），NBT 手写序列化 |
| `util/` | `ExplosionUtils`（排序/中心/粒子/音效）、`EmptyLevel`（全空气 LevelReader，用于间接位置探测）、`ExcludedBlocks`、`ExplosionContext`、`ExplosionDropController`、`FallingBlockSuppressor` |
| `mixin/` | `ExplosionDropsMixin`（@WrapMethod onExplosionHit）、`FallingBlockMixin`（@ModifyExpressionValue isFree） |
| `commands/CreeperHealingCommands.java` | `/creeper-healing` 全指令树（需要 OP 2） |

## 核心机制（改动前必读）

### 爆炸捕获链路
1. NeoForge 的 `ExplosionEvent.Detonate` 在 `Explosion.explode()` 内、**方块破坏之前**触发（`getAffectedBlocks()` = `toBlow`，方块仍在世界）。
2. `filterPositionsToHeal` 过滤（排除空气/TNT/火/ExcludedBlocks/白名单）→ 主位置。
3. `findIndirectlyAffectedPositions` BFS（512 深度）：找"支撑被炸后会掉落的方块"（火把、铁轨等），用 `EmptyLevel` 让 `canSurvive` 在空世界中判定。
4. 对主+间接位置快照 `BlockState` + `BlockEntity`（NBT 恢复的数据源）。
5. 计算掉落策略 → `ExplosionDropController.setPolicy(...)` → `manager.addExplosion(context)`。

### 掉落控制（三层，缺一不可）
1. **爆炸直接破坏**：`ExplosionDropsMixin` @WrapMethod 包住 `BlockStateBase.onExplosionHit`，`shouldSuppressDrops` 为真时跳过 loot 但仍 `state.onBlockExploded(...)` 破坏方块。
2. **容器内容**：抑制位置若 `restore_block_nbt` 开启，在破坏前 `removeBlockEntity`（否则 `ChestBlock.onRemove` 会把内容撒出来）。
3. **邻居更新破坏**（火把/铁轨因支撑方块被炸而掉落）：`BlockDropsEvent`（注意是**独立类** `net.neoforged.neoforge.event.level.BlockDropsEvent`，不是 BlockEvent 的内部类）取消掉落——否则物品掉落+方块被恢复=复制物品（上游 issue #6 的修复）。
4. 策略生命周期：**NeoForge 1.21.1 没有 `ExplosionEvent.End`**！策略在 Detonate 设置、下一个 Start 清理、`LevelTickEvent.Post` 兜底清理。所有掉落事件都在同一 tick 内同步发生，所以这个窗口是安全的。

### 恢复主循环
- 每维度 `ExplosionManager`，`LevelTickEvent.Post` 调用 `tick()`。
- `AbstractExplosionEvent.tick`：healTimer 递减 → 到 0 后逐个放方块（blockTimer 控制间隔）；放不下的（缺支撑）与后续可放的交换（delayAffectedBlock）。
- 排序：不透明优先 → 从下到上 → 从边缘到中心。
- 放方块时：`pushEntitiesUpwards` 推走实体；`make_falling_blocks_fall=false` 时 `FallingBlockSuppressor.suppressFall`；恢复 NBT 用 `BlockEntity.loadStatic`；音效/粒子。
- 白天模式**每 tick 重算**距日出的 tick 数（自动兼容睡觉/`/time` 命令，无需 mixin）。

### 持久化
- `SavedData` 每维度一份（`DATA_KEY="creeperhealing_events"`），`setDirty` 通过 `manager.setDirtyCallback(data::setDirty)` 联动。
- NBT 序列化：状态用 `BlockState.CODEC` + `registryAccess().createSerializationContext(NbtOps.INSTANCE)`；**加载必须容错**（`AffectedBlock.load` 返回 null 跳过、空列表跳过），否则坏档会崩服。

## NeoForge 1.21.1 的坑（都是实测踩出来的）

- ❌ `ExplosionEvent.End` —— **不存在**（只有 Start/Detonate）
- ❌ `BlockEvent.DropItemsEvent` —— **不存在**；爆炸掉落走 `BlockStateBase.onExplosionHit` → `canDropFromExplosion`（IBlockStateExtension 接口默认方法）
- ❌ `PotionEvent.PotionSplashEvent` —— **不存在**；用 `ProjectileImpactEvent`（`ThrownPotion` 继承 `ThrowableProjectile`，一定触发）
- ❌ `Explosion.getSourceMob()` —— **不存在**；分类用 `getDirectSourceEntity()`，黑名单用 `getIndirectSourceEntity()`（LivingEntity）
- ❌ `FallingBlock.canFallThrough` —— 1.21.1 叫 `isFree`（静态方法）
- ❌ `BlockEntityType.create(pos,state,tag,registries)` —— 不存在；用 `BlockEntity.loadStatic`
- ❌ `new BlockPos(int[])` —— 不存在；逐坐标构造
- ❌ `BlockState.isTransparent()` —— 不存在；用 `canOcclude()`
- ❌ `RandomSource.nextBetween(int,int)` —— 不存在；用 `nextIntBetweenInclusive`
- ❌ `SavedData.Factory(Supplier, Function)` —— 1.21.1 是 record：`Factory(Supplier, BiFunction<CompoundTag, HolderLookup.Provider, T>, DataFixTypes)`
- ❌ `MobEffects.HEALING` —— 1.21 改名 `MobEffects.HEAL`
- ❌ Mixin 的 `@At(target=...)` 格式：**方法名与描述符之间没有冒号**（`L...;method(desc)Z`）
- ❌ Mixin 类里的 public static 方法会触发 "non-private static method" 报错——辅助逻辑放在普通类（`FallingBlockSuppressor`）里
- ⚠️ `@WrapMethod` 的参数签名以**运行时实际签名**为准：`onExplosionHit` 实际是 `public void onExplosionHit(Level, BlockPos, Explosion, BiConsumer)`（4 参，无 BlockState）
- ⚠️ 验证运行时 API 用 `javap -p -cp build/moddev/artifacts/neoforge-21.1.244.jar <class>`；注意 PowerShell 里类名带 `$` 要用单引号（`'...BlockBehaviour$BlockStateBase'`）
- ⚠️ `BlockState.CODEC` 解码用 `registries.createSerializationContext(NbtOps.INSTANCE)`（RegistryOps）
- ⚠️ `EmptyLevelChunk` 构造器需要 3 参（Level, ChunkPos, Holder<Biome>），biome 用 `level.getBiome(pos)`

## 配置系统约定

- 新增配置项：`CreeperHealingConfig` 中加常量（`SECTION + ".key"`）+ DEFAULTS 默认值 + `buildFileContents()` 里的写出段 + 指令树注册。
- 配置文件名固定 `creeper-healing.toml`（与原版一致，用户可迁移旧配置）。
- 不要改用 `ModConfigSpec`——原版格式兼容是有意设计。
- 键值解析是手写的 TOML 子集：`#` 注释、`[section]`、`key = value`、引号剥离；replace_map 特殊处理（键也带引号）。

## Mixin 配置

- `src/main/resources/creeperhealing.mixins.json`（package 指向 mixin 包）
- `src/main/templates/META-INF/neoforge.mods.toml` 中 `[[mixins]]` 声明（`${mod_id}.mixins.json`）
- NeoForge 无需 refmap。新增 mixin 要同时改 json 清单。
- 能用事件/API 解决的不要加 mixin（当前 2 个都是因为 NeoForge 1.21.1 无等价事件）。

## 发布流程

1. `gradle.properties` 改 `mod_version` → 构建 → 自测（runServer + RCON）。
2. 提交、打 tag（`v<版本>`）、推 GitHub → CI `build.yml` 自动构建。
3. GitHub Release：建 release + 上传 `build/libs/creeperhealing-<版本>.jar`。
4. Modrinth：POST /v2/version（multipart：`data` JSON 字段 + `jar` 文件 part；字段名 `release_channel` 不是 version_types；许可证 SPDX 用 `LGPL-2.1-only`）。
5. **push main 会自动发布 alpha 到 Modrinth**（`.github/workflows/alpha.yml`，版本 `<基础>-alpha.<sha7>`，token 存 Actions Secret `MODRINTH_TOKEN`）。不要手动重复上传同名版本号。

## 许可证合规（红线）

- 本项目 **LGPL-2.1-only**。上游三处声明不一致（仓库 LICENSE 文件=LGPL-2.1、fabric.mod.json=LGPL-3.0-only、Modrinth 页面=Apache-2.0），**一律以仓库 LICENSE 文件（2.1）为准**——这是最保守且任何解读下都合法的选择。
- 保留 LICENSE 文件与原作者署名；改动要在 CHANGELOG 记录（可引用上游 issue 编号）。
- 禁止照抄 `CreeperHeal`（自定义许可证）的代码；其价值仅在于展示 NeoForge API 用法。
- 上游 fabric 代码可参考（LGPL-2.1），但需以 NeoForge API 重写，不得直接搬 fabric 的 mixin 结构。

## 代码风格

- Java 21，UTF-8；`options.encoding='UTF-8'` 已在 build.gradle 配置。
- 缩进 4 空格；类注释说明职责；关键逻辑（策略窗口、容错点）必须有注释解释"为什么"。
- 事件处理器全部注册在 `NeoForge.EVENT_BUS`，客户端侧一律 `isClientSide()` 守卫。
- 服务器线程单线程模型，无需并发原语；`ExplosionDropController`/registry 都是进程内静态映射。
