#include <jni.h>
#include "com_example_jniproject_jnidemo_JNIUtils.h"
#include <string>

JNIEXPORT jstring JNICALL Java_com_example_jniproject_jnidemo_JNIUtils_getNativeString
  (JNIEnv* env, jobject thiz){
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}