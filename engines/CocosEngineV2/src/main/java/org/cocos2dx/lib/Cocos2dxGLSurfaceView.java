/****************************************************************************
 * Copyright (c) 2010-2011 cocos2d-x.org
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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

public final class Cocos2dxGLSurfaceView extends GLSurfaceView {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final String TAG = "Cocos2dxGLSurfaceView";

    private final static int HANDLER_OPEN_IME_KEYBOARD = 2;
    private final static int HANDLER_CLOSE_IME_KEYBOARD = 3;

    // ===========================================================
    // Fields
    // ===========================================================

    // TODO Static handler -> Potential leak!
    private static Handler sHandler;

    private static Cocos2dxGLSurfaceView mCocos2dxGLSurfaceView;
    private static Cocos2dxTextInputWrapper sCocos2dxTextInputWrapper;

    private Cocos2dxRenderer mCocos2dxRenderer;
    private Cocos2dxEditText mCocos2dxEditText;

    // ===========================================================
    // Constructors
    // ===========================================================

    public Cocos2dxGLSurfaceView(final Context context) {
        super(context);

        initView();
    }

    public Cocos2dxGLSurfaceView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        initView();
    }

    protected void initView() {
        setEGLContextClientVersion(2);
        setFocusableInTouchMode(true);
        setEGLConfigChooser(5,6,5,0,16,8);

        mCocos2dxGLSurfaceView = this;
        sCocos2dxTextInputWrapper = new Cocos2dxTextInputWrapper(this);

        Cocos2dxGLSurfaceView.sHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {
                    case HANDLER_OPEN_IME_KEYBOARD:
                        if (null != Cocos2dxGLSurfaceView.this.mCocos2dxEditText && Cocos2dxGLSurfaceView.this.mCocos2dxEditText.requestFocus()) {
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.removeTextChangedListener(Cocos2dxGLSurfaceView.sCocos2dxTextInputWrapper);
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.setText("");
                            final String text = (String) msg.obj;
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.append(text);
                            Cocos2dxGLSurfaceView.sCocos2dxTextInputWrapper.setOriginText(text);
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.addTextChangedListener(Cocos2dxGLSurfaceView.sCocos2dxTextInputWrapper);
                            final InputMethodManager imm = (InputMethodManager) Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(Cocos2dxGLSurfaceView.this.mCocos2dxEditText, 0);
                            Log.d("GLSurfaceView", "showSoftInput");
                        }
                        break;

                    case HANDLER_CLOSE_IME_KEYBOARD:
                        if (null != Cocos2dxGLSurfaceView.this.mCocos2dxEditText) {
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.removeTextChangedListener(Cocos2dxGLSurfaceView.sCocos2dxTextInputWrapper);
                            final InputMethodManager imm = (InputMethodManager) Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(Cocos2dxGLSurfaceView.this.mCocos2dxEditText.getWindowToken(), 0);
                            Cocos2dxGLSurfaceView.this.requestFocus();
                            // can take effect after GLSurfaceView has focus
                            Cocos2dxActivity.COCOS_ACTIVITY.hideVirtualButton();
                            Log.d("GLSurfaceView", "HideSoftInput");
                        }
                        break;
                }
            }
        };
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================


    public static Cocos2dxGLSurfaceView getInstance() {
        return mCocos2dxGLSurfaceView;
    }

    public static void queueAccelerometer(final float x, final float y, final float z, final long timestamp) {
        mCocos2dxGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                Cocos2dxAccelerometer.onSensorChanged(x, y, z, timestamp);
            }
        });
    }

    public void setCocos2dxRenderer(final Cocos2dxRenderer renderer) {
        mCocos2dxRenderer = renderer;
        setRenderer(mCocos2dxRenderer);
    }

    public Cocos2dxRenderer getCocos2dxRenderer() {
        return mCocos2dxRenderer;
    }

    private String getContentText() {
        return mCocos2dxRenderer.getContentText();
    }

    public Cocos2dxEditText getCocos2dxEditText() {
        return mCocos2dxEditText;
    }

    public void setCocos2dxEditText(final Cocos2dxEditText pCocos2dxEditText) {
        mCocos2dxEditText = pCocos2dxEditText;
        if (null != mCocos2dxEditText && null != sCocos2dxTextInputWrapper) {
            mCocos2dxEditText.setOnEditorActionListener(sCocos2dxTextInputWrapper);
            mCocos2dxEditText.setCocos2dxGLSurfaceView(this);
            requestFocus();
        }
    }

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public void onResume() {
        //super.onResume();

        setRenderMode(RENDERMODE_CONTINUOUSLY);

        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCocos2dxRenderer.handleOnResume();
            }
        });
    }

    @Override
    public void onPause() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCocos2dxRenderer.handleOnPause();
            }
        });

        setRenderMode(RENDERMODE_WHEN_DIRTY);

        //super.onPause();
    }

    @Override
    public boolean onTouchEvent(final MotionEvent pMotionEvent) {
        if (!mCocos2dxRenderer.mIsNativeLibraryLoaded)
            return true;

        // these data are used in ACTION_MOVE and ACTION_CANCEL
        final int pointerNumber = pMotionEvent.getPointerCount();
        final int[] ids = new int[pointerNumber];
        final float[] xs = new float[pointerNumber];
        final float[] ys = new float[pointerNumber];

        for (int i = 0; i < pointerNumber; i++) {
            ids[i] = pMotionEvent.getPointerId(i);
            xs[i] = pMotionEvent.getX(i);
            ys[i] = pMotionEvent.getY(i);
        }

        switch (pMotionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                final int indexPointerDown = pMotionEvent.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int idPointerDown = pMotionEvent.getPointerId(indexPointerDown);
                final float xPointerDown = pMotionEvent.getX(indexPointerDown);
                final float yPointerDown = pMotionEvent.getY(indexPointerDown);

                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCocos2dxRenderer.handleActionDown(idPointerDown, xPointerDown, yPointerDown);
                    }
                });
                break;

            case MotionEvent.ACTION_DOWN:
                // there are only one finger on the screen
                final int idDown = pMotionEvent.getPointerId(0);
                final float xDown = xs[0];
                final float yDown = ys[0];

                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCocos2dxRenderer.handleActionDown(idDown, xDown, yDown);
                    }
                });
                break;

            case MotionEvent.ACTION_MOVE:
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCocos2dxRenderer.handleActionMove(ids, xs, ys);
                    }
                });
                break;

            case MotionEvent.ACTION_POINTER_UP:
                final int indexPointUp = pMotionEvent.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int idPointerUp = pMotionEvent.getPointerId(indexPointUp);
                final float xPointerUp = pMotionEvent.getX(indexPointUp);
                final float yPointerUp = pMotionEvent.getY(indexPointUp);

                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCocos2dxRenderer.handleActionUp(idPointerUp, xPointerUp, yPointerUp);
                    }
                });
                break;

            case MotionEvent.ACTION_UP:
                // there are only one finger on the screen
                final int idUp = pMotionEvent.getPointerId(0);
                final float xUp = xs[0];
                final float yUp = ys[0];

                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCocos2dxRenderer.handleActionUp(idUp, xUp, yUp);
                    }
                });
                break;

            case MotionEvent.ACTION_CANCEL:
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCocos2dxRenderer.handleActionCancel(ids, xs, ys);
                    }
                });
                break;
        }

        return true;
    }

    /*
     * This function is called before Cocos2dxRenderer.nativeInit(), so the
     * width and height is correct.
     */
    @Override
    protected void onSizeChanged(final int pNewSurfaceWidth, final int pNewSurfaceHeight, final int pOldSurfaceWidth, final int pOldSurfaceHeight) {
        if (!isInEditMode()) {
            mCocos2dxRenderer.setScreenWidthAndHeight(pNewSurfaceWidth, pNewSurfaceHeight);
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent keyEvent) {
        if (!mCocos2dxRenderer.mIsNativeLibraryLoaded)
            return super.onKeyDown(keyCode, keyEvent);

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCocos2dxRenderer.handleKeyDown(keyCode);
                    }
                });
                return true;
            default:
                return super.onKeyDown(keyCode, keyEvent);
        }
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    public static void openIMEKeyboard() {
        final Message msg = Message.obtain();
        msg.what = Cocos2dxGLSurfaceView.HANDLER_OPEN_IME_KEYBOARD;
        msg.obj = Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView.getContentText();
        Cocos2dxGLSurfaceView.sHandler.sendMessage(msg);
    }

    public static void closeIMEKeyboard() {
        final Message msg = Message.obtain();
        msg.what = Cocos2dxGLSurfaceView.HANDLER_CLOSE_IME_KEYBOARD;
        Cocos2dxGLSurfaceView.sHandler.sendMessage(msg);
    }

    public void insertText(final String pText) {
        if (mCocos2dxRenderer.mIsNativeLibraryLoaded) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mCocos2dxRenderer.handleInsertText(pText);
                }
            });
        }
    }

    public void deleteBackward() {
        if (mCocos2dxRenderer.mIsNativeLibraryLoaded) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mCocos2dxRenderer.handleDeleteBackward();
                }
            });
        }
    }

}
