package com.example.habloniloilo.database;

import android.content.Context;
import android.util.Log;

import com.example.habloniloilo.cloud.CloudDatabaseService;

import java.util.List;

/**
 * Hybrid database manager that handles both local and cloud database operations
 */
public class HybridDatabaseManager {
    private static final String TAG = "HybridDatabaseManager";
    
    private Context context;
    private ColorDatabaseHelper localColorDb;
    private ColorGroupDatabaseHelper localColorGroupDb;
    private CloudDatabaseService cloudService;
    
    public HybridDatabaseManager(Context context) {
        this.context = context;
        this.localColorDb = new ColorDatabaseHelper(context);
        this.localColorGroupDb = new ColorGroupDatabaseHelper(context);
        this.cloudService = new CloudDatabaseService(context);
    }
    
    /**
     * Add color to both local and cloud database
     */
    public void addColor(int color) {
        // Always add to local database first
        localColorDb.addColor(color);
        Log.d(TAG, "Color added to local database");
        
        // Sync to cloud if online
        if (cloudService.isOnline()) {
            cloudService.syncToCloud();
            Log.d(TAG, "Color synced to cloud");
        } else {
            Log.d(TAG, "Offline - color will be synced when online");
        }
    }
    
    /**
     * Add color group to both local and cloud database
     */
    public long addColorGroup(String name, List<Integer> colors) {
        // Always add to local database first
        long groupId = localColorGroupDb.addColorGroup(name, colors);
        Log.d(TAG, "Color group added to local database with ID: " + groupId);
        
        // Sync to cloud if online
        if (cloudService.isOnline()) {
            cloudService.syncToCloud();
            Log.d(TAG, "Color group synced to cloud");
        } else {
            Log.d(TAG, "Offline - color group will be synced when online");
        }
        
        return groupId;
    }
    
    /**
     * Get all colors from local database
     */
    public List<Integer> getAllColors() {
        return localColorDb.getAllColors();
    }
    
    /**
     * Get all color groups from local database
     */
    public List<ColorGroupDatabaseHelper.ColorGroup> getAllColorGroups() {
        return localColorGroupDb.getAllColorGroups();
    }
    
    /**
     * Delete color from both local and cloud database
     */
    public void deleteColor(int color) {
        // Delete from local database
        localColorDb.deleteColor(color);
        Log.d(TAG, "Color deleted from local database");
        
        // Sync to cloud if online
        if (cloudService.isOnline()) {
            cloudService.syncToCloud();
            Log.d(TAG, "Color deletion synced to cloud");
        }
    }
    
    /**
     * Delete color group from both local and cloud database
     */
    public void deleteColorGroup(long groupId) {
        // Delete from local database
        localColorGroupDb.deleteColorGroup(groupId);
        Log.d(TAG, "Color group deleted from local database");
        
        // Sync to cloud if online
        if (cloudService.isOnline()) {
            cloudService.syncToCloud();
            Log.d(TAG, "Color group deletion synced to cloud");
        }
    }
    
    /**
     * Clear all data from both local and cloud database
     */
    public void clearAllData() {
        // Clear local database
        localColorDb.clearAllColors();
        localColorGroupDb.clearAllColorGroups();
        Log.d(TAG, "All data cleared from local database");
        
        // Sync to cloud if online
        if (cloudService.isOnline()) {
            cloudService.syncToCloud();
            Log.d(TAG, "Data clearing synced to cloud");
        }
    }
    
    /**
     * Sync all local data to cloud
     */
    public void syncToCloud() {
        if (cloudService.isOnline()) {
            cloudService.syncToCloud();
            Log.d(TAG, "Manual sync to cloud initiated");
        } else {
            Log.d(TAG, "Cannot sync - no internet connection");
        }
    }
    
    /**
     * Download and merge cloud data to local database
     */
    public void syncFromCloud() {
        if (cloudService.isOnline()) {
            cloudService.syncFromCloud();
            Log.d(TAG, "Manual sync from cloud initiated");
        } else {
            Log.d(TAG, "Cannot sync - no internet connection");
        }
    }
    
    /**
     * Check if device is online
     */
    public boolean isOnline() {
        return cloudService.isOnline();
    }
    
    /**
     * Get sync status
     */
    public String getSyncStatus() {
        if (isOnline()) {
            return "Online - Auto-sync enabled";
        } else {
            return "Offline - Data will sync when online";
        }
    }
    
    /**
     * Shutdown the hybrid database manager
     */
    public void shutdown() {
        if (cloudService != null) {
            cloudService.shutdown();
        }
    }
}
