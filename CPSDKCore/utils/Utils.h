#ifndef __PLUGIN_UTILS_H__
#define __PLUGIN_UTILS_H__

#include <map>
#include <list>
#include <vector>
#include "JniHelper.h"
#include <android/log.h>
//#include <android_native_app_glue.h>

namespace gplay { namespace framework {

class Utils
{
public:
    static JNIEnv* getEnv();
    static void outputLog(int type, const char* logTag, const char* pFormat, ...);
};

}}

#endif //__PLUGIN_UTILS_H__
