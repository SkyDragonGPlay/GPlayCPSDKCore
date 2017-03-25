package org.cocos2dx.lib;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.res.AssetManager;
import android.opengl.ETC1Util;
import android.util.Log;

public final class Cocos2dxETCLoader {
    private static final String ASSETS_PATH = "assets/";

    public static boolean loadTexture(String filePath) {
        if (!ETC1Util.isETC1Supported()) {
            return false;
        }

        if (filePath.length() == 0) {
            return false;
        }

        // Create ETC1Texture
        InputStream inputStream;
        ETC1Util.ETC1Texture texture;
        AssetManager assetManager;
        try {
            if (filePath.charAt(0) == '/') {
                // absolute path
                inputStream = new FileInputStream(filePath);
            } else {
                // remove prefix: "assets/"
                if (filePath.startsWith(ASSETS_PATH)) {
                    filePath = filePath.substring(ASSETS_PATH.length());
                }
                assetManager = Cocos2dxActivity.GAME_ACTIVITY.getAssets();
                inputStream = assetManager.open(filePath);
            }

            texture = ETC1Util.createTexture(inputStream);
            inputStream.close();
        } catch (Exception e) {
            Log.d("Cocos2dx", "Unable to create texture for " + filePath);

            texture = null;
        }

        if (texture != null) {
            boolean ret = true;

            try {
                final int width = texture.getWidth();
                final int height = texture.getHeight();
                final int length = texture.getData().remaining();

                final byte[] data = new byte[length];
                final ByteBuffer buf = ByteBuffer.wrap(data);
                buf.order(ByteOrder.nativeOrder());
                buf.put(texture.getData());

                nativeSetTextureInfo(width,
                        height,
                        data,
                        length);
            } catch (Exception e) {
                Log.d("Cocos2dxETCLoader","invoke native function error:" + e.toString());
                ret = false;
            }

            return ret;
        } else {
            return false;
        }
    }

    public static void init() {

    }

    private static native void nativeSetTextureInfo(final int width, final int height, final byte[] data,
                                                    final int dataLength);
}
