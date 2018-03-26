#include "org_flysnow_app_example132_hello.h
JNIEXPORT jstring JNICALL Java_org_flysnow_app_example132_hello_get_1hello
  (JNIEnv *, jobject){
      return (*env)->newStringUTF(env,"hello jh!");
  }

