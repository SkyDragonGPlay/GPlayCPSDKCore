//
//  Const.h
//  cocosplay
//
//  Created by cocos2dx on 14-6-30.
//
//

#ifndef  __CONST_H_
#define  __CONST_H_


#include <string>

//#ifdef __cplusplus
//extern "C" {
//#endif

namespace gplay { namespace framework {

//urlencoude
std::string URLEncode(const std::string &sIn);
std::string URLDecode(const std::string &sIn);

//ckencode
std::string ckEncode(const std::string& str);
std::string ckDecode(const std::string& str);

std::string pluginDecode(const std::string& str);

std::string pluginEncode(const std::string& str);


}}

#endif
