package nusfsae.r18telemetry;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Process;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

import com.score.rahasak.utils.OpusDecoder;
import com.score.rahasak.utils.OpusEncoder;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PORT = 8018;
    private static final double MAX_BRAKE_PRESSURE = 70.0;
    private DatagramSocket socket;
    private static DatagramPacket udpPacket;
    private static byte[] data;
    private static final int DATA_LENGTH = 10;
    private Handler dataUpdateHandler;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;
    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private TextView speed;
    private TextView gear;
    private TextView rpm;
    private TextView tp;
    private ProgressBar tpProgress;
    private TextView brake;
    private ProgressBar brakeProgress;
    private TextView airTemp;
    private TextView oilTemp;
    private TextView engTemp;
    private TextView battery;
    private TextView oilPress;
    private TextView fuelPress;
    private TextView brakePress;
    private TextView brakeTemp;
    private TextView brakeBias;
    private TextView tyreTempRRI;
    private TextView tyreTempRRO;
    private SectionsPageAdapter tabManager;
    private ArrayList<MyTab> tabList = new ArrayList<>();
    private DataStorage dataStoreMan = new DataStorage();
    private AudioThread audioThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup drawer and toolbar
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close);
        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Linkup all the data views
        speed = (TextView) findViewById(R.id.speed);
        gear = (TextView) findViewById(R.id.gear);
        rpm = (TextView) findViewById(R.id.rpm);
        tp = (TextView) findViewById(R.id.tp);
        brake = (TextView) findViewById(R.id.brake);
        airTemp = (TextView) findViewById(R.id.airTemp);
        oilTemp = (TextView) findViewById(R.id.oilTemp);
        engTemp = (TextView) findViewById(R.id.engTemp);
        battery = (TextView) findViewById(R.id.batteryVolts);
        oilPress = (TextView) findViewById(R.id.oilPressure);
        fuelPress = (TextView) findViewById(R.id.fuelPressure);
        brakePress = (TextView) findViewById(R.id.brakePressureFront);
        brakeTemp = (TextView) findViewById(R.id.brakeTemp);
        brakeBias = (TextView) findViewById(R.id.brakeBias);
        tyreTempRRI = (TextView) findViewById(R.id.tyreTempRRI);
        tyreTempRRO = (TextView) findViewById(R.id.tyreTempRRO);
        tpProgress = (ProgressBar) findViewById(R.id.tpProgressBar);
        brakeProgress = (ProgressBar) findViewById(R.id.brakeProgressBar);
        // Set up the ViewPager and connect it to the Tablayout
//        tabManager = new SectionsPageAdapter(getSupportFragmentManager());
//        mViewPager = (ViewPager) findViewById(R.id.viewPager);
//        setupViewPager(mViewPager,tabManager);
//        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
//        tabLayout.setupWithViewPager(mViewPager);\

        // Start background UDP listener
        initializeUdpListener();
        initializeAudioStream();
        dataUpdateHandler = new Handler();
        dataUpdateHandler.postDelayed(updateUI, 500);
    }

    private Runnable updateUI = new Runnable() {
        //get the current active tab and the tiles
        //call the respective Tile's update function
        @Override
        public void run() {
            //Fragment currentTab = tabManager.getItem(mViewPager.getCurrentItem());
            int currentTp = dataStoreMan.getThrottleData();
            int currentBrake = dataStoreMan.getBrakeData();

            speed.setText(Integer.toString(dataStoreMan.getSpeedData()));
            gear.setText(Integer.toString(dataStoreMan.getGearData()));
            rpm.setText(Double.toString(dataStoreMan.getRpmData()));
            tp.setText(Integer.toString(currentTp));
            brake.setText(Integer.toString((int) (currentBrake / MAX_BRAKE_PRESSURE * 100)));
            airTemp.setText(Integer.toString(dataStoreMan.getAirTemp()));
            oilTemp.setText(Integer.toString(dataStoreMan.getOilTempData()));
            engTemp.setText(Integer.toString(dataStoreMan.getEngTempData()));
            battery.setText(Double.toString(dataStoreMan.getbatteryVoltsData()));
            oilPress.setText(Integer.toString(dataStoreMan.getOilPressureData()));
            fuelPress.setText(Integer.toString(dataStoreMan.getFuelPressure()));
            brakePress.setText(Double.toString(currentBrake / 10.0));
            brakeTemp.setText(Integer.toString(dataStoreMan.getBrakeTempData()));
            brakeBias.setText(Integer.toString(dataStoreMan.getBrakeBiasData()));
            tyreTempRRI.setText(Integer.toString(dataStoreMan.getTyreTempRRI()));
            tyreTempRRO.setText(Integer.toString(dataStoreMan.getTyreTempRRO()));
            tpProgress.setProgress(currentTp);
            brakeProgress.setProgress((int) (currentBrake / MAX_BRAKE_PRESSURE * 100));
            dataUpdateHandler.post(this);
        }
    };

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

    private void initializeAudioStream() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        audioThread = new AudioThread();
        audioThread.start();
    }

    private void initializeUdpListener() {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(PORT));
        } catch (Exception e) {
            Log.e("udpListener", e.toString(), e);
        }
        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        data = new byte[DATA_LENGTH];
                        udpPacket = new DatagramPacket(data, data.length);
                        socket.receive(udpPacket);
                        dataStoreMan.insertData(udpPacket.getData());
                    } catch (Exception e) {
                        Log.e("udpListener", "receive timeout", e);
                    }
                }
            }
        });
        networkThread.setPriority(Thread.NORM_PRIORITY);
        networkThread.start();
    }

    // Connects the ViewPager with the Fragment manager (adapter)
    private void setupViewPager(ViewPager viewPager, SectionsPageAdapter tabManager) {
//        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        tabList.add(new MyTab().setLayout(R.layout.driver_tab).setTitle(getApplicationContext().getString(R.string.Tab1)));
        tabList.add(new MyTab().setLayout(R.layout.engine_tab).setTitle(getApplicationContext().getString(R.string.Tab2)));
        tabList.add(new MyTab().setLayout(R.layout.suspension_tab).setTitle(getApplicationContext().getString(R.string.Tab3)));
        for (int i = 0; i < 3; ++i) {
            tabManager.addFragment(tabList.get(i), tabList.get(i).getTitle());
        }
        viewPager.setAdapter(tabManager);
    }

    @Override
    protected void onPause() {
        super.onPause();
        audioThread.interrupt();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                            String permissions[],
                                            int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeAudioStream();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // hamburger toggle button to activate the navigation drawer
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class AudioThread extends Thread {
        // Sample rate must be one supported by Opus.
        private static final int SAMPLE_RATE = 8000;

        // Number of samples per frame is not arbitrary,
        // it must match one of the predefined values, specified in the standard.
        private static final int FRAME_SIZE = 160;

        // 1 or 2
        private static final int NUM_CHANNELS = 1;

        private static final int AUDIO_PORT = 5002;

        private DatagramSocket audioSocket;
        private InetAddress remoteIP;

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
            int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

            // initialize audio recorder
            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize);

            // init opus encoder
            OpusEncoder encoder = new OpusEncoder();
            encoder.init(SAMPLE_RATE, NUM_CHANNELS, OpusEncoder.OPUS_APPLICATION_VOIP);

            try {
                remoteIP = InetAddress.getByName("255.255.255.255");
                audioSocket = new DatagramSocket(null);
                audioSocket.setReuseAddress(true);
                audioSocket.setBroadcast(true);
            } catch (Exception e) {
                Log.e("audio_socket", e.toString(), e);
            }

            recorder.startRecording();

            byte[] inBuf = new byte[FRAME_SIZE * NUM_CHANNELS * 2];
            byte[] encBuf = new byte[1024];

            try {
                while (true) {
                    // Encoder must be fed entire frames.
                    int to_read = inBuf.length;
                    int offset = 0;
                    while (to_read > 0) {
                        int read = recorder.read(inBuf, offset, to_read);
                        if (read < 0) {
                            throw new RuntimeException("recorder.read() returned error " + read);
                        }
                        to_read -= read;
                        offset += read;
                    }
                    int encoded = encoder.encode(inBuf, FRAME_SIZE, encBuf);
                    Log.v("Opus", "Encoded " + inBuf.length + " bytes of audio into " + encoded + " bytes");
                    //byte[] encData = Arrays.copyOf(encBuf, encoded);
                    DatagramPacket audioPacket = new DatagramPacket(encBuf, encBuf.length,remoteIP,AUDIO_PORT);
                    audioSocket.send(audioPacket);
                }
            } catch (Exception e) {
                Log.e("audio_socket",e.toString(),e);
            }
        }
    }
}

