/****************************************************************************
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

import android.util.Log;

public final class Cocos2dxLocalStorage {

    private static final String TAG = "Cocos2dxLocalStorage";

    private static Cocos2dxDatabase sDatabase;

    public static boolean init(String dbName, String tableName) {
        if (sDatabase == null) {
            sDatabase = new Cocos2dxDatabase();
            return sDatabase.init(dbName, tableName);
        } else {
            Log.w(TAG, "Cocos2dxLocalStorage was initialized, no need to initialize it again!");
        }
        return false;
    }

    public static void destory() {
        if (sDatabase != null) {
            sDatabase.destory();
            sDatabase = null;
        }
    }

    public static void setItem(String key, String value) {
        if (sDatabase != null) {
            sDatabase.setItem(key, value);
        } else {
            Log.e(TAG, "Cocos2dxLocalStorage wasn't initialized!");
        }
    }

    public static String getItem(String key) {
        String ret = null;
        if (sDatabase != null) {
            ret = sDatabase.getItem(key);
        } else {
            Log.e(TAG, "Cocos2dxLocalStorage wasn't initialized!");
        }
        return ret == null ? "" : ret;
    }

    public static void removeItem(String key) {
        if (sDatabase != null) {
            sDatabase.removeItem(key);
        } else {
            Log.e(TAG, "Cocos2dxLocalStorage wasn't initialized!");
        }
    }
}
