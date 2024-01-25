import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileInputStream;

// Author Anand Sainbileg 20327050
public class Client extends Node {
    static final int DEFAULT_SRC_PORT = 50000;
    static final int DEFAULT_DST_PORT = 50000; // Router's port
    static final String clientId = System.getenv("SOURCE_ID");
    static final String endpointId = System.getenv("DEST_ID");
    InetAddress localAddress;
    InetAddress previousSender;
    private List<File> files = new ArrayList<>();
    private int currentFileIndex = 0;

    public Client(int srcPort, String directoryPath) throws SocketException {
        localAddress = getLocalAddress();
        socket = new DatagramSocket(srcPort);
        listener.go();
        socket.setBroadcast(true);

        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] directoryFiles = directory.listFiles();
            if (directoryFiles != null) {
                for (File file : directoryFiles) {
                    if (file.isFile()) {
                        files.add(file);
                    }
                }
            }
        } else {
            System.err.println("Provided path is not a directory or does not exist: " + directoryPath);
        }
    }

	public synchronized void onReceipt(DatagramPacket packet) {
        try {
            previousSender = packet.getAddress();
            if (!previousSender.equals(localAddress) && packet.getData()[0] == 2) {
                socket.setBroadcast(false);
                System.out.println("Acknowldegdement");
                sendFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendFile() throws Exception{
    if (currentFileIndex < files.size()) {
        File fileToSend = files.get(currentFileIndex++);
        byte[] fileContent = readFileContent(fileToSend);
        if (fileContent != null) {
            try {
                String bothId = clientId + endpointId;
                byte[] bothIdBytes = bothId.getBytes(StandardCharsets.UTF_8);
                byte[] buffer = new byte[1 + bothIdBytes.length + fileContent.length];
                buffer[0] = 3;
                System.arraycopy(bothIdBytes, 0, buffer, 1, bothIdBytes.length);
                System.arraycopy(fileContent, 0, buffer, 1 + bothIdBytes.length, fileContent.length);
                DatagramPacket filePacket = new DatagramPacket(buffer, buffer.length, previousSender, DEFAULT_DST_PORT);
                socket.send(filePacket);
                System.out.println("Sent file: " + fileToSend.getName() + " to: " + previousSender.getHostAddress());
                TimeUnit.SECONDS.sleep(1); 
            } catch (IOException | InterruptedException e) {
                System.err.println("Error sending the file: " + fileToSend.getName());
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    } else {
            System.out.println("No more files now we remove the path");
            String bothId = clientId + endpointId;
            byte[] buffer = new byte[1 + bothId.length()];
            buffer[0] = 4; // Removal
            byte[] asciiBytes = bothId.getBytes();
            System.arraycopy(asciiBytes, 0, buffer, 1, asciiBytes.length);
            InetAddress localBroadcast = getBroadcastAddress();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, localBroadcast, DEFAULT_DST_PORT);
            System.out.println("Removal packet sent to: " + previousSender.getHostAddress());
            TimeUnit.SECONDS.sleep(2); 
            socket.setBroadcast(true);
            socket.send(packet);
        }
    }

    public synchronized void start() throws Exception {
        String bothId = clientId + endpointId;
        byte[] buffer = new byte[1 + bothId.length()];
        buffer[0] = 1; // Set the first byte to 0
        byte[] asciiBytes = bothId.getBytes();
        System.arraycopy(asciiBytes, 0, buffer, 1, asciiBytes.length);
        InetAddress localBroadcast = getBroadcastAddress();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, localBroadcast, DEFAULT_DST_PORT);
        System.out.println("Broadcasted packet to: " + localBroadcast.getHostAddress());
        TimeUnit.SECONDS.sleep(5);
        socket.send(packet);
        this.wait();
    }


private byte[] readFileContent(File file) {
    try (FileInputStream fis = new FileInputStream(file)) {
        byte[] fileContent = new byte[(int) file.length()];
        int bytesRead = fis.read(fileContent);
        if (bytesRead == fileContent.length) {
            return fileContent;
        } else {
            System.err.println("Incomplete read for file: " + file.getName());
        }
    } catch (IOException e) {
        System.err.println("Error reading the file: " + file.getName());
        e.printStackTrace();
    }
    return null;
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
            // Use the broadcast IP to send to all nodes in the subnet
            String directoryPath = "./files";
            new Client(DEFAULT_SRC_PORT, directoryPath).start();
            System.out.println("Client finished execution");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

