
package com.unity3d.player;

public class NativeLoader {
    static final native boolean load(String shareLibraryPath);

    static final native boolean unload();
}

