#include "UnitSDKObject.h"
#include <stdlib.h>
#include <string>
#include "utils/JniHelper.h"
#include "utils/Utils.h"

#include <android/log.h>

#define  LOG_TAG    "UnitSDKObject"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace std;
namespace gplay {namespace framework {
    
extern "C" {

    void Java_com_skydragon_gplay_unitsdk_nativewrapper_NativeWrapper_nativeAsyncActionResult(JNIEnv* env, jobject thiz, jint ret, jstring msg, jstring jCallbackID)
    {
        UnitSDK* unitSDK = UnitSDK::getInstance();
        if (unitSDK != NULL) {
            ActionResultListener* listener = unitSDK->getAsyncActionResultListener();
            if (NULL != listener) {
                string jsonResult = JniHelper::jstring2string(msg);
                string strCallbackID = JniHelper::jstring2string(jCallbackID);
                int callbackID = atoi(strCallbackID.c_str());

                // paying 回调的时候设为 false
                if(UnitSDKObject::_payingCallbacks.find(callbackID) != UnitSDKObject::_payingCallbacks.end() &&
                 UnitSDKObject::_payingCallbacks[callbackID] == 1) {
                    UnitSDK::_paying = false; 
                    UnitSDKObject::_payingCallbacks.clear();
                }

                listener->onActionResult(unitSDK, (ActionResultCode)ret, jsonResult.c_str(), callbackID);
            } else {
                LOGE("nativeAsyncActionResult:asyncActionResultListener not set correctly");
            }
        } else {
            LOGE("nativeAsyncActionResult:UnitSDKObject is null");
        }
    }

    void Java_com_skydragon_gplay_unitsdk_nativewrapper_NativeWrapper_nativeOutputLog(JNIEnv*  env, jobject thiz, jint type, jstring tag, jstring msg) {
        string stag = JniHelper::jstring2string(tag);
        string smsg = JniHelper::jstring2string(msg);
        Utils::outputLog((int)type, stag.c_str() ,smsg.c_str());
    }
}

UnitSDK* UnitSDK::s_unitSDK = NULL;
bool UnitSDK::_paying = false;

//转换callback id
static char s_convert_int_buffer[16];
static const char* gplay_itoa(int value)
{
    sprintf(s_convert_int_buffer, "%d", value);
    return s_convert_int_buffer;
}

//callback id, state
std::map<int, int> UnitSDKObject::_payingCallbacks;

jobject UnitSDKObject::jGlobalProxyObj = NULL;
const string UnitSDKObject::jProxyClass("com/skydragon/gplay/unitsdk/framework/UnitSDK");

UnitSDK* UnitSDK::getInstance() {
    if(s_unitSDK == NULL)
    {
        s_unitSDK = new UnitSDKObject();
    }
    return s_unitSDK;
}

UnitSDKObject::UnitSDKObject()
: _asyncResultListener(NULL)
{
}
    
UnitSDKObject::~UnitSDKObject() {
    JNIEnv* env = JniHelper::getEnv();
    env->DeleteLocalRef(jGlobalProxyObj);
}

void UnitSDKObject::init(const string& appKey,const string& appSecret,const string& privateKey) {
    LOGD("UnitSDKObject::init");
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t
            , jProxyClass.c_str()
            , "initUnitSDKParams"
            , "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"))
    {
        jstring strAppKey = JniHelper::newStringUTF(t.env, appKey);
        jstring strAppSecret = JniHelper::newStringUTF(t.env, appSecret);
        jstring strPrivateKey = JniHelper::newStringUTF(t.env, privateKey);
        t.env->CallStaticVoidMethod(t.classID, t.methodID, strAppKey,strAppSecret,strPrivateKey);

        t.env->DeleteLocalRef(t.classID);
        t.env->DeleteLocalRef(strAppSecret);
        t.env->DeleteLocalRef(strAppKey);
        t.env->DeleteLocalRef(strPrivateKey);
    }
}

const string& UnitSDKObject::getChannelId() {
    Utils::outputLog(ANDROID_LOG_DEBUG,"UnitSDKObject", "Try to get channelid");
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t
            , jProxyClass.c_str()
            , "getChannelId"
            , "()Ljava/lang/String;"))
    {
        jstring jstrRet = (jstring)t.env->CallStaticObjectMethod(t.classID,t.methodID);
        _channelID = JniHelper::jstring2string(jstrRet);
        t.env->DeleteLocalRef(t.classID);
    }
    else
        _channelID = "";
    
    return _channelID;
}

bool UnitSDKObject::isFunctionSupported(const string& functionName) {
    return callSyncStringFunc("isFunctionSupported", functionName) == "true";
}

void UnitSDKObject::login(int callbackID) {
    LOGD("UnitSDKObject::login()");
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t
            , jProxyClass.c_str()
            , "login"
            , "(Ljava/lang/String;)V"))
    {
        jstring strCallbackId = JniHelper::newStringUTF(t.env, gplay_itoa(callbackID));
        
        t.env->CallStaticVoidMethod(t.classID,t.methodID, strCallbackId);
        
        t.env->DeleteLocalRef(t.classID);
        t.env->DeleteLocalRef(strCallbackId);
    }
}

bool UnitSDKObject::isLogined() {
    JniMethodInfo t;
    bool ret = false;
    if (JniHelper::getStaticMethodInfo(t
            , jProxyClass.c_str()
            , "isLogin"
            , "()Z"))
    {
        ret = t.env->CallStaticBooleanMethod(t.classID,t.methodID);
        t.env->DeleteLocalRef(t.classID);
    }
    return ret;
}

const string& UnitSDKObject::getUserID() {
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t
            , jProxyClass.c_str()
            , "getUserID"
            , "()Ljava/lang/String;"))
    {
        jstring jstrRet = (jstring)t.env->CallStaticObjectMethod(t.classID,t.methodID);
        _userID = JniHelper::jstring2string(jstrRet);
        t.env->DeleteLocalRef(t.classID);
    }
    else
        _userID = "";

    return _userID;
}

void UnitSDKObject::payForProduct(int callbackID, const string& info) {
    if (UnitSDK::_paying) {
        Utils::outputLog(ANDROID_LOG_DEBUG, "UnitSDK", "Now is paying");
        onAsyncActionResult(callbackID, PAY_RESULT_NOW_PAYING, "Now is paying");
        return;
    }

    if (info.empty()) {
        onAsyncActionResult(callbackID, PAY_RESULT_FAIL, "Product info error");;
        Utils::outputLog(ANDROID_LOG_ERROR, "UnitSDK", "The product info is empty!");
        return;
    }
    else {
        UnitSDKObject::_payingCallbacks[callbackID] = 1;
        UnitSDK::_paying = true;

        JniMethodInfo t;
        if (JniHelper::getStaticMethodInfo(t
                , jProxyClass.c_str()
                , "pay"
                , "(Ljava/lang/String;Ljava/lang/String;)V"))
        {
            jstring strCallbackId = JniHelper::newStringUTF(t.env, gplay_itoa(callbackID));
            jstring strProductInfo = JniHelper::newStringUTF(t.env, info);

            // invoke java method
            t.env->CallStaticVoidMethod(t.classID, t.methodID, strCallbackId, strProductInfo);
            t.env->DeleteLocalRef(t.classID);
            t.env->DeleteLocalRef(strCallbackId);
            t.env->DeleteLocalRef(strProductInfo);
        }
    }
}

const string& UnitSDKObject::getOrderId() {
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t
            , jProxyClass.c_str()
            , "getOrderId"
            , "()Ljava/lang/String;"))
    {
        jstring jstrRet = (jstring)t.env->CallStaticObjectMethod(t.classID,t.methodID);
        _orderID = JniHelper::jstring2string(jstrRet);
        t.env->DeleteLocalRef(t.classID);
    }
    else
        _orderID = "";

    return _orderID;
}

void UnitSDKObject::share(int callbackID, const string& info) {
    if (info.empty()) {
        onAsyncActionResult(callbackID, SHARE_RESULT_FAIL, "Share info error");
        
        Utils::outputLog(ANDROID_LOG_DEBUG,"UnitSDK", "The Share info is empty!");
        return;
    }
    else {
        JniMethodInfo t;
        if (JniHelper::getStaticMethodInfo(t
                , jProxyClass.c_str()
                , "share"
                , "(Ljava/lang/String;Ljava/lang/String;)V"))
        {
            jstring strCallbackId = JniHelper::newStringUTF(t.env, gplay_itoa(callbackID));
            jstring strShareInfo = JniHelper::newStringUTF(t.env, info);

            // invoke java method
            t.env->CallStaticVoidMethod(t.classID, t.methodID, strCallbackId, strShareInfo);
            
            t.env->DeleteLocalRef(t.classID);
            t.env->DeleteLocalRef(strCallbackId);
            t.env->DeleteLocalRef(strShareInfo);
        }
    }
}

void UnitSDKObject::createShortCut(int callbackID) {
    LOGD("UnitSDKObject::createShortCut()");
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t
            , jProxyClass.c_str()
            , "createShortcut"
            , "(Ljava/lang/String;)V"))
    {
        jstring strCallbackId = JniHelper::newStringUTF(t.env, gplay_itoa(callbackID));

        // invoke java method
        t.env->CallStaticVoidMethod(t.classID, t.methodID, strCallbackId);
        
        t.env->DeleteLocalRef(t.classID);
        t.env->DeleteLocalRef(strCallbackId);
    }
}

    
// ======= handle listener ===========
    
void UnitSDKObject::setAsyncActionResultListener(ActionResultListener* listener) {
    LOGD("UnitSDKObject::setAsyncActionResultListener");
    if (NULL == listener)
        LOGE("input ActionResultListener is null");
    _asyncResultListener = listener;
}
    
ActionResultListener* UnitSDKObject::getAsyncActionResultListener() {
    if (NULL == _asyncResultListener)
        LOGE("_asyncResultListener is null");
    return _asyncResultListener;
}
    
    
// ============ unitSdk 通用扩展接口 ==============
    
string UnitSDKObject::callSyncStringFunc(const string& funcName, const string& jsonParams) {
    return callJavaSyncStringFunc(funcName, jsonParams);
}

void UnitSDKObject::callAsyncFunc(int callbackID, const string& funcName, const string& jsonParams) {
    callJavaAsyncFunc(callbackID, funcName, jsonParams);
}
    
    
// ========== 调用 java 通用扩展接口 ==========
string UnitSDKObject::callJavaSyncStringFunc(const string& funcName, const string& jsonParams) {
    if(funcName.empty()) return "";

    JNIEnv* env = JniHelper::getEnv();
    jstring jfunc = JniHelper::newStringUTF(env, funcName.c_str());
    jstring jparams = JniHelper::newStringUTF(env, jsonParams.c_str());

    string ret = "";

    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t, jProxyClass.c_str(), "callSyncFunction", 
        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")) {
        jstring jstrRet = (jstring)env->CallStaticObjectMethod(t.classID, t.methodID, jfunc, jparams);
        ret = JniHelper::jstring2string(jstrRet);
        t.env->DeleteLocalRef(t.classID);
    }
    env->DeleteLocalRef(jfunc);
    env->DeleteLocalRef(jparams);
    return ret;
}
    
void UnitSDKObject::callJavaAsyncFunc(int callbackID, const string& funcName, const string& jsonParams) {
    if(funcName.empty()) return;

    JNIEnv* env = JniHelper::getEnv();
    jstring jfunc = JniHelper::newStringUTF(env, funcName.c_str());
    jstring jparams = JniHelper::newStringUTF(env, jsonParams.c_str());
    jstring jcallbackid = JniHelper::newStringUTF(env, gplay_itoa(callbackID));

    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t, jProxyClass.c_str(), "callAsynFunctionFromNative",
     "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V")) {
        env->CallStaticVoidMethod(t.classID, t.methodID, jfunc, jparams, jcallbackid);
        t.env->DeleteLocalRef(t.classID);
    }
    env->DeleteLocalRef(jfunc);
    env->DeleteLocalRef(jparams);
    env->DeleteLocalRef(jcallbackid);
}
//=====================================

void UnitSDKObject::onAsyncActionResult(int callbackID, int ret, const char* msg)
{
    LOGD("UnitSDKObject::onAsyncActionResult");
    if (_asyncResultListener) {
        _asyncResultListener->onActionResult(this, (ActionResultCode)ret, msg, callbackID);
    }
    
    LOGD("result is : %d(%s)", ret, msg);
}

jmethodID UnitSDKObject::getJProxyCallSyncFunctionMethodID() {
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t, jProxyClass.c_str(), "callSyncFunction", 
        "(Ljava/lang/String;Ljava/util/List;)Ljava/lang/Object;")) {
        t.env->DeleteLocalRef(t.classID);
        return t.methodID;
    }
    return NULL;
}

jmethodID UnitSDKObject::getJProxyCallAsynFunctionMethodID() {
    JniMethodInfo t;
    if (JniHelper::getStaticMethodInfo(t, jProxyClass.c_str(), "callAsynFunction", 
        "(Ljava/lang/String;Ljava/util/List;)Ljava/lang/Object;")) {
        t.env->DeleteLocalRef(t.classID);
        return t.methodID;
    }
    return NULL;
}
    
}} // namespace gplay::framework::