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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.skydragon.gplay.runtime.callback.IActivityCallback;

import org.cocos2dx.lib.Cocos2dxHelper.Cocos2dxHelperListener;

import java.util.ArrayList;

public class Cocos2dxActivity extends Activity implements Cocos2dxHelperListener, IActivityCallback {
    // ===========================================================
    // Constants
    // ===========================================================

    private final static String TAG = "Cocos2dxActivity";

    // ===========================================================
    // Fields
    // ===========================================================

    static Activity GAME_ACTIVITY = null;
    static Cocos2dxActivity COCOS_ACTIVITY = null;

    private Cocos2dxGLSurfaceView mGLSurfaceView;
    private Cocos2dxHandler mHandler;

    //游戏在后台时延迟Runnable的执行
    private final ArrayList<Runnable> mDelayedRunnable = new ArrayList<>();
    private boolean mIsPause = false;

    public static Context getContext() {
        return GAME_ACTIVITY;
    }

    public static Cocos2dxActivity getCocosActivity() {
        return COCOS_ACTIVITY;
    }

    @Override
    public void onActivityCreate(Activity activity) {
        COCOS_ACTIVITY = this;
        GAME_ACTIVITY = activity;

        mHandler = new Cocos2dxHandler(GAME_ACTIVITY);
        initView();
        QuickHTTPInterface.makeNotBeRemoved();

        GAME_ACTIVITY.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    protected void hideVirtualButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            GAME_ACTIVITY.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onActivityPause() {
        Log.d(TAG, "onActivityPause()");

        mIsPause = true;
        Cocos2dxHelper.onPause();
        if (mGLSurfaceView != null)
            mGLSurfaceView.onPause();
    }

    @Override
    public void onActivityResume() {
        Log.d(TAG, "onActivityResume()");

        Cocos2dxHelper.onResume();
        if (mGLSurfaceView != null)
            mGLSurfaceView.onResume();
    }

    @Override
    public void onActivityDestroy() {
        Log.d(TAG, "onActivityDestroy()");
    }

    @Override
    public void onActivityWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "onActivityWindowFocusChanged :" + hasFocus);
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
    public void onRunOnGLThread(Runnable runnable) {
        runOnGLThread(runnable);
    }

    @Override
    public void onActivityResultCallback(int requestCode, int resultCode, Intent data) {
        for (PreferenceManager.OnActivityResultListener listener : Cocos2dxHelper.getOnActivityResultListeners()) {
             listener.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public ViewGroup getContainer() {
        return mFrameLayout;
    }

    @Override
    public void showDialog(final String title, final String message) {
        Message msg = Message.obtain();
        msg.what = Cocos2dxHandler.HANDLER_SHOW_DIALOG;
        msg.obj = new Cocos2dxHandler.DialogMessage(title, message);
        mHandler.sendMessage(msg);
    }

    @Override
    public void showEditTextDialog(final String title, final String content, final int inputMode, final int inputFlag, final int returnType, final int maxLength) {
        Message msg = Message.obtain();
        msg.what = Cocos2dxHandler.HANDLER_SHOW_EDITBOX_DIALOG;
        msg.obj = new Cocos2dxHandler.EditBoxMessage(title, content, inputMode, inputFlag, returnType, maxLength);
        mHandler.sendMessage(msg);
    }

    @Override
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

    private FrameLayout mFrameLayout;

    // ===========================================================
    // Methods
    // ===========================================================
    void initView() {
        ViewGroup.LayoutParams frameLayoutParams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        mFrameLayout = new FrameLayout(GAME_ACTIVITY);
        mFrameLayout.setLayoutParams(frameLayoutParams);

        // Cocos2dxEditText layout
        ViewGroup.LayoutParams layout_params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        Cocos2dxEditText editText = new Cocos2dxEditText(GAME_ACTIVITY);
        editText.setLayoutParams(layout_params);
        mFrameLayout.addView(editText);

        mGLSurfaceView = new Cocos2dxGLSurfaceView(GAME_ACTIVITY);
        mFrameLayout.addView(mGLSurfaceView);

        mGLSurfaceView.setCocos2dxRenderer(new Cocos2dxRenderer());
        mGLSurfaceView.setCocos2dxEditText(editText);
    }
}
