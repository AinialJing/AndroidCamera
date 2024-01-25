#include <jni.h>
#include <string>
#include "YuvUtil.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_androidcamera_YuvUtil_nv21ToI420(JNIEnv *env, jobject thiz,
                                                   jbyteArray src_nv21_array, jint width,
                                                   jint height, jbyteArray dst_i420_array) {
    jbyte *src_nv21_data = env->GetByteArrayElements(src_nv21_array, JNI_FALSE);
    jbyte *dst_i420_data = env->GetByteArrayElements(dst_i420_array, JNI_FALSE);
    nv21ToI420(src_nv21_data, width, height, dst_i420_data);

    env->ReleaseByteArrayElements(src_nv21_array, src_nv21_data, 0);
    env->ReleaseByteArrayElements(dst_i420_array, dst_i420_data, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_aniljing_androidcamera_YuvUtil_i420Rotate(JNIEnv *env, jobject thiz,
                                                   jbyteArray src_i420_array, jint width,
                                                   jint height, jbyteArray dst_i420_array,
                                                   jint degree) {
    jbyte *src_i420_data = env->GetByteArrayElements(src_i420_array, JNI_FALSE);
    jbyte *dst_i420_data = env->GetByteArrayElements(dst_i420_array, JNI_FALSE);

    rotateI420(src_i420_data, width, height, dst_i420_data, degree);

    env->ReleaseByteArrayElements(src_i420_array, src_i420_data, 0);
    env->ReleaseByteArrayElements(dst_i420_array, dst_i420_data, 0);
}