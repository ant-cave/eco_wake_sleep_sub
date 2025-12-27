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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendTitle(ChatColor.YELLOW + "检测主服务器状态中", ChatColor.YELLOW + "请稍安勿躁", 5, 20, 5);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            checkServerStatus();
            if (ServerState.get() == State.WAKE_CONNECTED) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    event.getPlayer().sendTitle(ChatColor.GREEN + "服务器在线", ChatColor.GREEN + "请勿关闭游戏", 5, 20, 5);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        transferService.startCountdown(event.getPlayer(), 5);
                    }, 20L);
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    event.getPlayer().sendTitle(ChatColor.RED + "服务器已关闭", ChatColor.RED + "请勿关闭游戏", 5, 20, 5);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        transferService.serverSleepStartCountdown(event.getPlayer(), config.timeToWait);
                    }, 20L);
                });
            }
        });
    }

    private void checkServerStatus() {
        if (NetworkUtils.ping(config.mainServerIp, config.pingTimeout)) {
            if (NetworkUtils.mcPing(config.mainServerIp, config.mainServerGamePort, config.pingTimeout)) {
                ServerState.set(State.WAKE_CONNECTED);
            } else {
                ServerState.set(State.WAKE);
            }
        } else {
            ServerState.set(State.SLEEP);
        }

        if (ServerState.hasChanged()) {
            Bukkit.getLogger().info("[ecoWakeSleepSub] 状态变化: " + ServerState.getPrevious().name() + " -> " + ServerState.get().name());
        }
    }
}
