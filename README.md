# Eco Wake Sleep Sub - Minecraft 服务器状态监控与自动唤醒插件

[![GitHub](https://img.shields.io/badge/License-GPL%20v3-blue)](https://gnu.ac.cn/licenses/gpl-3.0.txt)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-blue)](https://www.minecraft.net)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)

## 这是什么？

Eco Wake Sleep Sub 是一个 Minecraft 服务器插件，主要做一件事：监控你的主服务器状态，需要的时候自动唤醒它。

当玩家加入子服务器时，插件会检查主服务器是不是在睡觉（休眠）。如果是，就通过 Wake-on-LAN 把它叫醒，等服务器准备好了再把玩家传过去。

## 主要功能

- **状态监控**：检查主服务器的网络状态和 Minecraft 服务状态
- **自动唤醒**：支持 Wake-on-LAN，远程唤醒休眠的服务器
- **自动传送**：主服务器就绪后，自动把玩家传过去
- **多种检测方式**：支持 ICMP ping、TCP 端口检测和 Minecraft 协议检测
- **BungeeCord 集成**：可以通过 BungeeCord 跨服务器传送玩家
- **等待倒计时**：给玩家显示清晰的等待时间
- **管理命令**：提供一些网络诊断和管理的命令

## 快速开始

### 需要准备什么

- Minecraft 服务器（Spigot/Paper 1.21+）
- Java 21 或更高版本
- 主服务器要支持 Wake-on-LAN
- BungeeCord 代理服务器（可选，跨服务器传送才需要）

### 安装步骤

1. **下载插件**
   - 从 [Releases 页面](https://github.com/ant-cave/eco_wake_sleep_sub/releases) 下载最新的 JAR 文件
   - 或者自己构建（下面有说明）

2. **安装到服务器**
   - 把 `eco_wake_sleep_sub.jar` 放到服务器的 `plugins` 目录
   - 重启服务器

3. **配置插件**
   - 第一次运行后，插件会在 `plugins/eco_wake_sleep_sub` 目录生成 `config.yml`
   - 根据你的网络环境改一下配置

4. **设置权限**
   - 根据需要给 OP 玩家或特定权限组分配插件权限

## 配置说明

### 配置文件 (`config.yml`)

```yaml
configVersion: 1

# 主服务器休眠时的等待时间（秒）
timeToWait: 150

# 主服务器在线时的等待时间（秒）
serverOnlineTimeToWait: 5

# 是否启用自动唤醒
enableAutoWake: true

# 是否启用脚本功能
enableScript: true

# 主服务器的 MAC 地址（Wake-on-LAN 用）
mainServerMac: 00:00:00:00:00:00

# 主服务器的 IP 地址
mainServerIp: 192.168.1.1

# 主服务器的 Minecraft 端口
mainServerGamePort: 25565

# 主服务器在 BungeeCord 里的名字
mainServerName: "main"

# 网络检测超时时间（毫秒）
pingTimeout: 50

# 状态检测间隔（tick，20tick=1秒）
pingInterval: 100
```

### 配置项说明

1. **网络设置**
   - `mainServerMac`：主服务器的 MAC 地址，格式 `00:11:22:33:44:55` 或 `00-11-22-33-44-55` 都行
   - `mainServerIp`：主服务器的 IP 地址或域名
   - `mainServerGamePort`：主服务器的 Minecraft 端口（默认 25565）

2. **时间设置**
   - `timeToWait`：主服务器休眠时，玩家要等多久（秒）
   - `serverOnlineTimeToWait`：主服务器在线时，传送前的倒计时（秒）
   - `pingInterval`：状态检测频率（单位：tick）

3. **功能开关**
   - `enableAutoWake`：要不要自动唤醒休眠的主服务器
   - `enableScript`：要不要启用脚本功能（预留的）

## 命令列表

| 命令 | 权限 | 说明 | 用法 |
|------|------|------|------|
| `/status` | `eco_wake_sleep_sub.status` | 查看插件当前状态 | `/status` |
| `/ping` | `eco_wake_sleep_sub.ping` | Ping 指定 IP | `/ping <ip>` 或 `/ping` |
| `/wake` | `eco_wake_sleep_sub.wake` | 唤醒指定 MAC 地址的服务器 | `/wake <mac>` |
| `/connect` | `eco_wake_sleep_sub.connect` | 把玩家连到其他服务器 | `/connect <玩家名> <服务器名>` |
| `/tcping` | `eco_wake_sleep_sub.tcping` | 测试 TCP 端口通不通 | `/tcping <主机> <端口>` 或 `/tcping` |
| `/mcping` | `eco_wake_sleep_sub.mcping` | 测试 Minecraft 服务器状态 | `/mcping [主机] [端口]` |
| `/reload` | `eco_wake_sleep_sub.reload` | 重新加载插件配置 | `/reload` |

### 命令示例

```bash
# 查看插件状态
/status

# Ping 主服务器（用配置里的 IP）
/ping

# Ping 指定 IP
/ping 192.168.1.100

# 唤醒服务器
/wake 00:11:22:33:44:55

# 测试 Minecraft 服务器状态
/mcping
/mcping example.com 25565

# 重新加载配置
/reload
```

## 权限节点

所有命令默认只有 OP 玩家能用：

- `eco_wake_sleep_sub.status` - 用 `/status` 命令
- `eco_wake_sleep_sub.wake` - 用 `/wake` 命令
- `eco_wake_sleep_sub.ping` - 用 `/ping` 命令
- `eco_wake_sleep_sub.connect` - 用 `/connect` 命令
- `eco_wake_sleep_sub.tcping` - 用 `/tcping` 命令
- `eco_wake_sleep_sub.mcping` - 用 `/mcping` 命令
- `eco_wake_sleep_sub.reload` - 用 `/reload` 命令

## 自己构建

### 需要什么

- Java 21 JDK
- Gradle 8.0+

### 怎么构建

1. **克隆仓库**
   ```bash
   git clone https://github.com/ant-cave/eco_wake_sleep_sub.git
   cd eco_wake_sleep_sub
   ```

2. **构建插件**
   ```bash
   ./gradlew build
   ```

3. **拿构建好的文件**
   - 构建完成后，JAR 文件在：`build/libs/eco_wake_sleep_sub-v1.0.0.jar`

4. **跑测试服务器**（可选）
   ```bash
   ./gradlew runServer
   ```

### 开发环境

- **IDE**：推荐 IntelliJ IDEA 或 Eclipse
- **构建工具**：Gradle
- **代码风格**：按 Java 标准来

## 工作原理

### 状态机

插件用状态机来管主服务器的状态：

```java
enum State {
    WAKE,           // 服务器醒了但 Minecraft 服务还没好
    WAKE_CONNECTED, // 服务器完全好了
    SLEEP,          // 服务器在睡觉
    DEAD           // 服务器没反应
}
```

### 工作流程

1. **玩家加入子服务器**
2. **插件检查主服务器状态**
   - 先 ICMP ping 一下
   - 如果 ping 通了，再用 Minecraft 协议检查
   - 根据结果更新状态
3. **状态处理**
   - 如果是 `WAKE_CONNECTED`：倒计时几秒就传玩家过去
   - 如果是 `SLEEP`：倒计时久一点，同时尝试唤醒服务器
4. **自动唤醒**
   - 发 Wake-on-LAN 魔术包到主服务器的 MAC 地址
   - 一直检查直到服务器准备好
5. **玩家传送**
   - 通过 BungeeCord 把玩家传到主服务器

### 网络检测

1. **ICMP Ping**：看服务器回不回 ICMP 请求
2. **TCP 端口检测**：看指定端口开没开
3. **Minecraft 协议检测**：用 Minecraft 服务器协议完整检查

## 怎么贡献

欢迎来帮忙！按下面步骤来：

1. Fork 这个仓库
2. 创建功能分支 (`git checkout -b feature/你的功能`)
3. 提交更改 (`git commit -m '加了某个功能'`)
4. 推到分支 (`git push origin feature/你的功能`)
5. 开 Pull Request

### 代码规范

- 按 Java 编码规范来
- 加适当的注释
- 提交信息写清楚点
- 注意向后兼容

## 更新日志

### v1.0.0
- 第一个版本
- 基本的状态监控功能
- 支持 Wake-on-LAN 自动唤醒
- 完整的命令系统
- 支持 BungeeCord 玩家传送

## 常见问题

### 可能遇到的问题

1. **Wake-on-LAN 没反应**
   - 检查主服务器支不支持 Wake-on-LAN
   - 确认 MAC 地址格式对不对
   - 确保子服务器和主服务器在同一个局域网

2. **玩家传不过去**
   - 检查 BungeeCord 配置
   - 确认主服务器名字对不对
   - 检查网络连接

3. **状态检测不准**
   - 调一下 `pingTimeout` 和 `pingInterval` 参数
   - 检查防火墙设置

4. **插件加载失败**
   - 确认服务器是 Spigot/Paper 1.21+
   - 检查 Java 版本是不是 21+
   - 看服务器日志找详细错误

### 看日志

插件日志以 `[ecoWakeSleepSub]` 开头，在服务器控制台或日志文件里能看到。

## 许可证

本项目基于 GNU General Public License v3.0 开源。

欢迎使用、修改和分发这个软件。  
如果你在商业产品或公开项目里用了这个软件，**希望你能在合适的地方注明原作者信息**，比如：

> 原作者：ant-cave(https://github.com/ant-cave)

这不是法律强制要求，但能帮我们更好地维护开源项目，也让更多人愿意来帮忙。

## 作者

- **ANTmmmmm** - 主要开发者
- 项目主页: [ant-cave.github.io](https://ant-cave.github.io)

## 感谢

- SpigotMC 社区
- BungeeCord 开发团队
- Velocity 开发团队
- 所有帮忙的人和用户

## 支持与反馈

- 提交 Issue: [GitHub Issues](https://github.com/ant-cave/eco_wake_sleep_sub/issues)
- 讨论区: [GitHub Discussions](https://github.com/ant-cave/eco_wake_sleep_sub/discussions)

---

**提示**：在生产环境用之前，最好在测试服务器上多试试。确保你了解 Wake-on-LAN 的网络要求和限制。
