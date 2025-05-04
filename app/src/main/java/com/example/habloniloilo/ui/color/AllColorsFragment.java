package com.example.habloniloilo.ui.color;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habloniloilo.R;
import com.example.habloniloilo.database.ColorDatabaseHelper;
import com.example.habloniloilo.databinding.FragmentAllColorsBinding;

import java.util.ArrayList;
import java.util.List;

public class AllColorsFragment extends Fragment {
    private static final String TAG = "AllColorsFragment";
    private FragmentAllColorsBinding binding;
    private ColorAdapter colorAdapter;
    private List<Integer> savedColors = new ArrayList<>();
    private ColorDatabaseHelper dbHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAllColorsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize database helper
        dbHelper = new ColorDatabaseHelper(requireContext());

        // Enable back button in action bar
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("All Colors");
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        try {
            // Load colors from database
            savedColors = dbHelper.getAllColors();

            // Setup RecyclerView
            colorAdapter = new ColorAdapter(savedColors, new ColorAdapter.OnColorClickListener() {
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
                public void onDeleteClick(int color) {
                    // Delete color from database
                    dbHelper.deleteColor(color);
                    savedColors.remove(Integer.valueOf(color));
                    colorAdapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Color removed", Toast.LENGTH_SHORT).show();
                }
            });
            binding.colorsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.colorsRecyclerView.setAdapter(colorAdapter);

            // Setup clear button
            binding.clearButton.setOnClickListener(v -> {
                dbHelper.clearAllColors();
                savedColors.clear();
                colorAdapter.notifyDataSetChanged();
                Toast.makeText(requireContext(), "All colors cleared", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting up all colors", e);
            Toast.makeText(requireContext(), "Error displaying colors", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) {
            dbHelper.close();
        }
        binding = null;
    }

    private static class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {
        private final List<Integer> colors;
        private final OnColorClickListener listener;

        interface OnColorClickListener {
            void onColorClick(int color);
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
            View colorView;
            TextView colorHex;
            ImageButton deleteButton;

            ColorViewHolder(View itemView) {
                super(itemView);
                colorView = itemView.findViewById(R.id.color_view);
                colorHex = itemView.findViewById(R.id.color_hex);
                deleteButton = itemView.findViewById(R.id.delete_button);
            }
        }
    }
} 