package com.eftyg4n.campusmate.fragments;

import android.text.format.DateUtils;
import com.eftyg4n.campusmate.activities.DeadlinesActivity;
import com.eftyg4n.campusmate.models.Deadline;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.repositories.DeadlineRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import java.util.List;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.activities.WelcomeActivity;
import com.eftyg4n.campusmate.databinding.FragmentHomeBinding;
import com.eftyg4n.campusmate.receivers.SystemStatusReceiver;
import com.eftyg4n.campusmate.utils.SystemStatusUtils;

public class HomeFragment extends Fragment {

    private static final String KEY_CLICK_COUNT = "key_click_count";

    private FragmentHomeBinding binding;
    private int clickCount = 0;

    private SystemStatusReceiver statusReceiver;
    private boolean receiverRegistered = false;

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
        binding.buttonOpenDeadlines.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), DeadlinesActivity.class)));

        // The receiver tells us when the system changes; we update the card and show a message.
        statusReceiver = new SystemStatusReceiver(new SystemStatusReceiver.Listener() {
            @Override
            public void onAirplaneModeChanged(boolean enabled) {
                refreshSystemStatus();
                showToast(enabled ? R.string.system_toast_airplane_on
                        : R.string.system_toast_airplane_off);
            }

            @Override
            public void onPowerSaveChanged(boolean enabled) {
                refreshSystemStatus();
                showToast(enabled ? R.string.system_toast_power_on
                        : R.string.system_toast_power_off);
            }
        });

        return binding.getRoot();
    }
    // Refresh the deadline card every time the screen comes back (after adding one, etc.).
    @Override
    public void onResume() {
        super.onResume();
        refreshDeadlineCard();
    }

    private void refreshDeadlineCard() {
        if (binding == null) {
            return;
        }
        Semester current = new SemesterRepository(requireContext()).getCurrent();
        if (current == null) {
            binding.textDeadlineTitle.setText(R.string.deadline_card_title);
            binding.textDeadlineList.setText(R.string.home_no_semester);
            return;
        }

        DeadlineRepository repo = new DeadlineRepository(requireContext());
        int pending = repo.countPending(current.getId());
        List<Deadline> next = repo.getUpcoming(current.getId(), 3);

        binding.textDeadlineTitle.setText(getString(R.string.deadline_card_title)
                + " · " + getString(R.string.deadline_card_summary, pending));

        if (next.isEmpty()) {
            binding.textDeadlineList.setText(R.string.deadline_card_none);
            return;
        }
        StringBuilder lines = new StringBuilder();
        for (Deadline d : next) {
            if (lines.length() > 0) {
                lines.append("\n");
            }
            String due = DateUtils.formatDateTime(requireContext(), d.getDueAt(),
                    DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE
                            | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL);
            lines.append(getString(R.string.deadline_card_line, d.getTitle(), due));
        }
        binding.textDeadlineList.setText(lines.toString());
    }

    // Start listening only while this screen is visible.
    @Override
    public void onStart() {
        super.onStart();
        // Events that happened while we were not listening are missed,
        // so always read the current state first.
        refreshSystemStatus();

        if (!receiverRegistered) {
            ContextCompat.registerReceiver(requireContext(), statusReceiver,
                    SystemStatusReceiver.createFilter(), ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
    }

    // Stop listening when the screen is not visible (avoids leaks and wasted work).
    @Override
    public void onStop() {
        super.onStop();
        if (receiverRegistered) {
            requireContext().unregisterReceiver(statusReceiver);
            receiverRegistered = false;
        }
    }

    private void refreshSystemStatus() {
        if (binding == null) {
            return;
        }

        boolean airplane = SystemStatusUtils.isAirplaneModeOn(requireContext());
        boolean powerSave = SystemStatusUtils.isPowerSaveOn(requireContext());

        binding.textAirplane.setText(getString(R.string.system_airplane, stateLabel(airplane)));
        binding.textPowerSave.setText(getString(R.string.system_power_save, stateLabel(powerSave)));

        StringBuilder hint = new StringBuilder();
        if (airplane) {
            hint.append(getString(R.string.system_hint_airplane));
        }
        if (powerSave) {
            if (hint.length() > 0) {
                hint.append("\n");
            }
            hint.append(getString(R.string.system_hint_power_save));
        }

        if (hint.length() == 0) {
            binding.textStatusHint.setVisibility(View.GONE);
        } else {
            binding.textStatusHint.setText(hint.toString());
            binding.textStatusHint.setVisibility(View.VISIBLE);
        }
    }

    private String stateLabel(boolean on) {
        return getString(on ? R.string.system_state_on : R.string.system_state_off);
    }

    private void showToast(int messageId) {
        if (isAdded()) {
            Toast.makeText(requireContext(), messageId, Toast.LENGTH_SHORT).show();
        }
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