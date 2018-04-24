package nusfsae.r18telemetry;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    
    private static final double MAX_BRAKE_PRESSURE = 70.0;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final String AUDIO_RECEIVE_ACTIVE_MESSAGE = "Transmitting:";
    private static final String AUDIO_TRANSMIT_REJECTED_MESSAGE = "Channel in use";

    private AudioStreamThread audioThread ;
    private AudioReceiveThread audioReceiveThread;
    private UdpListener udpListener;
    private DataStorage dataStorage;
    private TcpClient mTcpClient;
    private TcpClient.OnMessageReceived tcpDelegate;

    private Handler dataUpdateHandler;

    private static String transmitting_client_ID = "";
    private static boolean audio_receive_active = false;
    private static boolean audio_transmit_rejected = false;
    private static boolean audio_transmit_enabled = false;
    private static boolean registered = false;
    private static boolean snackbarOff = true;
    private CoordinatorLayout parentView;
    private Snackbar snackbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;
    private Toolbar mToolbar;
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
    private FloatingActionButton fab;
    private SectionsPageAdapter tabManager;
    private ViewPager mViewPager;
    private ArrayList<MyTab> tabList = new ArrayList<>();
    
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
        fab = (FloatingActionButton) findViewById(R.id.fab);
        parentView = (CoordinatorLayout) findViewById(R.id.parentView);
        // Set up the ViewPager and connect it to the Tablayout
//        tabManager = new SectionsPageAdapter(getSupportFragmentManager());
//        mViewPager = (ViewPager) findViewById(R.id.viewPager);
//        setupViewPager(mViewPager,tabManager);
//        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
//        tabLayout.setupWithViewPager(mViewPager);\

        initializeUdpListener();
        initializeSnackBar();
        initializeFAB();
        dataUpdateHandler = new Handler();
        dataUpdateHandler.postDelayed(updateUI, 500);
        initializeAudioStream();
        connectToServer();
    }

    private  void initializeFAB() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(registered && snackbarOff && !audio_transmit_enabled && !audio_receive_active) {
                    new RequestAudioTransmissionTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else if(audio_transmit_enabled) {
                    audio_transmit_enabled = false;
                    deactivateAudioStream();
                    mTcpClient.terminateAudioTransmission();
                }
            }
        });
    }

    // OnMessageReceived() from server implemented here
    private void connectToServer() {
        mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
            @Override
            //here the messageReceived method is implemented
            public void messageReceived(String message) {
                // process the received message from TCP server
                if(message.equals("Registered")) {
                    registered = true;
                } else if(message.equals("Granted")) {   //grant audio access command
                    audio_transmit_enabled = true;
                    Log.e("onMessageReceived","audio_transmit_enabled" );
                } else if(message.equals(AUDIO_TRANSMIT_REJECTED_MESSAGE)) {
                    audio_transmit_rejected = true;
                } else if(message.contains(AUDIO_RECEIVE_ACTIVE_MESSAGE)) {
                    audio_transmit_enabled = false;
                    audio_transmit_rejected = false;
                    audio_receive_active = true;
                    transmitting_client_ID = message;
                }
//                audioReceiveThread = new AudioReceiveThread();
//                audioReceiveThread.start();
            }
        });
        new ConnectTcpServerTask().execute();
    }

    private void initializeAudioStream() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
    }

    private void activateAudioStream() {
        audioThread = new AudioStreamThread();
        audioThread.start();
    }

    private void deactivateAudioStream() {
        audioThread.interrupt();
    }

    private void initializeSnackBar() {
        snackbar = Snackbar.make(parentView, "Transmitting", Snackbar.LENGTH_INDEFINITE);
    }

    private void initializeUdpListener() {
        dataStorage = new DataStorage();
        udpListener = new UdpListener(dataStorage);
        udpListener.setPriority(Thread.NORM_PRIORITY);
        udpListener.start();
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
        if(audioThread != null && audioThread.isAlive()) {
            audioThread.interrupt();
        }
        if(audioReceiveThread != null) {
            audioReceiveThread.interrupt();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mTcpClient != null) {
            mTcpClient.stopClient();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                            String permissions[],
                                            int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //initializeAudioStream();
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

    private Runnable updateUI = new Runnable() {
        //get the current active tab and the tiles
        //call the respective Tile's update function
        @Override
        public void run() {
            if(audio_transmit_enabled) {
                snackbar.make(parentView, "Transmitting", Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("Action", null).show();
                snackbarOff = false;
                fab.setImageResource(R.mipmap.ic_mic_black_24dp);
            } else if(audio_receive_active) {
                snackbar.make(parentView, transmitting_client_ID + " Transmitting", Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("Action", null).show();
                snackbarOff = false;
            } else {
                snackbar.dismiss();
                snackbarOff = true;
                fab.setImageResource(R.mipmap.ic_mic_none_black_24dp);
            }
            //Fragment currentTab = tabManager.getItem(mViewPager.getCurrentItem());
            int currentTp = dataStorage.getThrottleData();
            int currentBrake = dataStorage.getBrakeData();

            speed.setText(Integer.toString(dataStorage.getSpeedData()));
            gear.setText(Integer.toString(dataStorage.getGearData()));
            rpm.setText(Double.toString(dataStorage.getRpmData()));
            tp.setText(Integer.toString(currentTp));
            brake.setText(Integer.toString((int) (currentBrake / MAX_BRAKE_PRESSURE * 100)));
            airTemp.setText(Integer.toString(dataStorage.getAirTemp()));
            oilTemp.setText(Integer.toString(dataStorage.getOilTempData()));
            engTemp.setText(Integer.toString(dataStorage.getEngTempData()));
            battery.setText(Double.toString(dataStorage.getbatteryVoltsData()));
            oilPress.setText(Integer.toString(dataStorage.getOilPressureData()));
            fuelPress.setText(Integer.toString(dataStorage.getFuelPressure()));
            brakePress.setText(Double.toString(currentBrake / 10.0));
            brakeTemp.setText(Integer.toString(dataStorage.getBrakeTempData()));
            brakeBias.setText(Integer.toString(dataStorage.getBrakeBiasData()));
            tyreTempRRI.setText(Integer.toString(dataStorage.getTyreTempRRI()));
            tyreTempRRO.setText(Integer.toString(dataStorage.getTyreTempRRO()));
            tpProgress.setProgress(currentTp);
            brakeProgress.setProgress((int) (currentBrake / MAX_BRAKE_PRESSURE * 100));
            dataUpdateHandler.post(this);
        }
    };

    private class RequestAudioTransmissionTask extends AsyncTask<String, String, Boolean> {
        @Override
        protected Boolean doInBackground(String... message) {
            mTcpClient.requestAudioTransmission();
            while(!audio_transmit_enabled && !audio_transmit_rejected); // wait for acknowledgement
            if(audio_transmit_enabled) {
                activateAudioStream();
            }
            // TODO : audio transmit rejected
            return false;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

        }
    }

    private class ConnectTcpServerTask extends AsyncTask<String, String, TcpClient> {
        @Override
        protected TcpClient doInBackground(String... message) {
            while(dataStorage.getServerIP().isEmpty()); // wait for serverIP to be received
            mTcpClient.setServerIP(dataStorage.getServerIP());
            mTcpClient.run();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("test", "response " + values[0]);
            //process server response here....

        }
    }

}

