/****************************************************************************
 * Copyright (c) 2012 cocos2d-x.org
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
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public final class Cocos2dxEditText extends EditText {

    private Cocos2dxGLSurfaceView mCocos2dxGLSurfaceView;

    // ===========================================================
    // Constructors
    // ===========================================================

    public Cocos2dxEditText(final Context context) {
        super(context);
    }

    public Cocos2dxEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public Cocos2dxEditText(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public void setCocos2dxGLSurfaceView(final Cocos2dxGLSurfaceView cocos2dxGLSurfaceView) {
        mCocos2dxGLSurfaceView = cocos2dxGLSurfaceView;
    }

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent keyEvent) {
        super.onKeyDown(keyCode, keyEvent);

        /* Let GlSurfaceView get focus if back key is input. */
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mCocos2dxGLSurfaceView.requestFocus();
        }

        return true;
    }
}
