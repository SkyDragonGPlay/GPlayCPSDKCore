#ifndef __GPLAY_JNI_HELPER_H__
#define __GPLAY_JNI_HELPER_H__

#include <jni.h>
#include <string>

namespace gplay {namespace framework{

typedef struct PluginJniMethodInfo_
{
    JNIEnv *    env;
    jclass      classID;
    jmethodID   methodID;
} JniMethodInfo;

class JniHelper
{
public:
    static JavaVM* getJavaVM();
    static void setJavaVM(JavaVM *javaVM);
    static JNIEnv* getEnv();

    static bool getStaticMethodInfo(JniMethodInfo &methodinfo, const char *className, const char *methodName, const char *paramCode);
    static bool getMethodInfo(JniMethodInfo &methodinfo, const char *className, const char *methodName, const char *paramCode);
    static std::string jstring2string(jstring jstr);
    static jstring newStringUTF(JNIEnv* env, const std::string& utf8Str);

private:
    static JavaVM *_psJavaVM;
    static bool getMethodInfo_DefaultClassLoader(JniMethodInfo &methodinfo,
                                                 const char *className,
                                                 const char *methodName,
                                                 const char *paramCode);
};

}} //gplay::framework::

#endif // __GPLAY_JNI_HELPER_H__
