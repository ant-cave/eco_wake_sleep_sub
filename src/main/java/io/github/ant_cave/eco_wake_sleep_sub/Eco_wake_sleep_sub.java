package io.github.ant_cave.eco_wake_sleep_sub;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.ChatColor;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

enum State {
    WAKE,
    WAKE_CONNECTED,
    SLEEP,
    DEAD
}

public final class Eco_wake_sleep_sub extends JavaPlugin implements Listener {

    Logger logger = Bukkit.getLogger();
    State currentStatus = State.SLEEP;// 服务器连接状态

    Integer timeToWait;
    boolean enableAutoWake;
    boolean enableScript;
    String mainServerMac;
    Integer mainServerGamePort;
    String mainServerIp;
    Integer pingTimeout;
    Integer pingInterval = 5000;

    String mainServerName;

    boolean loop;

    @Override
    public void onEnable() {
        // Plugin startup logic
        logger.info("Eco_wake_sleep_sub is enabled");// 启动提示

        loop = true;

        saveDefaultConfig();// 保存默认配置文件
        initConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("status").setExecutor(this);
        getCommand("wake").setExecutor(this);
        getCommand("ping").setExecutor(this);
        getCommand("connect").setExecutor(this);
        getCommand("tcping").setExecutor(this);
        getCommand("mcping").setExecutor(this);

        // 注册BungeeCord通道
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        logger.info("BungeeCord plugin messaging channels registered");

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            mainLoop();
        }, 0, pingInterval);
    }

    /**
     * 初始化配置文件
     */
    void initConfig() {
        timeToWait = getConfig().getInt("timeToWait", 150);

        enableAutoWake = getConfig().getBoolean("enableAutoWake", true);
        enableScript = getConfig().getBoolean("enableScript", true);
        mainServerMac = getConfig().getString("mainServerMac", "00:00:00:00:00:00");
        mainServerIp = getConfig().getString("mainServerIp", "192.168.1.1");

        mainServerGamePort = getConfig().getInt("mainServerGamePort", 25565);

        pingInterval = getConfig().getInt("pingInterval", 5000);
        pingTimeout = getConfig().getInt("pingTimeout", 10);
        mainServerName = getConfig().getString("mainServerName", "main");

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
            return issueCommandStatus(sender);
        } else if (command.getName().equalsIgnoreCase("ping")) {
            return issueCommandPingAsync(sender, args);
        } else if (command.getName().equalsIgnoreCase("wake")) {
            return issueCommandWake(sender, args);
        } else if (command.getName().equalsIgnoreCase("connect")) {
            return issueCommandConnect(sender, args);
        } else if (command.getName().equalsIgnoreCase("tcping")) {
            return issueCommandTcpingAsync(sender, args);
        } else if (command.getName().equalsIgnoreCase("mcping")) {
            return issueCommandMcpingAsync(sender, args);
        }
        return false;
    }

    /**
     * 状态查询
     * 
     * @param sender
     * @return
     */
    boolean issueCommandStatus(CommandSender sender) {
        sender.sendMessage("Current plugin state: " + currentStatus.name());
        return true;
    }

    /**
     * 异步测试网络连通性
     * 
     * @param sender
     * @param args
     * @return
     */
    boolean issueCommandPingAsync(CommandSender sender, String[] args) {
        final String targetIp;

        if (args.length == 1) {
            targetIp = args[0];
        } else if (args.length == 0) {
            targetIp = mainServerIp;
        } else {
            sender.sendMessage("Usage: /ping <ip>");
            return false;
        }

        sender.sendMessage(ChatColor.YELLOW + "正在ping " + targetIp + "，请稍候...");

        // 使用Bukkit调度器异步执行ping操作
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            boolean result = NetworkUtils.ping(targetIp, pingTimeout);

            // 回到主线程发送消息
            Bukkit.getScheduler().runTask(this, () -> {
                if (result) {
                    sender.sendMessage(ChatColor.GREEN + "Ping结果: " + targetIp + " 可达");
                } else {
                    sender.sendMessage(ChatColor.RED + "Ping结果: " + targetIp + " 不可达");
                }
            });
        });

        return true;
    }

    boolean issueCommandWake(CommandSender sender, String[] args) {
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

    /**
     * 处理connect命令
     */
    boolean issueCommandConnect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("使用方法: /connect <玩家名> <服务器名>");
            return false;
        }

        String playerName = args[0];
        String serverName = args[1];

        // 检查玩家是否在线
        Player targetPlayer = getServer().getPlayer(playerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("玩家 " + playerName + " 不在线或不存在");
            return false;
        }

        // 传送玩家到指定服务器
        connectPlayerToServer(targetPlayer, serverName);
        sender.sendMessage("正在传送玩家 " + playerName + " 到服务器 " + serverName);
        return true;
    }

    /**
     * 异步测试TCP端口连通性
     * 
     * @param sender
     * @param args
     * @return
     */
    boolean issueCommandTcpingAsync(CommandSender sender, String[] args) {
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
            targetHost = mainServerIp;
            targetPort = mainServerGamePort;
        } else {
            sender.sendMessage("使用方法: /tcping <主机> <端口>");
            return false;
        }

        sender.sendMessage(ChatColor.YELLOW + "正在测试TCP连接 " + targetHost + ":" + targetPort + "，请稍候...");

        // 使用Bukkit调度器异步执行tcping操作
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            boolean result = NetworkUtils.tcping(targetHost, targetPort, pingTimeout);

            // 回到主线程发送消息
            Bukkit.getScheduler().runTask(this, () -> {
                if (result) {
                    sender.sendMessage(ChatColor.GREEN + "TCP连接测试结果: " + targetHost + ":" + targetPort + " 可连接");
                } else {
                    sender.sendMessage(ChatColor.RED + "TCP连接测试结果: " + targetHost + ":" + targetPort + " 不可连接");
                }
            });
        });

        return true;
    }

    /**
     * 异步测试Minecraft服务器连通性
     * 
     * @param sender
     * @param args
     * @return
     */
    boolean issueCommandMcpingAsync(CommandSender sender, String[] args) {
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
            targetPort = mainServerGamePort;
        } else if (args.length == 0) {
            targetHost = mainServerIp;
            targetPort = mainServerGamePort;
        } else {
            sender.sendMessage("使用方法: /mcping [主机] [端口]");
            return false;
        }

        sender.sendMessage(ChatColor.YELLOW + "正在测试Minecraft服务器 " + targetHost + ":" + targetPort + "，请稍候...");

        // 使用Bukkit调度器异步执行mcping操作
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            boolean result = NetworkUtils.mcPing(targetHost, targetPort, pingTimeout);

            // 回到主线程发送消息
            Bukkit.getScheduler().runTask(this, () -> {
                if (result) {
                    sender.sendMessage(ChatColor.GREEN + "Minecraft服务器测试结果: " + targetHost + ":" + targetPort + " 在线");
                } else {
                    sender.sendMessage(ChatColor.RED + "Minecraft服务器测试结果: " + targetHost + ":" + targetPort + " 离线");
                }
            });
        });

        return true;
    }

    /**
     * 传送玩家到指定服务器
     * 
     * @param player     要传送的玩家
     * @param serverName 目标服务器名
     */
    private void connectPlayerToServer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
            logger.info("Connecting player " + player.getName() + " to server " + serverName);
        } catch (Exception e) {
            logger.warning("Failed to connect player to server: " + e.getMessage());
        }
    }

    void mainLoop() {
        // logger.info(ChatColor.YELLOW + "Main loop started");

        if (NetworkUtils.ping(mainServerIp, pingTimeout)) {
            if (NetworkUtils.mcPing(mainServerIp, mainServerGamePort, pingTimeout)) {
                currentStatus = State.WAKE_CONNECTED;
                // logger.info("Remote machine is connected");
            } else {
                currentStatus = State.WAKE;
                // logger.info("Remote machine is connected");
            }
        } else {
            currentStatus = State.SLEEP;
        }

        // logger.info("Current status: " + currentStatus);

    }
}

class NetworkUtils {

    /**
     * 发送Wake-on-LAN魔术包
     * 
     * @param mac MAC地址 (格式: "00:11:22:33:44:55" 或 "00-11-22-33-44-55")
     */
    public static void wakeOnLan(String mac) throws IOException {
        // 清理MAC地址格式
        String cleanMac = mac.replaceAll("[:.-]", "");

        // 构建魔术包 (6×0xFF + 16×MAC地址)
        byte[] macBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            macBytes[i] = (byte) Integer.parseInt(cleanMac.substring(i * 2, i * 2 + 2), 16);
        }

        byte[] magicPacket = new byte[102];
        for (int i = 0; i < 6; i++) {
            magicPacket[i] = (byte) 0xFF;
        }
        for (int i = 6; i < magicPacket.length; i += 6) {
            System.arraycopy(macBytes, 0, magicPacket, i, 6);
        }

        // 发送到广播地址
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(
                    magicPacket, magicPacket.length,
                    InetAddress.getByName("255.255.255.255"), 9);
            socket.send(packet);
        }
    }

    /**
     * 测试网络连通性
     * 
     * @param ip      目标IP地址或域名
     * @param timeout 超时时间(毫秒)
     * @return 是否可达
     */
    public static boolean ping(String ip, int timeout) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isReachable(timeout);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 测试TCP端口连通性
     * 
     * @param host    主机地址
     * @param port    端口号
     * @param timeout 超时时间(毫秒)
     * @return 是否可连接
     */
    public static boolean tcping(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检测 Minecraft 服务器是否在线并返回状态
     * 
     * @param host    服务器主机地址
     * @param port    服务器端口号
     * @param timeout 超时时间（毫秒）
     * @return 服务器是否在线
     */
    public static boolean mcPing(String host, int port, int timeout) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeout);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            // Protocol version (763 for Minecraft 1.20.4)
            int protocolVersion = 763;
            // Next state: 1 for Status
            int nextState = 1;

            // Build handshake packet
            ByteArrayOutputStream handshakeBuffer = new ByteArrayOutputStream();
            writeVarInt(handshakeBuffer, 0x00); // Handshake packet ID
            writeVarInt(handshakeBuffer, protocolVersion);
            writeVarInt(handshakeBuffer, host.length());
            handshakeBuffer.write(host.getBytes("UTF-8"));
            handshakeBuffer.write((port >> 8) & 0xFF);
            handshakeBuffer.write(port & 0xFF);
            writeVarInt(handshakeBuffer, nextState);

            // Write handshake packet with length prefix
            byte[] handshakeData = handshakeBuffer.toByteArray();
            ByteArrayOutputStream fullHandshake = new ByteArrayOutputStream();
            writeVarInt(fullHandshake, handshakeData.length);
            fullHandshake.write(handshakeData);
            output.write(fullHandshake.toByteArray());
            output.flush();

            // Send status request packet
            ByteArrayOutputStream statusRequest = new ByteArrayOutputStream();
            writeVarInt(statusRequest, 1); // Length
            statusRequest.write(0x00); // Packet ID
            output.write(statusRequest.toByteArray());
            output.flush();

            // Read response
            int responseLength = readVarInt(input);
            if (responseLength < 0) {
                return false;
            }

            int packetId = readVarInt(input);
            if (packetId != 0x00) {
                return false;
            }

            // Read the actual response data
            byte[] responseData = new byte[responseLength - getVarIntLength(packetId)];
            input.readFully(responseData);

            // If we successfully read the response, the server is online
            return true;

        } catch (IOException e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore close exception
                }
            }
        }
    }

    /**
     * 写入 VarInt 到输出流
     */
    private static void writeVarInt(ByteArrayOutputStream buffer, int value) {
        while (true) {
            if ((value & 0xFFFFFF80) == 0) {
                buffer.write(value);
                return;
            }
            buffer.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    /**
     * 从输入流读取 VarInt
     */
    private static int readVarInt(DataInputStream input) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = input.readByte();
            value |= (currentByte & 0x7F) << position;

            if (position >= 32) {
                throw new RuntimeException("VarInt is too big");
            }

            if ((currentByte & 0x80) == 0) {
                break;
            }

            position += 7;
        }

        return value;
    }

    /**
     * 获取 VarInt 编码后的字节长度（用于计算响应数据长度）
     */
    private static int getVarIntLength(int value) {
        if ((value & 0xFFFFFF80) == 0)
            return 1;
        if ((value & 0xFFFFC000) == 0)
            return 2;
        if ((value & 0xFFE00000) == 0)
            return 3;
        if ((value & 0xF0000000) == 0)
            return 4;
        return 5;
    }

}
