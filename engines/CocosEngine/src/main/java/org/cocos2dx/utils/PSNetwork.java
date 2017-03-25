package org.cocos2dx.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;

import java.net.HttpURLConnection;
import java.net.URL;

public final class PSNetwork {
    static ConnectivityManager mConnManager = null;

    public static void init(Context context) {
        mConnManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static boolean isLocalWiFiAvailable() {
        if (mConnManager == null) {
            return false;
        }
        State state = mConnManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        return State.CONNECTED == state;
    }

    public static boolean isInternetConnectionAvailable() {
        if (mConnManager == null) {
            return false;
        }

        if (isLocalWiFiAvailable()) {
            return true;
        } 
        
        try {
            State state = mConnManager.getNetworkInfo(
                    ConnectivityManager.TYPE_MOBILE).getState();
            return State.CONNECTED == state;
        } catch (Exception e) { 
            return false;
        }
    }

    public static boolean isHostNameReachable(String hostName) {
        int counts = 0;
        if (hostName == null || hostName.length() <= 0) {
            return false;
        }
        while (counts < 3) {
            try {
                URL url = new URL(hostName);
                int state = ((HttpURLConnection) url.openConnection())
                        .getResponseCode();
                return state == 200;
            } catch (Exception ex) {
                counts++;
            }
        }
        return false;
    }

    public static int getInternetConnectionStatus() {
        if (isLocalWiFiAvailable()) {
            return 1; // wifi
        }
        if (isInternetConnectionAvailable()) {
            return 2; // gprs
        }
        return 0;
    }
}
