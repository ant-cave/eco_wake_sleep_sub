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

package io.github.ant_cave.eco_wake_sleep_sub.service;

import com.google.common.io.ByteStreams;
import com.google.common.io.ByteArrayDataOutput;

import io.github.ant_cave.eco_wake_sleep_sub.config.PluginConfig;
import io.github.ant_cave.eco_wake_sleep_sub.utils.NetworkUtils;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * 玩家传输服务类，负责处理玩家在不同服务器间的传输以及服务器唤醒逻辑
 */
public class PlayerTransferService {

    private Plugin plugin;
    private PluginConfig config;
    private Logger logger;
    private java.util.Set<Player> transferringPlayers = new java.util.HashSet<>();

    /**
     * 构造玩家传输服务实例
     * @param plugin 插件实例
     * @param config 插件配置
     * @param logger 日志记录器
     */
    public PlayerTransferService(Plugin plugin, PluginConfig config, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.logger = logger;
    }

    /**
     * 连接玩家到指定服务器
     * @param player 要连接的玩家
     * @param serverName 目标服务器名称
     */
    public void connectPlayer(Player player, String serverName) {
        if (transferringPlayers.contains(player)) {
            return;
        }
        transferringPlayers.add(player);
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            logger.info("[ecoWakeSleepSub] Connecting player " + player.getName() + " to server " + serverName);
        } catch (Exception e) {
            logger.warning("[ecoWakeSleepSub] Failed to connect player to server: " + e.getMessage());
            transferringPlayers.remove(player);
        }
    }

    /**
     * 从传送列表中移除玩家
     * @param player 要移除的玩家
     */
    public void removeTransferringPlayer(Player player) {
        transferringPlayers.remove(player);
    }

    /**
     * 开始倒计时传送玩家
     * @param player 要传送的玩家
     * @param seconds 倒计时秒数
     */
    public void startCountdown(Player player, int seconds) {
        if (transferringPlayers.contains(player)) {
            return;
        }
        for (int i = seconds; i > 0; i--) {
            final int currentSec = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                String countdownText = String.valueOf(currentSec);
                player.sendTitle(ChatColor.GREEN + "将在" + ChatColor.YELLOW + countdownText + ChatColor.GREEN + "后传送",
                        ChatColor.GREEN + "服务器在线 请稍安勿躁", 5, 20, 5);
            }, (seconds - i) * 20L);
        }

        // 倒计时结束后执行传送
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                transferringPlayers.remove(player);
                return;
            }
            player.sendTitle(ChatColor.GREEN + "正在传送", ChatColor.GREEN + "请稍安勿躁", 5, 20, 5);
            player.sendMessage(ChatColor.GREEN + player.getName() + " | " + config.mainServerName);
            connectPlayer(player, config.mainServerName);
        }, seconds * 20L);
    }

    /**
     * 服务器休眠启动倒计时
     * @param player 需要传送的玩家
     * @param seconds 倒计时秒数
     */
    public void serverSleepStartCountdown(Player player, int seconds) {
        // 第一次异步唤醒服务器
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (isServerSleeping()) {
                    NetworkUtils.wakeOnLan(config.mainServerMac);
                    logger.info("[ecoWakeSleepSub] 第一次WOL唤醒尝试");
                }
            } catch (IOException e) {
                logger.warning("[ecoWakeSleepSub] WOL失败: " + e.getMessage());
            }
        });

        // 每5秒发送一次WOL
        int wolInterval = 5; // 每5秒一次
        int wolCount = seconds / wolInterval;
        for (int i = 0; i < wolCount; i++) {
            final int currentWol = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        if (isServerSleeping()) {
                            NetworkUtils.wakeOnLan(config.mainServerMac);
                            logger.info("[ecoWakeSleepSub] 第" + (currentWol + 2) + "次WOL唤醒尝试");
                        }
                    } catch (IOException e) {
                        logger.warning("[ecoWakeSleepSub] WOL失败: " + e.getMessage());
                    }
                });
            }, (i + 1) * wolInterval * 20L);
        }

        // 每秒检查一次服务器状态并显示倒计时
        for (int i = 0; i < seconds; i++) {
            final int currentTick = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (checkServerConnected(player)) {
                    return;
                }

               // 显示剩余总等待时间
                int remainingSec = seconds - currentTick;
                int totalWaitSec = remainingSec + config.serverOnlineTimeToWait;
                player.sendTitle(ChatColor.YELLOW + "将在" + totalWaitSec + "秒后传送",
                        ChatColor.YELLOW + "服务器正在开机... (第" + ((currentTick/5)+1) + "次唤醒尝试)", 5, 20, 5);
            }, i * 20L);
        }

        // 倒计时结束后进入传送流程
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!checkServerConnected(player)) {
                player.sendTitle(ChatColor.GREEN + "正在传送", ChatColor.GREEN + "请稍安勿躁", 5, 20, 5);
                startCountdown(player, config.serverOnlineTimeToWait);
            }
        }, seconds * 20L);
    }

    /**
     * 检查服务器是否已连接
     * @param player 玩家对象
     * @return 如果服务器已连接返回true，否则返回false
     */
    private boolean checkServerConnected(Player player) {
        if (isServerConnected()) {
            player.sendTitle(ChatColor.GREEN + "正在传送", ChatColor.GREEN + "请稍安勿躁", 5, 20, 5);
            startCountdown(player, config.serverOnlineTimeToWait);
            return true;
        }
        return false;
    }

    /**
     * 检查服务器是否已连接
     * @return 如果服务器已连接返回true，否则返回false
     */
    private boolean isServerConnected() {
        return NetworkUtils.ping(config.mainServerIp, config.pingTimeout)
                && NetworkUtils.mcPing(config.mainServerIp, config.mainServerGamePort, config.pingTimeout);
    }

    /**
     * 检查服务器是否处于休眠状态
     * @return 如果服务器休眠返回true，否则返回false
     */
    private boolean isServerSleeping() {
        return !NetworkUtils.ping(config.mainServerIp, config.pingTimeout);
    }

    /**
     * 服务器启动中倒计时（LAUNCHING阶段）
     * @param player 需要传送的玩家
     * @param seconds 倒计时秒数
     */
    public void serverLaunchStartCountdown(Player player, int seconds) {
        // 每秒显示倒计时，提示服务器正在启动
        for (int i = 0; i < seconds; i++) {
            final int currentTick = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (checkServerConnected(player)) {
                    return;
                }

                // 显示剩余总等待时间
                int remainingSec = seconds - currentTick;
                int totalWaitSec = remainingSec + config.serverOnlineTimeToWait;
                player.sendTitle(ChatColor.GOLD + "将在" + totalWaitSec + "秒后传送",
                        ChatColor.GOLD + "已经开机，正在开服...", 5, 20, 5);
            }, i * 20L);
        }

        // 倒计时结束后进入传送流程
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!checkServerConnected(player)) {
                player.sendTitle(ChatColor.GREEN + "正在传送", ChatColor.GREEN + "请稍安勿躁", 5, 20, 5);
                startCountdown(player, config.serverOnlineTimeToWait);
            }
        }, seconds * 20L);
    }

    /**
     * 服务器关机中倒计时（SHUTTING阶段）
     * @param player 需要传送的玩家
     * @param seconds 倒计时秒数
     */
    public void serverShuttingStartCountdown(Player player, int seconds) {
        // 每秒显示倒计时，提示服务器刚刚关机
        for (int i = 0; i < seconds; i++) {
            final int currentTick = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (checkServerConnected(player)) {
                    return;
                }

                // 显示剩余总等待时间
                int remainingSec = seconds - currentTick;
                int totalWaitSec = remainingSec + config.serverOnlineTimeToWait;
                player.sendTitle(ChatColor.RED + "将在" + totalWaitSec + "秒后传送",
                        ChatColor.RED + "服务器刚刚关机，请稍候...", 5, 20, 5);
            }, i * 20L);
        }

        // 倒计时结束后进入传送流程
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!checkServerConnected(player)) {
                player.sendTitle(ChatColor.GREEN + "正在传送", ChatColor.GREEN + "请稍安勿躁", 5, 20, 5);
                startCountdown(player, config.serverOnlineTimeToWait);
            }
        }, seconds * 20L);
    }
}
