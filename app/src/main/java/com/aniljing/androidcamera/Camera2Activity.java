package com.aniljing.androidcamera;

import android.os.Bundle;

import com.aniljing.androidcamera.databinding.ActivityCamera2Binding;

import androidx.appcompat.app.AppCompatActivity;

public class Camera2Activity extends AppCompatActivity {
    private final String TAG = Camera2Activity.class.getSimpleName();
    private ActivityCamera2Binding mBinding;
    private Camera2WithYUV mCamera2WithYUV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCamera2Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mCamera2WithYUV = new Camera2WithYUV(Camera2Activity.this);
        mCamera2WithYUV.initTexture(mBinding.preview);
        mCamera2WithYUV.setYUVDataCallBack((data, width, height, orientation) -> {

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCamera2WithYUV != null) {
            mCamera2WithYUV.releaseCamera();
        }
    }
}