package nusfsae.r18telemetry;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient {

    private static final char REGISTER_USER_BYTE_MESSAGE = 0x7E;
    private static final char REQUEST_AUDIO_TRANSMISSION = 0x8E;
    private static final char TERMINATE_AUDIO_TRANSMISSION = 0XAE;
    private static final String userName = "Test";
    public static final int SERVER_PORT = 1880;
    // serverIp address
    private String serverIP = "192.168.0.100";
    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(OnMessageReceived listener) {
        this.mMessageListener = listener;
    }

    // This method must be called before calling run()
    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    Log.d("TCP Client", "Sending: " + message);
                    mBufferOut.print(message);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void sendByte(final char command) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    Log.d("TCP Client", "Sending: " + command);
                    mBufferOut.print(command);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void requestAudioTransmission() {
        sendByte(REQUEST_AUDIO_TRANSMISSION);
    }

    public void terminateAudioTransmission() {
        sendByte(TERMINATE_AUDIO_TRANSMISSION);
    }

    public void registerUser() {
        sendMessage(REGISTER_USER_BYTE_MESSAGE + userName);
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public void run() {

        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(serverIP);

            Log.e("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, SERVER_PORT);
            socket.setSoTimeout(100);

            try {

                //sends the message to the server
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //receives the message which the server sends back
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                registerUser();

                //in this while the client listens for the messages sent by the server
                while (mRun) {

                    try {
                        mServerMessage = mBufferIn.readLine();
                        if (mServerMessage != null && mMessageListener != null) {
                            //call the method messageReceived from MyActivity class
                            Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");
                            mMessageListener.messageReceived(mServerMessage);
                        }
                    } catch (Exception e) {
                        //Log.e("TCP", "S: Error", e);
                    }
                }
            } catch (Exception e) {

                Log.e("TCP", "S: Error", e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                //socket.close();
            }

        } catch (Exception e) {

            Log.e("TCP", "C: Error", e);

        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

}
