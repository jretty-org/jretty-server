package org.jretty.server.app.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jretty.server.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class PermissionActivity extends AppCompatActivity {
    private static final String TAG = "Jetty";
    private static final int REQUEST_CODE = 1024;
    private boolean hasPermission=true;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermission();

        if(hasPermission) {
            onPermissionGranted();
        } else {
            onPermissionDenied();
        }
    }

    protected abstract void onPermissionDenied();

    protected abstract void onPermissionGranted();

    private String[] getNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
        }
        return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = getDeniedPermissions(getNeededPermissions());
            if (permissions.length != 0) {
                hasPermission = false;
                // Toast.makeText(this, "缺少权限："+ Arrays.deepToString(permissions), Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        permissions,
                        REQUEST_CODE);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!haveAllFilesAccessPermission()) {
                        hasPermission = false;
                        requestAllFilesAccessPermission();
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CODE) {
            return;
        }

        int grantedPermissions = 0;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions++;
            }
        }

        if (grantedPermissions == grantResults.length) {
            hasPermission = true;
            Log.i(TAG, "User granted permission.");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!haveAllFilesAccessPermission()) {
                    hasPermission = false;
                    requestAllFilesAccessPermission();
                }
            }

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                if (!haveAllFilesAccessPermission()) {
//                    requestAllFilesAccessPermission();
//                }
//            }
        } else {
            Log.w(TAG, "User denied permission.");
            Toast.makeText(this, R.string.dialog_storage_permission_not_granted, Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(this, PermissionActivity.class);
//            this.startActivity(intent);
        }
    }

    // 从系统权限设置页返回判断
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!haveAllFilesAccessPermission()) {
                Toast.makeText(this, R.string.dialog_all_files_access_not_supported, Toast.LENGTH_SHORT).show();
            } else {
                hasPermission = true;
            }
        }
    }

    private String[] getDeniedPermissions(String[] permissions) {
        List<String> ret = new ArrayList<String>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ret.add(permission);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    @TargetApi(30)
    private boolean haveAllFilesAccessPermission() {
        return Environment.isExternalStorageManager();
    }

    @TargetApi(30)
    private void requestAllFilesAccessPermission() {
        Boolean intentFailed = false;
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        try {
            ComponentName componentName = intent.resolveActivity(getPackageManager());
            if (componentName != null) {
                String className = componentName.getClassName();
                if (className != null) {
                    // Launch "Allow all files access?" dialog.
                    //startActivity(intent);
                    startActivityForResult(intent, REQUEST_CODE);
                    return;
                }
                intentFailed = true;
            } else {
                Log.w(TAG, "Request all files access not supported");
                intentFailed = true;
            }
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Request all files access not supported", e);
            intentFailed = true;
        }
        if (intentFailed) {
            // Some devices don't support this request.
            Toast.makeText(this, R.string.dialog_all_files_access_not_supported, Toast.LENGTH_LONG).show();
        }
    }


}
