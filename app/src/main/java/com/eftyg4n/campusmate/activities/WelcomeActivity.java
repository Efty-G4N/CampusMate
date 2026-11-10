package com.eftyg4n.campusmate.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eftyg4n.campusmate.databinding.ActivityWelcomeBinding;
import com.eftyg4n.campusmate.repositories.UserProfileRepository;
import com.eftyg4n.campusmate.utils.AnimUtils;
import com.eftyg4n.campusmate.utils.AppPrefs;

// Shown once to a new student. Three honest choices; an account is never forced,
// because every feature except the cloud ones works offline without it.
public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;

    // AuthActivity reports back with RESULT_OK after a successful sign in / registration.
    private final ActivityResultLauncher<Intent> authLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    finishOnboarding(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        binding.buttonCreate.setOnClickListener(view -> openAuth(true));
        binding.buttonSignIn.setOnClickListener(view -> openAuth(false));
        binding.buttonSkip.setOnClickListener(view -> finishOnboarding(false));

        if (savedInstanceState == null) {
            AnimUtils.staggerIn(binding.imageLogo, binding.textTitle, binding.textTagline,
                    binding.layoutFeatures, binding.buttonCreate, binding.buttonSignIn,
                    binding.buttonSkip, binding.textNote);
        }
    }

    // Explicit Intent with an extra: AuthActivity opens directly in "Create account" mode.
    private void openAuth(boolean register) {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra(AuthActivity.EXTRA_START_REGISTER, register);
        authLauncher.launch(intent);
    }

    // After sign in a student without a profile lands on the Profile tab to fill it in.
    private void finishOnboarding(boolean signedIn) {
        AppPrefs.setOnboarded(this);
        boolean needsProfile = signedIn && new UserProfileRepository(this).getProfile() == null;

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_FIRST_SETUP, needsProfile);
        startActivity(intent);
        finish();
    }
}
