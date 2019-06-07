package org.mozilla.gecko;


import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class OuinetBroadcastReceiver extends BroadcastReceiver {
    public static final String EXTRA_ACTION_STOP = "org.mozilla.gecko.OuinetBroadcastReceiver.STOP";
    private static final String TAG = "OuinetBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent: "+ intent +", shutting down Ouinet service");
        killPackageProcesses(context);
        Process.killProcess(Process.myPid());
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, OuinetBroadcastReceiver.class);
        intent.putExtra(EXTRA_ACTION_STOP, /* unused */ 1);
        return intent;
    }

    private void killPackageProcesses(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return;
        }
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) {
            return;
        }
        int myPid = Process.myPid();
        String thisPkg = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process.pid == myPid || process.pkgList == null) {
                // Current process will be killed last
                continue;
            }

            List<String> pkgs = Arrays.asList(process.pkgList);
            if (pkgs.contains(thisPkg)) {
                Log.i(TAG, "Killing process: " + process.processName + " (" + process.pid + ")");
                Process.killProcess(process.pid);
            }
        }
    }
}
