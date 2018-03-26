LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := jh_hello
LOCAL_SRC_FILES := org_flysnow_app_example131_hello.c
#LOCAL_LDFLAGS := -L$(LOCAL_PATH)/vvw/libs/$(TARGET_ARCH_ABI)
LOCAL_LDLIBS := \
   -lz \
   -lm \
#   -lvvw\
#LOCAL_SHARED_LIBRARIES := vvw
include $(BUILD_SHARED_LIBRARY)

