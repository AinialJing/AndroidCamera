package com.aniljing.androidcamera;

import android.os.Bundle;

import com.aniljing.androidcamera.databinding.ActivityCameraXactivityBinding;

import androidx.appcompat.app.AppCompatActivity;

public class CameraXActivity extends AppCompatActivity {
    private final String TAG = CameraXActivity.class.getSimpleName();
    private ActivityCameraXactivityBinding mBinding;
    private CameraXWithYUV mXWithYUV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCameraXactivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mXWithYUV = new CameraXWithYUV(this, mBinding.viewFinder);
        mXWithYUV.startCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mXWithYUV != null) {
            mXWithYUV.releaseCamera();
        }
    }
}
