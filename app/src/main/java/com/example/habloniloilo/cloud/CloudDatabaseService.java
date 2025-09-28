package com.example.habloniloilo.cloud;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.habloniloilo.database.ColorDatabaseHelper;
import com.example.habloniloilo.database.ColorGroupDatabaseHelper;

public class CloudDatabaseService {
    private static final String TAG = "CloudDatabaseService";
    private static final String CLOUD_BASE_URL = "https://your-api-endpoint.com/api"; // Replace with your actual API endpoint
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private Context context;
    private OkHttpClient httpClient;
    private ExecutorService executorService;
    private ColorDatabaseHelper colorDbHelper;
    private ColorGroupDatabaseHelper colorGroupDbHelper;
    
    public CloudDatabaseService(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
        this.executorService = Executors.newFixedThreadPool(2);
        this.colorDbHelper = new ColorDatabaseHelper(context);
        this.colorGroupDbHelper = new ColorGroupDatabaseHelper(context);
    }
    
    /**
     * Check if device is connected to internet
     */
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
    
    /**
     * Sync all local data to cloud when online
     */
    public void syncToCloud() {
        if (!isOnline()) {
            Log.d(TAG, "No internet connection, skipping cloud sync");
            return;
        }
        
        executorService.execute(() -> {
            try {
                syncColorsToCloud();
                syncColorGroupsToCloud();
                Log.d(TAG, "Cloud sync completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during cloud sync", e);
            }
        });
    }
    
    /**
     * Sync individual colors to cloud
     */
    private void syncColorsToCloud() {
        try {
            List<Integer> localColors = colorDbHelper.getAllColors();
            if (localColors.isEmpty()) {
                Log.d(TAG, "No colors to sync");
                return;
            }
            
            JSONObject requestBody = new JSONObject();
            JSONArray colorsArray = new JSONArray();
            
            for (Integer color : localColors) {
                JSONObject colorObj = new JSONObject();
                colorObj.put("color", color);
                colorObj.put("timestamp", System.currentTimeMillis());
                colorsArray.put(colorObj);
            }
            
            requestBody.put("colors", colorsArray);
            requestBody.put("device_id", getDeviceId());
            
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(CLOUD_BASE_URL + "/colors/sync")
                    .post(body)
                    .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to sync colors to cloud", e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Colors synced to cloud successfully");
                    } else {
                        Log.e(TAG, "Failed to sync colors: " + response.code());
                    }
                    response.close();
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for color sync", e);
        }
    }
    
    /**
     * Sync color groups to cloud
     */
    private void syncColorGroupsToCloud() {
        try {
            List<ColorGroupDatabaseHelper.ColorGroup> localGroups = colorGroupDbHelper.getAllColorGroups();
            if (localGroups.isEmpty()) {
                Log.d(TAG, "No color groups to sync");
                return;
            }
            
            JSONObject requestBody = new JSONObject();
            JSONArray groupsArray = new JSONArray();
            
            for (ColorGroupDatabaseHelper.ColorGroup group : localGroups) {
                JSONObject groupObj = new JSONObject();
                groupObj.put("id", group.getId());
                groupObj.put("name", group.getName());
                groupObj.put("timestamp", group.getTimestamp());
                
                JSONArray colorsArray = new JSONArray();
                for (Integer color : group.getColors()) {
                    colorsArray.put(color);
                }
                groupObj.put("colors", colorsArray);
                
                groupsArray.put(groupObj);
            }
            
            requestBody.put("color_groups", groupsArray);
            requestBody.put("device_id", getDeviceId());
            
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(CLOUD_BASE_URL + "/color-groups/sync")
                    .post(body)
                    .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to sync color groups to cloud", e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Color groups synced to cloud successfully");
                    } else {
                        Log.e(TAG, "Failed to sync color groups: " + response.code());
                    }
                    response.close();
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for color group sync", e);
        }
    }
    
    /**
     * Download and merge cloud data to local database
     */
    public void syncFromCloud() {
        if (!isOnline()) {
            Log.d(TAG, "No internet connection, skipping cloud download");
            return;
        }
        
        executorService.execute(() -> {
            try {
                downloadColorsFromCloud();
                downloadColorGroupsFromCloud();
                Log.d(TAG, "Cloud download completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during cloud download", e);
            }
        });
    }
    
    /**
     * Download colors from cloud
     */
    private void downloadColorsFromCloud() {
        Request request = new Request.Builder()
                .url(CLOUD_BASE_URL + "/colors?device_id=" + getDeviceId())
                .get()
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to download colors from cloud", e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray colorsArray = jsonResponse.getJSONArray("colors");
                        
                        for (int i = 0; i < colorsArray.length(); i++) {
                            JSONObject colorObj = colorsArray.getJSONObject(i);
                            int color = colorObj.getInt("color");
                            colorDbHelper.addColor(color);
                        }
                        
                        Log.d(TAG, "Downloaded " + colorsArray.length() + " colors from cloud");
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing colors from cloud", e);
                    }
                } else {
                    Log.e(TAG, "Failed to download colors: " + response.code());
                }
                response.close();
            }
        });
    }
    
    /**
     * Download color groups from cloud
     */
    private void downloadColorGroupsFromCloud() {
        Request request = new Request.Builder()
                .url(CLOUD_BASE_URL + "/color-groups?device_id=" + getDeviceId())
                .get()
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to download color groups from cloud", e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray groupsArray = jsonResponse.getJSONArray("color_groups");
                        
                        for (int i = 0; i < groupsArray.length(); i++) {
                            JSONObject groupObj = groupsArray.getJSONObject(i);
                            String name = groupObj.getString("name");
                            JSONArray colorsArray = groupObj.getJSONArray("colors");
                            
                            List<Integer> colors = new java.util.ArrayList<>();
                            for (int j = 0; j < colorsArray.length(); j++) {
                                colors.add(colorsArray.getInt(j));
                            }
                            
                            colorGroupDbHelper.addColorGroup(name, colors);
                        }
                        
                        Log.d(TAG, "Downloaded " + groupsArray.length() + " color groups from cloud");
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing color groups from cloud", e);
                    }
                } else {
                    Log.e(TAG, "Failed to download color groups: " + response.code());
                }
                response.close();
            }
        });
    }
    
    /**
     * Get unique device identifier
     */
    private String getDeviceId() {
        return android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
        );
    }
    
    /**
     * Shutdown the service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
