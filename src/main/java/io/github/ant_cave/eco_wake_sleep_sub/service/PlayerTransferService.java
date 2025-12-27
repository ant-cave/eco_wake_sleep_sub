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

public class PlayerTransferService {

    private Plugin plugin;
    private PluginConfig config;
    private Logger logger;

    public PlayerTransferService(Plugin plugin, PluginConfig config, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.logger = logger;
    }

    public void connectPlayer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            logger.info("[ecoWakeSleepSub] Connecting player " + player.getName() + " to server " + serverName);
        } catch (Exception e) {
            logger.warning("[ecoWakeSleepSub] Failed to connect player to server: " + e.getMessage());
        }
    }

    public void startCountdown(Player player, int seconds) {
        if (seconds <= 0) {
            return;
        }

        String countdownText = String.valueOf(seconds);
        player.sendTitle(ChatColor.GREEN + "将在" + ChatColor.YELLOW + countdownText + ChatColor.GREEN + "后传送",
                ChatColor.GREEN + "服务器在线 请稍安勿躁", 5, 20, 5);

        if (seconds > 1) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                startCountdown(player, seconds - 1);
            }, 20L);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendTitle(ChatColor.GREEN + "正在传送", ChatColor.GREEN + "请稍安勿躁", 5, 20, 5);
                player.sendMessage(ChatColor.GREEN + player.getName() + " | " + config.mainServerName);
                connectPlayer(player, config.mainServerName);
            }, 20L);
        }
    }

    public void serverSleepStartCountdown(Player player, int seconds) {
        if (checkServerConnected(player)) {
            return;
        }

        if (seconds <= 0) {
            player.sendTitle(ChatColor.GREEN + "正在传送", ChatColor.GREEN + "请稍安勿躁", 5, 20, 5);
            startCountdown(player, config.serverOnlineTimeToWait);
            return;
        }

        String countdownText = String.valueOf(seconds);
        player.sendTitle(ChatColor.YELLOW + "将在" + (Integer.valueOf(countdownText) + config.serverOnlineTimeToWait) + "后传送",
                ChatColor.YELLOW + "正在努力唤醒服务器资源", 5, 20, 5);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (isServerSleeping()) {
                    NetworkUtils.wakeOnLan(config.mainServerMac);
                }
            } catch (IOException e) {
                logger.warning("[ecoWakeSleepSub] " + e.getMessage());
            }
        });

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            serverSleepStartCountdown(player, seconds - 1);
        }, 20L);
    }

    private boolean checkServerConnected(Player player) {
        if (isServerConnected()) {
            player.sendTitle(ChatColor.GREEN + "正在传送", ChatColor.GREEN + "请稍安勿躁", 5, 20, 5);
            startCountdown(player, config.serverOnlineTimeToWait);
            return true;
        }
        return false;
    }

    private boolean isServerConnected() {
        return NetworkUtils.ping(config.mainServerIp, config.pingTimeout)
                && NetworkUtils.mcPing(config.mainServerIp, config.mainServerGamePort, config.pingTimeout);
    }

    private boolean isServerSleeping() {
        return !NetworkUtils.ping(config.mainServerIp, config.pingTimeout);
    }
}
