#ifndef __GPLAY_H__
#define __GPLAY_H__

#include <string>

namespace gplay { namespace common {

    typedef enum {
        GPLAY_INIT_SUCCESS = 0,                 // succeeding in initing sdk
        GPLAY_INIT_FAIL = 1,                    // failing to init sdk

        USER_LOGIN_RESULT_SUCCESS = 10000,      // login success
        USER_LOGIN_RESULT_FAIL = 10001,         // login failed
        USER_LOGIN_RESULT_CANCEL = 10002,       // login canceled
        USER_LOGOUT_RESULT_SUCCESS = 10003,     // logout success
        USER_LOGOUT_RESULT_FAIL = 10004,        // logout failed
        USER_REGISTER_RESULT_SUCCESS = 10005,   // regiister sucess
        USER_REGISTER_RESULT_FAIL = 10006,      // regiister failed
        USER_REGISTER_RESULT_CANCEL = 10007,    // regiister Cancel
        USER_BIND_RESULT_SUCESS = 10008,        // bind sucess
        USER_BIND_RESULT_CANCEL = 10009,        // bind Cancel
        USER_BIND_RESULT_FAILED = 100010,       // bind failed
        USER_RESULT_NETWROK_ERROR = 10011,      // network error
        USER_RESULT_USEREXTENSION = 19999,      // extension code

        PAY_RESULT_SUCCESS = 20000,             // pay success
        PAY_RESULT_FAIL = 20001,                // pay fail
        PAY_RESULT_CANCEL = 20002,              // pay cancel
        PAY_RESULT_INVALID = 20003,             // incompleting info
        PAY_RESULT_NETWORK_ERROR = 20004,       // network error
        PAY_RESULT_NOW_PAYING = 20005,          // paying now
        PAY_RESULT_PAYEXTENSION = 29999,        // extension code
        
        SHARE_RESULT_SUCCESS = 30000,           // share success
        SHARE_RESULT_FAIL = 30001,              // share failed
        SHARE_RESULT_CANCEL = 30002,            // share canceled
        SHARE_RESULT_NETWORK_ERROR = 30003,     // network error
        SHARE_RESULT_SHAREREXTENSION = 39999,   // extension code
        
        SHORTCUT_RESULT_SUCCESS = 40000,
        SHORTCUT_RESULT_FAILED = 40001,

        CAPTURE_SCREEN_SUCCESS = 41000,
        CAPTURE_SCREEN_FAILED = 41001,

        PRELOAD_RESULT_SUCCESS = 50000,
        PRELOAD_RESULT_PROGRESS,
        PRELOAD_RESULT_FAILED,
        
        PRELOAD_ERROR_NETWORK = 60000,
        PRELOAD_ERROR_VERIFY_FAILED,
        PRELOAD_ERROR_NO_SPACE,
        PRELOAD_ERROR_UNKNOWN,
        PRELOAD_ERROR_NONE
    } GplayActionResultCode;

    typedef enum {
        NO_NETWORK = -1,  
        MOBILE = 0,
        WIFI
    } GplayNetWorkStatus;

    typedef void(*ActionResultCallback)(int result, const char* jsonResult, int callbackID);

    bool isInGplayEnv();
    
    void initSDK(const std::string& appKey, const std::string& appSecret, const std::string& privateKey);

    GplayNetWorkStatus getNetworkType();

    /**
     * ext:
     *      0: CP don't want to customize the UI interface.
     *      1: CP want to customize the UI interface.
     */
    void preloadGroups(const std::string& jsonGroups, int ext = 0);

    void retryPreload();

    bool isLogined();

    const std::string& getUserID();

    void login(int callbackID, ActionResultCallback callback);

    void logout(int callbackID, ActionResultCallback callback);

    void quitGame();

    void share(int callbackID, const std::string& jsonShareInfo, ActionResultCallback callback);

    void pay(int callbackID, const std::string& jsonPayInfo, ActionResultCallback callback);

    const std::string& getOrderId();

    const std::string& getChannelID();

    void createShortcut(int callbackID, ActionResultCallback callback);

    bool isFunctionSupported(const std::string& funcName);

    std::string callSyncFunc(const std::string& funcName, const std::string& params);

    void callAsyncFunc(int callbackID, const std::string& funcName, 
        const std::string& params, ActionResultCallback callback);

    const std::string& getCurrPreloadSceneName();

    void noticePreloadBegin(const std::string& sceneName);
    
    void noticePreloadEnd(const std::string& sceneName);

    //SDK internal extension API
    std::string callExtensionSyncAPI(const char* method, bool& isFuncCallSuccess, const char* strArg, int intArg, double doubleArg);
    void callExtensionASyncAPI(const char* method, bool& isFuncCallSuccess, const char* strArg, int intArg, double doubleArg);

    typedef void(*NativeExtensionCallback)(const char* method, const char* strArg, int intArg, double doubleArg);
    void setNativeExtensionCallback(NativeExtensionCallback callback);
    //SDK internal extension API end

}}

#endif /** __GPLAY_H__ */