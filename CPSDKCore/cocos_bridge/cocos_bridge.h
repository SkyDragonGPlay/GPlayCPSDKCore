#ifndef __GPLAY_COCOS_BRIDGE_H__
#define __GPLAY_COCOS_BRIDGE_H__

#include <string>

namespace gplay {

    void RTRuntimeInit();
    void RTSetDefaultResourceRootPath(const char* resRootPath);
    void RTAddSearchPath(const char* resSearchPath);
    bool RTDipspatchEvent(int keycode, bool isPressed);
    void RTPreloadResponse(const char* responseJson, bool isDone, long ext);

    typedef void(*GPlayPreloadSuccessCallback)(int,const char*);
    void RTSetPreloadSuccessCallback(GPlayPreloadSuccessCallback callback);

    typedef void(*GPlayPreloadResponseCallback)(int,int,const std::string&,float,float);
    void RTSetPreloadResponseCallback(GPlayPreloadResponseCallback callback);

    void RTQuitGame();
}
#endif /** __GPLAY_COCOS_BRIDGE_H__ */