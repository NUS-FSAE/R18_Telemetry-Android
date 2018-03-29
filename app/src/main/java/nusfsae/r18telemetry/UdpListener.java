package nusfsae.r18telemetry;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpListener extends AsyncTask<Void, Void, Void> {
    private static DataStorage dataStoreMan;
    private static DatagramSocket socket;
    private static DatagramPacket udpPacket;
    private static byte[] data;
    private static final int DATA_LENGTH = 10;

    public UdpListener(DatagramSocket socket, DataStorage dataStoreMan) {
        this.socket = socket;
        this.dataStoreMan = dataStoreMan;
    }

    @Override
    protected void onPreExecute() {
        data = new byte[DATA_LENGTH];
        udpPacket = new DatagramPacket(data,data.length);
    }

    @Override
    protected void onPostExecute(Void params) {

    }

    @Override
    protected Void doInBackground(Void... parms ) {
        try {
            socket.receive(udpPacket);
            dataStoreMan.insertData(udpPacket.getData());
        } catch (Exception e) {
            Log.e("udpListener","receive timeout",e);
        }
        return null;
    }
}
