package com.tencent.mobility;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.os.Build.VERSION_CODES.M;

public class BaseActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!hasPermissions() && Build.VERSION.SDK_INT >= M) {
            requestPermissions();
        }
    }

    private boolean hasPermissions() {
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        int granted = pm.checkPermission(RECORD_AUDIO, packageName);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(M)
    private void requestPermissions() {
        final String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        boolean showRationale = false;
        for (String perm : permissions) {
            showRationale |= shouldShowRequestPermissionRationale(perm);
        }
        if (!showRationale) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage("使用腾讯出行demo")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (DialogInterface dialog, int which) ->
                        ActivityCompat.requestPermissions(BaseActivity.this,
                                permissions, REQUEST_PERMISSIONS)
                )
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }
}