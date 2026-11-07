package com.eftyg4n.campusmate.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eftyg4n.campusmate.databinding.ActivityWelcomeBinding;
import com.eftyg4n.campusmate.R;
public class WelcomeActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "extra_student_name";

    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String name = getIntent().getStringExtra(EXTRA_NAME);
        if (name == null) {
            name = getString(R.string.welcome_default_name);
        }
        binding.textWelcome.setText(getString(R.string.welcome_message, name));

        binding.buttonBack.setOnClickListener(view -> finish());
    }
}