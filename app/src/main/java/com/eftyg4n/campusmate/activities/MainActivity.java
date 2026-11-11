package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ActivityMainBinding;
import com.eftyg4n.campusmate.firebase.SyncManager;
import com.eftyg4n.campusmate.fragments.CoursesFragment;
import com.eftyg4n.campusmate.fragments.HomeFragment;
import com.eftyg4n.campusmate.fragments.ProfileFragment;
import com.eftyg4n.campusmate.fragments.ResourcesFragment;
import com.eftyg4n.campusmate.fragments.ScheduleFragment;
import com.eftyg4n.campusmate.utils.AppLock;

public class MainActivity extends AppCompatActivity {

    // Set by the welcome screen after sign in: open the Profile tab so the student fills it in.
    public static final String EXTRA_FIRST_SETUP = "extra_first_setup";

    private static final String TAG = "CampusMate_Lifecycle";

    private ActivityMainBinding binding;
    private boolean promptShowing;   // the lock prompt is on screen right now

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity onCreate");

        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.buttonUnlock.setOnClickListener(view -> askToUnlock());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showFragment(new HomeFragment());
            } else if (id == R.id.nav_courses) {
                showFragment(new CoursesFragment());
            } else if (id == R.id.nav_schedule) {
                showFragment(new ScheduleFragment());
            } else if (id == R.id.nav_resources) {
                showFragment(new ResourcesFragment());
            } else if (id == R.id.nav_profile) {
                showFragment(new ProfileFragment());
            } else {
                return false;
            }
            return true;
        });

        // Signed in: fetch changes made to the account from another phone (profile included).
        if (savedInstanceState == null && SyncManager.currentUser() != null) {
            SyncManager.syncNow(getApplicationContext(), null);
        }

        if (savedInstanceState == null) {
            boolean firstSetup = getIntent().getBooleanExtra(EXTRA_FIRST_SETUP, false);
            binding.bottomNav.setSelectedItemId(firstSetup ? R.id.nav_profile : R.id.nav_home);
            if (firstSetup) {
                Toast.makeText(this, R.string.profile_setup_hint, Toast.LENGTH_LONG).show();
            }
        }
    }

    // Lets a fragment (for example the Home cards) jump to another tab.
    public void openTab(int menuId) {
        binding.bottomNav.setSelectedItemId(menuId);
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.cm_fade_in, R.anim.cm_fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity onStart");
        // Cover the dashboard BEFORE the first frame is drawn, then ask for the fingerprint.
        if (!promptShowing && AppLock.shouldLockNow(this)) {
            binding.lockCover.setVisibility(View.VISIBLE);
            askToUnlock();
        }
    }

    private void askToUnlock() {
        if (promptShowing) {
            return;
        }
        promptShowing = true;
        new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        promptShowing = false;
                        AppLock.markUnlocked();
                        binding.lockCover.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence message) {
                        // Cancelled or failed: the cover stays, the Unlock button tries again.
                        promptShowing = false;
                    }
                }).authenticate(AppLock.promptInfo(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity onStop");
        if (!isChangingConfigurations()) {
            AppLock.markStopped();   // starts the "come back within 30 s" timer
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "MainActivity onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity onDestroy");
    }
}