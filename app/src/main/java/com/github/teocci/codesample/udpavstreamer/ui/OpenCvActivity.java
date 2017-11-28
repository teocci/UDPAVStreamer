package com.github.teocci.codesample.udpavstreamer.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.github.teocci.codesample.udpavstreamer.R;
import com.github.teocci.codesample.udpavstreamer.av.CustomCameraPreview;
import com.github.teocci.codesample.udpavstreamer.utils.LogHelper;
import com.github.teocci.codesample.udpavstreamer.utils.StorageHelper;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Size;

import static org.bytedeco.javacpp.opencv_core.LINE_8;
import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Feb-02
 */
public class OpenCvActivity extends Activity implements CustomCameraPreview.CvCameraViewListener
{
    private final static String TAG = LogHelper.makeLogTag(OpenCvActivity.class);

    private CascadeClassifier faceDetector;
    private int absoluteFaceSize = 0;
    private CustomCameraPreview cameraView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_opencv);

        cameraView = (CustomCameraPreview) findViewById(R.id.camera_view);
        cameraView.setCvCameraViewListener(this);

        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... voids)
            {
                faceDetector = StorageHelper.loadClassifierCascade(OpenCvActivity.this, R.raw.frontalface);
                return null;
            }
        }.execute();
    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        absoluteFaceSize = (int) (width * 0.32f);
    }

    @Override
    public void onCameraViewStopped()
    {

    }

    @Override
    public Mat onCameraFrame(Mat rgbaMat)
    {
        if (faceDetector != null) {
            Mat grayMat = new Mat(rgbaMat.rows(), rgbaMat.cols());

            cvtColor(rgbaMat, grayMat, CV_BGR2GRAY);

            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(grayMat, faces, 1.25f, 3, 1,
                    new Size(absoluteFaceSize, absoluteFaceSize),
                    new Size(4 * absoluteFaceSize, 4 * absoluteFaceSize));
            if (faces.size() == 1) {
                int x = faces.get(0).x();
                int y = faces.get(0).y();
                int w = faces.get(0).width();
                int h = faces.get(0).height();
                rectangle(rgbaMat, new Point(x, y), new Point(x + w, y + h), opencv_core.Scalar.GREEN, 2, LINE_8, 0);
            }

            grayMat.release();
        }

        return rgbaMat;
    }
}
