package com.aniljing.androidcamera;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.aniljing.androidcamera.databinding.ActivityCamera2Binding;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;

public class Camera2Activity extends AppCompatActivity {
    private final String TAG = Camera2Activity.class.getSimpleName();
    private ActivityCamera2Binding mBinding;
    private Camera2WithYUV mCamera2WithYUV;
    private File mFile = new File(Environment.getExternalStorageDirectory(), "nopreview.yuv");
    private BufferedOutputStream bos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCamera2Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        try {
            if (mFile.exists()) {
                mFile.delete();
            }
            bos = new BufferedOutputStream(new FileOutputStream(mFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        mCamera2WithYUV = new Camera2WithYUV(Camera2Activity.this);
        mCamera2WithYUV.initTexture();
        mCamera2WithYUV.setYUVDataCallBack((data, width, height, orientation) -> {
            Log.e(TAG, "setYUVDataCallBack:"+width+"x"+height);
            try {
                bos.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCamera2WithYUV != null) {
            mCamera2WithYUV.releaseCamera();
        }
        if (bos != null) {
            try {
                bos.flush();
                bos.close();
                bos = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}