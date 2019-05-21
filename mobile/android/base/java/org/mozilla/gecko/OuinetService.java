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
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ie.equalit.ouinet.Ouinet;

public class OuinetService extends Service {
    private static final String TAG = "OuinetService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ouinet-notification-channel";

    // Binder given to clients
    private final IBinder binder = new OuinetBinder();
    private Ouinet mOuinet;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service starting, intent:" + intent);

        synchronized (this) {
            if (mOuinet != null) {
                Log.d(TAG, "Service already started.");
                return Service.START_NOT_STICKY;
            }
            mOuinet = new Ouinet(this, getConfig());
        }
        startForeground(NOTIFICATION_ID, createNotification());
        startOuinet();
        return Service.START_NOT_STICKY;
    }

    private void startOuinet() {
        new Thread(new Runnable(){
            @Override
            public void run(){
                // Start Ouinet and set proxy in a different thread to avoid strict mode violations.
                setProxyProperties();
                mOuinet.start();
            }
        }).start();
    }

    private void setProxyProperties() {
        Log.d(TAG, "Setting proxy system properties");
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "8080");

        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "8080");
    }

    @SuppressLint("NewApi")
    private Notification createNotification() {
        Intent intent = OuinetBroadcastReceiver.createIntent(this);
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String channel_id = CHANNEL_ID;
        if (!AppConstants.Versions.preO) {
            // Create a notification channel for Ouinet notifications. Recreating a notification
            // that already exists has no effect.
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.ouinet_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel_id = channel.getId();
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channel_id)
                .setSmallIcon(R.drawable.ic_status_logo)
                .setContentTitle(getString(R.string.ouinet_notification_title))
                .setContentText(getString(R.string.ouinet_notification_description))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // Close on tap.
                .build();
    }

    public class OuinetBinder extends Binder {
        OuinetService getService() {
            // Return this instance of OuinetService so clients can call public methods
            return OuinetService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public String getPathToCARootCert() {
        return mOuinet.pathToCARootCert();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying service");
        if (mOuinet != null) {
            mOuinet.stop();
        }
        mOuinet = null;
        Log.d(TAG, "Service destroyed");
    }

    private Ouinet.Config getConfig() {
        Ouinet.Config config = new Ouinet.Config();
        config.index_bep44_pubkey   = getResources().getString(R.string.ouinet_index_bep44_pubkey);
        config.index_ipns_id     = getResources().getString(R.string.ouinet_index_ipns_id);
        config.injector_endpoint    = getResources().getString(R.string.ouinet_injector_endpoint);
        config.injector_credentials = getResources().getString(R.string.ouinet_injector_credentials);
        config.injector_tls_cert    = getResources().getString(R.string.ouinet_injector_tls_cert);
        config.tls_ca_cert_store_path = "file:///android_asset/ceno/cacert.pem";
        return config;
    }
}
