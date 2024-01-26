package com.aniljing.androidcamera;

import android.os.Environment;
import android.util.Log;
import android.util.Size;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import static androidx.camera.lifecycle.ProcessCameraProvider.getInstance;

public class CameraXWithYUV {
    private final String TAG = CameraXWithYUV.class.getSimpleName();
    private AppCompatActivity mContext;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private YuvUtil mYuvUtil;
    private PreviewView mPreviewView;
    private File mFile = new File(Environment.getExternalStorageDirectory(), "camerax.yuv");
    private BufferedOutputStream bos;
    private byte[] i420;
    private byte[] i420Rotate;

    public CameraXWithYUV(AppCompatActivity context, PreviewView previewView) {
        mContext = context;
        mPreviewView = previewView;
        cameraExecutor = Executors.newSingleThreadExecutor();
        mYuvUtil = new YuvUtil();
        if (mFile.exists()) {
            mFile.delete();
        }
        try {
            bos = new BufferedOutputStream(new FileOutputStream(mFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = getInstance(mContext);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                //预览
                Preview preview = new Preview.Builder().setTargetResolution(new Size(640, 480)).build();
                preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
                //数据分析
                ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .build();
                imageAnalyzer.setAnalyzer(cameraExecutor, new LuminosityAnalyzer());
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        mContext, cameraSelector, preview, imageAnalyzer);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, ContextCompat.getMainExecutor(mContext));
    }

    private class LuminosityAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            Log.e(TAG, "analyze size:" + image.getWidth() + "*" + image.getHeight());
            Log.e(TAG, "analyze image format:" + image.getFormat());
            byte[] nv21 = yuv420ToNv21(image);
            if (i420 == null) {
                i420 = new byte[nv21.length];
            }
            if (i420Rotate == null) {
                i420Rotate = new byte[nv21.length];
            }
            mYuvUtil.nv21ToI420(nv21, image.getWidth(), image.getHeight(), i420);
            mYuvUtil.i420Rotate(i420, image.getWidth(), image.getHeight(), i420Rotate, 90);
            try {
                bos.write(i420Rotate);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            image.close();
        }
    }

    public static byte[] yuv420ToNv21(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        int size = image.getWidth() * image.getHeight();
        byte[] nv21 = new byte[size * 3 / 2];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);

        byte[] u = new byte[uSize];
        uBuffer.get(u);

        //每隔开一位替换V，达到VU交替
        int pos = ySize + 1;
        for (int i = 0; i < uSize; i++) {
            if (i % 2 == 0) {
                nv21[pos] = u[i];
                pos += 2;
            }
        }
        return nv21;
    }

    public void releaseCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
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
