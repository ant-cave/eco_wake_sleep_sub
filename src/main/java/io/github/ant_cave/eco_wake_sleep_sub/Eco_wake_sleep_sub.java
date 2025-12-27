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

package io.github.ant_cave.eco_wake_sleep_sub;

import io.github.ant_cave.eco_wake_sleep_sub.commands.CommandHandlers;
import io.github.ant_cave.eco_wake_sleep_sub.config.PluginConfig;
import io.github.ant_cave.eco_wake_sleep_sub.events.PlayerJoinHandler;
import io.github.ant_cave.eco_wake_sleep_sub.service.PlayerTransferService;
import io.github.ant_cave.eco_wake_sleep_sub.state.ServerState;
import io.github.ant_cave.eco_wake_sleep_sub.state.ServerStatus;
import io.github.ant_cave.eco_wake_sleep_sub.state.State;
import io.github.ant_cave.eco_wake_sleep_sub.utils.NetworkUtils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class Eco_wake_sleep_sub extends JavaPlugin {

    private PluginConfig config;
    private PlayerTransferService transferService;
    private CommandHandlers commandHandlers;
    private PlayerJoinHandler joinHandler;
    private Integer sleepTime = 0;

    @Override
    public void onEnable() {
        Bukkit.getLogger().info("[ecoWakeSleepSub] Eco_wake_sleep_sub is enabled");

        saveDefaultConfig();
        config = new PluginConfig(getConfig());
        transferService = new PlayerTransferService(this, config, getLogger());
        commandHandlers = new CommandHandlers(config, transferService);
        joinHandler = new PlayerJoinHandler(this, config, transferService);

        getServer().getPluginManager().registerEvents(joinHandler, this);
        getCommand("status").setExecutor(this);
        getCommand("wake").setExecutor(this);
        getCommand("ping").setExecutor(this);
        getCommand("connect").setExecutor(this);
        getCommand("tcping").setExecutor(this);
        getCommand("mcping").setExecutor(this);
        getCommand("reload").setExecutor(this);
        getCommand("runningstatus").setExecutor(this);

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getLogger().info("[ecoWakeSleepSub] BungeeCord plugin messaging channels registered");

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::mainLoop, 0, config.pingInterval);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("status")) {
            return commandHandlers.status(sender);
        } else if (command.getName().equalsIgnoreCase("ping")) {
            return commandHandlers.ping(sender, args);
        } else if (command.getName().equalsIgnoreCase("wake")) {
            return commandHandlers.wake(sender, args);
        } else if (command.getName().equalsIgnoreCase("connect")) {
            return commandHandlers.connect(sender, args);
        } else if (command.getName().equalsIgnoreCase("tcping")) {
            return commandHandlers.tcping(sender, args);
        } else if (command.getName().equalsIgnoreCase("mcping")) {
            return commandHandlers.mcping(sender, args);
        } else if (command.getName().equalsIgnoreCase("reload")) {
            return commandHandlers.reload(sender);
        } else if (command.getName().equalsIgnoreCase("runningstatus")) {
            return commandHandlers.runningStatus(sender);
        }
        return false;
    }

    private void mainLoop() {
        
        if (NetworkUtils.ping(config.mainServerIp, config.pingTimeout)) {
            if (NetworkUtils.mcPing(config.mainServerIp, config.mainServerGamePort, config.pingTimeout)) {
                ServerState.set(State.WAKE_CONNECTED);
            } else {
                ServerState.set(State.WAKE);
            }
        } else {
            ServerState.set(State.SLEEP);
        }

        // 处理 runningStatus 状态转换
        if (ServerState.hasChanged()) {
            State currStatus = ServerState.get();
            State prev = ServerState.getPrevious();
            
            // SLEEP -> WAKE: 进入 LAUNCHING
            if (prev == State.SLEEP && currStatus == State.WAKE) {
                ServerState.setRunningStatus(ServerStatus.LAUNCHING);
                sleepTime = 0;
            }
            // WAKE_CONNECTED -> WAKE: 进入 SHUTTING
            else if (prev == State.WAKE_CONNECTED && currStatus == State.WAKE) {
                ServerState.setRunningStatus(ServerStatus.SHUTTING);
                sleepTime = 0;
            }
            // 其他状态变化重置 sleepTime
            else if (currStatus == State.SLEEP) {
                sleepTime = 0;
            }
        }

        // 状态保持时的处理
        if (!ServerState.hasChanged()) {
            State curr = ServerState.get();
            
            // WAKE_CONNECTED -> RUNNING
            if (curr == State.WAKE_CONNECTED && ServerState.getRunningStatus() == ServerStatus.LAUNCHING) {
                ServerState.setRunningStatus(ServerStatus.RUNNING);
            }
            // SHUTTING 计时 15 秒后回到 SLEEP
            else if (curr == State.SLEEP && ServerState.getRunningStatus() == ServerStatus.SHUTTING) {
                if (sleepTime < 15) {
                    sleepTime++;
                } else {
                    ServerState.setRunningStatus(ServerStatus.SLEEP);
                    sleepTime = 0;
                }
            }
        }

        if (ServerState.hasChanged()) {
            getLogger().info(
                    "[ecoWakeSleepSub] 状态变化: " + ServerState.getPrevious().name() + " -> " + ServerState.get().name());
        }
    }
}
