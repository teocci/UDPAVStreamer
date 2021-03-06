package com.github.teocci.codesample.udpavstreamer.ui;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.github.teocci.codesample.udpavstreamer.R;
import com.github.teocci.codesample.udpavstreamer.av.AudioRecordRunnable;
import com.github.teocci.codesample.udpavstreamer.av.CustomCameraView;
import com.github.teocci.codesample.udpavstreamer.interfaces.AudioDataListener;
import com.github.teocci.codesample.udpavstreamer.interfaces.CustomCameraViewListener;
import com.github.teocci.codesample.udpavstreamer.utils.LogHelper;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.nio.ShortBuffer;

import static com.github.teocci.codesample.udpavstreamer.utils.Config.AUDIO_SAMPLE_RATE;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_NONE;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Feb-02
 */
public class StreamActivity extends Activity implements OnClickListener, CustomCameraViewListener, AudioDataListener
{
    private final static String TAG = LogHelper.makeLogTag(StreamActivity.class);
    private final static String CLASS_LABEL = StreamActivity.class.getName();

    private PowerManager.WakeLock wakeLock;
    private boolean recording;
    private CustomCameraView cameraView;
    private Button btnRecorderControl;
    private FFmpegFrameRecorder recorder;
    private long startTime = 0;
    private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
    private final Object semaphore = new Object();

    // Audio data getting thread
    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;
    volatile boolean runAudioThread = true;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_record);

        cameraView = (CustomCameraView) findViewById(R.id.camera_view);

        initPartialWakeLock();

        initLayout();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (wakeLock == null) {
            initPartialWakeLock();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }

        if (recorder != null) {
            try {
                recorder.release();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (recording) {
                stopRecording();
            }

            finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v)
    {
        if (!recording) {
            startRecording();
            recording = true;
            LogHelper.i(TAG, "Start Button Pushed");
            btnRecorderControl.setText("Stop");
            btnRecorderControl.setBackgroundResource(R.drawable.bg_red_circle_button);
        } else {
            // This will trigger the audio recording loop to stop and then set isRecorderStart = false;
            stopRecording();
            recording = false;
            LogHelper.i(TAG, "Stop Button Pushed");
//            btnRecorderControl.setText("Start");
            btnRecorderControl.setVisibility(View.GONE);
            Toast.makeText(this, "Stream stopped", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        initRecorder(width, height);
    }

    @Override
    public void onCameraViewStopped()
    {
        stopRecording();
    }

    @Override
    public Mat onCameraFrame(Mat mat)
    {
        if (audioRecordRunnable == null) {
            startTime = System.currentTimeMillis();
            return mat;
        }
        if (recording && mat != null) {
            synchronized (semaphore) {
                try {
                    Frame frame = converterToMat.convert(mat);
                    long t = 1000 * (System.currentTimeMillis() - startTime);
                    if (t > recorder.getTimestamp()) {
                        recorder.setTimestamp(t);
                    }
                    recorder.record(frame);
                } catch (FFmpegFrameRecorder.Exception e) {
                    LogHelper.i(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return mat;
    }

    @Override
    public void onSampleReady(ShortBuffer audioData)
    {
        if (recorder == null) return;
        if (recording && audioData == null) return;

        try {
            long t = 1000 * (System.currentTimeMillis() - startTime);
            if (t > recorder.getTimestamp()) {
                recorder.setTimestamp(t);
            }
//            LogHelper.e(TAG, "audioData: " + audioData);
            recorder.recordSamples(audioData);
        } catch (FFmpegFrameRecorder.Exception e) {
            LogHelper.v(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private void initPartialWakeLock() {}

    private void initLayout()
    {
        btnRecorderControl = (Button) findViewById(R.id.recorder_control);
        btnRecorderControl.setText("Start");
        btnRecorderControl.setOnClickListener(this);

        cameraView.setCvCameraViewListener(this);
    }

    private void initRecorder(int width, int height)
    {
        int degree = getRotationDegree();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraView.getCameraId(), info);
        boolean isFrontFaceCamera = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        LogHelper.e(TAG, "init recorder with width = " + width + " and height = " + height + " and degree = "
                + degree + " and isFrontFaceCamera = " + isFrontFaceCamera);
        int frameWidth, frameHeight;

        // 0 = 90 CounterClockwise and Vertical Flip (default)
        // 1 = 90 Clockwise
        // 2 = 90 CounterClockwise
        // 3 = 90 Clockwise and Vertical Flip
        switch (degree) {
            case 0:
                frameWidth = width;
                frameHeight = height;
                break;
            case 90:
                frameWidth = height;
                frameHeight = width;
                break;
            case 180:
                frameWidth = width;
                frameHeight = height;
                break;
            case 270:
                frameWidth = height;
                frameHeight = width;
                break;
            default:
                frameWidth = width;
                frameHeight = height;
        }

//        LogHelper.i(TAG, "saved file path: " + savePath.getAbsolutePath());

        String streamURL = "udp://192.168.1.127:8090";
        recorder = new FFmpegFrameRecorder(streamURL, frameWidth, frameHeight, 1);
        recorder.setInterleaved(false);
        // video options //
        recorder.setFormat("mpegts");
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoBitrate(5 * 1024 * 1024);
        recorder.setFrameRate(30);
        recorder.setSampleRate(AUDIO_SAMPLE_RATE);
        recorder.setVideoCodec(AV_CODEC_ID_H264);
        recorder.setAudioCodec(AV_CODEC_ID_AAC);

//        recorder.setOption("protocol_whitelist", "tcp");
//        recorder.setVideoOption("crf", "25");

//        recorder.setFormat("rtp");
//        recorder.setOption("protocol_whitelist", "udp");
//        recorder.setVideoCodec(AV_CODEC_ID_H264);
//        recorder.setAudioCodec(AV_CODEC_ID_AAC);
//        recorder.setFrameRate(30);
//        recorder.setVideoBitrate(1024);

//        recorder.setFormat("mp4");
//        recorder.setVideoCodec(AV_CODEC_ID_MPEG4);
//        recorder.setVideoQuality(1);
//        // Set in the surface changed method
//        recorder.setFrameRate(16);

        audioRecordRunnable = new AudioRecordRunnable(this);
        audioThread = new Thread(audioRecordRunnable);
        runAudioThread = true;

        LogHelper.i(TAG, "recorder initialize success");
    }

    private int getRotationDegree()
    {
        int result;

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        if (Build.VERSION.SDK_INT >= 9) {
            // on >= API 9 we can proceed with the CameraInfo method
            // and also we have to keep in mind that the camera could be the front one
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraView.getCameraId(), info);

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {
                // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
        } else {
            // TODO: on the majority of API 8 devices, this trick works good
            // and doesn't produce an upside-down preview.
            // ... but there is a small amount of devices that don't like it!
            result = Math.abs(degrees - 90);
        }
        return result;
    }

    public void startRecording()
    {
        try {
            synchronized (semaphore) {
                recorder.start();
                audioThread.start();
            }
            recording = true;
        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
            if (recorder == null) {
                LogHelper.e(TAG, "Recorder is null");
                recording = false;
            }
            if (audioThread == null) {
                LogHelper.e(TAG, "AudioThread is null");
                recording = false;
            }
        }
    }

    public void stopRecording()
    {
        if (audioThread == null) return;

        if (audioThread.isAlive()) {
            audioThread.interrupt();
        }

        runAudioThread = false;
        audioRecordRunnable = null;
        audioThread = null;

        if (recorder != null && recording) {
            recording = false;
            LogHelper.i(TAG, "Finishing recording, calling stop and release on recorder");
            try {
                synchronized (semaphore) {
                    recorder.stop();
                    recorder.release();
                }
            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            recorder = null;
        }
    }
}