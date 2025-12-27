package io.github.ant_cave.eco_wake_sleep_sub.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NetworkUtils {

    /**
     * 发送Wake-on-LAN魔术包
     * 
     * @param mac MAC地址 (格式: "00:11:22:33:44:55" 或 "00-11-22-33-44-55")
     */
    public static void wakeOnLan(String mac) throws IOException {
        String cleanMac = mac.replaceAll("[:.-]", "");

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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> future = executor.submit(() -> mcPingInternal(host, port));
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean mcPingInternal(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            int protocolVersion = 763;
            int nextState = 1;

            ByteArrayOutputStream handshakeBuffer = new ByteArrayOutputStream();
            writeVarInt(handshakeBuffer, 0x00);
            writeVarInt(handshakeBuffer, protocolVersion);
            writeVarInt(handshakeBuffer, host.length());
            handshakeBuffer.write(host.getBytes("UTF-8"));
            handshakeBuffer.write((port >> 8) & 0xFF);
            handshakeBuffer.write(port & 0xFF);
            writeVarInt(handshakeBuffer, nextState);

            byte[] handshakeData = handshakeBuffer.toByteArray();
            ByteArrayOutputStream fullHandshake = new ByteArrayOutputStream();
            writeVarInt(fullHandshake, handshakeData.length);
            fullHandshake.write(handshakeData);
            output.write(fullHandshake.toByteArray());
            output.flush();

            ByteArrayOutputStream statusRequest = new ByteArrayOutputStream();
            writeVarInt(statusRequest, 1);
            statusRequest.write(0x00);
            output.write(statusRequest.toByteArray());
            output.flush();

            int responseLength = readVarInt(input);
            if (responseLength < 0) {
                return false;
            }

            int packetId = readVarInt(input);
            if (packetId != 0x00) {
                return false;
            }

            byte[] responseData = new byte[responseLength - getVarIntLength(packetId)];
            input.readFully(responseData);

            return true;

        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
        }
    }

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
