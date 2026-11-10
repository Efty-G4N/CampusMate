package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.LectureAdapter;
import com.eftyg4n.campusmate.databinding.ActivityLecturesBinding;
import com.eftyg4n.campusmate.databinding.DialogLectureBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Lecture;
import com.eftyg4n.campusmate.models.SyllabusTopic;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.LectureRepository;
import com.eftyg4n.campusmate.repositories.SyllabusRepository;
import com.eftyg4n.campusmate.utils.DayUtils;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// A diary of what was taught in ONE course. While logging a lecture you can also tick
// the syllabus topics it covered, so the Syllabus progress moves by itself.
public class LecturesActivity extends AppCompatActivity implements LectureAdapter.Listener {

    private ActivityLecturesBinding binding;
    private LectureRepository repository;
    private SyllabusRepository syllabusRepository;
    private LectureAdapter adapter;
    private String courseId;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLecturesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        courseId = getIntent().getStringExtra(CourseDetailActivity.EXTRA_COURSE_ID);
        Course course = courseId == null ? null : new CourseRepository(this).getById(courseId);
        if (course == null) {
            Toast.makeText(this, R.string.course_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.textCourse.setText(getString(R.string.attendance_course_line,
                course.getCode(), course.getTitle()));
        binding.textEmpty.setText(R.string.lectures_empty);

        repository = new LectureRepository(this);
        syllabusRepository = new SyllabusRepository(this);
        adapter = new LectureAdapter(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.buttonAddLecture.setOnClickListener(view -> showLectureDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null) {
            refresh();
        }
    }

    private void refresh() {
        List<Lecture> lectures = repository.getByCourse(courseId);
        adapter.setData(lectures);
        binding.textEmpty.setVisibility(lectures.isEmpty() ? View.VISIBLE : View.GONE);

        binding.textCount.setText(getResources().getQuantityString(
                R.plurals.lectures_count, lectures.size(), lectures.size()));

        List<SyllabusTopic> topics = syllabusRepository.getByCourse(courseId);
        int done = 0;
        for (SyllabusTopic t : topics) {
            if (t.isDone()) {
                done++;
            }
        }
        binding.textCovered.setText(topics.isEmpty()
                ? getString(R.string.lectures_no_syllabus)
                : getString(R.string.lectures_covered, done, topics.size()));
    }

    @Override
    public void onLectureClick(Lecture lecture) {
        showLectureDialog(lecture);
    }

    // Topic ids chosen in the "mark topics covered" step of the open dialog.
    private final List<String> pickedTopicIds = new ArrayList<>();
    private long pickedDate;

    private void showLectureDialog(Lecture existing) {
        pickedTopicIds.clear();
        pickedDate = existing != null ? existing.getLectureDate()
                : DayUtils.startOfDay(System.currentTimeMillis());

        DialogLectureBinding form = DialogLectureBinding.inflate(getLayoutInflater());
        if (existing != null) {
            form.editTitle.setText(existing.getTitle());
            form.editNote.setText(existing.getNote());
        }
        form.buttonLectureDate.setText(dateFormat.format(new Date(pickedDate)));
        form.buttonLectureDate.setOnClickListener(view -> pickDate(form));
        form.buttonPickTopics.setOnClickListener(view -> pickTopics(form));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.lectures_add : R.string.lectures_edit)
                .setView(form.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null);
        if (existing != null) {
            builder.setNeutralButton(R.string.delete, (d, w) -> confirmDelete(existing));
        }
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String title = String.valueOf(form.editTitle.getText()).trim();
            if (title.isEmpty()) {
                form.editTitle.setError(getString(R.string.lecture_error_title));
                return;
            }
            String note = String.valueOf(form.editNote.getText()).trim();

            Lecture lecture = existing != null ? existing : new Lecture();
            lecture.setCourseId(courseId);
            lecture.setLectureDate(pickedDate);
            lecture.setTitle(title);
            lecture.setNote(note.isEmpty() ? null : note);

            boolean ok = existing != null ? repository.update(lecture) : repository.insert(lecture);
            if (!ok) {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            syllabusRepository.setDoneMany(pickedTopicIds);
            dialog.dismiss();
            refresh();
        });
    }

    private void pickDate(DialogLectureBinding form) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(DayUtils.toPicker(pickedDate))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now()).build())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            pickedDate = DayUtils.fromPicker(selection);
            form.buttonLectureDate.setText(dateFormat.format(new Date(pickedDate)));
        });
        picker.show(getSupportFragmentManager(), "lecture_date");
    }

    // Multi-choice list of topics that are still pending.
    private void pickTopics(DialogLectureBinding form) {
        List<SyllabusTopic> pending = new ArrayList<>();
        for (SyllabusTopic t : syllabusRepository.getByCourse(courseId)) {
            if (!t.isDone()) {
                pending.add(t);
            }
        }
        if (pending.isEmpty()) {
            Toast.makeText(this, R.string.lecture_no_pending_topics, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[pending.size()];
        boolean[] checked = new boolean[pending.size()];
        for (int i = 0; i < pending.size(); i++) {
            names[i] = pending.get(i).getTitle();
            checked[i] = pickedTopicIds.contains(pending.get(i).getId());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lecture_pick_topics)
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    pickedTopicIds.clear();
                    List<String> chosenNames = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            pickedTopicIds.add(pending.get(i).getId());
                            chosenNames.add(names[i]);
                        }
                    }
                    // Empty title: use the chosen topics as the lecture title.
                    if (!chosenNames.isEmpty()
                            && String.valueOf(form.editTitle.getText()).trim().isEmpty()) {
                        form.editTitle.setText(String.join(", ", chosenNames));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDelete(Lecture lecture) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lectures_delete_title)
                .setMessage(lecture.getTitle())
                .setPositiveButton(R.string.delete, (d, w) -> {
                    repository.delete(lecture.getId());
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
