package ru.rufp.tvlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

class AppsReceiver extends BroadcastReceiver {

    private final Handler handler;
    private final Runnable refresh;

    AppsReceiver(Handler handler, Runnable refresh) {
        this.handler = handler;
        this.refresh = refresh;
    }

    @Override
    public void onReceive(Context c, Intent i) {
        handler.removeCallbacks(refresh);
        handler.postDelayed(refresh, 600);
    }
}
