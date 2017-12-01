package com.github.teocci.codesample.udpavstreamer.av;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.github.teocci.codesample.udpavstreamer.interfaces.AudioDataListener;
import com.github.teocci.codesample.udpavstreamer.utils.LogHelper;

import java.nio.ShortBuffer;

import static android.os.Process.THREAD_PRIORITY_URGENT_AUDIO;
import static com.github.teocci.codesample.udpavstreamer.utils.Config.AUDIO_SAMPLE_RATE;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-01
 */

public class AudioRecordRunnable implements Runnable
{
    private final static String TAG = LogHelper.makeLogTag(AudioRecordRunnable.class);

    private AudioRecord audioRecord;
    private AudioDataListener audioDataListener;

    public AudioRecordRunnable(AudioDataListener audioDataListener)
    {
        this.audioDataListener = audioDataListener;
    }

    @Override
    public void run()
    {
        android.os.Process.setThreadPriority(THREAD_PRIORITY_URGENT_AUDIO);

        // Audio
        int bufferSize;
        ShortBuffer audioData;
        int bufferReadResult;

        bufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        audioData = ShortBuffer.allocate(bufferSize);

        LogHelper.e(TAG, "audioRecord.startRecording()");
        audioRecord.startRecording();

        // ffmpeg_audio encoding loop
        while (!Thread.interrupted()) {
            bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
            audioData.limit(bufferReadResult);
            if (bufferReadResult > 0) {
//                LogHelper.e(TAG, "bufferReadResult: " + bufferReadResult);
                if (audioDataListener == null) {
                    audioDataListener.onSampleReady(audioData);
                }
            }
        }

        LogHelper.v(TAG, "AudioThread Finished, release audioRecord");

            /* encoding finish, release recorder */
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;

            LogHelper.v(TAG, "audioRecord released");
        }
    }
}
