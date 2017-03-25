#ifndef __GPLAY_RUNTIME_H_
#define __GPLAY_RUNTIME_H_

enum RTCallbackType{
    RT_START_GAME = 0,          // 开始游戏
    RT_QUIT_GAME,               // 退出游戏
    RT_GET_RUNTIME_SO_VERSION,  // 获取 runtime 版本
    RT_PRELOAD_RESPONSE,        // 预加载分组的回调
    RT_DOWNLOAD_REMOTE_FILE,    // 下载网络文件
    RT_SET_RUNTIME_CONFIG,      // 设置 runtime 配置
    RT_SET_XXTEA_KEY_AND_SIGN   // 设置加密签名
};

// @note arg memory must be allocated at stack
typedef void* (*RTCallback)(RTCallbackType type, void* arg1, void* arg2, void* arg3);

void RTSetListener(RTCallback callback);
int  RTGetNetworkTypeJNI();
void RTGetRemoteFileDataJNI(const char* config, long ext);
void RTQuitGameJNI();
void RTPreloadGroupsJNI(const char* str, int ext);

#endif /** __GPLAY_RUNTIME_H_ */
