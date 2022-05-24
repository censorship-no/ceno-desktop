package org.mozilla.gecko;

import org.mozilla.gecko.util.GeckoBundle;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class OuinetBroadcastReceiver extends BroadcastReceiver {
    // The value constants also force us to use
    // the right type check for the extras bundle.
    public static final String EXTRA_ACTION_STOP = "org.mozilla.gecko.OuinetBroadcastReceiver.STOP";
    public static final String EXTRA_ACTION_PURGE = "org.mozilla.gecko.OuinetBroadcastReceiver.PURGE";
    private static final String TAG = "OuinetBroadcastReceiver";

    // Use this to check that this system supports the purge operation.
    // Only offer a purge feature if the device actually supports it,
    // otherwise the user may have the false impression that the action does purge the data,
    // while it only makes the app crash, and the data stays there.
    public static boolean canPurge() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent: "+ intent +", shutting down Ouinet service");
        boolean doStop = intent.hasExtra(EXTRA_ACTION_STOP);
        boolean doPurge = intent.hasExtra(EXTRA_ACTION_PURGE);

        if (!doStop) {
            return;  // purging only is not allowed
        }

        if (doPurge) {
            if (!canPurge()) {
                Log.e(TAG, "Received purge intent but SDK is too old, imminent crash!");
                // Let the application crash on the missing `ActivityManager.clearApplicationUserData()`
                // to let the user know that something is not ok.
            }
            // Shut down the service the hard way
            // to prevent it from creating files after clearing app data.
            killPackageProcesses(context);
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.clearApplicationUserData();
            }
            Process.killProcess(Process.myPid());
        }

        OuinetService.stopOuinetService(context);  // shut down gracefully

        //Process.killProcess(Process.myPid());  // very harsh

        //GeckoThread.forceQuit();  // slightly better
        //GeckoApplication.shutdown(null);  // probably similar

        // Gentler, but the simplification implies
        // ignoring user settings on what data to clear on explicit quit.
        // See `GeckoApp.onOptionsItemSelected()`.
        final GeckoBundle res = new GeckoBundle(2);
        res.putBundle("sanitize", new GeckoBundle(0));
        res.putBoolean("dontSaveSession", false);
        EventDispatcher.getInstance().dispatch("Browser:Quit", res);

        //GeckoApp.getInstance().onMenuItemClick(R.id.quit);  // if only...
    }

    public static Intent createStopIntent(Context context) {
        Intent intent = new Intent(context, OuinetBroadcastReceiver.class);
        intent.putExtra(EXTRA_ACTION_STOP, 1);
        return intent;
    }

    public static Intent createPurgeIntent(Context context) {
        Intent intent = createStopIntent(context);
        intent.putExtra(EXTRA_ACTION_PURGE, 1);
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
