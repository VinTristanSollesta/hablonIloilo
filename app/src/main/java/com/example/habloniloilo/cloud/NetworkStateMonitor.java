package com.example.habloniloilo.cloud;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.habloniloilo.database.HybridDatabaseManager;

/**
 * Monitors network connectivity and automatically syncs data when connection is restored
 */
public class NetworkStateMonitor {
    private static final String TAG = "NetworkStateMonitor";
    
    private Context context;
    private HybridDatabaseManager hybridDbManager;
    private boolean isRegistered = false;
    private boolean wasOnline = false;
    
    public NetworkStateMonitor(Context context, HybridDatabaseManager hybridDbManager) {
        this.context = context;
        this.hybridDbManager = hybridDbManager;
    }
    
    /**
     * Start monitoring network state
     */
    public void startMonitoring() {
        if (!isRegistered) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(networkReceiver, filter);
            isRegistered = true;
            wasOnline = hybridDbManager.isOnline();
            Log.d(TAG, "Network monitoring started. Initial state: " + (wasOnline ? "Online" : "Offline"));
        }
    }
    
    /**
     * Stop monitoring network state
     */
    public void stopMonitoring() {
        if (isRegistered) {
            context.unregisterReceiver(networkReceiver);
            isRegistered = false;
            Log.d(TAG, "Network monitoring stopped");
        }
    }
    
    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isCurrentlyOnline = hybridDbManager.isOnline();
            
            if (isCurrentlyOnline && !wasOnline) {
                // Connection restored - sync local data to cloud
                Log.d(TAG, "Connection restored - syncing local data to cloud");
                hybridDbManager.syncToCloud();
            } else if (!isCurrentlyOnline && wasOnline) {
                // Connection lost
                Log.d(TAG, "Connection lost - working in offline mode");
            }
            
            wasOnline = isCurrentlyOnline;
        }
    };
}
