#ifndef __GPLAY_GPLAYUNITSDKIMPL_H__
#define __GPLAY_GPLAYUNITSDKIMPL_H__

#include "UnitSDK.h"
#include <jni.h>
#include <vector>

using namespace std;
namespace gplay { namespace framework {

class UnitSDKObject : public  UnitSDK{
public:
    UnitSDKObject();
    virtual ~UnitSDKObject();
    virtual void init(const string& appKey,const string& appSecret,const string& privateKey);

    virtual void setAsyncActionResultListener(ActionResultListener* listener);
    virtual ActionResultListener* getAsyncActionResultListener();

    virtual bool isFunctionSupported(const string& functionName);
    virtual bool isLogined();

    virtual const string& getChannelId();
    virtual const string& getOrderId();
    virtual const string& getUserID();

    virtual void login(int callbackID);
    virtual void payForProduct(int callbackID, const string& info);
    virtual void share(int callbackID, const string& info);
    virtual void createShortCut(int callbackID);
    
    virtual string callSyncStringFunc(const string& funcName, const string& jsonParams);
    virtual void callAsyncFunc(int callbackID, const string& funcName, const string& jsonParams);

    void onAsyncActionResult(int callbackID, int ret, const char* msg);

private:
    static jmethodID getJProxyCallSyncFunctionMethodID();
    static jmethodID getJProxyCallAsynFunctionMethodID();
    static string callJavaSyncStringFunc(const string& funcName, const string& jsonParams);
    static void callJavaAsyncFunc(int callbackID, const string& funcName, const string& jsonParams);
    
    static jobject jGlobalProxyObj;
    static const string jProxyClass;

    ActionResultListener* _asyncResultListener;

    std::string _channelID;
    std::string _orderID;
    std::string _userID;
    
public:
    static map<int, int> _payingCallbacks;
};
    
}} // namespace gplay::framework::


#endif //__GPLAY_GPLAYUNITSDKIMPL_H__