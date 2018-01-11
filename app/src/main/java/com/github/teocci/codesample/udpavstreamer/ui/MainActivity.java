package com.github.teocci.codesample.udpavstreamer.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.github.teocci.codesample.udpavstreamer.R;
import com.github.teocci.codesample.udpavstreamer.utils.LogHelper;

import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Feb-02
 */
public class MainActivity extends AppCompatActivity
{
    private final static String TAG = LogHelper.makeLogTag(MainActivity.class);
    private boolean permissionReady;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermissions();
        initEventHandlers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        Map<String, Integer> perm = new HashMap<>();
        perm.put(CAMERA, PERMISSION_DENIED);
        perm.put(WRITE_EXTERNAL_STORAGE, PERMISSION_DENIED);
        for (int i = 0; i < permissions.length; i++) {
            perm.put(permissions[i], grantResults[i]);
        }
        if (perm.get(CAMERA) == PERMISSION_GRANTED
                && perm.get(WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            permissionReady = true;
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)
                    || !ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.permission_warning)
                        .setPositiveButton(R.string.dismiss, null)
                        .show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initPermissions()
    {
        int cameraPermission = ContextCompat.checkSelfPermission(this, CAMERA);
        int storagePermission = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
        permissionReady = cameraPermission == PERMISSION_GRANTED && storagePermission == PERMISSION_GRANTED;
        if (!permissionReady) {
            requirePermissions();
        }
    }

    private void initEventHandlers()
    {
        findViewById(R.id.btnRecord).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (permissionReady) {
                    startActivity(new Intent(MainActivity.this, RecordActivity.class));
                }
            }
        });

        findViewById(R.id.btnStream).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (permissionReady) {
                    startActivity(new Intent(MainActivity.this, StreamActivity.class));
                }
            }
        });

        findViewById(R.id.btnOpenCv).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (permissionReady) {
                    startActivity(new Intent(MainActivity.this, OpenCvActivity.class));
                }
            }
        });
    }

    private void requirePermissions()
    {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, 11);
    }
}