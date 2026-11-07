package com.eftyg4n.campusmate.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eftyg4n.campusmate.databinding.FragmentResourcesBinding;

public class ResourcesFragment extends Fragment {

    private FragmentResourcesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResourcesBinding.inflate(inflater, container, false);

        binding.buttonOpenLink.setOnClickListener(view -> openLink());
        binding.buttonShare.setOnClickListener(view -> shareText());

        return binding.getRoot();
    }

    private void openLink() {
        String url = binding.editUrl.getText().toString().trim();
        if (url.isEmpty()) {
            binding.editUrl.setError("Enter a link");
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No app found to open this link",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void shareText() {
        String text = binding.editShareText.getText().toString().trim();
        if (text.isEmpty()) {
            binding.editShareText.setError("Enter some text");
            return;
        }

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(sendIntent, "Share via"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}