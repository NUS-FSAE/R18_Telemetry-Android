package nusfsae.r18telemetry;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.util.Log;

import com.score.rahasak.utils.OpusDecoder;
import android.media.AudioTrack;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * Created by FSAE on 06-Apr-18.
 */

public class AudioReceiveThread extends Thread {
    // Sample rate must be one supported by Opus.
    private static final int SAMPLE_RATE = 8000;

    // Number of samples per frame is not arbitrary,
    // it must match one of the predefined values, specified in the standard.
    private static final int FRAME_SIZE = 160;
    // 1 or 2
    private static final int NUM_CHANNELS = 1;

    private static final int AUDIO_PORT = 5002;
    private DatagramSocket audioSocket;
    private AudioTrack track;
    private OpusDecoder decoder;
    private static DatagramPacket udpPacket;
    private static byte[] encodedData;
    private static short[] decodedData;

    public AudioReceiveThread() {
        track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(FRAME_SIZE)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

//        track = new AudioTrack(new AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_MEDIA)
//                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
//                .build(),
//                new AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
//                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
//                        .build(),
//                AudioTrack.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
//                AudioTrack.MODE_STREAM,
//                audioManager.generateAudioSessionId());
        track.play();

        try {
            audioSocket = new DatagramSocket(null);
            audioSocket.setReuseAddress(true);
            audioSocket.setBroadcast(true);
            audioSocket.bind(new InetSocketAddress(AUDIO_PORT));
            Log.i("AudioReceiveThread","thread successfully created");
        } catch (Exception e) {
            Log.e("AudioReceiveThread", e.toString(), e);
        }
    }

    @Override
    public void run() {
        // init opus decoder
        decoder = new OpusDecoder();
        decoder.init(SAMPLE_RATE, NUM_CHANNELS);
        encodedData = new byte[FRAME_SIZE];
        decodedData = new short[FRAME_SIZE];

        while (!this.isInterrupted()) {
            try {
                udpPacket = new DatagramPacket(encodedData, encodedData.length);
                audioSocket.receive(udpPacket);
                int decoded = decoder.decode(udpPacket.getData(), decodedData, FRAME_SIZE);
                track.write(decodedData,0,decoded*NUM_CHANNELS);
            } catch (Exception e) {
                Log.e("AudioReceiveThread", "receive timeout", e);
            }
        }

    }
}
