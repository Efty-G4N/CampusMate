package com.eftyg4n.campusmate.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.ScheduleAdapter;
import com.eftyg4n.campusmate.databinding.FragmentScheduleBinding;
import com.eftyg4n.campusmate.models.Routine;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.models.UserProfile;
import com.eftyg4n.campusmate.repositories.RoutineRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.repositories.UserProfileRepository;
import com.eftyg4n.campusmate.utils.ScheduleUtils;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class ScheduleFragment extends Fragment {

    private static final String KEY_WEEKLY = "key_weekly";
    private static final String KEY_DAY = "key_day";
    private static final int WORKING_DAYS = 5; // Sunday..Thursday = tabs 0..4

    private FragmentScheduleBinding binding;
    private ScheduleAdapter adapter;
    private boolean weeklyMode = false;
    private int selectedDay = 0;
    private List<Routine> weekRoutines = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScheduleBinding.inflate(inflater, container, false);

        if (savedInstanceState != null) {
            weeklyMode = savedInstanceState.getBoolean(KEY_WEEKLY, false);
            selectedDay = savedInstanceState.getInt(KEY_DAY, 0);
        } else {
            // Start on today; Friday and Saturday have no classes, so use Sunday.
            int today = ScheduleUtils.todayIndex();
            selectedDay = today < WORKING_DAYS ? today : 0;
        }

        adapter = new ScheduleAdapter(requireContext());
        binding.recyclerSchedule.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSchedule.setAdapter(adapter);

        String[] shortDays = getResources().getStringArray(R.array.schedule_days_short);
        for (int i = 0; i < WORKING_DAYS; i++) {
            binding.tabDays.addTab(binding.tabDays.newTab().setText(shortDays[i]));
        }
        TabLayout.Tab tab = binding.tabDays.getTabAt(selectedDay);
        if (tab != null) {
            tab.select();
        }
        binding.tabDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedDay = tab.getPosition();
                refreshList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        binding.toggleView.check(weeklyMode ? R.id.buttonWeekly : R.id.buttonDaily);
        binding.toggleView.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                weeklyMode = (checkedId == R.id.buttonWeekly);
                refreshList();
            }
        });

        return binding.getRoot();
    }

    // Reload every time the screen becomes visible (the profile may have changed).
    @Override
    public void onResume() {
        super.onResume();
        loadRoutine();
    }

    private void loadRoutine() {
        if (binding == null) {
            return;
        }

        weekRoutines = new ArrayList<>();
        Semester current = new SemesterRepository(requireContext()).getCurrent();
        UserProfile profile = new UserProfileRepository(requireContext()).getProfile();

        if (current == null || profile == null
                || profile.getSection() == null || profile.getSection().isEmpty()) {
            binding.textHeader.setText("");
            showMessage(getString(R.string.schedule_no_profile));
            return;
        }

        String section = profile.getSection();
        String labGroup = profile.getLabGroup();
        if (labGroup == null || labGroup.isEmpty()) {
            binding.textHeader.setText(getString(R.string.schedule_header_section, section));
        } else {
            binding.textHeader.setText(
                    getString(R.string.schedule_header_group, section, labGroup));
        }

        weekRoutines = new RoutineRepository(requireContext())
                .getForClass(current.getId(), section, labGroup);

        if (weekRoutines.isEmpty()) {
            showMessage(getString(R.string.schedule_no_routine, section));
            return;
        }

        updateNextClass();
        refreshList();
    }

    // Shows a full-screen message and hides everything else.
    private void showMessage(String message) {
        binding.cardNext.setVisibility(View.GONE);
        binding.toggleView.setVisibility(View.GONE);
        binding.tabDays.setVisibility(View.GONE);
        binding.recyclerSchedule.setVisibility(View.GONE);
        binding.textEmpty.setText(message);
        binding.textEmpty.setVisibility(View.VISIBLE);
    }

    private void refreshList() {
        if (binding == null || weekRoutines.isEmpty()) {
            return;
        }

        binding.toggleView.setVisibility(View.VISIBLE);
        binding.tabDays.setVisibility(weeklyMode ? View.GONE : View.VISIBLE);

        List<Routine> shown = new ArrayList<>();
        if (weeklyMode) {
            shown.addAll(weekRoutines);
        } else {
            for (int i = 0; i < weekRoutines.size(); i++) {
                if (weekRoutines.get(i).getDayOfWeek() == selectedDay) {
                    shown.add(weekRoutines.get(i));
                }
            }
        }

        adapter.setData(shown, weeklyMode);

        if (shown.isEmpty()) {
            binding.recyclerSchedule.setVisibility(View.GONE);
            binding.textEmpty.setText(R.string.schedule_empty_day);
            binding.textEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerSchedule.setVisibility(View.VISIBLE);
            binding.textEmpty.setVisibility(View.GONE);
        }
    }

    private void updateNextClass() {
        int today = ScheduleUtils.todayIndex();
        int now = ScheduleUtils.nowMinute();
        Routine next = ScheduleUtils.findNextClass(weekRoutines, today, now);

        if (next == null) {
            binding.cardNext.setVisibility(View.GONE);
            return;
        }

        binding.cardNext.setVisibility(View.VISIBLE);
        binding.textNextLabel.setText(getString(R.string.schedule_next_label,
                whenLabel(next, today, now)));
        binding.textNextCourse.setText(getString(R.string.schedule_next_course,
                next.getCourseCode(), next.getCourseTitle()));
        binding.textNextTime.setText(getString(R.string.schedule_time_range,
                ScheduleUtils.formatMinute(requireContext(), next.getStartMinute()),
                ScheduleUtils.formatMinute(requireContext(), next.getEndMinute())));

        if (next.getRoom() == null || next.getRoom().isEmpty()) {
            binding.textNextRoom.setVisibility(View.GONE);
        } else {
            binding.textNextRoom.setText(getString(R.string.schedule_room, next.getRoom()));
            binding.textNextRoom.setVisibility(View.VISIBLE);
        }

        if (next.getTeacherCodes() == null || next.getTeacherCodes().isEmpty()) {
            binding.textNextTeachers.setVisibility(View.GONE);
        } else {
            binding.textNextTeachers.setText(getString(R.string.schedule_teachers,
                    ScheduleUtils.formatTeachers(requireContext(), next.getTeacherCodes())));
            binding.textNextTeachers.setVisibility(View.VISIBLE);
        }
    }

    // "Now", "Today", "Tomorrow" or the weekday name (also for next week's class).
    private String whenLabel(Routine routine, int today, int now) {
        int day = routine.getDayOfWeek();
        if (day == today && routine.getEndMinute() > now) {
            if (routine.getStartMinute() <= now) {
                return getString(R.string.schedule_when_now);
            }
            return getString(R.string.schedule_when_today);
        }
        if (day == today + 1) {
            return getString(R.string.schedule_when_tomorrow);
        }
        return ScheduleUtils.dayName(requireContext(), day);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WEEKLY, weeklyMode);
        outState.putInt(KEY_DAY, selectedDay);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}