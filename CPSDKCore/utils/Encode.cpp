#include "Encode.h"
#include "ctype.h"
#include "JniHelper.h"
#include "base64.h"
#include "Utils.h"

namespace gplay { namespace framework {

extern "C" {
    JNIEXPORT jstring JNICALL Java_com_skydragon_gplay_unitsdk_framework_UnitSDK_nativePluginDecode(JNIEnv*  env, jobject thiz, jstring jstr)
    {
        std::string str = JniHelper::jstring2string(jstr);
        return JniHelper::newStringUTF(env, pluginDecode(str));
    }

    JNIEXPORT jstring JNICALL Java_com_skydragon_gplay_unitsdk_framework_UnitSDK_nativePluginEncode(JNIEnv*  env, jobject thiz, jstring jstr)
    {
        std::string str = JniHelper::jstring2string(jstr);
        return JniHelper::newStringUTF(env, pluginEncode(str));
    }
}
unsigned char ToHex(unsigned char x)
{
    return  x > 9 ? x + 55 : x + 48;
}

unsigned char FromHex(unsigned char x)
{
    unsigned char y;
    if (x >= 'A' && x <= 'Z') {
        y = x - 'A' + 10;
    }
    else if (x >= 'a' && x <= 'z') {
        y = x - 'a' + 10;
    }
    else if (x >= '0' && x <= '9') {
        y = x - '0';
    }
    else printf("should not less than 0.\n");
    return y;
}

std::string URLEncode(const std::string& str)
{
    std::string strTemp = "";
    size_t length = str.length();
    for (size_t i = 0; i < length; i++)
    {
        if (isalnum((unsigned char)str[i]) ||
            (str[i] == '-') ||
            (str[i] == '_') ||
            (str[i] == '.') ||
            (str[i] == '~'))
            strTemp += str[i];
        else if (str[i] == ' ')
            strTemp += "+";
        else
        {
            strTemp += '%';
            strTemp += ToHex((unsigned char)str[i] >> 4);
            strTemp += ToHex((unsigned char)str[i] % 16);
        }
    }
    return strTemp;
}

std::string URLDecode(const std::string& str)
{
    std::string strTemp = "";
    size_t length = str.length();
    for (size_t i = 0; i < length; i++)
    {
        if (str[i] == '+') strTemp += ' ';
        else if (str[i] == '%')
        {
            unsigned char high = FromHex((unsigned char)str[++i]);
            unsigned char low = FromHex((unsigned char)str[++i]);
            strTemp += high*16 + low;
        }
        else strTemp += str[i];
    }
    return strTemp;
}

///ckEncode
std::string ckEncode(const std::string& str)
{
    return str;
}

std::string ckDecode(const std::string& str)
{
    return str;
}

std::string pluginDecode(const std::string& str)
{
    return str;
}

std::string pluginEncode(const std::string& str)
{
    return str;
}
    
}}
