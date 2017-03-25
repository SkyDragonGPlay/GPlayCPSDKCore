# to include <GLES2/gl2platform>
APP_PLATFORM := android-10

# c++_static gnustl_static stlport_static
# APP_STL :=  stlport_static # assigned by outer command parameter
$(warning $(APP_STL))

APP_BUILD_SCRIPT := Android.mk
APP_MODULES := gplay_static
APP_ABI := armeabi armeabi-v7a x86 arm64-v8a

#-std=c++11
APP_CPPFLAGS := -frtti -fsigned-char

# APP_CPPFLAGS := -frtti -DCC_ENABLE_CHIPMUNK_INTEGRATION=1 -std=c++11 -fsigned-char
#APP_LDFLAGS := -latomic

#USE_ARM_MODE := 1

#ifeq ($(NDK_DEBUG),1)
  #APP_CPPFLAGS += -DCOCOS2D_DEBUG=1
  #APP_OPTIM := debug
#else
  #APP_CPPFLAGS += -DNDEBUG
  #APP_OPTIM := release
#endif
	