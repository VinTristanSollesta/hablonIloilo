package com.example.habloniloilo.ui.color;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habloniloilo.R;
import com.example.habloniloilo.database.ColorGroupDatabaseHelper;
import com.example.habloniloilo.database.ColorGroupDatabaseHelper.ColorGroup;
import com.example.habloniloilo.databinding.FragmentCompareColorsBinding;

import java.util.ArrayList;
import java.util.List;

public class CompareColorsFragment extends Fragment {
    private FragmentCompareColorsBinding binding;
    private ColorGroupDatabaseHelper groupDbHelper;
    private List<Integer> newColors = new ArrayList<>();
    private List<ColorGroup> groups = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCompareColorsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        groupDbHelper = new ColorGroupDatabaseHelper(requireContext());

        // get new colors from arguments
        if (getArguments() != null) {
            ArrayList<Integer> colors = getArguments().getIntegerArrayList("new_colors");
            if (colors != null) newColors = colors;
        }

        // get latest palette group colors (instead of all existing colors)
        groups = groupDbHelper.getAllColorGroups();

        // compare exact RGB matches
        // captured list (no status, just HSV)
        List<CompareItem> capturedList = new ArrayList<>();
        for (int c : newColors) capturedList.add(new CompareItem(c, null));

        // existing palette grouped sections
        List<GroupSection> existingSections = new ArrayList<>();
        for (ColorGroup group : groups) {
            existingSections.add(new GroupSection(group.getName(), group.getColors()));
        }

        ColorAdapter capturedAdapter = new ColorAdapter(capturedList, new ColorAdapter.OnColorClickListener() {
            @Override
            public void onColorClick(CompareItem item) {
                showColorToast(item.color, null);
            }
            @Override public void onSelectionChanged(CompareItem item, boolean isSelected) {}
            @Override public void onDeleteClick(CompareItem item) {}
        });

        binding.capturedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.capturedRecyclerView.setAdapter(capturedAdapter);

        ExistingAdapter existingAdapter = new ExistingAdapter(existingSections, this::showColorToast);
        binding.existingRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.existingRecyclerView.setAdapter(existingAdapter);

        binding.savePaletteButton.setOnClickListener(v -> promptAndSavePalette());

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupDbHelper != null) groupDbHelper.close();
        binding = null;
    }

    private void showColorToast(int color, String groupName) {
        float[] hsv = colorToHsv(color);
        String prefix = groupName != null ? "Group: " + groupName + "\n" : "";
        String msg = String.format(
                "%sHSV: (H: %.1f°, S: %.2f, V: %.2f)",
                prefix, hsv[0], hsv[1], hsv[2]
        );
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void promptAndSavePalette() {
        if (newColors == null || newColors.isEmpty()) {
            Toast.makeText(requireContext(), "No captured colors to save", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText input = new EditText(requireContext());
        input.setHint("Palette name");
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Save as New Palette")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String nameInput = input.getText() != null ? input.getText().toString().trim() : "";
                    String name = nameInput.isEmpty()
                            ? "Captured Palette " + System.currentTimeMillis()
                            : nameInput;
                    long id = groupDbHelper.addColorGroup(name, new ArrayList<>(newColors));
                    if (id != -1) {
                        Toast.makeText(requireContext(), "Saved as new palette", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
                    } else {
                        Toast.makeText(requireContext(), "Failed to save palette", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static float[] colorToHsv(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv;
    }

    // Local adapter copy (keeps coupling minimal). Move to shared file if needed.
    private static class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {
        private final List<CompareItem> items;
        private final OnColorClickListener listener;

        interface OnColorClickListener {
            void onColorClick(CompareItem item);
            void onSelectionChanged(CompareItem item, boolean isSelected);
            void onDeleteClick(CompareItem item);
        }

        ColorAdapter(List<CompareItem> items, OnColorClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_color, parent, false);
            return new ColorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            CompareItem item = items.get(position);
            holder.colorView.setBackgroundColor(item.color);

            float[] hsv = colorToHsv(item.color);
            String prefix = item.groupName != null ? "Group: " + item.groupName + "\n" : "";
            holder.colorHex.setText(String.format("%sHSV: (H: %.1f°, S: %.2f, V: %.2f)",
                    prefix, hsv[0], hsv[1], hsv[2]));

            holder.checkbox.setOnCheckedChangeListener(null);
            holder.checkbox.setChecked(false);
            holder.checkbox.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> listener.onColorClick(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

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

    private static class CompareItem {
        final int color;
        final String groupName;

        CompareItem(int color, String groupName) {
            this.color = color;
            this.groupName = groupName;
        }
    }

    private static class GroupSection {
        final String name;
        final List<Integer> colors;
        boolean expanded = false;

        GroupSection(String name, List<Integer> colors) {
            this.name = name;
            this.colors = colors != null ? colors : new ArrayList<>();
        }
    }

    private static class ExistingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_COLOR = 1;

        private final List<GroupSection> sections;
        private final List<Row> rows = new ArrayList<>();
        private final ColorClickListener listener;

        interface ColorClickListener {
            void onColorClick(int color, String groupName);
        }

        ExistingAdapter(List<GroupSection> sections, ColorClickListener listener) {
            this.sections = sections;
            this.listener = listener;
            rebuildRows();
        }

        private void rebuildRows() {
            rows.clear();
            for (GroupSection section : sections) {
                rows.add(new HeaderRow(section));
                if (section.expanded) {
                    for (int c : section.colors) {
                        rows.add(new ColorRow(c, section.name));
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof HeaderRow ? TYPE_HEADER : TYPE_COLOR;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_group_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_color, parent, false);
                return new ColorViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            if (holder instanceof HeaderViewHolder) {
                HeaderRow headerRow = (HeaderRow) row;
                HeaderViewHolder hvh = (HeaderViewHolder) holder;
                hvh.title.setText(headerRow.section.name);
                hvh.itemView.setOnClickListener(v -> {
                    headerRow.section.expanded = !headerRow.section.expanded;
                    rebuildRows();
                });
            } else if (holder instanceof ColorViewHolder) {
                ColorRow colorRow = (ColorRow) row;
                ColorViewHolder cvh = (ColorViewHolder) holder;
                cvh.colorView.setBackgroundColor(colorRow.color);
                float[] hsv = colorToHsv(colorRow.color);
                String prefix = colorRow.groupName != null ? "Group: " + colorRow.groupName + "\n" : "";
                cvh.colorHex.setText(String.format("%sHSV: (H: %.1f°, S: %.2f, V: %.2f)",
                        prefix, hsv[0], hsv[1], hsv[2]));
                cvh.checkbox.setVisibility(View.GONE);
                cvh.deleteButton.setVisibility(View.GONE);
                cvh.itemView.setOnClickListener(v -> listener.onColorClick(colorRow.color, colorRow.groupName));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        private interface Row {}

        private static class HeaderRow implements Row {
            final GroupSection section;
            HeaderRow(GroupSection section) { this.section = section; }
        }

        private static class ColorRow implements Row {
            final int color;
            final String groupName;
            ColorRow(int color, String groupName) {
                this.color = color;
                this.groupName = groupName;
            }
        }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            final TextView title;
            HeaderViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.header_title);
            }
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
