package org.cocos2dx.lib;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.skydragon.gplay.runtime.bridge.CocosRuntimeBridge;
import com.skydragon.gplay.runtime.bridge.IBridgeProxy;

import java.io.File;
import java.io.IOException;

public final class Cocos2dxDatabase {
    private static final String TAG = "Cocos2dxDatabase";

    private SQLiteDatabase mDatabase;
    private String mDatabaseName;
    private String mTableName;

    public boolean init(String dbName, String tableName) {
        mDatabaseName = dbName;
        mTableName = tableName;
        return checkStorage();
    }

    private boolean checkStorage() {
        IBridgeProxy bridgeProxy = CocosRuntimeBridge.getInstance().getBridgeProxy();
        String sGameDir = (String)bridgeProxy.invokeMethodSync("getGameDir", null);

        if (sGameDir != null && !sGameDir.equals("")) {
            File f = new File(sGameDir + "/" + mDatabaseName);
            if (!f.exists()) {
                try {
                    f.createNewFile();//创建文件
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            try{
                mDatabase = SQLiteDatabase.openOrCreateDatabase(f, null);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return SQLiteOnCreate();
        }

        return false;
    }

    private boolean SQLiteOnCreate(){
        boolean ret = false;
        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + mTableName + "(key TEXT PRIMARY KEY,value TEXT);");
            ret = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void destory() {
        if (mDatabase != null) {
            mDatabase.close();
            mDatabase = null;
        }
    }

    public void setItem(String key, String value) {
        try {
            String sql = "replace into " + mTableName + "(key,value)values(?,?)";
            mDatabase.execSQL(sql, new Object[] { key, value });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getItem(String key) {
        String ret = null;
        try {
            String sql = "select value from " + mTableName + " where key=?";
            Cursor c = mDatabase.rawQuery(sql, new String[]{key});
            while (c.moveToNext()) {
                // only return the first value
                if (ret != null)
                {
                    Log.e(TAG, "The key contains more than one value.");
                    break;
                }
                ret = c.getString(c.getColumnIndex("value"));
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void removeItem(String key) {
        try {
            String sql = "delete from " + mTableName + " where key=?";
            mDatabase.execSQL(sql, new Object[] {key});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
