package com.aniljing.androidcamera;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.aniljing.androidcamera.databinding.ActivityMainBinding;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};


    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (!EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            EasyPermissions.requestPermissions(MainActivity.this, "", 4000, PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    public void openCamera1(View view) {
        startActivity(new Intent(this, CameraActivity.class));
    }

    public void openCamera2(View view) {
        startActivity(new Intent(this, Camera2Activity.class));
    }

    public void openCameraX(View view) {
        startActivity(new Intent(this, CameraXActivity.class));
    }
}