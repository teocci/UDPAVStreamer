package com.github.teocci.codesample.udpavstreamer.ui;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import com.github.teocci.codesample.udpavstreamer.av.CustomCameraView;
import com.github.teocci.codesample.udpavstreamer.interfaces.CustomCameraViewListener;
import com.github.teocci.codesample.udpavstreamer.utils.LogHelper;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_MPEG4;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Feb-02
 */
public class RecordActivity extends Activity implements OnClickListener, CustomCameraViewListener
{
    private final static String TAG = LogHelper.makeLogTag(RecordActivity.class);
    private final static String CLASS_LABEL = RecordActivity.class.getName();

    private PowerManager.WakeLock wakeLock;
    private boolean recording;
    private CustomCameraView cameraView;
    private Button btnRecorderControl;
    private File savePath = new File(Environment.getExternalStorageDirectory(), "stream.mp4");
    private FFmpegFrameRecorder recorder;
    private long startTime = 0;
    private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
    private final Object semaphore = new Object();

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
            Toast.makeText(this, "Video file was saved to \"" + savePath + "\"", Toast.LENGTH_LONG).show();
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

    private void initPartialWakeLock()
    {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, CLASS_LABEL);
            wakeLock.acquire();
        }
    }

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
        LogHelper.i(TAG, "init recorder with width = " + width + " and height = " + height + " and degree = "
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

        LogHelper.i(TAG, "saved file path: " + savePath.getAbsolutePath());
        recorder = new FFmpegFrameRecorder(savePath, frameWidth, frameHeight, 0);
        recorder.setFormat("mp4");
        recorder.setVideoCodec(AV_CODEC_ID_MPEG4);
        recorder.setVideoQuality(1);
        // Set in the surface changed method
        recorder.setFrameRate(16);

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
            }
            startTime = System.currentTimeMillis();
            recording = true;
        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording()
    {
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