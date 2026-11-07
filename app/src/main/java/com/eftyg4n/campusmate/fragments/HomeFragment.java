package com.eftyg4n.campusmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.activities.WelcomeActivity;
import com.eftyg4n.campusmate.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private static final String KEY_CLICK_COUNT = "key_click_count";

    private FragmentHomeBinding binding;
    private int clickCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        if (savedInstanceState != null) {
            clickCount = savedInstanceState.getInt(KEY_CLICK_COUNT, 0);
        }
        if (clickCount > 0) {
            showClickStatus();
        }

        binding.buttonTest.setOnClickListener(view -> {
            clickCount++;
            showClickStatus();
        });

        binding.buttonOpenWelcome.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), WelcomeActivity.class);
            intent.putExtra(WelcomeActivity.EXTRA_NAME,
                    getString(R.string.welcome_default_name));
            startActivity(intent);
        });

        return binding.getRoot();
    }

    private void showClickStatus() {
        binding.textStatus.setText(getString(R.string.home_status_clicked, clickCount));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CLICK_COUNT, clickCount);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}