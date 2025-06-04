package com.example.habloniloilo.ui.color;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habloniloilo.R;
import com.example.habloniloilo.database.ColorDatabaseHelper;
import com.example.habloniloilo.database.ColorGroupDatabaseHelper;
import com.example.habloniloilo.databinding.FragmentColorDisplayBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ColorDisplayFragment extends Fragment {
    private static final String TAG = "ColorDisplayFragment";
    private FragmentColorDisplayBinding binding;
    private ColorAdapter colorAdapter;
    private List<Integer> extractedColors = new ArrayList<>();
    private Set<Integer> selectedColors = new HashSet<>();
    private ColorDatabaseHelper dbHelper;
    private ColorGroupDatabaseHelper groupDbHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentColorDisplayBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize database helpers
        dbHelper = new ColorDatabaseHelper(requireContext());
        groupDbHelper = new ColorGroupDatabaseHelper(requireContext());

        // Enable back button in action bar
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Select Colors to Save");
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        try {
            // Get colors from arguments
            if (getArguments() != null) {
                ArrayList<Integer> colors = getArguments().getIntegerArrayList("colors");
                if (colors != null) {
                    extractedColors = colors;
                }
            }

            // Setup RecyclerView
            colorAdapter = new ColorAdapter(extractedColors, new ColorAdapter.OnColorClickListener() {
                @Override
                public void onColorClick(int color) {
                    // Show color details
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);
                    String message = String.format("RGB: (%d, %d, %d)\nHex: #%06X", r, g, b, (0xFFFFFF & color));
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onSelectionChanged(int color, boolean isSelected) {
                    if (isSelected) {
                        selectedColors.add(color);
                    } else {
                        selectedColors.remove(color);
                    }
                    updateSaveButtonState();
                }

                @Override
                public void onDeleteClick(int color) {
                    // Remove color from the list
                    extractedColors.remove(Integer.valueOf(color));
                    selectedColors.remove(color);
                    colorAdapter.notifyDataSetChanged();
                    updateSaveButtonState();
                    Toast.makeText(requireContext(), "Color removed", Toast.LENGTH_SHORT).show();
                }
            });
            binding.colorsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.colorsRecyclerView.setAdapter(colorAdapter);

            // Setup save button
            binding.saveButton.setOnClickListener(v -> {
                if (selectedColors.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select colors to save", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Save selected colors to database
                for (int color : selectedColors) {
                    dbHelper.addColor(color);
                }
                
                // Create a color group with the selected colors
                String groupName = "Camera Colors " + System.currentTimeMillis();
                long groupId = groupDbHelper.addColorGroup(groupName, new ArrayList<>(selectedColors));
                
                if (groupId != -1) {
                    Toast.makeText(requireContext(), "Colors saved to palette", Toast.LENGTH_SHORT).show();
                    // Navigate to color palette screen
                    Navigation.findNavController(v).navigate(R.id.navigation_color_palette);
                } else {
                    Toast.makeText(requireContext(), "Error saving colors", Toast.LENGTH_SHORT).show();
                }
            });

            // Initially disable save button
            updateSaveButtonState();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up color display", e);
            Toast.makeText(requireContext(), "Error displaying colors", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }

        return root;
    }

    private void updateSaveButtonState() {
        binding.saveButton.setEnabled(!selectedColors.isEmpty());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (groupDbHelper != null) {
            groupDbHelper.close();
        }
        binding = null;
    }

    private static class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {
        private final List<Integer> colors;
        private final OnColorClickListener listener;
        private final Set<Integer> selectedColors = new HashSet<>();

        interface OnColorClickListener {
            void onColorClick(int color);
            void onSelectionChanged(int color, boolean isSelected);
            void onDeleteClick(int color);
        }

        ColorAdapter(List<Integer> colors, OnColorClickListener listener) {
            this.colors = colors;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_color, parent, false);
                return new ColorViewHolder(view);
            } catch (Exception e) {
                Log.e(TAG, "Error creating view holder", e);
                throw new RuntimeException("Error creating view holder", e);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            try {
                int color = colors.get(position);
                holder.colorView.setBackgroundColor(color);
                holder.colorHex.setText(String.format("#%06X", (0xFFFFFF & color)));
                
                // Remove previous listener to prevent unwanted callbacks
                holder.checkbox.setOnCheckedChangeListener(null);
                // Set the checkbox state based on whether the color is selected
                holder.checkbox.setChecked(selectedColors.contains(color));
                // Add the listener back
                holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedColors.add(color);
                    } else {
                        selectedColors.remove(color);
                    }
                    listener.onSelectionChanged(color, isChecked);
                });
                
                holder.itemView.setOnClickListener(v -> listener.onColorClick(color));
                holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(color));
            } catch (Exception e) {
                Log.e(TAG, "Error binding view holder", e);
            }
        }

        @Override
        public int getItemCount() {
            return colors.size();
        }

        static class ColorViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkbox;
            View colorView;
            TextView colorHex;
            ImageButton deleteButton;

            ColorViewHolder(View itemView) {
                super(itemView);
                checkbox = itemView.findViewById(R.id.color_checkbox);
                colorView = itemView.findViewById(R.id.color_view);
                colorHex = itemView.findViewById(R.id.color_hex);
                deleteButton = itemView.findViewById(R.id.delete_button);
            }
        }
    }
} 