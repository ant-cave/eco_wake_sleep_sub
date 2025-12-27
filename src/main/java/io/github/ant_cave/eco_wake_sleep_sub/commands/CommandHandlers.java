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

package io.github.ant_cave.eco_wake_sleep_sub.commands;

import io.github.ant_cave.eco_wake_sleep_sub.config.PluginConfig;
import io.github.ant_cave.eco_wake_sleep_sub.service.PlayerTransferService;
import io.github.ant_cave.eco_wake_sleep_sub.state.ServerState;
import io.github.ant_cave.eco_wake_sleep_sub.utils.NetworkUtils;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandlers {

    private PluginConfig config;
    private PlayerTransferService transferService;

    public CommandHandlers(PluginConfig config, PlayerTransferService transferService) {
        this.config = config;
        this.transferService = transferService;
    }

    public boolean status(CommandSender sender) {
        sender.sendMessage("Current plugin state: " + ServerState.get().name());
        return true;
    }

    public boolean runningStatus(CommandSender sender) {
        sender.sendMessage("远程服务器运行状态: " + ServerState.getRunningStatus().name());
        return true;
    }

    public boolean ping(CommandSender sender, String[] args) {
        final String targetIp;

        if (args.length == 1) {
            targetIp = args[0];
        } else if (args.length == 0) {
            targetIp = config.mainServerIp;
        } else {
            sender.sendMessage("Usage: /ping <ip>");
            return false;
        }

        sender.sendMessage(ChatColor.YELLOW + "正在ping " + targetIp + "，请稍候...");

        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Eco_wake_sleep_sub"), () -> {
            boolean result = NetworkUtils.ping(targetIp, config.pingTimeout);

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Eco_wake_sleep_sub"), () -> {
                if (result) {
                    sender.sendMessage(ChatColor.GREEN + "Ping结果: " + targetIp + " 可达");
                } else {
                    sender.sendMessage(ChatColor.RED + "Ping结果: " + targetIp + " 不可达");
                }
            });
        });

        return true;
    }

    public boolean wake(CommandSender sender, String[] args) {
        if (args.length == 1) {
            try {
                NetworkUtils.wakeOnLan(args[0]);
                sender.sendMessage(ChatColor.GREEN + "Wake-on-LAN成功");
                return true;
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Wake-on-LAN失败");
                return false;
            }
        } else {
            sender.sendMessage("Usage: /wake <mac>");
            return false;
        }
    }

    public boolean connect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("使用方法: /connect <玩家名> <服务器名>");
            return false;
        }

        String playerName = args[0];
        String serverName = args[1];

        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("玩家 " + playerName + " 不在线或不存在");
            return false;
        }

        transferService.connectPlayer(targetPlayer, serverName);
        sender.sendMessage("正在传送玩家 " + playerName + " 到服务器 " + serverName);
        return true;
    }

    public boolean tcping(CommandSender sender, String[] args) {
        final String targetHost;
        final int targetPort;

        if (args.length == 2) {
            targetHost = args[0];
            try {
                targetPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("端口号必须是有效的数字");
                return false;
            }
        } else if (args.length == 0) {
            targetHost = config.mainServerIp;
            targetPort = config.mainServerGamePort;
        } else {
            sender.sendMessage("使用方法: /tcping <主机> <端口>");
            return false;
        }

        sender.sendMessage(ChatColor.YELLOW + "正在测试TCP连接 " + targetHost + ":" + targetPort + "，请稍候...");

        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Eco_wake_sleep_sub"), () -> {
            boolean result = NetworkUtils.tcping(targetHost, targetPort, config.pingTimeout);

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Eco_wake_sleep_sub"), () -> {
                if (result) {
                    sender.sendMessage(ChatColor.GREEN + "TCP连接测试结果: " + targetHost + ":" + targetPort + " 可连接");
                } else {
                    sender.sendMessage(ChatColor.RED + "TCP连接测试结果: " + targetHost + ":" + targetPort + " 不可连接");
                }
            });
        });

        return true;
    }

    public boolean mcping(CommandSender sender, String[] args) {
        final String targetHost;
        final int targetPort;

        if (args.length == 2) {
            targetHost = args[0];
            try {
                targetPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("端口号必须是有效的数字");
                return false;
            }
        } else if (args.length == 1) {
            targetHost = args[0];
            targetPort = config.mainServerGamePort;
        } else if (args.length == 0) {
            targetHost = config.mainServerIp;
            targetPort = config.mainServerGamePort;
        } else {
            sender.sendMessage("使用方法: /mcping [主机] [端口]");
            return false;
        }

        sender.sendMessage(ChatColor.YELLOW + "正在测试Minecraft服务器 " + targetHost + ":" + targetPort + "，请稍候...");

        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Eco_wake_sleep_sub"), () -> {
            boolean result = NetworkUtils.mcPing(targetHost, targetPort, config.pingTimeout);

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Eco_wake_sleep_sub"), () -> {
                if (result) {
                    sender.sendMessage(ChatColor.GREEN + "Minecraft服务器测试结果: " + targetHost + ":" + targetPort + " 在线");
                } else {
                    sender.sendMessage(ChatColor.RED + "Minecraft服务器测试结果: " + targetHost + ":" + targetPort + " 离线");
                }
            });
        });

        return true;
    }

    public boolean reload(CommandSender sender) {
        Bukkit.getPluginManager().getPlugin("Eco_wake_sleep_sub").reloadConfig();
        config.load();
        sender.sendMessage(ChatColor.GREEN + "[ecoWakeSleepSub] 配置已重新加载");
        return true;
    }
}
