package com.example.habloniloilo.ui.sync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.habloniloilo.R;
import com.example.habloniloilo.database.HybridDatabaseManager;

public class SyncSettingsFragment extends Fragment {
    private static final String TAG = "SyncSettingsFragment";
    
    private HybridDatabaseManager hybridDbManager;
    private TextView syncStatusText;
    private Button syncToCloudButton;
    private Button syncFromCloudButton;
    private Button clearDataButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sync_settings, container, false);
        
        // Initialize hybrid database manager
        hybridDbManager = new HybridDatabaseManager(requireContext());
        
        // Enable back button in action bar
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Sync Settings");
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize views
        syncStatusText = root.findViewById(R.id.sync_status_text);
        syncToCloudButton = root.findViewById(R.id.sync_to_cloud_button);
        syncFromCloudButton = root.findViewById(R.id.sync_from_cloud_button);
        clearDataButton = root.findViewById(R.id.clear_data_button);
        
        // Set up click listeners
        syncToCloudButton.setOnClickListener(v -> {
            hybridDbManager.syncToCloud();
            Toast.makeText(requireContext(), "Syncing to cloud...", Toast.LENGTH_SHORT).show();
            updateSyncStatus();
        });
        
        syncFromCloudButton.setOnClickListener(v -> {
            hybridDbManager.syncFromCloud();
            Toast.makeText(requireContext(), "Syncing from cloud...", Toast.LENGTH_SHORT).show();
            updateSyncStatus();
        });
        
        clearDataButton.setOnClickListener(v -> {
            hybridDbManager.clearAllData();
            Toast.makeText(requireContext(), "All data cleared", Toast.LENGTH_SHORT).show();
            updateSyncStatus();
        });
        
        // Update sync status
        updateSyncStatus();
        
        return root;
    }
    
    private void updateSyncStatus() {
        String status = hybridDbManager.getSyncStatus();
        syncStatusText.setText(status);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (hybridDbManager != null) {
            hybridDbManager.shutdown();
        }
    }
}
