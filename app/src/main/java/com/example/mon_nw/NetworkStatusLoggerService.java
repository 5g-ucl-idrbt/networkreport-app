package com.example.mon_nw;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NetworkStatusLoggerService extends Service {
    private static final String TAG = "NetworkStatusLogger";
    private NetworkStatusReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        receiver = new NetworkStatusReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);
        Log.d(TAG, "Service created and receiver registered");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.d(TAG, "Service destroyed and receiver unregistered");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class NetworkStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network activeNetwork = cm.getActiveNetwork();
            NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(activeNetwork);

            boolean isConnected = networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String logEntry = (isConnected ? "Connected: " : "Disconnected: ") + currentTime;

            Log.d(TAG, logEntry);

            // Save log to shared preferences
            SharedPreferences prefs = context.getSharedPreferences("network_log", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String existingLogs = prefs.getString("logs", "");
            editor.putString("logs", existingLogs + logEntry + "\n");
            editor.apply();
        }
    }
}
