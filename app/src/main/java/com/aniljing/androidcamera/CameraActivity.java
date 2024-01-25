package com.aniljing.androidcamera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import com.aniljing.androidcamera.databinding.ActivityCameraBinding;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity {
    private final String TAG = CameraActivity.class.getSimpleName();
    private Context mContext;
    private ActivityCameraBinding mBinding;
    private Camera mCamera;
    private File mFile = new File(Environment.getExternalStorageDirectory(), "libyuv_rotated.yuv");
    private BufferedOutputStream bos;
    private YuvUtil mYuvUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mYuvUtil = new YuvUtil();
        mBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        try {
            if (mFile.exists()) {
                mFile.delete();
            }
            bos = new BufferedOutputStream(new FileOutputStream(mFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        mBinding.preview.getHolder().addCallback(new SurfaceHolder.Callback2() {
            @Override
            public void surfaceRedrawNeeded(@NonNull SurfaceHolder holder) {
                mCamera = Camera.open();
                try {
                    mCamera.setPreviewDisplay(holder);
                    Camera.Parameters parameters = mCamera.getParameters();
                    // 设置预览尺寸
                    int desiredWidth = 1280; // 设置所需的宽度
                    int desiredHeight = 720; // 设置所需的高度
                    Camera.Size bestSize = getBestPreviewSize(parameters, desiredWidth, desiredHeight);
                    parameters.setPreviewSize(bestSize.width, bestSize.height);
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    parameters.setPictureFormat(ImageFormat.NV21);
                    mCamera.setParameters(parameters);
                    mCamera.setPreviewCallback(new PreviewCallBack());
                    mCamera.setDisplayOrientation(90);
                    mCamera.startPreview();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

    private class PreviewCallBack implements Camera.PreviewCallback {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            try {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                byte[] i420 = new byte[data.length];
                byte[] rotated = new byte[data.length];
                mYuvUtil.nv21ToI420(data,previewSize.width,previewSize.height,i420);
                mYuvUtil.i420Rotate(i420,previewSize.width,previewSize.height,rotated,90);
                bos.write(rotated);
                Log.e(TAG, "onPreviewFrame:" + previewSize.width + "x" + previewSize.height);
                Log.e(TAG, "Image format:" + camera.getParameters().getPictureFormat());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        try {
            bos.flush();
            bos.close();
            bos = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Camera.Size getBestPreviewSize(Camera.Parameters parameters, int desiredWidth, int desiredHeight) {
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
        Camera.Size bestSize = null;
        int bestDiff = Integer.MAX_VALUE;

        for (Camera.Size size : supportedSizes) {
            int diff = Math.abs(size.width - desiredWidth) + Math.abs(size.height - desiredHeight);
            if (diff < bestDiff) {
                bestSize = size;
                bestDiff = diff;
            }
        }

        return bestSize;
    }
}