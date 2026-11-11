package com.eftyg4n.campusmate.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.eftyg4n.campusmate.utils.AccessGate;
import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.activities.AddEditDeadlineActivity;
import com.eftyg4n.campusmate.activities.AttendanceActivity;
import com.eftyg4n.campusmate.activities.DeadlinesActivity;
import com.eftyg4n.campusmate.activities.MainActivity;
import com.eftyg4n.campusmate.activities.NoticesActivity;
import com.eftyg4n.campusmate.databinding.FragmentHomeBinding;
import com.eftyg4n.campusmate.firebase.SyncManager;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Deadline;
import com.eftyg4n.campusmate.models.Routine;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.models.UserProfile;
import com.eftyg4n.campusmate.receivers.SystemStatusReceiver;
import com.eftyg4n.campusmate.repositories.AttendanceRepository;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.DeadlineRepository;
import com.eftyg4n.campusmate.repositories.RoutineRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.repositories.UserProfileRepository;
import com.eftyg4n.campusmate.utils.AnimUtils;
import com.eftyg4n.campusmate.utils.AppPrefs;
import com.eftyg4n.campusmate.utils.AttendanceUtils;
import com.eftyg4n.campusmate.utils.ScheduleUtils;
import com.eftyg4n.campusmate.utils.SystemStatusUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// The dashboard. Everything here is a SUMMARY with a tap that leads to the real screen.
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private SystemStatusReceiver statusReceiver;
    private boolean receiverRegistered = false;

    // The course shown on the attendance card, so a tap opens exactly that course.
    private String attendanceCourseId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.cardNextClass.setOnClickListener(view -> openTab(R.id.nav_schedule));
        binding.cardAttendance.setOnClickListener(view -> {
            if (!AccessGate.require(requireContext(), R.string.gate_attendance)) {
                return;
            }
            if (attendanceCourseId == null) {
                openTab(R.id.nav_courses);
                return;
            }
            Intent intent = new Intent(requireContext(), AttendanceActivity.class);
            intent.putExtra(AttendanceActivity.EXTRA_COURSE_ID, attendanceCourseId);
            startActivity(intent);
        });
        binding.buttonOpenDeadlines.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), DeadlinesActivity.class)));

        binding.buttonActionDeadline.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), AddEditDeadlineActivity.class)));
        binding.buttonActionCourses.setOnClickListener(view -> openTab(R.id.nav_courses));
        binding.buttonActionResources.setOnClickListener(view -> openTab(R.id.nav_resources));
        binding.buttonActionNotices.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), NoticesActivity.class)));
        binding.buttonFinishProfile.setOnClickListener(view -> openTab(R.id.nav_profile));

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

        // A profile arriving from the cloud (sign in) updates the greeting by itself.
        SyncManager.profileChanged.observe(getViewLifecycleOwner(), version -> {
            if (binding != null) {
                refreshHome();
            }
        });

        // Only on a fresh screen (not after a rotation): cards rise in one after another.
        if (savedInstanceState == null) {
            AnimUtils.staggerIn(binding.cardHeader, binding.cardNextClass,
                    binding.cardAttendance, binding.cardDeadlines, binding.textActionsTitle,
                    binding.rowActions1, binding.rowActions2, binding.cardSystemStatus);
        }
        return binding.getRoot();
    }

    // Everything is read again each time the screen comes back (new deadline, new profile...).
    @Override
    public void onResume() {
        super.onResume();
        refreshHome();
    }

    private void openTab(int menuId) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openTab(menuId);
        }
    }

    private void refreshHome() {
        if (binding == null) {
            return;
        }
        Context context = requireContext();
        UserProfile profile = new UserProfileRepository(context).getProfile();
        Semester current = new SemesterRepository(context).getCurrent();

        showHeader(profile);
        binding.cardProfileNudge.setVisibility(profile == null ? View.VISIBLE : View.GONE);
        showNextClass(profile, current);
        showAttendance(current);
        showDeadlines(current);
    }

    // ---------- Header ----------

    private void showHeader(UserProfile profile) {
        Context context = requireContext();

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = getString(hour < 12 ? R.string.home_greet_morning
                : hour < 17 ? R.string.home_greet_afternoon : R.string.home_greet_evening);
        String first = firstName(profile);
        binding.textGreeting.setText(first == null ? greeting
                : getString(R.string.home_greeting_named, greeting, first));

        binding.textDate.setText(DateUtils.formatDateTime(context, System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_YEAR));

        StringBuilder info = new StringBuilder();
        if (profile != null && hasText(profile.getSection())) {
            info.append(getString(R.string.home_class_info_section, profile.getSection()));
        }
        if (profile != null && hasText(profile.getLabGroup())) {
            if (info.length() > 0) {
                info.append("  ·  ");
            }
            info.append(getString(R.string.home_class_info_lab, profile.getLabGroup()));
        }
        binding.textClassInfo.setText(info);
        binding.textClassInfo.setVisibility(info.length() == 0 ? View.GONE : View.VISIBLE);
    }

    // "Md. Asif Zaman" -> "Asif": a leading Md / Mohammad is not what people are called.
    private String firstName(UserProfile profile) {
        if (profile == null || !hasText(profile.getName())) {
            return null;
        }
        String[] parts = profile.getName().trim().split("\\s+");
        for (String part : parts) {
            if (!part.matches("(?i)(md\\.?|mohammad|muhammad|mohd\\.?)")) {
                return part;
            }
        }
        return parts[0];
    }

    // ---------- Next class ----------

    private void showNextClass(UserProfile profile, Semester current) {
        Context context = requireContext();
        binding.textNextTitle.setVisibility(View.GONE);
        binding.textNextTime.setVisibility(View.GONE);
        binding.textNextWhen.setVisibility(View.GONE);

        if (profile == null || current == null || !hasText(profile.getSection())) {
            binding.textNextCourse.setText(R.string.home_next_need_profile);
            return;
        }

        List<Routine> week = new RoutineRepository(context).getForClass(
                current.getId(), profile.getSection(), profile.getLabGroup());
        int today = ScheduleUtils.todayIndex();
        int now = ScheduleUtils.nowMinute();
        Routine next = ScheduleUtils.findNextClass(week, today, now);
        if (next == null) {
            binding.textNextCourse.setText(R.string.home_next_none);
            return;
        }

        binding.textNextCourse.setText(next.getCourseCode());

        String title = next.getCourseTitle() == null ? "" : next.getCourseTitle();
        if (hasText(next.getTeacherCodes())) {
            title = title + "  ·  " + ScheduleUtils.formatTeachers(context, next.getTeacherCodes());
        }
        binding.textNextTitle.setText(title);
        binding.textNextTitle.setVisibility(title.isEmpty() ? View.GONE : View.VISIBLE);

        String time = ScheduleUtils.formatMinute(context, next.getStartMinute()) + " – "
                + ScheduleUtils.formatMinute(context, next.getEndMinute());
        if (hasText(next.getRoom())) {
            time = time + "  ·  " + next.getRoom();
        }
        binding.textNextTime.setText(time);
        binding.textNextTime.setVisibility(View.VISIBLE);

        binding.textNextWhen.setText(whenText(next, today, now));
        binding.textNextWhen.setVisibility(View.VISIBLE);
    }

    private String whenText(Routine next, int today, int now) {
        int day = next.getDayOfWeek();
        if (day == today) {
            if (next.getStartMinute() <= now && now < next.getEndMinute()) {
                return getString(R.string.home_next_running);
            }
            if (next.getStartMinute() > now) {
                int diff = next.getStartMinute() - now;
                return diff < 60 ? getString(R.string.home_next_in_minutes, diff)
                        : getString(R.string.home_next_today);
            }
            // Today's classes are over: this is the same weekday NEXT week.
            return getString(R.string.home_next_week_day,
                    ScheduleUtils.dayName(requireContext(), day));
        }
        if (day == (today + 1) % 7) {
            return getString(R.string.home_next_tomorrow);
        }
        return ScheduleUtils.dayName(requireContext(), day);
    }

    // ---------- Attendance watch: the course that needs attention first ----------

    private void showAttendance(Semester current) {
        Context context = requireContext();
        attendanceCourseId = null;
        binding.progressAttendance.setVisibility(View.GONE);

        // Attendance is personal: a guest only sees a lock message.
        if (!AccessGate.isSignedIn()) {
            binding.textAttendanceLine.setText(R.string.home_attendance_locked);
            binding.textAttendanceHint.setText("");
            return;
        }

        if (current == null) {
            binding.textAttendanceLine.setText(R.string.home_no_semester);
            binding.textAttendanceHint.setText("");
            return;
        }

        AttendanceRepository repo = new AttendanceRepository(context);
        Course worst = null;
        AttendanceRepository.Summary worstSummary = null;
        double worstPercent = 101;
        for (Course course : new CourseRepository(context).getBySemester(current.getId())) {
            AttendanceRepository.Summary summary = repo.getSummary(course.getId());
            if (summary.total == 0) {
                continue;
            }
            double percent = AttendanceUtils.percent(summary.attended, summary.total);
            if (percent < worstPercent) {
                worstPercent = percent;
                worst = course;
                worstSummary = summary;
            }
        }

        if (worst == null) {
            binding.textAttendanceLine.setText(R.string.home_attendance_none);
            binding.textAttendanceHint.setText("");
            return;
        }

        attendanceCourseId = worst.getId();
        int required = AppPrefs.requiredAttendance(context);
        boolean onTrack = AttendanceUtils.isOnTrack(worstSummary.attended, worstSummary.total,
                required);

        binding.textAttendanceLine.setText(getString(R.string.home_attendance_line,
                worst.getCode(), String.format(Locale.getDefault(), "%.1f", worstPercent)));
        binding.textAttendanceHint.setText(getString(onTrack ? R.string.home_attendance_ok
                : R.string.home_attendance_low, required));

        int color = ContextCompat.getColor(context,
                onTrack ? R.color.cm_secondary : R.color.cm_error);
        binding.progressAttendance.setIndicatorColor(color);
        binding.progressAttendance.setVisibility(View.VISIBLE);
        binding.progressAttendance.setProgressCompat((int) Math.round(worstPercent), true);
    }

    // ---------- Deadlines ----------

    private void showDeadlines(Semester current) {
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

    // ---------- System status (BroadcastReceiver, Lecture 10) ----------

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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
