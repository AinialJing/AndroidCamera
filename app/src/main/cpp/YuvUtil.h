//
// Created by company on 2024-01-25.
//

#ifndef ANDROIDCAMERA_YUVUTIL_H
#define ANDROIDCAMERA_YUVUTIL_H
#include <jni.h>
#include <libyuv.h>

void nv21ToI420(jbyte *src_nv21_data,jint width,jint height,jbyte* dst_i420_data);
/**
 * i420旋转
 * @param src_i420_data
 * @param width
 * @param height
 * @param dst_i420_data
 * @param degree
 */
void rotateI420(jbyte*src_i420_data,jint width,jint height,jbyte*dst_i420_data,jint degree);

#endif //ANDROIDCAMERA_YUVUTIL_H
