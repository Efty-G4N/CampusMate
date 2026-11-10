package com.eftyg4n.campusmate.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.AttendanceAdapter;
import com.eftyg4n.campusmate.databinding.ActivityAttendanceBinding;
import com.eftyg4n.campusmate.databinding.DialogRequiredPercentBinding;
import com.eftyg4n.campusmate.models.Attendance;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.repositories.AttendanceRepository;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.utils.AttendanceUtils;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

// Attendance of ONE course: today's quick buttons, any other date, the history list,
// and a percentage with "how many classes can I still miss".
public class AttendanceActivity extends AppCompatActivity implements AttendanceAdapter.Listener {

    public static final String EXTRA_COURSE_ID = "extra_course_id";

    private static final String PREFS = "campusmate_prefs";
    private static final String KEY_REQUIRED = "attendance_required_percent";

    private static final String[] STATUSES = {
            Attendance.STATUS_PRESENT, Attendance.STATUS_LATE, Attendance.STATUS_ABSENT
    };

    private ActivityAttendanceBinding binding;
    private AttendanceRepository repository;
    private AttendanceAdapter adapter;
    private String courseId;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttendanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.toolbar.inflateMenu(R.menu.attendance_menu);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_required_percent) {
                showRequiredDialog();
                return true;
            }
            return false;
        });

        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        Course course = courseId == null ? null : new CourseRepository(this).getById(courseId);
        if (course == null) {
            Toast.makeText(this, R.string.course_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.textCourse.setText(getString(R.string.attendance_course_line,
                course.getCode(), course.getTitle()));

        repository = new AttendanceRepository(this);
        adapter = new AttendanceAdapter(this, this);
        binding.recyclerAttendance.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAttendance.setAdapter(adapter);

        binding.buttonPresentToday.setOnClickListener(view ->
                save(startOfDay(System.currentTimeMillis()), Attendance.STATUS_PRESENT, null));
        binding.buttonAbsentToday.setOnClickListener(view ->
                save(startOfDay(System.currentTimeMillis()), Attendance.STATUS_ABSENT, null));
        binding.buttonAddOther.setOnClickListener(view -> pickOtherDate());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null) {
            refresh();
        }
    }

    // ---------- Showing the numbers ----------

    private void refresh() {
        List<Attendance> records = repository.getByCourse(courseId);
        adapter.setData(records);
        binding.textEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);

        AttendanceRepository.Summary summary = repository.getSummary(courseId);
        int required = getRequiredPercent();
        double percent = AttendanceUtils.percent(summary.attended, summary.total);
        boolean onTrack = AttendanceUtils.isOnTrack(summary.attended, summary.total, required);

        binding.textPercent.setText(getString(R.string.attendance_percent,
                String.format(Locale.getDefault(), "%.1f", percent)));
        binding.textCounts.setText(getString(R.string.attendance_counts,
                summary.attended, summary.total, summary.absent(), required));
        binding.textAdvice.setText(adviceFor(summary, required, onTrack));

        int color = ContextCompat.getColor(this,
                onTrack ? R.color.cm_secondary : R.color.cm_error);
        binding.textPercent.setTextColor(color);
        binding.progress.setIndicatorColor(color);
        binding.progress.setProgressCompat((int) Math.round(percent), true);
    }

    private String adviceFor(AttendanceRepository.Summary s, int required, boolean onTrack) {
        if (s.total == 0) {
            return getString(R.string.attendance_advice_none);
        }
        if (onTrack) {
            int canMiss = AttendanceUtils.canMiss(s.attended, s.total, required);
            if (canMiss == 0) {
                return getString(R.string.attendance_advice_edge, required);
            }
            return getResources().getQuantityString(R.plurals.attendance_advice_can_miss,
                    canMiss, canMiss, required);
        }
        int need = AttendanceUtils.needToAttend(s.attended, s.total, required);
        if (need < 0) {
            return getString(R.string.attendance_advice_impossible);
        }
        return getResources().getQuantityString(R.plurals.attendance_advice_need,
                need, need, required);
    }

    // ---------- Saving ----------

    private void save(long classDate, String status, String note) {
        if (repository.upsert(courseId, classDate, status, note)) {
            Toast.makeText(this, R.string.attendance_saved, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.attendance_save_failed, Toast.LENGTH_SHORT).show();
        }
        refresh();
    }

    private void pickOtherDate() {
        // Only days up to today make sense for attendance.
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.attendance_pick_date)
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection ->
                chooseStatus(utcPickToLocalMidnight(selection)));
        picker.show(getSupportFragmentManager(), "attendance_date");
    }

    private void chooseStatus(long classDate) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.attendance_pick_status,
                        dateFormat.format(new Date(classDate))))
                .setItems(statusLabels(false), (dialog, which) ->
                        save(classDate, STATUSES[which], null))
                .show();
    }

    // Tap on a history row: change its status or remove it.
    @Override
    public void onRecordClick(Attendance record) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(dateFormat.format(new Date(record.getClassDate())))
                .setItems(statusLabels(true), (dialog, which) -> {
                    if (which < STATUSES.length) {
                        save(record.getClassDate(), STATUSES[which], record.getNote());
                    } else {
                        repository.delete(record.getId());
                        Toast.makeText(this, R.string.attendance_deleted,
                                Toast.LENGTH_SHORT).show();
                        refresh();
                    }
                })
                .show();
    }

    private String[] statusLabels(boolean withDelete) {
        String[] labels = {
                getString(R.string.attendance_status_present),
                getString(R.string.attendance_status_late),
                getString(R.string.attendance_status_absent),
                getString(R.string.attendance_delete_record)
        };
        return withDelete ? labels : java.util.Arrays.copyOf(labels, STATUSES.length);
    }

    // ---------- Required percentage (a setting, not a fixed rule) ----------

    private int getRequiredPercent() {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_REQUIRED, AttendanceUtils.DEFAULT_REQUIRED_PERCENT);
    }

    private void showRequiredDialog() {
        DialogRequiredPercentBinding dialogBinding =
                DialogRequiredPercentBinding.inflate(getLayoutInflater());
        dialogBinding.editRequired.setText(String.valueOf(getRequiredPercent()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.attendance_required_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.attendance_save, null)
                .setNegativeButton(R.string.attendance_cancel, null)
                .create();
        // Set the click listener after show() so a wrong number keeps the dialog open.
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    int value = parsePercent(dialogBinding.editRequired.getText());
                    if (value < 1 || value > 100) {
                        dialogBinding.layoutRequired.setError(
                                getString(R.string.attendance_required_error));
                        return;
                    }
                    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putInt(KEY_REQUIRED, value).apply();
                    dialog.dismiss();
                    refresh();
                }));
        dialog.show();
    }

    private int parsePercent(CharSequence text) {
        try {
            return Integer.parseInt(text == null ? "" : text.toString().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ---------- Date helpers ----------

    private long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    // MaterialDatePicker returns UTC midnight; we store LOCAL midnight of the same day.
    private long utcPickToLocalMidnight(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH));
        return local.getTimeInMillis();
    }
}
