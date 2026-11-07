package com.eftyg4n.campusmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eftyg4n.campusmate.activities.WelcomeActivity;
import com.eftyg4n.campusmate.databinding.FragmentHomeBinding;
import com.eftyg4n.campusmate.R;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private int clickCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.buttonTest.setOnClickListener(view -> {
            clickCount++;
            binding.textStatus.setText("Button clicked " + clickCount + " time(s)");
            binding.textStatus.setText(getString(R.string.home_status_clicked, clickCount));
        });

        binding.buttonOpenWelcome.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), WelcomeActivity.class);
            intent.putExtra(WelcomeActivity.EXTRA_NAME, "Student");
            intent.putExtra(WelcomeActivity.EXTRA_NAME, getString(R.string.welcome_default_name));
            startActivity(intent);
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}