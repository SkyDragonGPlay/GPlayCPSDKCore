#!/bin/sh
rm -r ./obj/
rm -r ./prebuilt/

for stl_kind in {"c++_static","gnustl_static"}
do
    echo $stl_kind
    ndk-build NDK_DEBUG=0 NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=./Android.mk NDK_APPLICATION_MK=./Application.mk APP_STL=$stl_kind
    for arch in {"arm64-v8a","armeabi","armeabi-v7a","x86"}
    do
        rm -r ./obj/local/$arch/objs/
        echo ./obj/local/$arch/objs/
    done
    mv ./obj/local ./obj/$stl_kind
done

mv ./obj/ ./prebuilt
