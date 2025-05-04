package com.example.habloniloilo.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.habloniloilo.R;
import com.example.habloniloilo.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.btnCamera.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.navigation_dashboard)
        );

        binding.btnColorPalette.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.navigation_color_palette)
        );

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}