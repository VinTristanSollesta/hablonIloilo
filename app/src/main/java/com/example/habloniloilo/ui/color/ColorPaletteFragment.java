package com.example.habloniloilo.ui.color;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habloniloilo.R;
import com.example.habloniloilo.database.ColorGroupDatabaseHelper;
import com.example.habloniloilo.databinding.FragmentColorPaletteBinding;

import java.util.ArrayList;
import java.util.List;

public class ColorPaletteFragment extends Fragment {
    private static final String TAG = "ColorPaletteFragment";
    private FragmentColorPaletteBinding binding;
    private ColorGroupAdapter groupAdapter;
    private List<ColorGroupDatabaseHelper.ColorGroup> colorGroups = new ArrayList<>();
    private ColorGroupDatabaseHelper groupDbHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentColorPaletteBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize database helper
        groupDbHelper = new ColorGroupDatabaseHelper(requireContext());

        // Enable back button in action bar
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Color Groups");
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        try {
            // Load color groups from database
            colorGroups = groupDbHelper.getAllColorGroups();

            // Setup RecyclerView
            groupAdapter = new ColorGroupAdapter(colorGroups, new ColorGroupAdapter.OnGroupClickListener() {
                @Override
                public void onGroupClick(ColorGroupDatabaseHelper.ColorGroup group) {
                    // Show group details
                    StringBuilder message = new StringBuilder();
                    message.append("Group: ").append(group.getName()).append("\n");
                    message.append("Colors: ").append(group.getColors().size()).append("\n");
                    Toast.makeText(requireContext(), message.toString(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onEditClick(ColorGroupDatabaseHelper.ColorGroup group) {
                    // Navigate to edit group screen with group ID
                    Bundle args = new Bundle();
                    args.putLong("group_id", group.getId());
                    Navigation.findNavController(requireView()).navigate(R.id.navigation_color_group, args);
                }

                @Override
                public void onDeleteClick(ColorGroupDatabaseHelper.ColorGroup group) {
                    // Delete group from database
                    groupDbHelper.deleteColorGroup(group.getId());
                    colorGroups.remove(group);
                    groupAdapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Group removed", Toast.LENGTH_SHORT).show();
                }
            });
            binding.colorGroupsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.colorGroupsRecyclerView.setAdapter(groupAdapter);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up color groups", e);
            Toast.makeText(requireContext(), "Error displaying groups", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupDbHelper != null) {
            groupDbHelper.close();
        }
        binding = null;
    }

    private static class ColorGroupAdapter extends RecyclerView.Adapter<ColorGroupAdapter.GroupViewHolder> {
        private final List<ColorGroupDatabaseHelper.ColorGroup> groups;
        private final OnGroupClickListener listener;

        interface OnGroupClickListener {
            void onGroupClick(ColorGroupDatabaseHelper.ColorGroup group);
            void onEditClick(ColorGroupDatabaseHelper.ColorGroup group);
            void onDeleteClick(ColorGroupDatabaseHelper.ColorGroup group);
        }

        ColorGroupAdapter(List<ColorGroupDatabaseHelper.ColorGroup> groups, OnGroupClickListener listener) {
            this.groups = groups;
            this.listener = listener;
        }

        @NonNull
        @Override
        public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_color_group, parent, false);
                return new GroupViewHolder(view);
            } catch (Exception e) {
                Log.e(TAG, "Error creating view holder", e);
                throw new RuntimeException("Error creating view holder", e);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
            try {
                ColorGroupDatabaseHelper.ColorGroup group = groups.get(position);
                holder.groupName.setText(group.getName());
                holder.colorCount.setText(String.format("%d colors", group.getColors().size()));
                
                // Setup color preview adapter
                ColorPreviewAdapter previewAdapter = new ColorPreviewAdapter(group.getColors());
                holder.colorPreviewRecycler.setLayoutManager(new LinearLayoutManager(
                    holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
                holder.colorPreviewRecycler.setAdapter(previewAdapter);
                
                holder.itemView.setOnClickListener(v -> listener.onGroupClick(group));
                holder.editButton.setOnClickListener(v -> listener.onEditClick(group));
                holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(group));
            } catch (Exception e) {
                Log.e(TAG, "Error binding view holder", e);
            }
        }

        @Override
        public int getItemCount() {
            return groups.size();
        }

        static class GroupViewHolder extends RecyclerView.ViewHolder {
            TextView groupName;
            TextView colorCount;
            Button editButton;
            Button deleteButton;
            RecyclerView colorPreviewRecycler;

            GroupViewHolder(View itemView) {
                super(itemView);
                groupName = itemView.findViewById(R.id.group_name);
                colorCount = itemView.findViewById(R.id.color_count);
                editButton = itemView.findViewById(R.id.edit_button);
                deleteButton = itemView.findViewById(R.id.delete_button);
                colorPreviewRecycler = itemView.findViewById(R.id.color_preview_recycler);
            }
        }
    }

    private static class ColorPreviewAdapter extends RecyclerView.Adapter<ColorPreviewAdapter.ColorPreviewViewHolder> {
        private final List<Integer> colors;

        ColorPreviewAdapter(List<Integer> colors) {
            this.colors = colors;
        }

        @NonNull
        @Override
        public ColorPreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_color_preview, parent, false);
            return new ColorPreviewViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ColorPreviewViewHolder holder, int position) {
            holder.colorView.setBackgroundColor(colors.get(position));
        }

        @Override
        public int getItemCount() {
            return colors.size();
        }

        static class ColorPreviewViewHolder extends RecyclerView.ViewHolder {
            View colorView;

            ColorPreviewViewHolder(View itemView) {
                super(itemView);
                colorView = itemView;
            }
        }
    }
} 