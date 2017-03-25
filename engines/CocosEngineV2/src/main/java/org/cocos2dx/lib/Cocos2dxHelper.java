/****************************************************************************
 * Copyright (c) 2010-2013 cocos2d-x.org
 * <p/>
 * http://www.cocos2d-x.org
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ****************************************************************************/
package org.cocos2dx.lib;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.skydragon.gplay.runtime.bridge.CocosRuntimeBridge;
import com.skydragon.gplay.runtime.bridge.IBridgeProxy;
import com.skydragon.gplay.runtime.bridge.ICallback;

import org.json.JSONObject;

public final class Cocos2dxHelper {
    // ===========================================================
    // Constants
    // ===========================================================
    public static final String TAG = "Cocos2dxHelper";
    private static final String PREFS_NAME = "Cocos2dxPrefsFile";

    // ===========================================================
    // Fields
    // ===========================================================

    private static Cocos2dxMusic sCocos2dMusic;
    private static Cocos2dxSound sCocos2dSound;
    private static AssetManager sAssetManager;
    private static Cocos2dxAccelerometer sCocos2dxAccelerometer;
    private static boolean sAccelerometerEnabled;
    private static String sPackageName;
    private static String sFileDirectory;

    // ===========================================================
    // Constructors
    // ===========================================================

    private static boolean sInitialized = false;

    public static void init(final Activity activity, final Cocos2dxActivity cocos2dxActivity) {
        if (sInitialized)
            return;
        sInitialized = true;

        final ApplicationInfo applicationInfo = activity.getApplicationInfo();

        sPackageName = applicationInfo.packageName;

        IBridgeProxy bridgeProxy = CocosRuntimeBridge.getInstance().getBridgeProxy();
        String gameDir = (String) bridgeProxy.invokeMethodSync("getGameDir", null);
        sFileDirectory = gameDir + "writableDir";
        ensureDirExist(sFileDirectory);

        nativeSetApkPath(applicationInfo.sourceDir);

        sCocos2dxAccelerometer = new Cocos2dxAccelerometer(activity);
        sCocos2dMusic = new Cocos2dxMusic(activity);
        sCocos2dSound = new Cocos2dxSound(activity);
        sAssetManager = activity.getAssets();

        Cocos2dxBitmap.setContext(activity);
        Cocos2dxETCLoader.init();
    }

    private static void ensureDirExist(String sPath) {
        File f = new File(sPath);
        if(!f.exists()) {
            f.mkdirs();
        }
    }

    public static void onEnterBackground() {

    }

    public static void onEnterForeground() {

    }

    private static Set<PreferenceManager.OnActivityResultListener> onActivityResultListeners = new LinkedHashSet<>();

    public static void addOnActivityResultListener(PreferenceManager.OnActivityResultListener listener) {
        onActivityResultListeners.add(listener);
    }

    public static Set<PreferenceManager.OnActivityResultListener> getOnActivityResultListeners() {
        return onActivityResultListeners;
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

    // ===========================================================
    // Methods
    // ===========================================================

    private static native void nativeSetApkPath(final String apkPath);

    private static native void nativeSetEditTextDialogResult(final byte[] pBytes);

    public static String getCocos2dxPackageName() {
        return sPackageName;
    }

    public static String getCocos2dxWritablePath() {
        return sFileDirectory;
    }

    public static String getCurrentLanguage() {
        return Locale.getDefault().getLanguage();
    }

    public static String getDeviceModel() {
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
        sCocos2dMusic.stopBackgroundMusic();
    }

    public static void rewindBackgroundMusic() {
        sCocos2dMusic.rewindBackgroundMusic();
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

    public static int playEffect(final String path, final boolean isLoop) {
        return sCocos2dSound.playEffect(path, isLoop);
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

    public static void end() {
        sCocos2dMusic.end();
        sCocos2dSound.end();
    }

    public static void onResume() {
        if (sAccelerometerEnabled) {
            sCocos2dxAccelerometer.enable();
        }
    }

    public static void onPause() {
        if (sAccelerometerEnabled) {
            sCocos2dxAccelerometer.disable();
        }
    }

    public static void terminateProcess() {
        //android.os.Process.killProcess(android.os.Process.myPid());

        CocosRuntimeBridge bridge = CocosRuntimeBridge.getInstance();
        IBridgeProxy runtimeProxy = bridge.getBridgeProxy();
        if (runtimeProxy != null) {
            runtimeProxy.invokeMethodSync("onQuitGame", null);
        } else {
            Log.e(TAG, "Cocos Runtime proxy is null!");
        }
        Cocos2dxGLSurfaceView.getInstance().getCocos2dxRenderer().stopRendering();
    }

    public static void showDialog(final String pTitle, final String pMessage) {
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
                    nativeSetEditTextDialogResult(bytesUTF8);
                }
            });
        } catch (UnsupportedEncodingException pUnsupportedEncodingException) {
            /* Nothing. */
        }
    }

    public static int getDPI() {
        if (Cocos2dxActivity.GAME_ACTIVITY != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = Cocos2dxActivity.GAME_ACTIVITY.getWindowManager();
            if (wm != null) {
                Display d = wm.getDefaultDisplay();
                if (d != null) {
                    d.getMetrics(metrics);
                    return (int) (metrics.density * 160.0f);
                }
            }
        }
        return -1;
    }

    // ===========================================================
    // Functions for CCUserDefault
    // ===========================================================

    public static boolean getBoolForKey(String key, boolean defaultValue) {
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        try {
            return settings.getBoolean(key, defaultValue);
        } catch (Exception ex) {
            ex.printStackTrace();

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if (value instanceof String) {
                return Boolean.parseBoolean(value.toString());
            } else if (value instanceof Integer) {
                int intValue = ((Integer) value).intValue();
                return (intValue != 0);
            } else if (value instanceof Float) {
                float floatValue = ((Float) value).floatValue();
                return (floatValue != 0.0f);
            }
        }

        return false;
    }

    public static int getIntegerForKey(String key, int defaultValue) {
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        try {
            return settings.getInt(key, defaultValue);
        } catch (Exception ex) {
            ex.printStackTrace();

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if (value instanceof String) {
                return Integer.parseInt(value.toString());
            } else if (value instanceof Float) {
                return ((Float) value).intValue();
            } else if (value instanceof Boolean) {
                boolean booleanValue = ((Boolean) value).booleanValue();
                if (booleanValue)
                    return 1;
            }
        }

        return 0;
    }

    public static float getFloatForKey(String key, float defaultValue) {
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        try {
            return settings.getFloat(key, defaultValue);
        } catch (Exception ex) {
            ex.printStackTrace();
            ;

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if (value instanceof String) {
                return Float.parseFloat(value.toString());
            } else if (value instanceof Integer) {
                return ((Integer) value).floatValue();
            } else if (value instanceof Boolean) {
                boolean booleanValue = ((Boolean) value).booleanValue();
                if (booleanValue)
                    return 1.0f;
            }
        }

        return 0.0f;
    }

    public static double getDoubleForKey(String key, double defaultValue) {
        // SharedPreferences doesn't support saving double value
        return getFloatForKey(key, (float) defaultValue);
    }

    public static String getStringForKey(String key, String defaultValue) {
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        try {
            return settings.getString(key, defaultValue);
        } catch (Exception ex) {
            ex.printStackTrace();

            return settings.getAll().get(key).toString();
        }
    }

    public static void setBoolForKey(String key, boolean value) {
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static void setIntegerForKey(String key, int value) {
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static void setFloatForKey(String key, float value) {
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    public static void setDoubleForKey(String key, double value) {
        // SharedPreferences doesn't support recording double value
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, (float) value);
        editor.commit();
    }

    public static void setStringForKey(String key, String value) {
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void deleteValueForKey(String key) {
        SharedPreferences settings = Cocos2dxActivity.GAME_ACTIVITY.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key);
        editor.commit();
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    public interface Cocos2dxHelperListener {
        void showDialog(final String pTitle, final String pMessage);

        void showEditTextDialog(final String pTitle, final String pMessage, final int pInputMode, final int pInputFlag, final int pReturnType, final int pMaxLength);

        void runOnGLThread(final Runnable pRunnable);
    }
}
