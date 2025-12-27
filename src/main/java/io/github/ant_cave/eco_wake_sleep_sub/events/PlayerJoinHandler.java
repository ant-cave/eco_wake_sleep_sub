/*
 * Copyright (C) 2025 ant-cave <ANTmmmmm@outlook.com> / <antmmmmm@126.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.ant_cave.eco_wake_sleep_sub.events;

import io.github.ant_cave.eco_wake_sleep_sub.config.PluginConfig;
import io.github.ant_cave.eco_wake_sleep_sub.service.PlayerTransferService;
import io.github.ant_cave.eco_wake_sleep_sub.state.ServerState;
import io.github.ant_cave.eco_wake_sleep_sub.state.ServerStatus;
import io.github.ant_cave.eco_wake_sleep_sub.state.State;
import io.github.ant_cave.eco_wake_sleep_sub.utils.NetworkUtils;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class PlayerJoinHandler implements Listener {

    private Plugin plugin;
    private PluginConfig config;
    private PlayerTransferService transferService;


    public PlayerJoinHandler(Plugin plugin, PluginConfig config, PlayerTransferService transferService) {
        this.plugin = plugin;
        this.config = config;
        this.transferService = transferService;
    }

    /**
     * 处理玩家离开事件
     * 清理传送状态
     * 
     * @param event 玩家离开事件对象
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        transferService.removeTransferringPlayer(event.getPlayer());
    }

    /**
     * 处理玩家加入事件
     * 当玩家加入服务器时，检测主服务器状态并根据状态显示相应信息，启动相应的倒计时转移服务
     * 
     * @param event 玩家加入事件对象
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendTitle(ChatColor.YELLOW + "检测主服务器状态中", ChatColor.YELLOW + "请稍安勿躁", 5, 20, 5);

        // 异步执行服务器状态检测
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            checkServerStatus();
            ServerStatus status = ServerState.getRunningStatus();
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (status == ServerStatus.RUNNING) {
                    // 服务器在线状态处理
                    event.getPlayer().sendTitle(ChatColor.GREEN + "服务器在线", ChatColor.GREEN + "请勿关闭游戏", 5, 20, 5);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        transferService.startCountdown(event.getPlayer(), 5);
                    }, 20L);
                } 
                else if (status == ServerStatus.SLEEP) {
                    // sleep阶段：服务器正在开机
                    event.getPlayer().sendTitle(ChatColor.YELLOW + "服务器正在开机", ChatColor.YELLOW + "请稍等片刻", 5, 20, 5);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        transferService.serverSleepStartCountdown(event.getPlayer(), config.timeToWait);
                    }, 20L);
                }
                else if (status == ServerStatus.LAUNCHING) {
                    // launch阶段：已经开机，正在开服
                    event.getPlayer().sendTitle(ChatColor.GOLD + "已经开机", ChatColor.GOLD + "正在开服...", 5, 20, 5);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        transferService.serverLaunchStartCountdown(event.getPlayer(), config.timeToWait);
                    }, 20L);
                }
                else if (status == ServerStatus.SHUTTING) {
                    // shutting阶段：来得不巧，刚刚关机
                    event.getPlayer().sendTitle(ChatColor.RED + "来得不巧", ChatColor.RED + "服务器刚刚关机", 5, 20, 5);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        transferService.serverShuttingStartCountdown(event.getPlayer(), config.timeToWait);
                    }, 20L);
                }
            });
        });
    }

    /**
     * 检查服务器状态并更新服务器状态
     * 通过ping和mcPing检测主服务器的连接状态，设置相应的服务器状态
     * 并在状态发生变化时记录日志
     */
    private void checkServerStatus() {
        // 检测主服务器IP是否可达，如果可达则进一步检测游戏端口连接状态
        if (NetworkUtils.ping(config.mainServerIp, config.pingTimeout)) {
            if (NetworkUtils.mcPing(config.mainServerIp, config.mainServerGamePort, config.pingTimeout)) {
                ServerState.set(State.WAKE_CONNECTED);
            } else {
                ServerState.set(State.WAKE);
            }
        } else {
            ServerState.set(State.SLEEP);
        }

        // 检查服务器状态是否发生变化，如果变化则记录状态转换日志
        if (ServerState.hasChanged()) {
            Bukkit.getLogger().info(
                    "[ecoWakeSleepSub] 状态变化: " + ServerState.getPrevious().name() + " -> " + ServerState.get().name());
        }
    }
}
