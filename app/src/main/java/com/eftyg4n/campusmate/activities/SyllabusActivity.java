package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.TopicAdapter;
import com.eftyg4n.campusmate.databinding.ActivitySyllabusBinding;
import com.eftyg4n.campusmate.databinding.DialogTopicsBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.SyllabusTopic;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.SyllabusRepository;
import com.eftyg4n.campusmate.utils.MarksUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

// The syllabus of ONE course as a checklist. Tap a topic when it has been covered;
// the progress bar shows how much of the course is left before the exam.
public class SyllabusActivity extends AppCompatActivity implements TopicAdapter.Listener {

    private ActivitySyllabusBinding binding;
    private SyllabusRepository repository;
    private TopicAdapter adapter;
    private String courseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySyllabusBinding.inflate(getLayoutInflater());
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
        binding.textEmpty.setText(R.string.syllabus_empty);

        repository = new SyllabusRepository(this);
        adapter = new TopicAdapter(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.buttonAddTopics.setOnClickListener(view -> showAddDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null) {
            refresh();
        }
    }

    private void refresh() {
        List<SyllabusTopic> topics = repository.getByCourse(courseId);
        adapter.setData(topics);
        binding.textEmpty.setVisibility(topics.isEmpty() ? View.VISIBLE : View.GONE);

        int done = 0;
        for (SyllabusTopic t : topics) {
            if (t.isDone()) {
                done++;
            }
        }
        int percent = topics.isEmpty() ? 0
                : (int) Math.round(MarksUtils.percent(done, topics.size()));
        binding.textProgress.setText(getString(R.string.syllabus_progress,
                done, topics.size(), percent));
        binding.progress.setProgressCompat(percent, true);
    }

    @Override
    public void onTopicClick(SyllabusTopic topic) {
        repository.setDone(topic.getId(), !topic.isDone());
        refresh();
    }

    @Override
    public void onTopicLongClick(SyllabusTopic topic) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.syllabus_delete_title)
                .setMessage(topic.getTitle())
                .setPositiveButton(R.string.delete, (d, w) -> {
                    repository.delete(topic.getId());
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // One topic per line, so a whole syllabus can be pasted at once.
    private void showAddDialog() {
        DialogTopicsBinding form = DialogTopicsBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.syllabus_add)
                .setView(form.getRoot())
                .setPositiveButton(R.string.save, (d, w) -> {
                    List<String> titles = new ArrayList<>();
                    for (String line : String.valueOf(form.editTopics.getText()).split("\\r?\\n")) {
                        // Remove list bullets or numbers that came along with a paste.
                        String clean = line.trim().replaceFirst("^([-*•]|\\d+[.)])\\s*", "");
                        if (!clean.isEmpty()) {
                            titles.add(clean);
                        }
                    }
                    if (titles.isEmpty()) {
                        return;
                    }
                    int saved = repository.insertMany(courseId, titles);
                    Toast.makeText(this, getString(R.string.syllabus_added, saved),
                            Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
