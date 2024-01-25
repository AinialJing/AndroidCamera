package com.aniljing.androidcamera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @ClassName Camera2ProviderPreviewWithYUV
 * Camera2 两路预览：
 * 1、使用TextureView预览，直接输出。
 * 2、使用ImageReader获取数据，输出格式为ImageFormat.YUV_420_888，java端转化为NV21
 */
public class Camera2WithYUV {
    private static final String TAG = Camera2WithYUV.class.getSimpleName();
    private Activity mContext;
    private String mCameraId;
    private HandlerThread handlerThread;
    private Handler mCameraHandler;
    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;
    private Size mPreviewSize;
    private final Point previewViewSize;
    private Range<Integer> fpsRanges;
    private byte[] i420;
    private YUVDataCallBack mYUVDataCallBack;
    private int orientation;
    private YuvUtil mYuvUtil;

    public Camera2WithYUV(Activity mContext) {
        this.mContext = mContext;
        handlerThread = new HandlerThread("camera");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
        previewViewSize = new Point();
        previewViewSize.x = 640;
        previewViewSize.y = 480;
        mYuvUtil=new YuvUtil();
    }

    public void initTexture(TextureView textureView) {
        Log.e(TAG, "initTexture:" + textureView);
        mTextureView = textureView;
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (int i = 0; i < cameraIds.length; i++) {
                //描述相机设备的属性类
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraIds[i]);
                Range<Integer>[] allFpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                fpsRanges = allFpsRanges[allFpsRanges.length - 1];
                //获取是前置还是后置摄像头
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //使用后置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    //寻找一个 最合适的尺寸     ---》 一模一样
                    mPreviewSize = getBestSupportedSize(new ArrayList<Size>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class))));
                    if (map != null) {
                        mCameraId = cameraIds[i];
                    }
                    orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    //maxImages 此时需要2路，一路渲染到屏幕，一路用于网络传输
                    mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                            ImageFormat.YUV_420_888, 2);
                    mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mCameraHandler);
                }
            }
        } catch (CameraAccessException r) {
            Log.e(TAG, "openCamera:" + r);
            Log.e(TAG, "openCamera getMessage:" + r.getMessage());
            Log.e(TAG, "openCamera getLocalizedMessage:" + r.getLocalizedMessage());
        }
    }

    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            try {
                mCameraDevice = camera;
                SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(1280, 720);
                //Surface负责渲染
                Surface previewSurface = new Surface(surfaceTexture);
                //创建请求
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewBuilder.addTarget(previewSurface);
                mPreviewBuilder.addTarget(mImageReader.getSurface());
                //创建会话
                mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), mCaptureSessionStateCallBack, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(TAG, "onOpened:" + e.getMessage());
                Log.e(TAG, "onOpened:" + e.getLocalizedMessage());
                Log.e(TAG, "onOpened:" + e);
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e(TAG, "onDisconnected");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "onError:" + error);
            camera.close();
            mCameraDevice = null;
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @SuppressLint("LongLogTag")
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            // Y:U:V == 4:2:2
            if (mYUVDataCallBack != null && image.getFormat() == ImageFormat.YUV_420_888) {
                Rect crop = image.getCropRect();
                int format = image.getFormat();
                int width = crop.width();
                int height = crop.height();
                if (i420 == null) {
                    i420 = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
                }
                i420 = getI420(image);
                if (mYUVDataCallBack != null) {
                    mYUVDataCallBack.yuvData(i420, width, height, orientation);
                }
            }
            image.close();
        }
    };

    private final CameraCaptureSession.StateCallback mCaptureSessionStateCallBack = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                mCaptureSession = session;
                CaptureRequest request = mPreviewBuilder.build();
                // Finally, we start displaying the camera preview.
                session.setRepeatingRequest(request, null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e(TAG, "onConfigureFailed:");
        }
    };

    public void releaseCamera() {
        Log.e(TAG, "releaseCamera");
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mPreviewBuilder != null) {
            mPreviewBuilder = null;
        }
        if (mImageReader != null) {
            mImageReader.setOnImageAvailableListener(null, null);
            mImageReader.close();
            mImageReader = null;
        }

        if (handlerThread != null && !handlerThread.isInterrupted()) {
            handlerThread.quit();
            handlerThread.interrupt();
            handlerThread = null;
        }
        if (mCameraHandler != null) {
            mCameraHandler = null;
        }

    }


    public interface YUVDataCallBack {
        void yuvData(byte[] data, int width, int height, int orientation);
    }

    public void setYUVDataCallBack(YUVDataCallBack YUVDataCallBack) {
        mYUVDataCallBack = YUVDataCallBack;
    }

    private Size getBestSupportedSize(List<Size> sizes) {
        Point maxPreviewSize = new Point(640, 480);
        Point minPreviewSize = new Point(480, 320);
        Size defaultSize = sizes.get(0);
        Size[] tempSizes = sizes.toArray(new Size[0]);
        Arrays.sort(tempSizes, (o1, o2) -> {
            if (o1.getWidth() > o2.getWidth()) {
                return -1;
            } else if (o1.getWidth() == o2.getWidth()) {
                return o1.getHeight() > o2.getHeight() ? -1 : 1;
            } else {
                return 1;
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));
        for (int i = sizes.size() - 1; i >= 0; i--) {
            if (maxPreviewSize != null) {
                if (sizes.get(i).getWidth() > maxPreviewSize.x || sizes.get(i).getHeight() > maxPreviewSize.y) {
                    sizes.remove(i);
                    continue;
                }
            }
            if (minPreviewSize != null) {
                if (sizes.get(i).getWidth() < minPreviewSize.x || sizes.get(i).getHeight() < minPreviewSize.y) {
                    sizes.remove(i);
                }
            }
        }
        if (sizes.size() == 0) {
            return defaultSize;
        }
        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {
            previewViewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }

        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }

        for (Size s : sizes) {
            if (Math.abs((s.getHeight() / (float) s.getWidth()) - previewViewRatio) < Math.abs(bestSize.getHeight() / (float) bestSize.getWidth() - previewViewRatio)) {
                bestSize = s;
            }
        }
        return bestSize;
    }


    private byte[] getI420(Image image) {
        try {
            int w = image.getWidth(), h = image.getHeight();
            // size是宽乘高的1.5倍 可以通过ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)得到
            int i420Size = w * h * 3 / 2;

            Image.Plane[] planes = image.getPlanes();
            //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176 Y分量byte数组的size
            int remaining0 = planes[0].getBuffer().remaining();
            int remaining1 = planes[1].getBuffer().remaining();
            //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1 V分量byte数组的size
            int remaining2 = planes[2].getBuffer().remaining();
            //获取pixelStride，可能跟width相等，可能不相等
            int pixelStride = planes[2].getPixelStride();
            int rowOffest = planes[2].getRowStride();
            byte[] nv21 = new byte[i420Size];
            //分别准备三个数组接收YUV分量。
            byte[] yRawSrcBytes = new byte[remaining0];
            byte[] uRawSrcBytes = new byte[remaining1];
            byte[] vRawSrcBytes = new byte[remaining2];
            planes[0].getBuffer().get(yRawSrcBytes);
            planes[1].getBuffer().get(uRawSrcBytes);
            planes[2].getBuffer().get(vRawSrcBytes);
            if (pixelStride == image.getWidth()) {
                //两者相等，说明每个YUV块紧密相连，可以直接拷贝
                System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * h);
                System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * h, rowOffest * h / 2 - 1);
            } else {
                //根据每个分量的size先生成byte数组
                byte[] ySrcBytes = new byte[w * h];
                byte[] uSrcBytes = new byte[w * h / 2 - 1];
                byte[] vSrcBytes = new byte[w * h / 2 - 1];
                for (int row = 0; row < h; row++) {
                    //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                    System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w);
                    //y执行两次，uv执行一次
                    if (row % 2 == 0) {
                        //最后一行需要减一
                        if (row == h - 2) {
                            System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w - 1);
                        } else {
                            System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w);
                        }
                    }
                }
                //yuv拷贝到一个数组里面
                System.arraycopy(ySrcBytes, 0, nv21, 0, w * h);
                System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1);
                byte[] i420 = new byte[nv21.length];
                mYuvUtil.nv21ToI420(nv21, w, h, i420);
                return i420;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
