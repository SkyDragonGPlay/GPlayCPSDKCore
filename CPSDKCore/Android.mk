LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := gplay_static

LOCAL_SRC_FILES := gplay.cpp \
                   runtime/gplay_runtime.cpp \
                   unitsdk/UnitSDKObject.cpp \
                   utils/Utils.cpp \
                   utils/JniHelper.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
                    $(LOCAL_PATH)/utils \
                    $(LOCAL_PATH)/runtime \
                    $(LOCAL_PATH)/unitsdk

#LOCAL_ARM_MODE := arm

include $(BUILD_STATIC_LIBRARY)
