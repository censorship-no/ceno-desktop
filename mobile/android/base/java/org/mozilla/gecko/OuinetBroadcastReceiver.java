package org.mozilla.gecko;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

public class OuinetBroadcastReceiver extends BroadcastReceiver {
    public static final String EXTRA_ACTION_STOP = "org.mozilla.gecko.OuinetBroadcastReceiver.STOP";
    private static final String TAG = "OuinetBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent: "+ intent +", shutting down Ouinet service");
        Process.killProcess(Process.myPid());
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, OuinetBroadcastReceiver.class);
        intent.putExtra(EXTRA_ACTION_STOP, /* unused */ 1);
        return intent;
    }
}
