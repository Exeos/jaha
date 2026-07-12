#include <jni.h>
#include "me_exeos_jaha_util_NativeDefine.h"

JNIEXPORT jclass JNICALL Java_me_exeos_jaha_util_NativeDefine_defineBootstrapClass
  (JNIEnv *env, jclass, jstring name, jbyteArray data) {
    const char *nameChars = env->GetStringUTFChars(name, nullptr);
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);

    jclass defined = env->DefineClass(nameChars, nullptr, bytes, len);

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    env->ReleaseStringUTFChars(name, nameChars);
    return defined;
}