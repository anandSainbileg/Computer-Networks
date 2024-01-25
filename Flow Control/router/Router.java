import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.*;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

// Author Anand Sainbileg 20327050
public class Router extends Node {
	static final int DEFAULT_PORT = 50000;
    private RoutingTable routingTable;
    InetAddress previousSender;
    InetAddress localAddress;
    InetAddress secondLocalAddress;

	Router(int srcPort) throws SocketException {
        socket = new DatagramSocket(srcPort);
        routingTable = new RoutingTable();
        previousSender = null;
        localAddress = getLocalAddress();
        List<InetAddress> otherAddress = getNonMatchingAddresses(localAddress);
        secondLocalAddress = otherAddress.get(0);
		listener.go();
    }
	
	public synchronized void onReceipt(DatagramPacket packet) {
        try {
                previousSender = packet.getAddress();
                byte[] buffer = packet.getData();
                String sourceOfSender = decodeSource(buffer);
                if(!previousSender.equals(localAddress) && !previousSender.equals(secondLocalAddress)){
                    if(buffer[0] == 1){ //search
                        System.out.println("Received packet from " + packet.getSocketAddress());
                        List<InetAddress> nonMatchingAddresses = getNonMatchingAddresses(previousSender);
                        InetAddress broadcastAddress = getNextBroadcastAddress(nonMatchingAddresses.get(0));
                        packet.setSocketAddress(new InetSocketAddress(broadcastAddress, DEFAULT_PORT));
                        System.out.println("Sent packet to: " + broadcastAddress.getHostAddress());
                        socket.setBroadcast(true);
                        socket.send(packet);
                        String nextHop = previousSender.getHostAddress();
                        long timer = 10;  
                        routingTable.addEntry(sourceOfSender, nextHop, timer);
                    }   
                    else if(buffer[0] == 2){    //Acknowledgement
                        socket.setBroadcast(false);
                        System.out.println("Received packet from " + packet.getSocketAddress());
                        String clientId = decodeDestination(buffer);
                        String nextHopAddressStr = routingTable.getNextHop(clientId);
                        InetAddress nextHopAddress = InetAddress.getByName(nextHopAddressStr);
                        packet.setSocketAddress(new InetSocketAddress(nextHopAddress, DEFAULT_PORT));
                        System.out.println("Sent packet to: " + nextHopAddress.getHostAddress());
                        socket.send(packet);
                        String nextHop = previousSender.getHostAddress();
                        long timer = 10;
                        routingTable.addEntry(sourceOfSender, nextHop, timer);     
                    }
                    else if(buffer[0] == 3){        //stream
                        System.out.println("Received packet from " + packet.getSocketAddress());
                        String serverId = decodeDestination(buffer);
                        String nextHopAddressStr = routingTable.getNextHop(serverId);
                        InetAddress nextHopAddress = InetAddress.getByName(nextHopAddressStr);
                        packet.setSocketAddress(new InetSocketAddress(nextHopAddress, DEFAULT_PORT));
                        System.out.println("Sent packet to: " + nextHopAddress.getHostAddress());
                        socket.send(packet);
                        String nextHop = previousSender.getHostAddress();
                        long timer = 10;
                        routingTable.addEntry(sourceOfSender, nextHop, timer);
                    }
                    else if(buffer[0] == 4){        //removal of entry
                        System.out.println("REMOVAL PACKET from: " + packet.getSocketAddress());
                        List<InetAddress> nonMatchingAddresses = getNonMatchingAddresses(previousSender);
                        InetAddress broadcastAddress = getNextBroadcastAddress(nonMatchingAddresses.get(0));
                        packet.setSocketAddress(new InetSocketAddress(broadcastAddress, DEFAULT_PORT));
                        System.out.println("Sent removal packet to: " + broadcastAddress.getHostAddress());
                        socket.setBroadcast(true);
                        socket.send(packet);

                        routingTable.removeEntry(sourceOfSender);
                    }
                }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

	public synchronized void start() throws Exception {
        startExpiryCheck();
		this.wait();
	}

    private void startExpiryCheck() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                routingTable.removeExpiredEntries();
            }
        }, 0, 1000); 
    }

    private String decodeDestination(byte[] data) {
        int startIndex = 9;
        if (data.length >= startIndex + 8) {
            return new String(data, startIndex, 8, StandardCharsets.US_ASCII);
        }
        return null; 
    }

    private String decodeSource(byte[] data) {
        if (data.length >= 9) {
            return new String(data, 1, 8, StandardCharsets.US_ASCII);
        }
        return null; 
    }

    public static InetAddress getNextBroadcastAddress(InetAddress nextAddress) throws SocketException {
        if (nextAddress == null) {
            throw new SocketException("Local address not found");
        }
        byte[] ip = nextAddress.getAddress();
        ip[3] = (byte) 255;  // Set the last byte to 255 for the broadcast address
        ip[2] = (byte) 255;  // Set the last byte to 255 for the broadcast address
        try {
            return InetAddress.getByAddress(ip);
        } catch (UnknownHostException e) {
            throw new SocketException("Broadcast address not found");
        }
    }

    public static List<InetAddress> getNonMatchingAddresses(InetAddress toExclude) throws SocketException {
        List<InetAddress> nonMatchingAddresses = new ArrayList<>();
        byte[] toExcludeBytes = toExclude.getAddress();

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface intf : Collections.list(interfaces)) {
            if (intf.isLoopback() || !intf.isUp()) {
                continue;
            }
            for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                    byte[] ipBytes = addr.getAddress();
                    if ((ipBytes[0] & 0xFF) != (toExcludeBytes[0] & 0xFF) || (ipBytes[1] & 0xFF) != (toExcludeBytes[1] & 0xFF)) {
                        nonMatchingAddresses.add(addr);
                    }
                }
            }
        }
        return nonMatchingAddresses;
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
        
			(new Router(DEFAULT_PORT)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}

