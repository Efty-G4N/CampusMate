package com.eftyg4n.campusmate.fragments;
import android.content.Intent;
import android.text.format.DateUtils;
import com.eftyg4n.campusmate.activities.AuthActivity;
import com.eftyg4n.campusmate.activities.MainActivity;
import com.eftyg4n.campusmate.activities.SemestersActivity;
import com.eftyg4n.campusmate.activities.WelcomeActivity;
import com.eftyg4n.campusmate.utils.AppLock;
import com.eftyg4n.campusmate.utils.AppPrefs;
import android.content.Context;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.eftyg4n.campusmate.activities.ClassGroupsActivity;
import com.eftyg4n.campusmate.firebase.SyncManager;
import com.eftyg4n.campusmate.utils.NetworkUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.FragmentProfileBinding;
import com.eftyg4n.campusmate.models.ClassGroup;
import com.eftyg4n.campusmate.models.Classmate;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.models.UserProfile;
import com.eftyg4n.campusmate.repositories.ClassGroupRepository;
import com.eftyg4n.campusmate.repositories.ClassmateRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.repositories.UserProfileRepository;
import com.eftyg4n.campusmate.utils.RollUtils;
import com.eftyg4n.campusmate.utils.SimpleTextWatcher;

public class ProfileFragment extends Fragment {

    private static final int DEFAULT_SERIAL_DIGITS = 3;

    private FragmentProfileBinding binding;

    private UserProfileRepository profileRepo;
    private ClassGroupRepository groupRepo;
    private ClassmateRepository classmateRepo;
    private SemesterRepository semesterRepo;

    private UserProfile profile; // null until the first save
    private boolean loading = false; // true while we fill the form from the database

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        profileRepo = new UserProfileRepository(requireContext());
        groupRepo = new ClassGroupRepository(requireContext());
        classmateRepo = new ClassmateRepository(requireContext());
        semesterRepo = new SemesterRepository(requireContext());

        SimpleTextWatcher detectWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                detectFromRoll();
            }
        };
        binding.editRoll.addTextChangedListener(detectWatcher);
        binding.editSerialDigits.addTextChangedListener(detectWatcher);

        binding.buttonAdvanced.setOnClickListener(view ->
                showAdvanced(binding.layoutSerialDigits.getVisibility() != View.VISIBLE));
        binding.buttonSave.setOnClickListener(view -> saveProfile());
        binding.buttonClassGroups.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), ClassGroupsActivity.class)));

        binding.buttonSignIn.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), AuthActivity.class)));
        binding.buttonSyncNow.setOnClickListener(view -> syncNow());
        binding.buttonSignOut.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(requireContext(), R.string.account_toast_signed_out,
                    Toast.LENGTH_SHORT).show();
            updateAccountCard();
        });

        setupSettingsCard();
        loadProfile();
        return binding.getRoot();
    }

    // ---------- App settings: lock, semesters, welcome preview ----------

    private boolean updatingLockSwitch;

    private void setupSettingsCard() {
        binding.switchAppLock.setChecked(AppPrefs.isLockEnabled(requireContext()));
        binding.switchAppLock.setOnCheckedChangeListener((button, checked) -> {
            if (!updatingLockSwitch) {
                changeLock(checked);
            }
        });
        binding.buttonSemesters.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), SemestersActivity.class)));
        // A way to see the welcome screen again without clearing the app's data.
        binding.buttonShowWelcome.setOnClickListener(view -> {
            AppPrefs.clearOnboarded(requireContext());
            startActivity(new Intent(requireContext(), WelcomeActivity.class));
        });
    }

    private void setLockSwitch(boolean value) {
        if (binding == null) {
            return;
        }
        updatingLockSwitch = true;
        binding.switchAppLock.setChecked(value);
        updatingLockSwitch = false;
    }

    // Turning the lock on or off both need a successful fingerprint / screen lock check,
    // so a stranger holding an unlocked phone cannot switch it off.
    private void changeLock(boolean enable) {
        Context app = requireContext().getApplicationContext();
        boolean usable = AppLock.canUseLock(app);

        if (enable && !usable) {
            Toast.makeText(app, R.string.lock_unavailable, Toast.LENGTH_LONG).show();
            setLockSwitch(false);
            return;
        }
        if (!enable && !usable) {
            // The phone's screen lock was removed: nothing to check against, just switch off.
            AppPrefs.setLockEnabled(app, false);
            return;
        }

        new BiometricPrompt(this, ContextCompat.getMainExecutor(app),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        AppPrefs.setLockEnabled(app, enable);
                        if (enable) {
                            AppLock.markUnlocked();   // no second prompt right after enabling
                        }
                        Toast.makeText(app, enable ? R.string.lock_toast_on
                                : R.string.lock_toast_off, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence message) {
                        setLockSwitch(!enable);   // cancelled: go back to the old state
                    }
                }).authenticate(AppLock.promptInfo(app));
    }

    // Coming back from AuthActivity: show the new account state.
    @Override
    public void onResume() {
        super.onResume();
        updateAccountCard();
    }

    // ---------- Cloud account ----------

    private void updateAccountCard() {
        if (binding == null) {
            return;
        }
        FirebaseUser user = SyncManager.currentUser();
        boolean signedIn = user != null;

        binding.buttonSignIn.setVisibility(signedIn ? View.GONE : View.VISIBLE);
        binding.buttonSyncNow.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        binding.buttonSignOut.setVisibility(signedIn ? View.VISIBLE : View.GONE);

        if (!signedIn) {
            binding.textAccountStatus.setText(R.string.account_signed_out);
            binding.textLastSync.setVisibility(View.GONE);
            return;
        }
        binding.textAccountStatus.setText(getString(R.string.account_signed_in_as,
                user.getEmail() == null ? "" : user.getEmail()));
        long last = SyncManager.lastSyncAt(requireContext());
        binding.textLastSync.setVisibility(View.VISIBLE);
        binding.textLastSync.setText(last == 0 ? getString(R.string.account_never_synced)
                : getString(R.string.account_last_sync, DateUtils.getRelativeTimeSpanString(
                last, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)));
    }

    private void syncNow() {
        if (!NetworkUtils.isOnline(requireContext())) {
            Toast.makeText(requireContext(), R.string.resource_toast_offline,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressSync.setVisibility(View.VISIBLE);
        binding.buttonSyncNow.setEnabled(false);
        SyncManager.syncNow(requireContext(), new SyncManager.Callback() {
            @Override
            public void onDone(int pushed, int pulled) {
                if (binding == null) {
                    return;
                }
                finishSync();
                Toast.makeText(requireContext(),
                        getString(R.string.account_sync_done, pushed, pulled),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(int reason) {
                if (binding == null) {
                    return;
                }
                finishSync();
                int text = reason == SyncManager.ERROR_NOT_CONFIGURED
                        ? R.string.account_sync_not_configured
                        : reason == SyncManager.ERROR_BUSY ? R.string.account_sync_busy
                        : R.string.account_sync_failed;
                Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void finishSync() {
        binding.progressSync.setVisibility(View.GONE);
        binding.buttonSyncNow.setEnabled(true);
        updateAccountCard();
        // A fresh install may have just received the profile from the cloud.
        // (Only then: reloading would throw away what the student typed but did not save.)
        if (profile == null) {
            loadProfile();
        }
    }

    // Fills the form with the saved profile (or with defaults for a new student).
    private void loadProfile() {
        loading = true;
        profile = profileRepo.getProfile();

        if (profile != null) {
            binding.editRoll.setText(profile.getRollNo());
            binding.editName.setText(profile.getName());
            binding.editUniversity.setText(profile.getUniversity());
            binding.editDepartment.setText(profile.getDepartment());
            binding.editSession.setText(profile.getSession());
            binding.editYear.setText(profile.getYearNo() > 0
                    ? String.valueOf(profile.getYearNo()) : "");
            binding.editTerm.setText(profile.getTermNo() > 0
                    ? String.valueOf(profile.getTermNo()) : "");
            binding.editSection.setText(profile.getSection());
            binding.editLabGroup.setText(profile.getLabGroup());
            binding.editSerialDigits.setText(String.valueOf(profile.getRollSerialDigits()));
            // The advanced field stays hidden unless this student uses a different format.
            showAdvanced(profile.getRollSerialDigits() != DEFAULT_SERIAL_DIGITS);
        } else {
            Semester current = semesterRepo.getCurrent();
            if (current != null) {
                binding.editSession.setText(current.getSession());
                binding.editYear.setText(String.valueOf(current.getYearNo()));
                binding.editTerm.setText(String.valueOf(current.getTermNo()));
            }
            binding.editSerialDigits.setText(String.valueOf(DEFAULT_SERIAL_DIGITS));
            showAdvanced(false);
        }

        loading = false;
        updateHeader();
    }

    private void showAdvanced(boolean show) {
        binding.layoutSerialDigits.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Roll number -> name, section and lab group.
    // Rule: if the typed roll is the SAVED roll of this student, the saved values come back
    // (a teacher may have moved the student to another lab group). For any other roll,
    // the class list and the roll ranges decide.
    private void detectFromRoll() {
        if (loading || binding == null) {
            return;
        }

        String roll = text(binding.editRoll);
        int digits = parseInt(text(binding.editSerialDigits), DEFAULT_SERIAL_DIGITS);
        int serial = RollUtils.serialOf(roll, digits);
        if (serial < 0) {
            binding.textDetectStatus.setVisibility(View.GONE);
            return;
        }

        Classmate classmate = classmateRepo.getByRoll(roll);
        if (classmate != null) {
            binding.textDetectStatus.setText(
                    getString(R.string.profile_detect_found, classmate.getFullName()));
        } else {
            binding.textDetectStatus.setText(R.string.profile_detect_not_found);
        }
        binding.textDetectStatus.setVisibility(View.VISIBLE);

        boolean isSavedRoll = profile != null
                && roll.equals(profile.getRollNo())
                && digits == profile.getRollSerialDigits();

        if (isSavedRoll) {
            binding.editName.setText(profile.getName());
            binding.editSection.setText(profile.getSection());
            binding.editLabGroup.setText(profile.getLabGroup());
            return;
        }

        if (classmate != null) {
            binding.editName.setText(classmate.getFullName());
        }

        Semester current = semesterRepo.getCurrent();
        if (current != null) {
            ClassGroup section = groupRepo.findSection(current.getId(), serial);
            ClassGroup labGroup = groupRepo.findLabGroup(current.getId(), serial);
            binding.editSection.setText(section != null ? section.getName() : "");
            binding.editLabGroup.setText(labGroup != null ? labGroup.getName() : "");
        }
    }

    private void saveProfile() {
        clearErrors();

        String roll = text(binding.editRoll);
        String name = text(binding.editName);
        String university = text(binding.editUniversity);
        String department = text(binding.editDepartment);
        String session = text(binding.editSession);
        String section = text(binding.editSection);
        String labGroup = text(binding.editLabGroup);

        boolean valid = true;

        int digits = parseInt(text(binding.editSerialDigits), -1);
        if (digits < 1 || digits > 6) {
            showAdvanced(true); // the error must be visible
            binding.layoutSerialDigits.setError(getString(R.string.profile_error_digits));
            valid = false;
        }

        if (roll.isEmpty()) {
            binding.layoutRoll.setError(getString(R.string.profile_error_required));
            valid = false;
        } else if (digits >= 1 && digits <= 6 && RollUtils.serialOf(roll, digits) < 0) {
            binding.layoutRoll.setError(getString(R.string.profile_error_roll, digits));
            valid = false;
        }

        if (name.isEmpty()) {
            binding.layoutName.setError(getString(R.string.profile_error_required));
            valid = false;
        }
        if (university.isEmpty()) {
            binding.layoutUniversity.setError(getString(R.string.profile_error_required));
            valid = false;
        }
        if (department.isEmpty()) {
            binding.layoutDepartment.setError(getString(R.string.profile_error_required));
            valid = false;
        }

        int year = parseInt(text(binding.editYear), -1);
        if (year < 1 || year > 8) {
            binding.layoutYear.setError(getString(R.string.profile_error_year));
            valid = false;
        }
        int term = parseInt(text(binding.editTerm), -1);
        if (term < 1 || term > 4) {
            binding.layoutTerm.setError(getString(R.string.profile_error_term));
            valid = false;
        }

        if (!valid) {
            return;
        }

        boolean firstSave = (profile == null);
        UserProfile toSave = (profile != null) ? profile : new UserProfile();
        toSave.setRollNo(roll);
        toSave.setRollSerialDigits(digits);
        toSave.setName(name);
        toSave.setUniversity(university);
        toSave.setDepartment(department);
        toSave.setSession(session.isEmpty() ? null : session);
        toSave.setYearNo(year);
        toSave.setTermNo(term);
        toSave.setSection(section.isEmpty() ? null : section);
        toSave.setLabGroup(labGroup.isEmpty() ? null : labGroup);

        if (profileRepo.save(toSave)) {
            profile = toSave;
            updateHeader();
            Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
            if (firstSave && requireActivity() instanceof MainActivity) {
                // First profile ever: show the student what it unlocked (routine, attendance).
                ((MainActivity) requireActivity()).openTab(R.id.nav_home);
            }
        } else {
            Toast.makeText(requireContext(), R.string.profile_save_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Updates the top part: initials circle, name and "Roll · Section · Lab group".
    private void updateHeader() {
        String name = (profile != null) ? profile.getName() : null;
        if (hasText(name)) {
            binding.textName.setText(name);
            binding.textInitials.setText(initialsOf(name));
        } else {
            binding.textName.setText(R.string.profile_name_placeholder);
            binding.textInitials.setText("?");
        }

        StringBuilder summary = new StringBuilder();
        if (profile != null) {
            if (hasText(profile.getRollNo())) {
                appendPart(summary, getString(R.string.profile_summary_roll, profile.getRollNo()));
            }
            if (hasText(profile.getSection())) {
                appendPart(summary,
                        getString(R.string.profile_summary_section, profile.getSection()));
            }
            if (hasText(profile.getLabGroup())) {
                appendPart(summary,
                        getString(R.string.profile_summary_lab, profile.getLabGroup()));
            }
        }
        binding.textSummary.setText(summary.toString());
    }

    private void clearErrors() {
        binding.layoutRoll.setError(null);
        binding.layoutName.setError(null);
        binding.layoutUniversity.setError(null);
        binding.layoutDepartment.setError(null);
        binding.layoutYear.setError(null);
        binding.layoutTerm.setError(null);
        binding.layoutSerialDigits.setError(null);
    }

    private String text(EditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // First letter of the first word + first letter of the last word.
    private String initialsOf(String name) {
        String[] words = name.trim().split("\\s+");
        String first = words[0].substring(0, 1);
        if (words.length == 1) {
            return first.toUpperCase();
        }
        String last = words[words.length - 1].substring(0, 1);
        return (first + last).toUpperCase();
    }

    private void appendPart(StringBuilder builder, String part) {
        if (builder.length() > 0) {
            builder.append(" · ");
        }
        builder.append(part);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}