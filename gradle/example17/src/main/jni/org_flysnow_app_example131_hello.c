#include "org_flysnow_app_example131_hello.h"

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_flysnow_app_example131_hello
 * Method:    get_hello
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_flysnow_app_example131_hello_get_1hello
  (JNIEnv *env, jobject obj){
      return (*env)->NewStringUTF(env,"hello jh!");
  }

#ifdef __cplusplus
}
#endif

