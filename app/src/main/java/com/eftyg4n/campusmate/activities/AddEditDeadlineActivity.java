package com.eftyg4n.campusmate.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ActivityAddEditDeadlineBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Deadline;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.notifications.ReminderScheduler;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.DeadlineRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class AddEditDeadlineActivity extends AppCompatActivity {

    // No deadline id = add mode. With a deadline id = edit mode.
    public static final String EXTRA_DEADLINE_ID = "extra_deadline_id";
    // Optional in add mode: pre-select this course.
    public static final String EXTRA_COURSE_ID = "extra_course_id";

    private static final String KEY_DUE_AT = "key_due_at";
    private static final String KEY_COURSE_INDEX = "key_course_index";
    private static final String KEY_TYPE_INDEX = "key_type_index";
    private static final String KEY_PRIORITY = "key_priority";

    // "Remind me ... before": minutes. Order = the order of the chips in the layout.
    private static final int[] REMINDER_OPTIONS = {0, 10, 60, 1440};

    private ActivityAddEditDeadlineBinding binding;
    private DeadlineRepository deadlineRepo;
    private Deadline editing;            // null in add mode
    private String semesterId;
    private List<Course> courses = new ArrayList<>();

    // The selected values (kept across rotation in onSaveInstanceState).
    private final Calendar due = Calendar.getInstance();
    private int courseIndex = 0;         // 0 = "No course", 1.. = courses.get(index - 1)
    private int typeIndex = 0;
    private int priority = Deadline.PRIORITY_MEDIUM;

    // Asks for the notification permission, then continues with the save.
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            persist();
                        } else {
                            showPermissionDeniedDialog();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditDeadlineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        deadlineRepo = new DeadlineRepository(this);

        String deadlineId = getIntent().getStringExtra(EXTRA_DEADLINE_ID);
        if (deadlineId != null) {
            editing = deadlineRepo.getById(deadlineId);
            if (editing == null) {
                Toast.makeText(this, R.string.deadline_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            semesterId = editing.getSemesterId();
        } else {
            Semester current = new SemesterRepository(this).getCurrent();
            if (current == null) {
                Toast.makeText(this, R.string.course_form_no_semester,
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            semesterId = current.getId();
        }

        courses = new CourseRepository(this).getBySemester(semesterId);

        // Default due time for a NEW deadline: tomorrow, 11:00 PM.
        due.add(Calendar.DAY_OF_YEAR, 1);
        due.set(Calendar.HOUR_OF_DAY, 23);
        due.set(Calendar.MINUTE, 0);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);

        setupDropdowns();

        if (savedInstanceState != null) {
            due.setTimeInMillis(savedInstanceState.getLong(KEY_DUE_AT));
            courseIndex = savedInstanceState.getInt(KEY_COURSE_INDEX, 0);
            typeIndex = savedInstanceState.getInt(KEY_TYPE_INDEX, 0);
            priority = savedInstanceState.getInt(KEY_PRIORITY, Deadline.PRIORITY_MEDIUM);
        } else if (editing != null) {
            fillForm(editing);
        } else {
            preselectCourseFromIntent();
            binding.chipRemind1h.setChecked(true);   // a sensible default
        }
        showSelections();

        if (editing != null) {
            binding.toolbar.setTitle(R.string.deadline_form_title_edit);
            binding.buttonDelete.setVisibility(View.VISIBLE);
        } else {
            binding.toolbar.setTitle(R.string.deadline_form_title_add);
        }

        binding.buttonPickDate.setOnClickListener(view -> pickDate());
        binding.buttonPickTime.setOnClickListener(view -> pickTime());
        binding.buttonSave.setOnClickListener(view -> save());
        binding.buttonDelete.setOnClickListener(view -> confirmDelete());
    }

    // ---------- Dropdowns ----------

    private void setupDropdowns() {
        // Course: "No course" + every course of the semester.
        List<String> courseLabels = new ArrayList<>();
        courseLabels.add(getString(R.string.deadline_no_course));
        for (Course course : courses) {
            courseLabels.add(course.getCode() + " — " + course.getTitle());
        }
        binding.dropdownCourse.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, courseLabels));
        binding.dropdownCourse.setOnItemClickListener((parent, view, position, id) ->
                courseIndex = position);

        binding.dropdownType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.deadline_type_labels)));
        binding.dropdownType.setOnItemClickListener((parent, view, position, id) ->
                typeIndex = position);

        binding.dropdownPriority.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.deadline_priority_labels)));
        binding.dropdownPriority.setOnItemClickListener((parent, view, position, id) ->
                priority = position);
    }

    // setText(text, false): show the text without filtering the dropdown list.
    private void showSelections() {
        String[] typeLabels = getResources().getStringArray(R.array.deadline_type_labels);
        String[] priorityLabels = getResources().getStringArray(R.array.deadline_priority_labels);

        String courseLabel = getString(R.string.deadline_no_course);
        if (courseIndex > 0 && courseIndex <= courses.size()) {
            Course course = courses.get(courseIndex - 1);
            courseLabel = course.getCode() + " — " + course.getTitle();
        }
        binding.dropdownCourse.setText(courseLabel, false);
        binding.dropdownType.setText(typeLabels[typeIndex], false);
        binding.dropdownPriority.setText(priorityLabels[priority], false);
        showDueButtons();
    }

    private void showDueButtons() {
        binding.buttonPickDate.setText(DateUtils.formatDateTime(this, due.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_YEAR));
        binding.buttonPickTime.setText(DateUtils.formatDateTime(this, due.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_TIME));
    }

    private void preselectCourseFromIntent() {
        String wantedCourseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        if (wantedCourseId == null) {
            return;
        }
        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).getId().equals(wantedCourseId)) {
                courseIndex = i + 1;
            }
        }
    }

    // ---------- Date and time pickers (Material components, no extra library) ----------

    private void pickDate() {
        // MaterialDatePicker works in UTC midnight, so we convert both ways.
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(due.get(Calendar.YEAR), due.get(Calendar.MONTH), due.get(Calendar.DAY_OF_MONTH));

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.deadline_pick_date)
                .setSelection(utc.getTimeInMillis())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar picked = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            picked.setTimeInMillis(selection);
            due.set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH),
                    picked.get(Calendar.DAY_OF_MONTH));
            showDueButtons();
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void pickTime() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(due.get(Calendar.HOUR_OF_DAY))
                .setMinute(due.get(Calendar.MINUTE))
                .setTitleText(R.string.deadline_pick_time)
                .build();
        picker.addOnPositiveButtonClickListener(view -> {
            due.set(Calendar.HOUR_OF_DAY, picker.getHour());
            due.set(Calendar.MINUTE, picker.getMinute());
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
            showDueButtons();
        });
        picker.show(getSupportFragmentManager(), "time_picker");
    }

    // ---------- Form <-> data ----------

    private void fillForm(Deadline d) {
        binding.editTitle.setText(d.getTitle());
        binding.editDescription.setText(d.getDescription());
        due.setTimeInMillis(d.getDueAt());
        typeIndex = Deadline.typeIndex(d.getType());
        priority = d.getPriority();

        courseIndex = 0;
        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).getId().equals(d.getCourseId())) {
                courseIndex = i + 1;
            }
        }

        List<Integer> saved = deadlineRepo.getReminderMinutes(d.getId());
        binding.chipRemindAtDue.setChecked(saved.contains(REMINDER_OPTIONS[0]));
        binding.chipRemind10m.setChecked(saved.contains(REMINDER_OPTIONS[1]));
        binding.chipRemind1h.setChecked(saved.contains(REMINDER_OPTIONS[2]));
        binding.chipRemind1d.setChecked(saved.contains(REMINDER_OPTIONS[3]));
    }

    private List<Integer> selectedReminderMinutes() {
        List<Integer> list = new ArrayList<>();
        if (binding.chipRemindAtDue.isChecked()) list.add(REMINDER_OPTIONS[0]);
        if (binding.chipRemind10m.isChecked()) list.add(REMINDER_OPTIONS[1]);
        if (binding.chipRemind1h.isChecked()) list.add(REMINDER_OPTIONS[2]);
        if (binding.chipRemind1d.isChecked()) list.add(REMINDER_OPTIONS[3]);
        return list;
    }

    private String textOf(TextInputEditText edit) {
        if (edit.getText() == null) {
            return "";
        }
        return edit.getText().toString().trim();
    }

    // ---------- Save (with the runtime permission step) ----------

    private void save() {
        binding.layoutTitle.setError(null);
        if (textOf(binding.editTitle).isEmpty()) {
            binding.layoutTitle.setError(getString(R.string.deadline_form_error_title));
            return;
        }

        // Permission is requested ONLY now, when the student really asks for reminders.
        boolean wantsReminders = !selectedReminderMinutes().isEmpty();
        boolean missingPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;

        if (wantsReminders && missingPermission) {
            explainThenAskPermission();
        } else {
            persist();
        }
    }

    // Explain WHY first, then show the system permission dialog.
    private void explainThenAskPermission() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.perm_notifications_title)
                .setMessage(R.string.perm_notifications_message)
                .setNegativeButton(R.string.perm_not_now, (dialog, which) -> persist())
                .setPositiveButton(R.string.perm_allow, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        persist();
                    }
                })
                .show();
    }

    private void persist() {
        Deadline d = editing;
        if (d == null) {
            d = new Deadline();
            d.setSemesterId(semesterId);
        }

        String description = textOf(binding.editDescription);
        d.setTitle(textOf(binding.editTitle));
        d.setDescription(description.isEmpty() ? null : description);
        d.setCourseId(courseIndex == 0 ? null : courses.get(courseIndex - 1).getId());
        d.setType(Deadline.TYPES[typeIndex]);
        d.setPriority(priority);
        d.setDueAt(due.getTimeInMillis());

        List<Integer> minutes = selectedReminderMinutes();

        boolean saved;
        if (editing == null) {
            saved = deadlineRepo.insert(d, minutes) != null;
        } else {
            saved = deadlineRepo.update(d, minutes);
        }
        if (!saved) {
            Toast.makeText(this, R.string.deadline_form_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        // Replace the old plan with the new one (the due time or reminders may have changed).
        ReminderScheduler.cancel(this, d.getId());
        if (!d.isCompleted()) {
            ReminderScheduler.schedule(this, d, minutes);
        }

        Toast.makeText(this, R.string.deadline_form_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    // The student said no. The deadline is still useful, so we save it either way,
    // but we tell them reminders will not appear and offer the Settings screen.
    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.perm_notifications_denied_title)
                .setMessage(R.string.perm_notifications_denied_message)
                .setNegativeButton(R.string.perm_continue_without, (dialog, which) -> persist())
                .setPositiveButton(R.string.perm_open_settings, (dialog, which) -> {
                    // IMPLICIT intent: the system Settings app handles it.
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                    persist();
                })
                .setCancelable(false)
                .show();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.deadline_delete_title)
                .setMessage(getString(R.string.deadline_delete_message, editing.getTitle()))
                .setNegativeButton(R.string.course_delete_cancel, null)
                .setPositiveButton(R.string.course_delete_confirm, (dialog, which) -> {
                    ReminderScheduler.cancel(this, editing.getId());
                    if (deadlineRepo.delete(editing.getId())) {
                        Toast.makeText(this, R.string.deadline_form_deleted,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.deadline_form_save_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_DUE_AT, due.getTimeInMillis());
        outState.putInt(KEY_COURSE_INDEX, courseIndex);
        outState.putInt(KEY_TYPE_INDEX, typeIndex);
        outState.putInt(KEY_PRIORITY, priority);
    }
}
