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

package io.github.ant_cave.eco_wake_sleep_sub.config;

import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private FileConfiguration config;

    // 配置项
    public int timeToWait;
    public int serverOnlineTimeToWait;
    public boolean enableAutoWake;
    public boolean enableScript;
    public String mainServerMac;
    public int mainServerGamePort;
    public String mainServerIp;
    public int pingTimeout;
    public int pingInterval;
    public String mainServerName;

    public PluginConfig(FileConfiguration config) {
        this.config = config;
        load();
    }

    public void load() {
        timeToWait = config.getInt("timeToWait", 150);
        serverOnlineTimeToWait = config.getInt("serverOnlineTimeToWait", 5);
        enableAutoWake = config.getBoolean("enableAutoWake", true);
        enableScript = config.getBoolean("enableScript", true);
        mainServerMac = config.getString("mainServerMac", "00:00:00:00:00:00");
        mainServerIp = config.getString("mainServerIp", "192.168.1.1");
        mainServerGamePort = config.getInt("mainServerGamePort", 25565);
        pingInterval = config.getInt("pingInterval", 5000);
        pingTimeout = config.getInt("pingTimeout", 10);
        mainServerName = config.getString("mainServerName", "main");
    }
}
