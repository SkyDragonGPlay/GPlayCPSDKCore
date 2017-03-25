/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.app.Activity
 *  android.content.Context
 *  android.content.Intent
 *  android.os.Bundle
 */
package com.unity3d.player;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class UnityPlayerProxyActivity
        extends Activity {
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = new Intent(this, UnityPlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        this.startActivity(intent);
    }
}

