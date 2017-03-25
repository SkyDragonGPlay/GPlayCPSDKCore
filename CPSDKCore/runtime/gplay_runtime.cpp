#include "gplay_runtime.h"
#include "cocos_bridge/cocos_bridge.h"
#include "JniHelper.h"

#include <string>
#include <vector>

#include <android/log.h>

using namespace gplay;
using namespace gplay::framework;

#define  LOG_TAG    "gplay_runtime"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define return_if_failed(cond, msg)             if (!cond) { LOGE(msg); return; }
#define return_val_if_failed(cond, retVal, msg) if (!cond) { LOGE(msg); return retVal; }

static const char* s_runtimeNativeWrapperClass = "com/skydragon/gplay/runtime/RuntimeNativeWrapper";

// Static variables definition
static RTCallback __runtimeCallback = NULL;

void RTSetListener(RTCallback callback) {
    __runtimeCallback = callback;
}

int RTGetNetworkTypeJNI() {
    int networkStatus = -1;
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t, "com/skydragon/gplay/runtime/utils/Utils", "getAPNType", "()I")) {
        LOGD("RTGetNetworkTypeJNI ==> java: Utils.getAPNType");
        networkStatus = t.env->CallStaticIntMethod(t.classID, t.methodID);
        t.env->DeleteLocalRef(t.classID);
    }
    return networkStatus;
}

void RTPreloadGroupsJNI(const char* str, int ext) {
    return_if_failed(str, "RTPreloadGroupsJNI, str==NULL");

    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t, s_runtimeNativeWrapperClass, 
        "preloadGroupsFromNative", "(Ljava/lang/String;I)V")) {
        jstring params = t.env->NewStringUTF(str);
        t.env->CallStaticVoidMethod(t.classID, t.methodID, params, ext);
        t.env->DeleteLocalRef(params);
        t.env->DeleteLocalRef(t.classID);
    }
}

void RTQuitGameJNI() {
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t, s_runtimeNativeWrapperClass, "quitGameFromNative", "()V")) {
        LOGD("RTQuitGameJNI");

        t.env->CallStaticVoidMethod(t.classID, t.methodID);
        t.env->DeleteLocalRef(t.classID);
    }
}

void RTGetRemoteFileDataJNI(const char* config, long ext) {
    return_if_failed(config, "RTGetRemoteFileDataJNI, config==NULL");

    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t, s_runtimeNativeWrapperClass, 
        "downloadRemoteFileFromNative", "(Ljava/lang/String;J)V")) {
        jstring jconfig = t.env->NewStringUTF(config);
        t.env->CallStaticVoidMethod(t.classID, t.methodID, jconfig, ext);
        t.env->DeleteLocalRef(jconfig);
        t.env->DeleteLocalRef(t.classID);
    }
}

////////////////////////////////////////////////////////////////////

extern "C"
{
    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativeInitRuntimeJNI(JNIEnv*  env, jobject thiz, jstring jResRootPath)
    {
        LOGD("gplay runtime initRuntimeJni");
        JavaVM* gs_jvm;
        env->GetJavaVM(&gs_jvm);
        gplay::framework::JniHelper::setJavaVM(gs_jvm);

        RTRuntimeInit();
    }

    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativeSetDefaultResourceRootPath(JNIEnv*  env, jobject thiz, jstring jResRootPath)
    {
        const char* resRootPath = env->GetStringUTFChars(jResRootPath, NULL);
        LOGD("nativeSetDefaultResourceRootPath: %s", resRootPath);
        RTSetDefaultResourceRootPath(resRootPath);
        env->ReleaseStringUTFChars(jResRootPath, resRootPath);
    }

    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativeAddSearchPath(JNIEnv*  env, jobject thiz, jstring jSearchPath)
    {
        const char* newSearchPath = env->GetStringUTFChars(jSearchPath, NULL);
        LOGD("nativeAddSearchPath: %s", newSearchPath);
        RTAddSearchPath(newSearchPath);
        env->ReleaseStringUTFChars(jSearchPath, newSearchPath);
    }

    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativeCocosEngineOnStartGame(JNIEnv* env, jobject thiz)
    {
        LOGD("nativeCocosRuntimeOnStartGame ...");

        return_if_failed(__runtimeCallback, "nativeCocosRuntimeOnStartGame, __runtimeCallback==NULL");
        __runtimeCallback(RT_START_GAME, NULL, NULL, NULL);
    }

    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativeCocosEngineOnQuitGame(JNIEnv* env, jobject thiz)
    {
        LOGD("nativeCocosRuntimeOnQuitGame ...");
        return_if_failed(__runtimeCallback, "nativeCocosRuntimeOnQuitGame, __runtimeCallback==NULL");

        __runtimeCallback(RT_QUIT_GAME, NULL, NULL, NULL);
        __runtimeCallback = NULL;
    }
    
    jboolean Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativeGplayKeyEvent(JNIEnv* env, jobject thiz, jint keyCode, jboolean isPressed)
    {
        LOGD("nativeGplayKeyEvent ...");
        return RTDipspatchEvent(keyCode, isPressed);
    }

    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativePreloadResponse(JNIEnv* env, jobject thiz, jstring jResponseJson, jboolean isDone, jlong ext)
    {
        //当前版本不再支持这个接口
        LOGE("nativePreloadResponse should not be invoke!");
    }

    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativeDownloadFileCallback(JNIEnv* env, jobject thiz, jstring jResponseJson, jlong ext)
    {
        return_if_failed(__runtimeCallback, "nativeDownloadFileCallback, __runtimeCallback==NULL");

        std::string responseJson = JniHelper::jstring2string(jResponseJson);
        LOGD("nativeDownloadFileCallback...: %s", responseJson.c_str());

        __runtimeCallback(RT_DOWNLOAD_REMOTE_FILE, (void*)responseJson.c_str(), (void*)&ext, NULL);
    }

    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativeSetRuntimeConfig(JNIEnv* env, jobject thiz, jstring jResponseJson)
    {
        return_if_failed(__runtimeCallback, "nativeSetRuntimeConfig, __runtimeCallback==NULL");

        //当前只用于判断是否为gplay环境
        __runtimeCallback(RT_SET_RUNTIME_CONFIG, (void*)"..", NULL, NULL);
    }
}
