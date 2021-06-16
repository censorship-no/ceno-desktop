/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ie.equalit.ouinet.Ouinet;
import ie.equalit.ouinet.Config;

public class OuinetService extends Service {
    private static final String TAG = "OuinetService";
    private static final String CONFIG_EXTRA = "config";
    private static final String SHOW_PURGE_EXTRA = "show-purge";
    private static final String HIDE_PURGE_EXTRA = "hide-purge";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ouinet-notification-channel";

    private Ouinet mOuinet;

    // To see whether this service is running, you may try this command:
    // adb -s $mi shell dumpsys activity services OuinetService
    public static void startOuinetService(Context context, Config config) {
        Intent intent = new Intent(context, OuinetService.class);
        intent.putExtra(CONFIG_EXTRA, config);
        context.startService(intent);
    }

    public static void stopOuinetService(Context context) {
        Intent intent = new Intent(context, OuinetService.class);
        context.stopService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(HIDE_PURGE_EXTRA)) {
            Log.d(TAG, "Hiding purge action, intent:" + intent);
            startForeground(NOTIFICATION_ID, createNotification(false));
            return Service.START_NOT_STICKY;
        }

        if (intent.hasExtra(SHOW_PURGE_EXTRA)) {
            Log.d(TAG, "Showing purge action, intent:" + intent);
            startForeground(NOTIFICATION_ID, createNotification(true));

            // Show notification without purge action after some time.
            PendingIntent hidePurgePIntent = PendingIntent.getService(this, 0,
                                                                      createHidePurgeIntent(this),
                                                                      PendingIntent.FLAG_CANCEL_CURRENT);
            new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            hidePurgePIntent.send();
                        } catch (PendingIntent.CanceledException ce) {
                        }
                    }
                }, 3000 /* ms */);

            return Service.START_NOT_STICKY;
        }

        Log.d(TAG, "Service starting, intent:" + intent);
        if (!intent.hasExtra(CONFIG_EXTRA)) {
            throw new IllegalArgumentException("Service intent missing config extra");
        }
        Config config = intent.getParcelableExtra(CONFIG_EXTRA);

        synchronized (this) {
            if (mOuinet != null) {
                Log.d(TAG, "Service already started.");
                return Service.START_NOT_STICKY;
            }
            mOuinet = new Ouinet(this, config);
        }
        startForeground(NOTIFICATION_ID, createNotification(false));
        startOuinet();
        return Service.START_NOT_STICKY;
    }

    private void startOuinet() {
        new Thread(new Runnable(){
            @Override
            public void run(){
                synchronized (OuinetService.this) {
                    if (mOuinet == null) return;
                    // Start Ouinet and set proxy in a different thread to avoid strict mode violations.
                    setProxyProperties();
                    mOuinet.start();
                }
            }
        }).start();
    }

    private void setProxyProperties() {
        Log.d(TAG, "Setting proxy system properties");
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "8077");

        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "8077");
    }

    private Intent createHomeIntent(Context context) {
        // Intent characteristics from `GeckoApp.launchOrBringToFront`.
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("about:home"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.setClassName(AppConstants.ANDROID_PACKAGE_NAME,
                            AppConstants.MOZ_ANDROID_BROWSER_INTENT_CLASS);
        return intent;
    }

    private Intent createShowPurgeIntent(Context context) {
        Intent intent = new Intent(context, OuinetService.class);
        intent.putExtra(SHOW_PURGE_EXTRA, 1);
        return intent;
    }

    private Intent createHidePurgeIntent(Context context) {
        Intent intent = new Intent(context, OuinetService.class);
        intent.putExtra(HIDE_PURGE_EXTRA, 1);
        return intent;
    }

    @SuppressLint("NewApi")
    private Notification createNotification(boolean showRealPurgeAction) {
        String channel_id = CHANNEL_ID;
        if (!AppConstants.Versions.preO) {
            // Create a notification channel for Ouinet notifications. Recreating a notification
            // that already exists has no effect.
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.ceno_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel_id = channel.getId();
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        int requestCode = 0;
        PendingIntent stopPIntent = PendingIntent.getBroadcast(this, requestCode++,
                                                               OuinetBroadcastReceiver.createStopIntent(this),
                                                               PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent homePIntent = PendingIntent.getActivity(this, requestCode++,
                                                              createHomeIntent(this),
                                                              PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent showPurgePIntent = PendingIntent.getService(this, requestCode++,
                                                                  createShowPurgeIntent(this),
                                                                  PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifb = new NotificationCompat.Builder(this, channel_id)
                .setSmallIcon(R.drawable.ic_status_logo)
                .setContentTitle(getString(R.string.ceno_notification_title))
                .setContentText(getString(R.string.ceno_notification_description))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(stopPIntent)
                .setAutoCancel(true) // Close on tap.
                .addAction(R.drawable.ic_globe_pm,
                           getString(R.string.ceno_notification_home_description),
                           homePIntent)
                .addAction(R.drawable.ic_cancel_pm,
                           getString(R.string.ceno_notification_purge_description),
                           showPurgePIntent);

        if (showRealPurgeAction) {
            PendingIntent purgePIntent = PendingIntent.getBroadcast(this, requestCode++,
                                                                    OuinetBroadcastReceiver.createPurgeIntent(this),
                                                                    PendingIntent.FLAG_UPDATE_CURRENT);
            notifb.addAction(R.drawable.ic_cancel_pm,
                             getString(R.string.ceno_notification_purge_do_description),
                             purgePIntent);
        }

        return notifb.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying service");
        synchronized (this) {
            if (mOuinet != null) {
                Ouinet ouinet = mOuinet;
                mOuinet = null;
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        ouinet.stop();
                    }
                });
                thread.start();
                try {
                    // Wait a little to allow ouinet to finish gracefuly
                    thread.join(3000 /* ms */);
                } catch (Exception ex) {}

            }
        }
        Log.d(TAG, "Service destroyed");
    }
}
