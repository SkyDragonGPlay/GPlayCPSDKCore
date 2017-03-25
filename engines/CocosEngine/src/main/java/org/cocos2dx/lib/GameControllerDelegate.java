package org.cocos2dx.lib;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;

public interface GameControllerDelegate {
    int KEY_BASE = 1000;
    
    int THUMBSTICK_LEFT_X = KEY_BASE;
    int THUMBSTICK_LEFT_Y = KEY_BASE + 1;
    int THUMBSTICK_RIGHT_X = KEY_BASE + 2;
    int THUMBSTICK_RIGHT_Y = KEY_BASE + 3;
    
    int BUTTON_A = KEY_BASE + 4;
    int BUTTON_B = KEY_BASE + 5;
    int BUTTON_C = KEY_BASE + 6;
    int BUTTON_X = KEY_BASE + 7;
    int BUTTON_Y = KEY_BASE + 8;
    int BUTTON_Z = KEY_BASE + 9;
    
    int BUTTON_DPAD_UP = KEY_BASE + 10;
    int BUTTON_DPAD_DOWN = KEY_BASE + 11;
    int BUTTON_DPAD_LEFT = KEY_BASE + 12;
    int BUTTON_DPAD_RIGHT = KEY_BASE + 13;
    int BUTTON_DPAD_CENTER = KEY_BASE + 14;
    
    int BUTTON_LEFT_SHOULDER = KEY_BASE + 15;
    int BUTTON_RIGHT_SHOULDER = KEY_BASE + 16;
    int BUTTON_LEFT_TRIGGER = KEY_BASE + 17;
    int BUTTON_RIGHT_TRIGGER = KEY_BASE + 18;
    
    int BUTTON_LEFT_THUMBSTICK = KEY_BASE + 19;
    int BUTTON_RIGHT_THUMBSTICK = KEY_BASE + 20;
    
    int BUTTON_START = KEY_BASE + 21;
    int BUTTON_SELECT = KEY_BASE + 22;
    
    void onCreate(Context context);
    void onPause();
    void onResume();
    void onDestroy();
    
    boolean dispatchKeyEvent(KeyEvent event);
    boolean dispatchGenericMotionEvent(MotionEvent event);
    
    void setControllerEventListener(ControllerEventListener listener);
    
    public interface ControllerEventListener {
        void onButtonEvent(String vendorName, int controller, int button, boolean isPressed, float value, boolean isAnalog);
        void onAxisEvent(String vendorName, int controller, int axisID, float value, boolean isAnalog);
        
        void onConnected(String vendorName, int controller);
        void onDisconnected(String vendorName, int controller);
    }
}
