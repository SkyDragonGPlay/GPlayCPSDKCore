package org.cocos2dx.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.skydragon.gplay.runtime.callback.IActivityCallback;

import org.cocos2dx.utils.PSNative;
import org.cocos2dx.utils.PSNetwork;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

final class ResizeLayout extends FrameLayout{
    private  boolean mEnableForceDoLayout = false;
    private MyLayoutHandler myLayoutHandler = null;

    public ResizeLayout(Context context){
        super(context);

        myLayoutHandler = new MyLayoutHandler(this);
    }

    public ResizeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        myLayoutHandler = new MyLayoutHandler(this);
    }

    public void setEnableForceDoLayout(boolean flag){
        mEnableForceDoLayout = flag;
    }

    static final class MyLayoutHandler extends Handler {
        WeakReference<ResizeLayout> mReference;

        MyLayoutHandler(ResizeLayout layout)
        {
            mReference = new WeakReference<>(layout);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1024: {
                    ResizeLayout layout = mReference.get();
                    layout.requestLayout();
                    layout.invalidate();
                    break;
                }
                default:
                    break;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(mEnableForceDoLayout){
            /*This is a hot-fix for some android devices which don't do layout when the main window
            * is paned.  We refresh the layout in 24 frames per seconds.
            * When the editBox is lose focus or when user begin to type, the do layout is disabled.
            */
            myLayoutHandler.sendEmptyMessageDelayed(1024, 1000 / 24);
        }
    }
}

public class Cocos2dxActivity extends Activity implements IActivityCallback {
    // ===========================================================
    // Constants
    // ===========================================================

    private final static String TAG = "Cocos2dxActivity";

    public static Activity GAME_ACTIVITY = null;
    public static Cocos2dxActivity COCOS_ACTIVITY = null;
    static ResizeLayout ROOT_LAYOUT = null;

    static boolean GAME_IS_RUNNING = false;
    
    private Cocos2dxGLSurfaceView mGLSurfaceView;
    private Cocos2dxHandler mHandler = null;
    private Cocos2dxVideoHelper mVideoHelper = null;
    private Cocos2dxWebViewHelper mWebViewHelper = null;
    private Cocos2dxEditBoxHelper mEditBoxHelper = null;

    private static String mHostIPAddress = "0.0.0.0";

    //游戏在后台时延迟Runnable的执行
    private final ArrayList<Runnable> mDelayedRunnable = new ArrayList<>();
    private boolean mIsPause = false;

    public Cocos2dxGLSurfaceView getGLSurfaceView(){
        return  mGLSurfaceView;
    }

    public class Cocos2dxEGLConfigChooser implements GLSurfaceView.EGLConfigChooser
    {
        protected int[] configAttribs;

        public Cocos2dxEGLConfigChooser(int[] attribs)
        {
            configAttribs = attribs;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                EGLConfig config, int attribute, int defaultValue) {
            int[] value = new int[1];
            if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
                return value[0];
            }
            return defaultValue;
        }

        class ConfigValue implements Comparable<ConfigValue> {

            public EGLConfig config = null;
            public int[] configAttribs = null;
            public int value = 0;
            private void calcValue() {
                // depth factor 29bit and [6,12)bit
                if (configAttribs[4] > 0) {
                    value = value + (1 << 29) + ((configAttribs[4]%64) << 6);
                }
                // stencil factor 28bit and [0, 6)bit
                if (configAttribs[5] > 0) {
                    value = value + (1 << 28) + ((configAttribs[5]%64));
                }
                // alpha factor 30bit and [24, 28)bit
                if (configAttribs[3] > 0) {
                    value = value + (1 << 30) + ((configAttribs[3]%16) << 24);
                }
                // green factor [20, 24)bit
                if (configAttribs[1] > 0) {
                    value = value + ((configAttribs[1]%16) << 20);
                }
                // blue factor [16, 20)bit
                if (configAttribs[2] > 0) {
                    value = value + ((configAttribs[2]%16) << 16);
                }
                // red factor [12, 16)bit
                if (configAttribs[0] > 0) {
                    value = value + ((configAttribs[0]%16) << 12);
                }
            }

            public ConfigValue(int[] attribs) {
                configAttribs = attribs;
                calcValue();
            }

            public ConfigValue(EGL10 egl, EGLDisplay display, EGLConfig config) {
                this.config = config;
                configAttribs = new int[6];
                configAttribs[0] = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                configAttribs[1] = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                configAttribs[2] = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                configAttribs[3] = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
                configAttribs[4] = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
                configAttribs[5] = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
                calcValue();
            }

            @Override
            public int compareTo(ConfigValue another) {
                if (value < another.value) {
                    return -1;
                } else if (value > another.value) {
                    return 1;
                } else {
                    return 0;
                }
            }

            @Override
            public String toString() {
                return "{ color: " + configAttribs[3] + configAttribs[2] + configAttribs[1] + configAttribs[0] +
                        "; depth: " + configAttribs[4] + "; stencil: " + configAttribs[5] + ";}";
            }
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display)
        {
            int[] EGLattribs = {
                    EGL10.EGL_RED_SIZE, configAttribs[0],
                    EGL10.EGL_GREEN_SIZE, configAttribs[1],
                    EGL10.EGL_BLUE_SIZE, configAttribs[2],
                    EGL10.EGL_ALPHA_SIZE, configAttribs[3],
                    EGL10.EGL_DEPTH_SIZE, configAttribs[4],
                    EGL10.EGL_STENCIL_SIZE,configAttribs[5],
                    EGL10.EGL_RENDERABLE_TYPE, 4, //EGL_OPENGL_ES2_BIT
                    EGL10.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            boolean eglChooseResult = egl.eglChooseConfig(display, EGLattribs, configs, 1, numConfigs);
            if (eglChooseResult && numConfigs[0] > 0)
            {
                return configs[0];
            }

            // there's no config match the specific configAttribs, we should choose a closest one
            int[] EGLV2attribs = {
                    EGL10.EGL_RENDERABLE_TYPE, 4, //EGL_OPENGL_ES2_BIT
                    EGL10.EGL_NONE
            };
            eglChooseResult = egl.eglChooseConfig(display, EGLV2attribs, null, 0, numConfigs);
            if(eglChooseResult && numConfigs[0] > 0) {
                int num = numConfigs[0];
                ConfigValue[] cfgVals = new ConfigValue[num];

                // convert all config to ConfigValue
                configs = new EGLConfig[num];
                egl.eglChooseConfig(display, EGLV2attribs, configs, num, numConfigs);
                for (int i = 0; i < num; ++i) {
                    cfgVals[i] = new ConfigValue(egl, display, configs[i]);
                }

                ConfigValue e = new ConfigValue(configAttribs);
                // bin search
                int lo = 0;
                int hi = num;
                int mi;
                while (lo < hi - 1) {
                    mi = (lo + hi) / 2;
                    if (e.compareTo(cfgVals[mi]) < 0) {
                        hi = mi;
                    } else {
                        lo = mi;
                    }
                }
                if (lo != num - 1) {
                    lo = lo + 1;
                }
                Log.w("cocos2d", "Can't find EGLConfig match: " + e + ", instead of closest one:" + cfgVals[lo]);
                return cfgVals[lo].config;
            }

            Log.e(Activity.DEVICE_POLICY_SERVICE, "Can not select an EGLConfig for rendering.");
            return null;
        }

    }

    public static Activity getContext() {
        return GAME_ACTIVITY;
    }

    public static Cocos2dxActivity getCocosActivity() {
        return COCOS_ACTIVITY;
    }

    public static Cocos2dxActivity getCocos2dxActivity() { return  COCOS_ACTIVITY;}

    public void setKeepScreenOn(boolean value) {
        final boolean newValue = value;
        GAME_ACTIVITY.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGLSurfaceView.setKeepScreenOn(newValue);
            }
        });
    }

    protected void hideVirtualButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && GAME_ACTIVITY != null) {
            GAME_ACTIVITY.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    void showDialog(final String title, final String message) {
        Message msg = Message.obtain();
        msg.what = Cocos2dxHandler.HANDLER_SHOW_DIALOG;
        msg.obj = new Cocos2dxHandler.DialogMessage(title, message);
        mHandler.sendMessage(msg);
    }

    void showEditTextDialog(final String title, final String content, final int inputMode, final int inputFlag, final int returnType, final int maxLength) {
        Message msg = Message.obtain();
        msg.what = Cocos2dxHandler.HANDLER_SHOW_EDITBOX_DIALOG;
        msg.obj = new Cocos2dxHandler.EditBoxMessage(title, content, inputMode, inputFlag, returnType, maxLength);
        mHandler.sendMessage(msg);
    }

    // ===========================================================
    // Methods
    // ===========================================================
    void init() {
        // FrameLayout
        ViewGroup.LayoutParams frameLayoutParams =
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                       ViewGroup.LayoutParams.MATCH_PARENT);

        ROOT_LAYOUT = null;
        ROOT_LAYOUT = new ResizeLayout(GAME_ACTIVITY);
        ROOT_LAYOUT.setLayoutParams(frameLayoutParams);

        // Cocos2dxEditText layout
        ViewGroup.LayoutParams layoutParams =
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                       ViewGroup.LayoutParams.WRAP_CONTENT);
        Cocos2dxEditBox editText = new Cocos2dxEditBox(GAME_ACTIVITY);
        editText.setLayoutParams(layoutParams);

        ROOT_LAYOUT.addView(editText);

        // Cocos2dxGLSurfaceView
        mGLSurfaceView = new Cocos2dxGLSurfaceView(GAME_ACTIVITY);

        setEGLConfig(new int[] {5, 6, 5, 0, 16, 8});

        ROOT_LAYOUT.addView(mGLSurfaceView);

        mGLSurfaceView.setCocos2dxRenderer(new Cocos2dxRenderer());
        mGLSurfaceView.setCocos2dxEditText(editText);
    }

    protected void setEGLConfig(int[] attrs) {
        //this line is need on some device if we specify an alpha bits
        if (attrs[3] > 0) mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        Cocos2dxEGLConfigChooser chooser = new Cocos2dxEGLConfigChooser(attrs);
        mGLSurfaceView.setEGLConfigChooser(chooser);
    }

    @Override
    public ViewGroup getContainer() {
        return ROOT_LAYOUT;
    }

    protected void onCreate() {
        mHandler = new Cocos2dxHandler(GAME_ACTIVITY);

        init();

        if (mVideoHelper == null) {
            mVideoHelper = new Cocos2dxVideoHelper();
        }

        if(mWebViewHelper == null){
            mWebViewHelper = new Cocos2dxWebViewHelper(ROOT_LAYOUT);
        }

        if(mEditBoxHelper == null){
            mEditBoxHelper = new Cocos2dxEditBoxHelper();
        }

        Window window = GAME_ACTIVITY.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mHostIPAddress = getHostIpAddress();

        Cocos2dxBitmap.init();
        PSNative.init(GAME_ACTIVITY,GAME_ACTIVITY);
        PSNetwork.init(GAME_ACTIVITY);
    }

    @Override
    public void onActivityCreate(Activity activity) {
        Log.d(TAG, "onActivityCreate()");
        COCOS_ACTIVITY = this;
        GAME_ACTIVITY = activity;
        GAME_IS_RUNNING = true;

        onCreate();
        GAME_ACTIVITY.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onActivityPause() {
        Log.d(TAG, "onPause()");

        mIsPause = true;
        Cocos2dxHelper.onPause();
        if (mGLSurfaceView != null)
            mGLSurfaceView.onPause();
    }

    @Override
    public void onActivityResume() {
        Log.d(TAG, "onActivityResume()");

        Cocos2dxHelper.onResume();
        if (mGLSurfaceView != null) {
            mGLSurfaceView.onResume();
        }
    }

    public void runOnGLThread(final Runnable runnable) {
        if (mGLSurfaceView != null && runnable != null) {
            if (mIsPause) {
                synchronized (mDelayedRunnable) {
                    mDelayedRunnable.add(runnable);
                }
            } else {
                mGLSurfaceView.queueEvent(runnable);
            }
        }
    }

    @Override
    public void onRunOnGLThread(Runnable runnable) {
        runOnGLThread(runnable);
    }

    @Override
    public void onActivityWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "onActivityWindowFocusChanged " + hasFocus);
        if (hasFocus) {
            if (mGLSurfaceView != null) {
                ArrayList<Runnable> copyRunnableArray = null;
                synchronized (mDelayedRunnable) {
                    if (!mDelayedRunnable.isEmpty()) {
                        copyRunnableArray = new ArrayList<>(mDelayedRunnable);
                        mDelayedRunnable.clear();
                    }
                }

                if (copyRunnableArray != null) {
                    for (Runnable runnable : copyRunnableArray) {
                        mGLSurfaceView.queueEvent(runnable);
                    }
                }
            }

            mIsPause = false;
            hideVirtualButton();
        }
    }

    @Override
    public void onActivityDestroy() {
        Log.d(TAG, "onActivityDestroy()");
        GAME_IS_RUNNING = false;

        GAME_ACTIVITY = null;
        COCOS_ACTIVITY = null;
    }

    @Override
    public void onActivityResultCallback(int requestCode, int resultCode, Intent data) {
        for (OnActivityResultListener listener : Cocos2dxHelper.getOnActivityResultListeners()) {
            listener.onActivityResult(requestCode, resultCode, data);
        }
    }

    public String getHostIpAddress() {
        try {
            WifiManager wifiMgr = (WifiManager) GAME_ACTIVITY.getSystemService(Activity.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            return ((ip & 0xFF) + "." + ((ip >>>= 8) & 0xFF) + "." + ((ip >>>= 8) & 0xFF) + "." + ((ip >>>= 8) & 0xFF));
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    public static String getLocalIpAddress() {
        return mHostIPAddress;
    }
}
