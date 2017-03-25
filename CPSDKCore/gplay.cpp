#include "gplay.h"
#include <list>
#include <android/log.h>

#include "cocos_bridge/cocos_bridge.h"
#include "gplay_runtime.h"
#include "UnitSDKObject.h"
#include "utils/JniHelper.h"

#define  LOG_TAG    "gplay_runtime_common"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_DIVIDE_ERROR(...)  __android_log_print(ANDROID_LOG_ERROR, "gplay_divide_res", __VA_ARGS__)

using namespace std;
using namespace gplay::framework;

namespace gplay { 

extern void RTPreloadResponse(int resultCode, int errorCode, const std::string& groupName, float percent, float downloadSpeed);

namespace common {

    class ActionResultListenerImpl;

    static std::string s_gameRuntimeConfig;

    static ActionResultListenerImpl* s_ActionResultListener = NULL;
    static std::map<int, ActionResultCallback> s_callbackMap;

    static std::string s_currPreloadGroups;
    static int s_currPreloadExt = 0;

    class ActionResultListenerImpl : public ActionResultListener {
    private:
        ActionResultListenerImpl() {
        }

        class CGarbo // 垃圾工人
        {
        public:
            ~CGarbo() {
                if( s_ActionResultListener ){
                    s_callbackMap.clear();

                    delete s_ActionResultListener;
                    s_ActionResultListener = NULL;
                }
            }
        };
        static CGarbo Garbo;

    public:
        static ActionResultListenerImpl* getInstance(){
            if (NULL == s_ActionResultListener)
                s_ActionResultListener = new ActionResultListenerImpl();
            return s_ActionResultListener;
        }

        virtual void onActionResult(UnitSDK* unitSDK, ActionResultCode code, const char* msg, int callbackID)
        {
            std::map<int, ActionResultCallback>::iterator iter = s_callbackMap.find(callbackID);
            if ( iter != s_callbackMap.end()) {
                if(iter->second)
                    iter->second(code, msg, callbackID);

                s_callbackMap.erase(iter);
            }
        }
    };

    /**
     * Runtime 回调函数, 随着 JNI 调用本地函数方法的时候调用, 根据传入的回调类型调用不同的本地函数
     * typedef void* (*RTCallback)(RTCallbackType type, void* arg1, void* arg2, void* arg3);
     */
    static void* runtimeEventCallback(RTCallbackType type, void* arg1, void* arg2, void* arg3) {
        void* ret = NULL;
        switch (type)
        {
            case RT_START_GAME:
                LOGD("nativeCocosRuntimeOnStartGame");
                break;
            case RT_QUIT_GAME:
                gplay::RTQuitGame();
                break;
            case RT_SET_RUNTIME_CONFIG:
                s_gameRuntimeConfig = (const char*)arg1;
                break;
            case RT_SET_XXTEA_KEY_AND_SIGN:
                // do nothing for lua
                break;
            default:
                LOGE("RTSetListener, invalid RTCallbackType ...: type: %d", type);
                break;
        }
        return ret;
    }

    bool isInGplayEnv()
    {
        return !s_gameRuntimeConfig.empty();
    }

    void initSDK(const std::string& appKey, const std::string& appSecret, const std::string& privateKey) {
        UnitSDK* unitSDK = UnitSDK::getInstance();
        unitSDK->init(appKey, appSecret, privateKey);
    }

    GplayNetWorkStatus getNetworkType() {
        int status = RTGetNetworkTypeJNI();
        return (GplayNetWorkStatus)status;
    }

    // jsonGroups， eg: "{'scenes':['roles_group'，'music_group']}"
    void preloadGroups(const std::string& groups, int ext) {
        s_currPreloadGroups = groups;
        s_currPreloadExt = ext;
        RTPreloadGroupsJNI(groups.c_str(), ext);
    }

    void retryPreload() {
        RTPreloadGroupsJNI(s_currPreloadGroups.c_str(), s_currPreloadExt);
    }

    bool isLogined() {
        UnitSDK* unitSDK = UnitSDK::getInstance();
        return unitSDK->isLogined();
    }

    const std::string& getUserID() {
        UnitSDK* unitSDK = UnitSDK::getInstance();
        return unitSDK->getUserID();
    }

    const std::string& getOrderId() {
        UnitSDK* unitSDK = UnitSDK::getInstance();
        return unitSDK->getOrderId();
    }

    const std::string& getChannelID() {
        UnitSDK* unitSDK = UnitSDK::getInstance();
        return unitSDK->getChannelId();
    }

    void login(int callbackID, ActionResultCallback callback) {
        if (callback == NULL)
            return;

        s_callbackMap[callbackID] = callback;
        UnitSDK* unitSDK = UnitSDK::getInstance();
        unitSDK->login(callbackID);
    }

    void logout(int callbackID, ActionResultCallback callback) {
        s_callbackMap[callbackID] = callback;
        UnitSDK* unitSDK = UnitSDK::getInstance();
        unitSDK->callAsyncFunc(callbackID, "logout", "");
    }

    void share(int callbackID, const std::string& jsonShareInfo, ActionResultCallback callback) {
        s_callbackMap[callbackID] = callback;
        UnitSDK* unitSDK = UnitSDK::getInstance();
        unitSDK->share(callbackID, jsonShareInfo);
    }

    void pay(int callbackID, const std::string& jsonPayInfo, ActionResultCallback callback) {
        if (callback == NULL)
            return;
        
        s_callbackMap[callbackID] = callback;
        UnitSDK* unitSDK = UnitSDK::getInstance();
        unitSDK->payForProduct(callbackID, jsonPayInfo);
    }

    void createShortcut(int callbackID, ActionResultCallback callback) {
        s_callbackMap[callbackID] = callback;
        UnitSDK* unitSDK = UnitSDK::getInstance();
        unitSDK->createShortCut(callbackID);
    }

    std::string callSyncFunc(const std::string& funcName, const std::string& params) {
        UnitSDK* unitSDK = UnitSDK::getInstance();
        return unitSDK->callSyncStringFunc(funcName, params);
    }

    void callAsyncFunc(int callbackID, const std::string& funcName, 
        const std::string& params, ActionResultCallback callback) {
        s_callbackMap[callbackID] = callback;
        UnitSDK* unitSDK = UnitSDK::getInstance();
        unitSDK->callAsyncFunc(callbackID, funcName, params);
    }

    void quitGame() {
        RTQuitGameJNI();
    }
    
    bool isFunctionSupported(const std::string& funcName) {
        UnitSDK* unitSDK = UnitSDK::getInstance();
        return unitSDK->isFunctionSupported(funcName);
    }

    static std::string s_bootSceneName;
    static std::list<std::string> s_loadedScenes;

    const std::string& getCurrPreloadSceneName()
    {
        return s_loadedScenes.back();
    }

    void noticePreloadBegin(const std::string& sceneName)
    {
        if (sceneName.empty())
        {
            LOG_DIVIDE_ERROR("GPlayFileUtils::noticePreloadBegin error: sceneName is empty!");
            return;
        }

        s_loadedScenes.push_back(sceneName);
    }

    void noticePreloadEnd(const std::string& sceneName)
    {
        if (sceneName.empty())
        {
            LOG_DIVIDE_ERROR("GPlayFileUtils::noticePreloadEnd error: sceneName is empty!");
            return;
        }

        if (sceneName.compare(s_bootSceneName) == 0)
            return;

        for (std::list<std::string>::reverse_iterator rit = s_loadedScenes.rbegin(); 
            rit != s_loadedScenes.rend(); ++rit)
        {
            if (sceneName.compare(*rit) == 0)
            {
                s_loadedScenes.erase(--rit.base());
                break;
            }
        }
    }

    static const char* s_runtimeNativeWrapperClass = "com/skydragon/gplay/runtime/RuntimeNativeWrapper";

    std::string callExtensionSyncAPI(const char* method, bool& isFuncCallSuccess, const char* strArg, int intArg, double doubleArg)
    {
        std::string ret;
        isFuncCallSuccess = false;
    
        JniMethodInfo t;
        if (JniHelper::getStaticMethodInfo(t, s_runtimeNativeWrapperClass, 
            "extensionSyncAPI", "(Ljava/lang/String;Ljava/lang/String;ID)Ljava/lang/String;")) {
            isFuncCallSuccess = true;
            jstring jmethod = t.env->NewStringUTF(method);
            jstring jstrArg = t.env->NewStringUTF(strArg);
            jstring jret = (jstring)t.env->CallStaticObjectMethod(t.classID, t.methodID, jmethod, jstrArg, intArg, doubleArg);
            ret = JniHelper::jstring2string(jret);
            t.env->DeleteLocalRef(jmethod);
            t.env->DeleteLocalRef(jstrArg);
            t.env->DeleteLocalRef(t.classID);
        }
    
        return ret;
    }
    
    void callExtensionASyncAPI(const char* method, bool& isFuncCallSuccess, const char* strArg, int intArg, double doubleArg)
    {
        JniMethodInfo t;
        isFuncCallSuccess = false;

        if (JniHelper::getStaticMethodInfo(t, s_runtimeNativeWrapperClass, 
            "extensionASyncAPI", "(Ljava/lang/String;Ljava/lang/String;ID)V")) {
            isFuncCallSuccess = true;
            jstring jmethod = t.env->NewStringUTF(method);
            jstring jstrArg = t.env->NewStringUTF(strArg);
            t.env->CallStaticObjectMethod(t.classID, t.methodID, jmethod, jstrArg, intArg, doubleArg);
            t.env->DeleteLocalRef(jmethod);
            t.env->DeleteLocalRef(jstrArg);
            t.env->DeleteLocalRef(t.classID);
        }
    }

    static NativeExtensionCallback s_nativeExtensionCallback = NULL;

    void setNativeExtensionCallback(NativeExtensionCallback callback)
    {
        s_nativeExtensionCallback = callback;
    }

    /**
     * 对 runtimebridge.cpp 设置 runtime 回调函数
     */
    class StartupListener
    {
    public:
        StartupListener() {
            RTSetListener(runtimeEventCallback);
            UnitSDK* unitSDK = UnitSDK::getInstance();
            unitSDK->setAsyncActionResultListener(ActionResultListenerImpl::getInstance());

            s_bootSceneName = "boot_scene";
            s_loadedScenes.push_back(s_bootSceneName);
        }
    };

    static StartupListener __startupListener;
}}

////////////////////////////////////////////////////////////////////
extern "C"
{
    //runtime sdk java 和 runtime sdk native层扩展通信接口
    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativeExtensionAPI(JNIEnv* env, jobject thiz, jstring jmethod, jstring jstrArg, jint intArg, jdouble doubleArg)
    {
        if (gplay::common::s_nativeExtensionCallback)
        {
            std::string method = JniHelper::jstring2string(jmethod);
            std::string strArg = JniHelper::jstring2string(jstrArg);
            gplay::common::s_nativeExtensionCallback(method.c_str(), strArg.c_str(), intArg, doubleArg);
        }
    }

    void Java_com_skydragon_gplay_runtime_bridge_CocosRuntimeBridge_nativePreloadResponse2(
        JNIEnv* env, jobject thiz, 
        jboolean isDone, jboolean isFailed, jstring jerrorCode, 
        jfloat percent, jfloat downloadSpeed, jstring jgroupName)
    {
        int resultCode = gplay::common::PRELOAD_RESULT_PROGRESS;
        int errorCode = gplay::common::PRELOAD_ERROR_NONE;
        std::string groupName = JniHelper::jstring2string(jgroupName);

        if (isDone)
            resultCode = gplay::common::PRELOAD_RESULT_SUCCESS;
        else if (isFailed)
        {
            resultCode = gplay::common::PRELOAD_RESULT_FAILED;

            const char* errorCodeStr = JniHelper::jstring2string(jerrorCode).c_str();

            if (strcmp(errorCodeStr, "err_verify") == 0)
                errorCode = gplay::common::PRELOAD_ERROR_VERIFY_FAILED;
            else if (strcmp(errorCodeStr, "err_network") == 0)
                errorCode = gplay::common::PRELOAD_ERROR_NETWORK;
            else if (strcmp(errorCodeStr, "err_no_space") == 0)
                errorCode = gplay::common::PRELOAD_ERROR_NO_SPACE;
            else
                errorCode = gplay::common::PRELOAD_ERROR_UNKNOWN;
        }

        gplay::RTPreloadResponse(resultCode, errorCode, groupName, percent, downloadSpeed);
    }
}
