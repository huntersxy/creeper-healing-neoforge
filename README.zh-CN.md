# Creeper Healing（苦力怕治愈）

一个服务端为主、高度可自定义的 **NeoForge** 模组，能让世界在苦力怕爆炸（以及其他类型的爆炸）后自动、自然地恢复地形。

本模组是 [creeper-healing](https://github.com/ArkoSammy12/creeper-healing)（作者 **ArkoSammy12**，Fabric 版，LGPL-2.1）的 **NeoForge 1.21.1 移植版**。全部代码基于 NeoForge API 重写，保留原模组全部功能，并对 NeoForge 平台做了针对性优化。

> ⚠️ 这是非官方移植。遇到问题请在本仓库反馈，不要打扰原项目。

## 环境要求

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | **21.1.x**（任意小版本，如 21.1.0 – 21.1.244） |
| Java | 21 |

## 功能

### 恢复模式

- **默认模式**：等待配置的延迟后，逐个恢复被破坏的方块。
- **白天恢复模式**：爆炸会等到日出才开始恢复，且恢复过程中需要光照。
- **难度恢复模式**：根据世界难度加快或减慢恢复速度。
- **抗爆性恢复模式**：抗爆性越高的方块恢复越慢，并带随机偏移，方块会成批恢复。

方块只会恢复到玩家能够放置方块的位置。

### 爆炸来源

生物（苦力怕、恶魂等）、方块（床、重生锚、末地水晶）、TNT（方块与矿车）、触发型（风弹）以及其他来源的爆炸，可分别开关是否恢复，生物来源还支持黑名单。

### 掉落物控制

可按爆炸来源单独配置是否掉落物品；开启 `restore_block_nbt` 后容器内容也会保留，随方块 NBT 一起恢复。

### 其他特性

- 可配置爆炸开始延迟与单方块恢复间隔（最小 0.05 秒）。
- 恢复方块实体 NBT；可强制带 NBT 的方块必定恢复。
- 恢复的沙子、沙砾等掉落方块可以不落下，直到收到方块更新。
- 可选方块白名单。
- 替换表：可将某些方块恢复成其他方块（如钻石块恢复成石头），并保留方块属性。
- 恢复时的放置音效与云粒子。
- 向爆炸区域投掷**治疗药水**立即完成恢复，**再生药水**提前开始恢复。
- 全部设置可游戏内指令修改，配置文件支持热重载。
- 恢复进度按维度存档，服务器重启后自动续期。

## 安装

1. 安装 [Minecraft 1.21.1 的 NeoForge](https://neoforged.net/) 并至少启动一次。
2. 将 `creeperhealing-1.0.0.jar` 放入客户端或服务端的 `mods/` 文件夹。
3. 启动游戏。首次启动会自动生成配置文件 `config/creeper-healing.toml`。

## 配置文件

所有设置位于 `config/creeper-healing.toml`（与原模组布局一致，可沿用旧配置）。修改后执行 `/creeper-healing reload_config` 热重载，或用指令直接修改。

| 分区 | 设置项 | 默认值 | 说明 |
|---|---|---|---|
| `[delays]` | `explosion_heal_delay` | `3.0` | 爆炸开始恢复前的等待秒数。 |
| `[delays]` | `block_placement_delay` | `1.0` | 每个方块恢复的间隔秒数。 |
| `[explosion_item_drops]` | `drop_items_on_mob_explosions` | `false` | 生物爆炸是否掉落物品。 |
| `[explosion_item_drops]` | `drop_items_on_block_explosions` | `true` | 床/末地水晶爆炸是否掉落物品。 |
| `[explosion_item_drops]` | `drop_items_on_tnt_explosions` | `true` | TNT 爆炸是否掉落物品。 |
| `[explosion_item_drops]` | `drop_items_on_triggered_explosions` | `true` | 风弹爆炸是否掉落物品。 |
| `[explosion_item_drops]` | `drop_items_on_other_explosions` | `true` | 其他来源爆炸是否掉落物品。 |
| `[explosion_item_drops]` | `drop_items_on_mob_explosions_blacklist` | `["minecraft:placeholder"]` | 永不掉落物品的生物 id 黑名单。 |
| `[explosion_sources]` | `heal_mob_explosions` | `true` | 是否恢复生物爆炸。 |
| `[explosion_sources]` | `heal_block_explosions` | `false` | 是否恢复床/末地水晶爆炸。 |
| `[explosion_sources]` | `heal_tnt_explosions` | `false` | 是否恢复 TNT 爆炸。 |
| `[explosion_sources]` | `heal_triggered_explosions` | `false` | 是否恢复风弹爆炸。 |
| `[explosion_sources]` | `heal_other_explosions` | `false` | 是否恢复其他来源爆炸。 |
| `[explosion_sources]` | `heal_mob_explosions_blacklist` | `["minecraft:placeholder"]` | 永不恢复的生物 id 黑名单。 |
| `[explosion_healing_mode]` | `mode` | `"default_mode"` | `default_mode`、`daytime_healing_mode`、`difficulty_based_healing_mode`、`blast_resistance_based_healing_mode`。 |
| `[preferences]` | `restore_block_nbt` | `false` | 恢复时是否还原方块实体 NBT。 |
| `[preferences]` | `force_blocks_with_nbt_to_always_heal` | `false` | 是否强制带 NBT 的方块必定恢复。 |
| `[preferences]` | `make_falling_blocks_fall` | `true` | 恢复的沙子/沙砾是否下落；`false` 时收到方块更新才下落。 |
| `[preferences]` | `block_placement_sound_effect` | `true` | 恢复时是否播放放置音效。 |
| `[preferences]` | `block_placement_particles` | `true` | 恢复时是否产生云粒子。 |
| `[preferences]` | `heal_on_healing_potion_splash` | `true` | 治疗药水是否立即完成恢复。 |
| `[preferences]` | `heal_on_regeneration_potion_splash` | `true` | 再生药水是否提前开始恢复。 |
| `[preferences]` | `enable_whitelist` | `false` | 是否启用白名单。 |
| `[whitelist]` | `whitelist` | `["minecraft:placeholder"]` | 允许恢复的方块 id 列表。 |
| `[replace_map]` | `"旧方块" = "新方块"` | `"minecraft:diamond_block" = "minecraft:stone"` | 将 `旧方块` 恢复为 `新方块`。 |

> 注意：替换表中同一个键不能出现两次，否则模组将拒绝加载配置。

## 指令

所有指令需要 OP 权限。不带参数执行可查看当前值。

```
/creeper-healing reload_config
/creeper-healing mode [模式名]
/creeper-healing explosion_heal_delay [秒]
/creeper-healing block_placement_delay [秒]
/creeper-healing <设置项> [true|false]        # 任意布尔设置，如 heal_tnt_explosions
/creeper-healing <列表> add <id>               # 如 whitelist add minecraft:stone
/creeper-healing <列表> remove <id>
/creeper-healing <列表> list
/creeper-healing replace_map add <旧方块> <新方块>
/creeper-healing replace_map remove <旧方块>
/creeper-healing replace_map list
```

布尔设置项：`drop_items_on_mob_explosions`、`drop_items_on_block_explosions`、`drop_items_on_tnt_explosions`、`drop_items_on_triggered_explosions`、`drop_items_on_other_explosions`、`heal_mob_explosions`、`heal_block_explosions`、`heal_tnt_explosions`、`heal_triggered_explosions`、`heal_other_explosions`、`restore_block_nbt`、`force_blocks_with_nbt_to_always_heal`、`make_falling_blocks_fall`、`block_placement_sound_effect`、`block_placement_particles`、`heal_on_healing_potion_splash`、`heal_on_regeneration_potion_splash`、`enable_whitelist`。

列表设置项：`heal_mob_explosions_blacklist`、`drop_items_on_mob_explosions_blacklist`、`whitelist`。

## 从源码构建

环境要求：**JDK 21**（如 [Eclipse Temurin 21](https://adoptium.net/)）。

```
./gradlew build
```

构建产物位于 `build/libs/creeperhealing-1.0.0.jar`（取不带 `sources` 的那个 jar）。

## 许可证与致谢

- 基于 **GNU Lesser General Public License v2.1** 发布，详见 [LICENSE](LICENSE)。
- 本模组移植自 [creeper-healing](https://github.com/ArkoSammy12/creeper-healing)（作者 [ArkoSammy12](https://github.com/ArkoSammy12)，LGPL-2.1）。
- 模组图标来自 [Kioku](https://github.com/takoyakioku)（原模组致谢）。
- 项目骨架基于 NeoForge MDK 模板（MIT）。
