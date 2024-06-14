package com.example.mon_nw;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "network_log";
    private static final String LOGS_KEY = "logs";
    private static final String TOTAL_CONNECTED_TIME_KEY = "total_connected_time";

    private TextView networkStatusTextView;
    private TextView networkTypeTextView;
    private CalendarView calendarView;
    private TextView connectedTimeTextView;
    private TextView logsTextView;
    private SharedPreferences prefs;
    private NetworkStatusReceiver networkStatusReceiver;
    private Handler handler = new Handler();
    private List<String> logs = new ArrayList<>();
    private long totalConnectedTimeMillis = 0;
    private boolean isConnected = false;
    private Date connectedStartTime;
    private String currentNetworkType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        networkStatusTextView = findViewById(R.id.networkStatusTextView);
        networkTypeTextView = findViewById(R.id.networkTypeTextView);
        calendarView = findViewById(R.id.calendarView);
        connectedTimeTextView = findViewById(R.id.connectedTimeTextView);
        logsTextView = findViewById(R.id.logsTextView);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        retrieveLogs();
        retrieveTotalConnectedTime();

        registerNetworkReceiver();

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                Date selectedDate = calendar.getTime();
                displayLogsForDate(selectedDate);
            }
        });

        // Periodically update UI
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUI();
                handler.postDelayed(this, 1000); // Refresh every second
            }
        }, 1000); // Start after 1 second
    }

    private void retrieveLogs() {
        String savedLogs = prefs.getString(LOGS_KEY, "");
        if (!savedLogs.isEmpty()) {
            String[] logEntries = savedLogs.split("\n");
            for (String logEntry : logEntries) {
                logs.add(logEntry);
            }
        }
    }

    private void saveLogs() {
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder logBuilder = new StringBuilder();
        for (String log : logs) {
            logBuilder.append(log).append("\n");
        }
        editor.putString(LOGS_KEY, logBuilder.toString());
        editor.apply();
    }

    private void retrieveTotalConnectedTime() {
        totalConnectedTimeMillis = prefs.getLong(TOTAL_CONNECTED_TIME_KEY, 0);
    }

    private void saveTotalConnectedTime() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(TOTAL_CONNECTED_TIME_KEY, totalConnectedTimeMillis);
        editor.apply();
    }

    private void registerNetworkReceiver() {
        networkStatusReceiver = new NetworkStatusReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStatusReceiver, filter);
    }

    private void checkNetworkStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = cm.getActiveNetwork();
        NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(activeNetwork);

        boolean currentlyConnected = networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        if (currentlyConnected && !isConnected) {
            // Transition from disconnected to connected
            isConnected = true;
            connectedStartTime = new Date();

            // Determine network type
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                currentNetworkType = "WiFi";
            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                currentNetworkType = "Mobile Data";
            } else {
                currentNetworkType = "Unknown";
            }

            networkStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.green));
        } else if (!currentlyConnected && isConnected) {
            // Transition from connected to disconnected
            isConnected = false;
            if (connectedStartTime != null) {
                long connectedDurationMillis = new Date().getTime() - connectedStartTime.getTime();
                totalConnectedTimeMillis += connectedDurationMillis;
                connectedStartTime = null;
                saveTotalConnectedTime(); // Save the updated total connected time
            }
            currentNetworkType = ""; // Clear the network type when disconnected
            networkStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.red));
        }

        // Update network status text view
        String networkStatusText = "Status: " + (currentlyConnected ? "Connected" : "Disconnected");
        networkStatusTextView.setText(networkStatusText);

        // Update network type text view
        String networkTypeText = "Network Type: " + currentNetworkType;
        networkTypeTextView.setText(networkTypeText);

        // Log network status
        String logEntry = (currentlyConnected ? "Connected: " : "Disconnected: ") + getCurrentTime();
        logs.add(logEntry);
        logNetworkStatus(logEntry);
        saveLogs(); // Save the logs
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void logNetworkStatus(String logEntry) {
        Log.d(TAG, logEntry);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkStatusReceiver);
        handler.removeCallbacksAndMessages(null); // Remove all callbacks and messages
    }

    private void displayLogsForDate(Date date) {
        String selectedDateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
        StringBuilder filteredLogs = new StringBuilder();

        for (int i = 0; i < logs.size(); i++) {
            String logEntry = logs.get(i);
            if (logEntry.startsWith("Connected: " + selectedDateString) || logEntry.startsWith("Disconnected: " + selectedDateString)) {
                SpannableString spannableLogEntry = new SpannableString(logEntry);

                // Color connected logs green and disconnected logs red
                if (logEntry.startsWith("Connected: ")) {
                    spannableLogEntry.setSpan(new ForegroundColorSpan(Color.GREEN), 0, logEntry.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (logEntry.startsWith("Disconnected: ")) {
                    spannableLogEntry.setSpan(new ForegroundColorSpan(Color.RED), 0, logEntry.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                filteredLogs.append(spannableLogEntry).append(" (");

                // Calculate duration to next log
                if (i < logs.size() - 1) {
                    String nextLogEntry = logs.get(i + 1);
                    try {
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        Date currentLogTime = format.parse(logEntry.substring(logEntry.indexOf(":") + 2));
                        Date nextLogTime = format.parse(nextLogEntry.substring(nextLogEntry.indexOf(":") + 2));
                        long durationMillis = nextLogTime.getTime() - currentLogTime.getTime();
                        String duration = getFormattedDuration(durationMillis);
                        filteredLogs.append(duration);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                filteredLogs.append(")\n");
            }
        }

        if (filteredLogs.length() == 0) {
            filteredLogs.append("No network status changes for selected date.");
        }

        // Update report text view
        logsTextView.setText(filteredLogs);

        // Update total connected time
        String totalConnectedTime = "Total Connected Time: " + getFormattedDuration(totalConnectedTimeMillis);
        connectedTimeTextView.setText(totalConnectedTime);
    }

    private String getFormattedDuration(long durationMillis) {
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }

    private void updateUI() {
        // Update UI to reflect changes
        Date today = Calendar.getInstance().getTime();
        displayLogsForDate(today);
    }

    public class NetworkStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkNetworkStatus();
        }
    }
}
