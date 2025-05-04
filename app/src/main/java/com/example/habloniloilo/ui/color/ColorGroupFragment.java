package com.example.habloniloilo.ui.color;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import com.example.habloniloilo.databinding.FragmentColorGroupBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ColorGroupFragment extends Fragment {
    private static final String TAG = "ColorGroupFragment";
    private FragmentColorGroupBinding binding;
    private ColorAdapter colorAdapter;
    private List<Integer> availableColors = new ArrayList<>();
    private Set<Integer> selectedColors = new HashSet<>();
    private ColorDatabaseHelper colorDbHelper;
    private ColorGroupDatabaseHelper groupDbHelper;
    private long editingGroupId = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentColorGroupBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize database helpers
        colorDbHelper = new ColorDatabaseHelper(requireContext());
        groupDbHelper = new ColorGroupDatabaseHelper(requireContext());

        // Check if we're editing an existing group
        Bundle args = getArguments();
        if (args != null && args.containsKey("group_id")) {
            editingGroupId = args.getLong("group_id");
            ColorGroupDatabaseHelper.ColorGroup group = groupDbHelper.getColorGroup(editingGroupId);
            if (group != null) {
                selectedColors.addAll(group.getColors());
                binding.groupNameEditText.setText(group.getName());
            }
        }

        // Enable back button in action bar
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(editingGroupId == -1 ? "Create Color Group" : "Edit Color Group");
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        try {
            // Load colors from database
            availableColors = colorDbHelper.getAllColors();

            // Setup RecyclerView
            colorAdapter = new ColorAdapter(availableColors, selectedColors, new ColorAdapter.OnColorClickListener() {
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
                    availableColors.remove(Integer.valueOf(color));
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
                    Toast.makeText(requireContext(), "Please select colors for the group", Toast.LENGTH_SHORT).show();
                    return;
                }

                String groupName = binding.groupNameEditText.getText().toString().trim();
                if (groupName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a group name", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (editingGroupId == -1) {
                    // Create new group
                    long groupId = groupDbHelper.addColorGroup(groupName, new ArrayList<>(selectedColors));
                    if (groupId != -1) {
                        Toast.makeText(requireContext(), "Color group saved", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(v).navigate(R.id.navigation_color_palette);
                    } else {
                        Toast.makeText(requireContext(), "Error saving color group", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Update existing group
                    if (groupDbHelper.updateColorGroup(editingGroupId, groupName, new ArrayList<>(selectedColors))) {
                        Toast.makeText(requireContext(), "Color group updated", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(v).navigate(R.id.navigation_color_palette);
                    } else {
                        Toast.makeText(requireContext(), "Error updating color group", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Initially disable save button
            updateSaveButtonState();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up color group", e);
            Toast.makeText(requireContext(), "Error creating color group", Toast.LENGTH_SHORT).show();
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
        if (colorDbHelper != null) {
            colorDbHelper.close();
        }
        if (groupDbHelper != null) {
            groupDbHelper.close();
        }
        binding = null;
    }

    private static class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {
        private final List<Integer> colors;
        private final Set<Integer> selectedColors;
        private final OnColorClickListener listener;

        interface OnColorClickListener {
            void onColorClick(int color);
            void onSelectionChanged(int color, boolean isSelected);
            void onDeleteClick(int color);
        }

        ColorAdapter(List<Integer> colors, Set<Integer> selectedColors, OnColorClickListener listener) {
            this.colors = colors;
            this.selectedColors = selectedColors;
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
                holder.checkbox.setChecked(selectedColors.contains(color));
                
                holder.itemView.setOnClickListener(v -> listener.onColorClick(color));
                holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> 
                    listener.onSelectionChanged(color, isChecked));
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