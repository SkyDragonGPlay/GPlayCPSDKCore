/****************************************************************************
Copyright (c) 2010-2012 cocos2d-x.org
Copyright (c) 2013-2014 Chukong Technologies Inc.

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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.skydragon.gplay.runtime.bridge.CocosRuntimeBridge;
import com.skydragon.gplay.runtime.bridge.IBridgeProxy;
import com.skydragon.gplay.runtime.bridge.ICallback;

import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Cocos2dxHelper {
    public static final String TAG = "Cocos2dxHelper";

    // ===========================================================
    // Fields
    // ===========================================================

    private static Cocos2dxMusic sCocos2dMusic;
    private static Cocos2dxSound sCocos2dSound;
    private static AssetManager sAssetManager;
    private static Cocos2dxAccelerometer sCocos2dxAccelerometer;
    private static boolean sAccelerometerEnabled;
    private static boolean sActivityVisible;
    private static String sPackageName;
    private static String sFileDirectory;
    private static Set<OnActivityResultListener> onActivityResultListeners = new LinkedHashSet<>();
    private static Cocos2dxDatabase sUserDefaultDatabase;
    private static Vibrator sVibrateService = null;

    //Enhance API modification begin
    private static final int BOOST_TIME = 7;
    //Enhance API modification end

    // ===========================================================
    // Constructors
    // ===========================================================

    public static void runOnGLThread(final Runnable r) {
        Cocos2dxActivity.COCOS_ACTIVITY.runOnGLThread(r);
    }

    public static void initJNI() {
        final ApplicationInfo applicationInfo = Cocos2dxActivity.GAME_ACTIVITY.getApplicationInfo();
        sPackageName = applicationInfo.packageName;

        IBridgeProxy bridgeProxy = CocosRuntimeBridge.getInstance().getBridgeProxy();
        String gameDir = (String) bridgeProxy.invokeMethodSync("getGameDir", null);
        sFileDirectory = gameDir + "writableDir";
        ensureDirExist(Cocos2dxHelper.sFileDirectory);

        nativeSetApkPath(applicationInfo.sourceDir);
    }

    private static boolean sInitialized = false;
    public static void init(final Activity activity, Cocos2dxActivity cocos2dxActivity) {
        if (!sInitialized) {
            sCocos2dxAccelerometer = new Cocos2dxAccelerometer(activity);
            sCocos2dMusic = new Cocos2dxMusic(activity);
            sCocos2dSound = new Cocos2dxSound(activity);
            sAssetManager = activity.getAssets();
            nativeSetContext(cocos2dxActivity, sAssetManager);

            sUserDefaultDatabase = null;
            sVibrateService = (Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE);

            sInitialized = true;
        }
    }

    private static void ensureDirExist(String sPath) {
        File f = new File(sPath);
        if(!f.exists()) {
            f.mkdirs();
        }
    }

    //Enhance API modification begin
    private static ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            fastLoading(BOOST_TIME);
        }

        public void onServiceDisconnected(ComponentName name) {
            Cocos2dxActivity.GAME_ACTIVITY.getApplicationContext().unbindService(connection);
        }
    };
    //Enhance API modification end
    
    public static Activity getActivity() {
        return Cocos2dxActivity.GAME_ACTIVITY;
    }
    
    public static void addOnActivityResultListener(OnActivityResultListener listener) {
        onActivityResultListeners.add(listener);
    }
    
    public static Set<OnActivityResultListener> getOnActivityResultListeners() {
        return onActivityResultListeners;
    }
    
    public static boolean isActivityVisible(){
        return sActivityVisible;
    }

    // ===========================================================
    // Methods
    // ===========================================================

    private static native void nativeSetApkPath(final String pApkPath);

    private static native void nativeSetEditTextDialogResult(final byte[] pBytes);

    private static native void nativeSetContext(final Context pContext, final AssetManager pAssetManager);

    public static String getCocos2dxPackageName() {
        return sPackageName;
    }
    public static String getCocos2dxWritablePath() {
        return sFileDirectory;
    }

    public static String getCurrentLanguage() {
        return Locale.getDefault().getLanguage();
    }
    
    public static String getDeviceModel(){
        return Build.MODEL;
    }

    public static AssetManager getAssetManager() {
        return sAssetManager;
    }

    public static void enableAccelerometer() {
        sAccelerometerEnabled = true;
        sCocos2dxAccelerometer.enable();
    }

    public static void setAccelerometerInterval(float interval) {
        sCocos2dxAccelerometer.setInterval(interval);
    }

    public static void disableAccelerometer() {
        sAccelerometerEnabled = false;
        sCocos2dxAccelerometer.disable();
    }

    public static void setKeepScreenOn(boolean value) {
        Cocos2dxActivity.COCOS_ACTIVITY.setKeepScreenOn(value);
    }

    public static void vibrate(float duration) {
        sVibrateService.vibrate((long) (duration * 1000));
    }

    public static boolean openURL(String url) { 
        boolean ret = false;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            Cocos2dxActivity.GAME_ACTIVITY.startActivity(i);
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static void preloadBackgroundMusic(final String pPath) {
        sCocos2dMusic.preloadBackgroundMusic(pPath);
    }

    public static void playBackgroundMusic(final String pPath, final boolean isLoop) {
        sCocos2dMusic.playBackgroundMusic(pPath, isLoop);
    }

    public static void resumeBackgroundMusic() {
        sCocos2dMusic.resumeBackgroundMusic();
    }

    public static void pauseBackgroundMusic() {
        sCocos2dMusic.pauseBackgroundMusic();
    }

    public static void stopBackgroundMusic() {
        Cocos2dxHelper.sCocos2dMusic.stopBackgroundMusic();
    }

    public static void rewindBackgroundMusic() {
        sCocos2dMusic.rewindBackgroundMusic();
    }

    public static boolean willPlayBackgroundMusic() {
        return Cocos2dxHelper.sCocos2dMusic.willPlayBackgroundMusic();
    }

    public static boolean isBackgroundMusicPlaying() {
        return sCocos2dMusic.isBackgroundMusicPlaying();
    }

    public static float getBackgroundMusicVolume() {
        return sCocos2dMusic.getBackgroundVolume();
    }

    public static void setBackgroundMusicVolume(final float volume) {
        sCocos2dMusic.setBackgroundVolume(volume);
    }

    public static void preloadEffect(final String path) {
        sCocos2dSound.preloadEffect(path);
    }

    public static int playEffect(final String path, final boolean isLoop, final float pitch, final float pan, final float gain) {
        return sCocos2dSound.playEffect(path, isLoop, pitch, pan, gain);
    }

    public static void resumeEffect(final int soundId) {
        sCocos2dSound.resumeEffect(soundId);
    }

    public static void pauseEffect(final int soundId) {
        sCocos2dSound.pauseEffect(soundId);
    }

    public static void stopEffect(final int soundId) {
        sCocos2dSound.stopEffect(soundId);
    }

    public static float getEffectsVolume() {
        return sCocos2dSound.getEffectsVolume();
    }

    public static void setEffectsVolume(final float volume) {
        sCocos2dSound.setEffectsVolume(volume);
    }

    public static void unloadEffect(final String path) {
        sCocos2dSound.unloadEffect(path);
    }

    public static void pauseAllEffects() {
        sCocos2dSound.pauseAllEffects();
    }

    public static void resumeAllEffects() {
        sCocos2dSound.resumeAllEffects();
    }

    public static void stopAllEffects() {
        sCocos2dSound.stopAllEffects();
    }

    public static void onResume() {
        sActivityVisible = true;
        if (sAccelerometerEnabled && sInitialized) {
            sCocos2dxAccelerometer.enable();
        }
    }

    public static void onPause() {
        sActivityVisible = false;
        if (sAccelerometerEnabled && sInitialized) {
            sCocos2dxAccelerometer.disable();
        }
    }

    private static native void nativeOnCaptureScreenResult(int result);

    public static void captureScreen(final String params) {
        if (Cocos2dxGLSurfaceView.getInstance() != null) {
            Cocos2dxRenderer renderer = Cocos2dxGLSurfaceView.getInstance().getCocos2dxRenderer();
            if (renderer != null) {
                Map<String, Object> args = new HashMap<>();
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(params);
                }
                catch (Exception e) {
                    args.put(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_FILE_NAME, params);
                }

                if (jsonObject != null) {
                    if (jsonObject.has(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_FILE_NAME)) {
                        args.put(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_FILE_NAME,
                                jsonObject.optString(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_FILE_NAME));
                    }
                    if (jsonObject.has(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_QUALITY)) {
                        args.put(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_QUALITY,
                                jsonObject.optInt(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_QUALITY, 90));
                    }
                }

                renderer.setCaptureScreenCallbackByCP(new ICallback() {
                    @Override
                    public Object onCallback(String from, Map<String, Object> args) {
                        int result = (Integer) args.get(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_RESULT_CODE);
                        nativeOnCaptureScreenResult(result);
                        return null;
                    }
                }, args);

                return;
            }
        }

        nativeOnCaptureScreenResult(-1);
    }

    public static void end() {
        if (sCocos2dSound != null) {
            sCocos2dSound.end();
        }
        if (sCocos2dMusic != null) {
            sCocos2dMusic.end();
        }
    }

    public static void onEnterBackground() {
        if (sCocos2dSound != null) {
            sCocos2dSound.onEnterBackground();
        }
        if (sCocos2dMusic != null) {
            sCocos2dMusic.onEnterBackground();
        }
    }
    
    public static void onEnterForeground() {
        if (sCocos2dSound != null) {
            sCocos2dSound.onEnterForeground();
        }
        if (sCocos2dMusic != null) {
            sCocos2dMusic.onEnterForeground();
        }
    }
    
    public static void terminateProcess() {
        Log.d(TAG, "Cocos2dxHelper terminateProcess...");

        Cocos2dxActivity.GAME_IS_RUNNING = false;

        if (sUserDefaultDatabase != null) {
            sUserDefaultDatabase.destory();
            sUserDefaultDatabase = null;
        }

        CocosRuntimeBridge bridge = CocosRuntimeBridge.getInstance();
        IBridgeProxy runtimeProxy = bridge.getBridgeProxy();
        if (runtimeProxy != null) {
            runtimeProxy.invokeMethodSync("onQuitGame", null);
        } else {
            Log.e(TAG, "Cocos Runtime proxy is null!");
        }
        Cocos2dxActivity.COCOS_ACTIVITY.getGLSurfaceView().getCocos2dxRenderer().stopRendering();
    }

    private static void showDialog(final String pTitle, final String pMessage) {
        Cocos2dxActivity.COCOS_ACTIVITY.showDialog(pTitle, pMessage);
    }


    private static void showEditTextDialog(final String pTitle, final String pMessage, final int pInputMode, final int pInputFlag, final int pReturnType, final int pMaxLength) {
        Cocos2dxActivity.COCOS_ACTIVITY.showEditTextDialog(pTitle, pMessage, pInputMode, pInputFlag, pReturnType, pMaxLength);
    }

    public static void setEditTextDialogResult(final String pResult) {
        try {
            final byte[] bytesUTF8 = pResult.getBytes("UTF8");

            Cocos2dxActivity.COCOS_ACTIVITY.runOnGLThread(new Runnable() {
                @Override
                public void run() {
                    Cocos2dxHelper.nativeSetEditTextDialogResult(bytesUTF8);
                }
            });
        } catch (UnsupportedEncodingException pUnsupportedEncodingException) {
            /* Nothing. */
        }
    }

    public static int getDPI()
    {
        if (Cocos2dxActivity.GAME_ACTIVITY != null)
        {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = Cocos2dxActivity.GAME_ACTIVITY.getWindowManager();
            if (wm != null)
            {
                Display d = wm.getDefaultDisplay();
                if (d != null)
                {
                    d.getMetrics(metrics);
                    return (int)(metrics.density*160.0f);
                }
            }
        }
        return -1;
    }

    // ===========================================================
    // Functions for CCUserDefault
    // ===========================================================
    private static boolean lazyInitUserDefault() {
        if (sUserDefaultDatabase == null) {
            sUserDefaultDatabase = new Cocos2dxDatabase();
            if (sUserDefaultDatabase.init("gp_game_config.sqlite", "data")) {
                return true;
            } else {
                Log.e(TAG, "Cocos2dxHelper.lazyInitUserDefault failed!");
                return false;
            }
        }
        return true;
    }

    public static boolean getBoolForKey(String key, boolean defaultValue) {
        if (!lazyInitUserDefault()) {
            return defaultValue;
        }
        String value = sUserDefaultDatabase.getItem(key);
        if (value == null) {
            return defaultValue;
        }
        if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        }
        return defaultValue;
    }

    public static int getIntegerForKey(String key, int defaultValue) {
        if (!lazyInitUserDefault()) {
            return defaultValue;
        }
        String value = sUserDefaultDatabase.getItem(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public static float getFloatForKey(String key, float defaultValue) {
        if (!lazyInitUserDefault()) {
            return defaultValue;
        }
        String value = sUserDefaultDatabase.getItem(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public static double getDoubleForKey(String key, double defaultValue) {
        if (!lazyInitUserDefault()) {
            return defaultValue;
        }
        String value = sUserDefaultDatabase.getItem(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public static String getStringForKey(String key, String defaultValue) {
        if (!lazyInitUserDefault()) {
            return defaultValue;
        }
        String value = sUserDefaultDatabase.getItem(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static void setBoolForKey(String key, boolean value) {
        if (!lazyInitUserDefault()) {
            return;
        }
        sUserDefaultDatabase.setItem(key, Boolean.toString(value));
    }

    public static void setIntegerForKey(String key, int value) {
        if (!lazyInitUserDefault()) {
            return;
        }
        sUserDefaultDatabase.setItem(key, Integer.toString(value));
    }

    public static void setFloatForKey(String key, float value) {
        if (!lazyInitUserDefault()) {
            return;
        }
        sUserDefaultDatabase.setItem(key, Float.toString(value));
    }

    public static void setDoubleForKey(String key, double value) {
        if (!lazyInitUserDefault()) {
            return;
        }
        sUserDefaultDatabase.setItem(key, Double.toString(value));
    }

    public static void setStringForKey(String key, String value) {
        if (!lazyInitUserDefault()) {
            return;
        }
        sUserDefaultDatabase.setItem(key, value);
    }

    public static byte[] conversionEncoding(byte[] text, String fromCharset,String newCharset)
    {
        try {
            String str = new String(text,fromCharset);
            return str.getBytes(newCharset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    //Enhance API modification begin
    public static int setResolutionPercent(int per) {
        try {
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int setFPS(int fps) {
        try {
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int fastLoading(int sec) {
        try {
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int getTemperature() {
        try {
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int setLowPowerMode(boolean enable) {
        try {
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int getSDKVersion() {
        return Build.VERSION.SDK_INT;
    }

    //Enhance API modification end
}
