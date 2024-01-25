package com.aniljing.androidcamera;

public class YuvUtil {
    public native void nv21ToI420(byte[] src_nv21_data, int width, int height, byte[] dst_i420_data);

    public native void i420Rotate(byte[] src_i420_data, int width, int height, byte[] dst_i420_data, int degree);

    static {
        System.loadLibrary("androidcamera");
    }
}
