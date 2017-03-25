package com.skydragon.gplay.runtime.bridge;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.ViewGroup;

import com.skydragon.gplay.runtime.callback.IActivityCallback;
import com.skydragon.gplay.runtime.callback.OnRuntimeStatusChangedListener;
import com.skydragon.gplay.unitsdk.nativewrapper.NativeWrapper;

import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxGLSurfaceView;
import org.cocos2dx.lib.Cocos2dxHelper;
import org.cocos2dx.lib.Cocos2dxRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CocosRuntimeBridge implements IEngineRuntimeBridge {

    public static final String TAG = "CocosRuntimeBridge";

    private static final String VERSION = "3.10.0.25";
    private static final int VERSION_CODE = 25;

    private Activity mGameActivity = null;
    private IActivityCallback mActivityCallback = null;

    private List<OnRuntimeStatusChangedListener> mStatusChangedListeners = new ArrayList<>();

    private IBridgeProxy mBridgeProxy = null;

    private static CocosRuntimeBridge sInstance = null;

    private HashMap<String, Object> mOptionMap = new HashMap<>();
    private boolean isNativeLibraryLoaded;

    private static int mGameRunMode = 1;

    private static native void nativeInitRuntimeJNI();

    private static native void nativeSetDefaultResourceRootPath(String resRootPath);

    private static native void nativeAddSearchPath(String path);

    private static native void nativeCocosEngineOnStartGame();
    private static native void nativeCocosEngineOnQuitGame();

    public static native void nativeGplayKeyEvent(final int keyCode, boolean isPressed);

    private static native void nativePreloadResponse(String responseJson, boolean isDone, long ext);
    private static native void nativePreloadResponse2(boolean isDone, boolean isFailed, String errorCode, float percent, float downloadSpeed, String groupName);

    private static native void nativeDownloadFileCallback(String responseJson, long ext);
    private static native void nativeSetRuntimeConfig(String responseJson);

    public static native void nativeExtensionAPI(String method, String stringArg, int intArg, double doubleArg);

    @Override
    public Object invokeMethodSync(String method, Map<String, Object> args) {
        if (method == null) {
            Log.e(TAG, "invokeMethodSync method is null");
            return null;
        } else {
            Log.d(TAG, "invokeMethodSync method:" + method);
        }

        try {
            switch (method) {
                case "preloadResponse": {
                    nativePreloadResponse((String) args.get("responseJson"), (Boolean) args.get("isDone"), (Long) args.get("ext"));
                    break;
                }
                case "preloadResponse2": {
                    nativePreloadResponse2((Boolean) args.get("isDone"),
                            (Boolean) args.get("isFailed"), (String) args.get("errorCode"),
                            (Float) args.get("percent"), (Float) args.get("downloadSpeed"), (String) args.get("groupName"));
                    break;
                }
                case "onAsyncActionResult": {
                    int code = (Integer)args.get("ret");
                    String msg = (String)args.get("msg");
                    String callbackId = (String)args.get("callbackid");
                    NativeWrapper.onAsyncActionResult(code, msg, callbackId);
                    break;
                }
                case "downloadRemoteFileCallback": {
                    nativeDownloadFileCallback((String) args.get("responseJson"), (Long) args.get("ext"));
                    break;
                }
                case "setRuntimeEnvironment": {
                    break;
                }
                case "setRuntimeConfig": {
                    String jsonStr = (String) args.get("jsonStr");
                    nativeSetRuntimeConfig(jsonStr);
                    break;
                }
                case "nativeExtensionAPI": {
                    nativeExtensionAPI((String) args.get("method"), (String) args.get("string"),
                            (Integer) args.get("int"), (Double) args.get("double"));
                    break;
                }
                default:
                    Log.e(TAG, "CocosBridge.invokeMethodSync doesn't support ( " + method + " )");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void invokeMethodAsync(final String method, final Map<String, Object> args, final ICallback callback) {
        if (method != null)
            Log.d(TAG,  "invokeMethodAsync method:" + method);
        else {
            Log.e(TAG,  "invokeMethodAsync method is null");
            return;
        }

        switch (method) {
            case Cocos2dxRenderer.KEY_CAPTURE_SCREEN_METHOD: {
                if (Cocos2dxGLSurfaceView.getInstance() != null) {
                    Cocos2dxRenderer renderer = Cocos2dxGLSurfaceView.getInstance().getCocos2dxRenderer();
                    if (renderer != null) {
                        renderer.setCaptureScreenCallbackByChannel(callback, args);
                        return;
                    }
                }

                Map<String, Object> result = new HashMap<>();
                result.putAll(args);
                result.put(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_RESULT_CODE, -1);
                callback.onCallback(Cocos2dxRenderer.KEY_CAPTURE_SCREEN_METHOD, result);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void setOption(String key, Object value) {
        mOptionMap.put(key, value);
    }

    @Override
    public Object getOption(String key) {
        return mOptionMap.get(key);
    }

    @Override
    public void setBridgeProxy(IBridgeProxy proxy) {
        mBridgeProxy = proxy;
        Object runMode = proxy.invokeMethodSync("getGameRunMode", null);
        if (runMode != null)
            mGameRunMode = (Integer)runMode;
        else
            mGameRunMode = 1;
    }

    @Override
    public IBridgeProxy getBridgeProxy() {
        return mBridgeProxy;
    }
    
    public static CocosRuntimeBridge getInstance() {
        if (sInstance == null) {
            Log.e(TAG, "CocosRuntimeBridge has not been constructed!");
        }
        return sInstance;
    }
    
    public CocosRuntimeBridge() {
        sInstance = this;
        Log.d(TAG, "CocosRuntimeBridge has been constructed! VERSION:" + VERSION + ",VERSION_CODE:" + VERSION_CODE);
    }

    @Override
    public String getRuntimeVersion() {
        return VERSION;
    }

    public void addStatusChangedListener(OnRuntimeStatusChangedListener listener) {
        if (mStatusChangedListeners.contains(listener)) {
            Log.d(TAG, "listener exists, don't need to add it again!");
        } else {
            mStatusChangedListeners.add(listener);
        }
    }

    public void removeAllStatusChangedListeners() {
        mStatusChangedListeners.clear();
    }

    @Override
    public void init(Activity activity) {
        mGameActivity = activity;
        if (mActivityCallback == null) {
            mActivityCallback = new Cocos2dxActivity();
        }

        mActivityCallback.onActivityCreate(mGameActivity);
    }

    @Override
    public void loadSharedLibrary(List<String> listSo) {
        if(null == listSo || listSo.isEmpty()) return;
        for(String soFile : listSo) {
            System.load(soFile);
        }
    }

    @Override
    public void initRuntimeJNI() {
        Cocos2dxHelper.initJNI();
        nativeInitRuntimeJNI();
    }

    @Override
    public void notifyOnLoadSharedLibrary() {
        try {
            if(isNativeLibraryLoaded) {
                return;
            }
            isNativeLibraryLoaded = true;

            Cocos2dxHelper.init(mGameActivity, Cocos2dxActivity.getCocosActivity());

            for (OnRuntimeStatusChangedListener lis : mStatusChangedListeners) {
                lis.onNativeLibraryLoaded();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startGame() {
        Log.d(TAG, "startGame ...");

        String sGameDir = (String)mBridgeProxy.invokeMethodSync("getGameResourceDir",null);
        nativeSetDefaultResourceRootPath(sGameDir);
        nativeAddSearchPath(sGameDir);

        mBridgeProxy.invokeMethodSync("notifyOnPrepareEngineFinished", null);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause ...");
        mActivityCallback.onActivityPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume ...");
        mActivityCallback.onActivityResume();
    }

    @Override
    public void quitGame() {
        runOnGLThread(new Runnable() {
            public void run() {
                nativeCocosEngineOnQuitGame();
            }
        });
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop ...");
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent ...");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy ...");
        removeAllStatusChangedListeners();
        mActivityCallback.onActivityDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode:" + resultCode + ", data:" + data);
        mActivityCallback.onActivityResultCallback(requestCode, resultCode, data);
    }

    @Override
    public void runOnGLThread(Runnable runnable) {
        mActivityCallback.onRunOnGLThread(runnable);
    }

    @Override
    public boolean isRunOnEngineContext() {
        boolean isGLThread = Thread.currentThread().getName().contains("GL");
        if (!isGLThread) {
            Log.d(TAG, "Oops, makeSureIsInGLThread failed, it wasn't invoked from GL thread!");
        }
        return isGLThread;
    }

    @Override
    public ViewGroup getEngineLayout() {
        return mActivityCallback.getContainer();
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        mActivityCallback.onActivityWindowFocusChanged(hasFocus);
    }

    public static int getGameRunMode() {
        return mGameRunMode;
    }

}
