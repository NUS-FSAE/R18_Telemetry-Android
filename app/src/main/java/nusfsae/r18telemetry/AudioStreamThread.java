package nusfsae.r18telemetry;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.score.rahasak.utils.OpusEncoder;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioStreamThread extends Thread {

    // Sample rate must be one supported by Opus.
    private static final int SAMPLE_RATE = 8000;

    // Number of samples per frame is not arbitrary,
    // it must match one of the predefined values, specified in the standard.
    private static final int FRAME_SIZE = 160;

    // 1 or 2
    private static final int NUM_CHANNELS = 1;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private static final int AUDIO_PORT = 5002;

    private AudioRecord recorder;
    private OpusEncoder encoder;
    InetAddress remoteIP;
    DatagramSocket audioSocket;

    public AudioStreamThread(){

        int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        // initialize audio recorder
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize);

        // init opus encoder
        encoder = new OpusEncoder();
        encoder.init(SAMPLE_RATE, NUM_CHANNELS, OpusEncoder.OPUS_APPLICATION_VOIP);

        try {
            remoteIP = InetAddress.getByName("255.255.255.255");
            audioSocket = new DatagramSocket(null);
            audioSocket.setReuseAddress(true);
            audioSocket.setBroadcast(true);
        } catch (Exception e) {
            Log.e("audio_socket", e.toString(), e);
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);

        recorder.startRecording();

        byte[] inBuf = new byte[FRAME_SIZE * NUM_CHANNELS * 2];
        byte[] encBuf = new byte[1024];

        try {
            while (!this.isInterrupted()) {
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
            recorder.stop();
        } catch (Exception e) {
            Log.e("audio_socket",e.toString(),e);
        }
    }
}
