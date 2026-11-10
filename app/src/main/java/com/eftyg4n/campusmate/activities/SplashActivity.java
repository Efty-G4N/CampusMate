package com.eftyg4n.campusmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.eftyg4n.campusmate.databinding.ActivitySplashBinding;
import com.eftyg4n.campusmate.firebase.SyncManager;
import com.eftyg4n.campusmate.repositories.UserProfileRepository;
import com.eftyg4n.campusmate.utils.AppPrefs;

// The first screen: a short logo animation, then it decides where the student goes.
// New student -> Welcome. Someone who already signed in, or already has a profile -> Home.
public class SplashActivity extends AppCompatActivity {

    private static final long SHOW_MS = 1400;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable goNext = this::goNext;
    private boolean moved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imageLogo.setScaleX(0.6f);
        binding.imageLogo.setScaleY(0.6f);
        binding.imageLogo.setAlpha(0f);
        binding.imageLogo.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(650).setInterpolator(new OvershootInterpolator(1.2f)).start();

        float rise = 28 * getResources().getDisplayMetrics().density;
        binding.textName.setAlpha(0f);
        binding.textName.setTranslationY(rise);
        binding.textName.animate().alpha(1f).translationY(0f)
                .setStartDelay(300).setDuration(450).start();

        binding.textTagline.setAlpha(0f);
        binding.textTagline.setTranslationY(rise);
        binding.textTagline.animate().alpha(1f).translationY(0f)
                .setStartDelay(520).setDuration(450).start();
    }

    // Timer runs only while the screen is visible, so we never start a screen from the background.
    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(goNext, SHOW_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(goNext);
    }

    private void goNext() {
        if (moved || isFinishing()) {
            return;
        }
        moved = true;

        boolean knownStudent = AppPrefs.isOnboarded(this)
                || SyncManager.currentUser() != null
                || new UserProfileRepository(this).getProfile() != null;
        Intent next = new Intent(this,
                knownStudent ? MainActivity.class : WelcomeActivity.class);

        // A soft cross-fade from the splash, instead of the slide used between normal screens.
        startActivity(next, ActivityOptionsCompat
                .makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
                .toBundle());
        finish();
    }
}
