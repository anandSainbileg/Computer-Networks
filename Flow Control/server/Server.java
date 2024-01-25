import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

// Author Anand Sainbileg 20327050
public class Server extends Node {
    static final int DEFAULT_SRC_PORT = 50000;
    static final int DEFAULT_DST_PORT = 50000; 
    InetAddress localAddress;
    InetAddress previousSender;
    static final String serverId = System.getenv("SOURCE_ID");

    Server(int srcPort) throws SocketException {
        localAddress = getLocalAddress();
        socket = new DatagramSocket(srcPort);
		listener.go();
    }

	public synchronized void onReceipt(DatagramPacket packet){
	try{
        previousSender = packet.getAddress();
        byte[] data = packet.getData();
        if(!previousSender.equals(localAddress)){
            String destination = decodeDestination(data);
            if(destination.equals(serverId) && (data[0] == 1 || data[0] == 3)){
                System.out.println("Received packet from " + packet.getSocketAddress());
                String clientId = decodeSource(packet.getData());
                String bothId = serverId + clientId;
                byte[] buffer = new byte[1 + bothId.length()];
                buffer[0] = 2; // Acknowledgement
                byte[] asciiBytes = bothId.getBytes();
                System.arraycopy(asciiBytes, 0, buffer, 1, asciiBytes.length);
                DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length, previousSender, DEFAULT_DST_PORT);
                System.out.println("sent packet to: " + previousSender.getHostAddress());
                socket.send(ackPacket);
            }
            else if(data[0] == 4){
                System.out.println("Path no longer exists now");
            }
            }
		}catch(Exception e) {
            e.printStackTrace();
        }
	}

    public synchronized void start() throws Exception {
        this.wait(); 
    }

    private String decodeSource(byte[] data) {
        // Check if the first byte is 2 and the buffer has the required length
        if (data.length >= 9) {
            // Get the next 8 bytes and convert them to ASCII characters
            return new String(data, 1, 8, StandardCharsets.US_ASCII);
        }
        return null; // Return null if conditions are not met
    }

    private String decodeDestination(byte[] data) {
        int startIndex = 9; // This would be 9
        // Check if the buffer has the required length
        if (data.length >= startIndex + 8) {
            // Extract "DDAAAABB" starting from its index
            return new String(data, startIndex, 8, StandardCharsets.US_ASCII);
        }
        return null; // Return null if the buffer is too short
    }

    public static InetAddress getBroadcastAddress() throws SocketException {
        InetAddress localHost = getLocalAddress();
        if (localHost == null) {
            throw new SocketException("Local address not found");
        }
        byte[] ip = localHost.getAddress();
        ip[3] = (byte) 255;  // Set the last byte to 255 for the broadcast address
        ip[2] = (byte) 255;  // Set the last byte to 255 for the broadcast address
        try {
            return InetAddress.getByAddress(ip);
        } catch (UnknownHostException e) {
            throw new SocketException("Broadcast address not found");
        }
    }

     public static InetAddress getLocalAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface intf : Collections.list(interfaces)) {
            if (intf.isLoopback()) {
                continue;
            }
            for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                if (addr.isLoopbackAddress()) {
                    continue;
                }
                if (addr instanceof java.net.Inet4Address) {
                    return addr;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            InetAddress localAddress = getLocalAddress();
            if (localAddress == null) {
                System.out.println("No local address found.");
                return;
            }
        System.out.println("Local address: " + localAddress.getHostAddress());
            new Server( DEFAULT_SRC_PORT).start();
            System.out.println("Client finished execution");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

