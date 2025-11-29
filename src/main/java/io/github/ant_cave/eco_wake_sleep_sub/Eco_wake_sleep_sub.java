package io.github.ant_cave.eco_wake_sleep_sub;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

enum State {
    WAKE,
    WAKE_CONNECTED,
    SLEEP
}

public final class Eco_wake_sleep_sub extends JavaPlugin implements Listener {

    Logger logger = Bukkit.getLogger();
    State currentStatus = State.SLEEP;//服务器连接状态
    Integer timeToWait=0;


    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("status").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("Welcome to the server!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("status")) {
            return commandStatus(sender);
        }
        return false;
    }

    boolean commandStatus(CommandSender sender){
        sender.sendMessage("Current plugin state: " + currentStatus.name());
        return true;
    }
}