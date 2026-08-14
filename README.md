# Creeper Healing (NeoForge)

一个服务端为主、可高度自定义的 NeoForge 模组，能让世界在苦力怕爆炸（以及其他类型的爆炸）后自动、自然地恢复地形。

本模组是 [creeper-healing](https://github.com/ArkoSammy12/creeper-healing)（作者 ArkoSammy12，Fabric 版，LGPL-3.0）的 **NeoForge 1.21.1 移植版**，作者为 huntersxy。

## 功能

### 爆炸恢复模式

- **默认模式**：爆炸后等待配置的延迟，然后逐个恢复方块。
- **白天恢复模式**：爆炸会等到日出才开始恢复；恢复过程中需要光照。方块会在一天内分批恢复。
- **难度恢复模式**：根据世界难度加快或减慢恢复速度。
- **抗爆性恢复模式**：抗爆性越高的方块恢复越慢，并带有随机偏移，方块会成批恢复。

方块恢复的位置必须是玩家也能放置方块的位置。

### 不同爆炸来源

支持恢复多种来源的爆炸：生物（苦力怕、恶魂等）、TNT、床/末地水晶等方块、风弹等触发型爆炸，以及其他来源。可以为生物来源配置黑名单。

### 控制爆炸掉落物

可以单独配置不同来源的爆炸是否掉落物品。默认所有非生物爆炸**不**掉落物品（与 Fabric 原版一致），也可以逐项修改。另有生物掉落黑名单。

### 可配置延迟

- 爆炸开始恢复的等待时间
- 每个方块恢复的间隔时间

> 注意：两个延迟最小值均为 0.05 秒。

### 恢复方块实体 NBT

可开关方块 NBT 数据的恢复（关闭后容器内的物品会正常掉落）。也可以强制带 NBT 的方块必须恢复，保证原方块及其数据被还原。

### 掉落方块不落下

可以让沙子、沙砾等方块被恢复后停留在原位，只有收到方块更新时才会下落。

### 白名单

可选地启用白名单，只恢复白名单中的方块：

```toml
[whitelist]
	whitelist = ["minecraft:grass", "minecraft:stone", "minecraft:sand"]
```

### 替换表（Replace Map）

在配置文件中可以指定某个方块被恢复成另一种方块（例如钻石块恢复成石头），并保留原方块的属性（如朝向）：

```toml
[replace_map]
	"minecraft:diamond_block" = "minecraft:stone"
```

> 警告：同一个键不能出现两次，否则会导致启动崩溃。

### 其他设置

- 恢复方块时是否播放放置音效 / 产生粒子
- 向爆炸区域投掷**治疗药水**立即完成恢复，或投掷**再生药水**提前开始恢复

### 指令

所有设置都可以通过游戏内指令修改（需要 OP 权限），修改后会自动写入配置文件：

- `/creeper-healing explosion_heal_delay [秒]`
- `/creeper-healing block_placement_delay [秒]`
- `/creeper-healing mode [模式名]`
- `/creeper-healing heal_mob_explosions [true/false]` 等所有开关
- `/creeper-healing heal_mob_explosions_blacklist add/remove/list <实体id>`
- `/creeper-healing drop_items_on_mob_explosions_blacklist add/remove/list <实体id>`
- `/creeper-healing whitelist add/remove/list <方块id>`
- `/creeper-healing replace_map add/remove/list <旧方块> <新方块>`
- `/creeper-healing reload_config` —— 重新读取配置文件

不带参数执行某条指令会显示当前值。

## 配置文件

配置文件位于 `config/creeper-healing.toml`（首次启动自动生成）。修改后重启生效，或在游戏中执行 `/creeper-healing reload_config`。

## 构建

环境要求：**JDK 21**。

```
./gradlew build
```

构建产物位于 `build/libs/`，选择不带 `sources` 的 `.jar` 文件。

## 许可证

LGPL-3.0（详见 [LICENSE](LICENSE)）。

- 本模组移植自 [creeper-healing](https://github.com/ArkoSammy12/creeper-healing)（LGPL-3.0，作者 ArkoSammy12）。
- 模组图标来自 [Kioku](https://github.com/takoyakioku)（原模组致谢）。
