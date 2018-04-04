package nusfsae.r18telemetry;

import android.util.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class UdpListener extends Thread{

    private static final int DATA_LENGTH = 10;
    private static final int PORT = 8018;
    private DatagramSocket socket;
    private static DatagramPacket udpPacket;
    private static byte[] data;
    private DataStorage dataStorage;

    public UdpListener(DataStorage dataStorage) {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(PORT));
            this.dataStorage = dataStorage;
        } catch (Exception e) {
            Log.e("udpListener", e.toString(), e);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                data = new byte[DATA_LENGTH];
                udpPacket = new DatagramPacket(data, data.length);
                socket.receive(udpPacket);
                dataStorage.insertData(udpPacket.getData());
            } catch (Exception e) {
                Log.e("udpListener", "receive timeout", e);
            }
        }
    }
    //    public static byte[] getLocalIPAddress () {
//        byte ip[]=null;
//        try {
//            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
//                NetworkInterface intf = en.nextElement();
//                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
//                    InetAddress inetAddress = enumIpAddr.nextElement();
//                    if (!inetAddress.isLoopbackAddress()) {
//                        ip= inetAddress.getAddress();
//                    }
//                }
//            }
//        } catch (SocketException ex) {
//            Log.i("SocketException ", ex.toString());
//        }
//        return ip;
//
//    }
}
