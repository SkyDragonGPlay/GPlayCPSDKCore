/****************************************************************************
Copyright (c) 2010-2011 cocos2d-x.org

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/
package org.cocos2dx.lib;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.skydragon.gplay.runtime.bridge.CocosRuntimeBridge;
import com.skydragon.gplay.runtime.bridge.IBridgeProxy;
import com.skydragon.gplay.runtime.bridge.ICallback;
import com.skydragon.gplay.runtime.callback.OnRuntimeStatusChangedListener;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.skydragon.gplay.runtime.bridge.CocosRuntimeBridge.nativeGplayKeyEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class Cocos2dxRenderer implements GLSurfaceView.Renderer, OnRuntimeStatusChangedListener{
    public static final String TAG = "Cocos2dxRenderer";

    // ===========================================================
    // Constants
    // ===========================================================

    private final static long NANO_SECONDS_PER_SECOND = 1000000000L;
    private final static long NANO_SECONDS_PER_MICROSECOND = 1000000L;

    private final static long sDefaultInterval = (long) (1.0 / 60 * NANO_SECONDS_PER_SECOND);
    private static long sAnimationInterval = (long) (1.0 / 60 * NANO_SECONDS_PER_SECOND);

    // ===========================================================
    // Fields
    // ===========================================================

    private long mLastTickInNanoSeconds;
    private int mScreenWidth;
    private int mScreenHeight;
    private boolean mNativeInitCompleted = false;
    private boolean mIsNativeLibraryLoaded = false;

    // ===========================================================
    // Constructors
    // ===========================================================
    public Cocos2dxRenderer() {
        CocosRuntimeBridge.getInstance().addStatusChangedListener(this);
    }

    public static void setAnimationInterval(final float animationInterval) {
        sAnimationInterval = (long) (animationInterval * NANO_SECONDS_PER_SECOND);
    }

    public static void setAnimationInterval(final double animationInterval) {
        sAnimationInterval = (long) (animationInterval * NANO_SECONDS_PER_SECOND);
    }

    public void setScreenWidthAndHeight(final int surfaceWidth, final int surfaceHeight) {
        mScreenWidth = surfaceWidth;
        mScreenHeight = surfaceHeight;
    }

    protected void initNativeLibrary(final int width , final int height) {
        if (mNativeInitCompleted) {
            Log.d(TAG, "initNativeLibrary was invoked!");
            return;
        }

        Log.d(TAG, "before nativeInit, width: " + width + ", height:" + height);

        IBridgeProxy bridgeProxy = CocosRuntimeBridge.getInstance().getBridgeProxy();
        String gameOrientation = (String)bridgeProxy.invokeMethodSync("getGameOrientation", null);

        Log.d(TAG, "Game orientation: " + gameOrientation);

        boolean isSizeCorrect = false;
        if (isLandscape(gameOrientation)) {
            if (width >= height) {
                isSizeCorrect = true;
            }
        } else {
            if (width <= height) {
                isSizeCorrect = true;
            }
        }

        if (!isSizeCorrect) {
            Log.e(TAG, "screenSize isn't correct!");
            return;
        }

        nativeInit(width, height);
        Log.d(TAG, "after nativeInit ...");

        mLastTickInNanoSeconds = System.nanoTime();
        mNativeInitCompleted = true;

        Cocos2dxGLSurfaceView.getInstance().getLayoutParams().width = width;
        Cocos2dxGLSurfaceView.getInstance().getLayoutParams().height = height;

        //GPlay模式下和原生模式有差异,原生模式下nativeOnResume第一次被调用是在activity的on resume被调用
        /*Cocos2dxActivity.COCOS_ACTIVITY.runOnGLThread(new Runnable() {
            @Override
            public void run() {
                nativeOnResume();
            }
        });*/
    }

    public void stopRendering() {
        mNativeInitCompleted = false;
        mIsNativeLibraryLoaded = false;
    }

    @Override
    public void onSurfaceCreated(final GL10 GL10, final EGLConfig EGLConfig) {
        Log.d(TAG, "onSurfaceCreated ...");
    }

    @Override
    public void onSurfaceChanged(final GL10 GL10, final int width, final int height) {
        Log.d(TAG, "onSurfaceChanged, width: " + width + ", height:" + height);

        boolean sizeChanged = width != mScreenWidth || height != mScreenHeight;
        setScreenWidthAndHeight(width, height);
        if (!mIsNativeLibraryLoaded) {
            Log.w(TAG, "native library wasn't loaded!");
            return;
        }

        initNativeLibrary(width, height);

        if (mNativeInitCompleted && sizeChanged) {
            nativeOnSurfaceChanged(width, height);
        }
    }

    public static final String KEY_CAPTURE_SCREEN_METHOD = "CAPTURE_SCREEN";
    public static final String KEY_CAPTURE_SCREEN_FILE_NAME = "FILE_NAME";
    public static final String KEY_CAPTURE_SCREEN_QUALITY = "QUALITY";
    public static final String KEY_CAPTURE_SCREEN_RESULT_CODE = "result_code";
    public static final String KEY_CAPTURE_SCREEN_RESULT_MSG = "result_msg";

    private ICallback mCPCaptureScreenCallback = null;
    private Map<String, Object> mCPCaptureScreenArgs = null;
    private ICallback mChannelCaptureScreenCallback = null;
    private Map<String, Object> mChannelCaptureScreenArgs = null;

    public void setCaptureScreenCallbackByCP(ICallback callback, Map<String, Object> args) {
        if (callback != null && args != null) {
            mCPCaptureScreenCallback = callback;
            mCPCaptureScreenArgs = new HashMap<>(args);
        }
    }

    public void setCaptureScreenCallbackByChannel(ICallback callback, Map<String, Object> args) {
        if (args != null)
            Log.i(TAG, "setCaptureScreenCallbackByChannel args:" + args.toString());
        else
            Log.w(TAG, "setCaptureScreenCallbackByChannel args is null");

        if (callback == null)
            Log.w(TAG, "setCaptureScreenCallbackByChannel callback is null");

        if (callback != null && args != null) {
            mChannelCaptureScreenCallback = callback;
            mChannelCaptureScreenArgs = new HashMap<>(args);
        }
    }

    public void takeScreenShot(GL10 gl) {
        Log.i(TAG, "takeScreenShot begin...");

        FileOutputStream CPFileOutputStream = null;
        FileOutputStream ChannelFileOutputStream = null;
        String CPImageFilePath = null;
        String ChannelImageFilePath = null;
        Bitmap bitmap = null;
        int RESULT_CODE = -1;
        String result_msg = "capture screen success";

        try {
            if (mCPCaptureScreenArgs != null) {
                CPImageFilePath = (String) mCPCaptureScreenArgs.get(KEY_CAPTURE_SCREEN_FILE_NAME);
                File imagePath = new File(CPImageFilePath);
                if (imagePath.exists())
                    imagePath.delete();
                CPFileOutputStream = new FileOutputStream(imagePath);
            }

            if (mChannelCaptureScreenArgs != null) {
                ChannelImageFilePath = (String) mChannelCaptureScreenArgs.get(KEY_CAPTURE_SCREEN_FILE_NAME);
                File imagePath = new File(ChannelImageFilePath);
                if (imagePath.exists())
                    imagePath.delete();
                ChannelFileOutputStream = new FileOutputStream(imagePath);
            }

            bitmap = getBitmapFromGL(mScreenWidth, mScreenHeight, gl);

            if (CPFileOutputStream != null) {
                int quality = 90;
                if (mCPCaptureScreenArgs.containsKey(KEY_CAPTURE_SCREEN_QUALITY)) {
                    quality = (Integer) mCPCaptureScreenArgs.get(KEY_CAPTURE_SCREEN_QUALITY);
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, CPFileOutputStream);
                CPFileOutputStream.flush();
            }

            if (ChannelFileOutputStream != null) {
                int quality = 90;
                if (mChannelCaptureScreenArgs.containsKey(KEY_CAPTURE_SCREEN_QUALITY)) {
                    quality = (Integer) mChannelCaptureScreenArgs.get(KEY_CAPTURE_SCREEN_QUALITY);
                }

                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, ChannelFileOutputStream);
                ChannelFileOutputStream.flush();
            }

            RESULT_CODE = 1;
        } catch (Exception e) {
            e.printStackTrace();
            result_msg = "takeScreenShot: capture screen failed: " + e.toString();
        } finally {
            try {
                if(CPFileOutputStream != null)
                    CPFileOutputStream.close();

                if(ChannelFileOutputStream != null)
                    ChannelFileOutputStream.close();

                if(bitmap != null)
                    bitmap.recycle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mCPCaptureScreenArgs != null) {
            mCPCaptureScreenArgs.put(KEY_CAPTURE_SCREEN_RESULT_CODE, RESULT_CODE);
            mCPCaptureScreenArgs.put("file_name", CPImageFilePath);
            mCPCaptureScreenCallback.onCallback(KEY_CAPTURE_SCREEN_METHOD, mCPCaptureScreenArgs);
            mCPCaptureScreenArgs = null;
            mCPCaptureScreenCallback = null;
            Log.i(TAG, "takeScreenShot finished for CP");
        }

        if (mChannelCaptureScreenArgs != null) {
            mChannelCaptureScreenArgs.put(KEY_CAPTURE_SCREEN_RESULT_CODE, RESULT_CODE);
            mChannelCaptureScreenArgs.put("file_name", ChannelImageFilePath);
            mChannelCaptureScreenArgs.put(KEY_CAPTURE_SCREEN_RESULT_MSG, result_msg);

            final ICallback channelCaptureScreenCallback = mChannelCaptureScreenCallback;
            final Map<String, Object> args = new HashMap<>(mChannelCaptureScreenArgs);

            Cocos2dxActivity.COCOS_ACTIVITY.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "takeScreenShot:invoking channel callback");
                    channelCaptureScreenCallback.onCallback(KEY_CAPTURE_SCREEN_METHOD, args);
                }
            });

            mChannelCaptureScreenArgs = null;
            mChannelCaptureScreenCallback = null;
            Log.i(TAG, "takeScreenShot finished for channel");
        }

        Log.i(TAG, "takeScreenShot end");
    }

    private Bitmap getBitmapFromGL(int w, int h, GL10 gl) {
        int b[] = new int[w * (h)];
        int bt[] = new int[w * h];
        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);
        gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);
        for (int i = 0, k = 0; i < h; i++, k++) {
            for (int j = 0; j < w; j++) {
                int pix = b[i * w + j];
                int pb = (pix >> 16) & 0xff;
                int pr = (pix << 16) & 0xffff0000;
                int pix1 = (pix & 0xff00ff00) | pr | pb;
                bt[(h - k - 1) * w + j] = pix1;
            }
        }

        return Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        /*
         * No need to use algorithm in default(60 FPS) situation,
         * since onDrawFrame() was called by system 60 times per second by default.
         */
        if (sAnimationInterval <= sDefaultInterval) {
            if (mIsNativeLibraryLoaded) {
                nativeRender();
                if (mCPCaptureScreenCallback != null || mChannelCaptureScreenCallback != null) {
                    takeScreenShot(gl);
                }
            }
        } else {
            final long now = System.nanoTime();
            final long interval = now - mLastTickInNanoSeconds;
        
            if (interval < sAnimationInterval) {
                try {
                    Thread.sleep((sAnimationInterval - interval) / NANO_SECONDS_PER_MICROSECOND);
                } catch (final Exception e) {
                }
            }
            /*
             * Render time MUST be counted in, or the FPS will slower than appointed.
            */
            mLastTickInNanoSeconds = System.nanoTime();
            if (mIsNativeLibraryLoaded) {
                nativeRender();
                if (mCPCaptureScreenCallback != null || mChannelCaptureScreenCallback != null) {
                    takeScreenShot(gl);
                }
            }
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================

    private static native void nativeTouchesBegin(final int id, final float x, final float y);
    private static native void nativeTouchesEnd(final int id, final float x, final float y);
    private static native void nativeTouchesMove(final int[] ids, final float[] xs, final float[] ys);
    private static native void nativeTouchesCancel(final int[] ids, final float[] xs, final float[] ys);
    private static native boolean nativeKeyEvent(final int keyCode,boolean isPressed);
    private static native void nativeRender();
    private static native void nativeInit(final int width, final int height);
    private static native void nativeOnSurfaceChanged(final int width, final int height);
    private static native void nativeOnPause();
    private static native void nativeOnResume();

    public void handleActionDown(final int id, final float x, final float y) {
        if (mIsNativeLibraryLoaded) {
            nativeTouchesBegin(id, x, y);
        }
    }

    public void handleActionUp(final int id, final float x, final float y) {
        if (mIsNativeLibraryLoaded) {
            nativeTouchesEnd(id, x, y);
        }
    }

    public void handleActionCancel(final int[] ids, final float[] xs, final float[] ys) {
        if (mIsNativeLibraryLoaded) {
            nativeTouchesCancel(ids, xs, ys);
        }
    }

    public void handleActionMove(final int[] ids, final float[] xs, final float[] ys) {
        if (mIsNativeLibraryLoaded) {
            nativeTouchesMove(ids, xs, ys);
        }
    }

    public void handleKeyDown(final int keyCode) {
        if (mIsNativeLibraryLoaded) {
            //nativeKeyEvent(keyCode, true); // Mr.robot: use gplay key event instead
            nativeGplayKeyEvent(keyCode, true);
        }
    }

    public void handleKeyUp(final int keyCode) {
        if (mIsNativeLibraryLoaded) {
            //nativeKeyEvent(keyCode, false); // Mr.robot: use gplay key event instead
            nativeGplayKeyEvent(keyCode, false);
        }
    }

    public void handleOnPause() {
        /**
         * onPause may be invoked before onSurfaceCreated, 
         * and engine will be initialized correctly after
         * onSurfaceCreated is invoked. Can not invoke any
         * native method before onSurfaceCreated is invoked
         */
        if (! mNativeInitCompleted)
            return;

        if (mIsNativeLibraryLoaded) {
            Cocos2dxHelper.onEnterBackground();
            nativeOnPause();
        }
    }

    public void handleOnResume() {
        if (mIsNativeLibraryLoaded) {
            Cocos2dxHelper.onEnterForeground();
            nativeOnResume();
        }
    }

    private static native void nativeInsertText(final String text);
    private static native void nativeDeleteBackward();
    private static native String nativeGetContentText();

    public void handleInsertText(final String text) {
        if (mIsNativeLibraryLoaded) {
            nativeInsertText(text);
        }
    }

    public void handleDeleteBackward() {
        if (mIsNativeLibraryLoaded) {
            nativeDeleteBackward();
        }
    }

    public String getContentText() {
        return nativeGetContentText();
    }

    @Override
    public void onNativeLibraryLoaded() {
        mIsNativeLibraryLoaded = true;
    }

    private static final String ORIENTATION_PORTRAIT = "portrait";
    private static final String ORIENTATION_LANDSCAPE = "landscape";
    private static final String ORIENTATION_REVERSE_PORTRAIT = "reverse_portrait";
    private static final String ORIENTATION_REVERSE_LANDSCAPE = "reverse_landscape";
    private static final String ORIENTATION_SENSOR_PORTRAIT = "sensor_portrait";
    private static final String ORIENTATION_SENSOR_LANDSCAPE = "sensor_landscape";

    private static int getActivityOrientation(String orientation) {
        if (TextUtils.isEmpty(orientation)) {
            return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
        int orientation_int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if (orientation.equalsIgnoreCase(ORIENTATION_LANDSCAPE))
            orientation_int = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        else if (orientation.equalsIgnoreCase(ORIENTATION_PORTRAIT))
            orientation_int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        else if (orientation.equalsIgnoreCase(ORIENTATION_REVERSE_LANDSCAPE))
            orientation_int = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        else if (orientation.equalsIgnoreCase(ORIENTATION_REVERSE_PORTRAIT))
            orientation_int = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        else if (orientation.equalsIgnoreCase(ORIENTATION_SENSOR_LANDSCAPE))
            orientation_int = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        else if (orientation.equalsIgnoreCase(ORIENTATION_SENSOR_PORTRAIT))
            orientation_int = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        return orientation_int;
    }

    private static boolean isLandscape(String orientation) {
        return isLandscape(getActivityOrientation(orientation));
    }

    private static boolean isLandscape(int orientation) {
        return orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    }

    private static Point getScreenSize(Activity activity) {
        WindowManager w = activity.getWindowManager();
        Display d = w.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);
        // since SDK_INT = 1;
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        // includes window decorations (status bar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
            try {
                widthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                heightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
        }
        // includes window decorations (status bar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                Point realSize = new Point();
                Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                widthPixels = realSize.x;
                heightPixels = realSize.y;
            } catch (Exception ignored) {
            }
        }
        return new Point(widthPixels, heightPixels);
    }
}
