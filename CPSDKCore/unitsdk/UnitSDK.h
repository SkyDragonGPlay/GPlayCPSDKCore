#ifndef GPLAY_GPLAYUNITSDK_H
#define GPLAY_GPLAYUNITSDK_H

#include <vector>
#include <map>
#include <string>

using namespace std;
namespace gplay { namespace framework {

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
    SHORTCUT_RESULT_FAILED = 40001
} ActionResultCode;

class UnitSDK;

/**
 * 异步操作的回调监听器, 当回调触发时, unitSDK 会将之前传入的 callbackID 透传回来
 */
class ActionResultListener
{
public:
    // code: the result code to indicate the situation of execution
    // msg : the information of callback
    virtual void onActionResult(UnitSDK* unitSDK, ActionResultCode code, const char* msg, int callbackID) = 0;
};

class UnitSDK {
public:
    static UnitSDK* getInstance();

    virtual void init(const string& appKey,const string& appSecret,const string& privateKey) = 0;
    
    virtual void setAsyncActionResultListener(ActionResultListener* listener) = 0;
    virtual ActionResultListener* getAsyncActionResultListener() = 0;

    virtual bool isFunctionSupported(const string& functionName) = 0;
    virtual bool isLogined() = 0;

    virtual const string& getUserID() = 0; // if not login, return empty string
    virtual const string& getOrderId() = 0;
    virtual const string& getChannelId() = 0;

    virtual void login(int callbackID) = 0;
    virtual void payForProduct(int callbackID, const string& info) = 0;
    virtual void share(int callbackID, const string& info) = 0;
    virtual void createShortCut(int callbackID) = 0;

    virtual string callSyncStringFunc(const string& funcName, const string& jsonParams) = 0;
    virtual void callAsyncFunc(int callbackID, const string& funcName, const string& jsonParams) = 0;

    static void resetPayState() {_paying = false; }

    static bool _paying;
    
private:
    static UnitSDK* s_unitSDK;
};

}} // namespace gplay::framework::

#endif //GPLAY_GPLAYUNITSDK_H
