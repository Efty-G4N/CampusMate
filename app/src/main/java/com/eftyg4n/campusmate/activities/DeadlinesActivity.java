package com.eftyg4n.campusmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.DeadlineAdapter;
import com.eftyg4n.campusmate.databinding.ActivityDeadlinesBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Deadline;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.notifications.ReminderScheduler;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.DeadlineRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;

import java.util.List;

public class DeadlinesActivity extends AppCompatActivity implements DeadlineAdapter.Listener {

    // Optional: show only the deadlines of one course (sent by CourseDetailActivity).
    public static final String EXTRA_COURSE_ID = "extra_course_id";

    private ActivityDeadlinesBinding binding;
    private DeadlineRepository repo;
    private DeadlineAdapter adapter;
    private String semesterId;
    private String courseId;   // null = all courses
    private int filter = DeadlineRepository.FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeadlinesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        Semester current = new SemesterRepository(this).getCurrent();
        if (current == null) {
            Toast.makeText(this, R.string.course_form_no_semester, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        semesterId = current.getId();
        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        repo = new DeadlineRepository(this);

        binding.toolbar.setTitle(R.string.deadlines_title);
        if (courseId != null) {
            Course course = new CourseRepository(this).getById(courseId);
            if (course != null) {
                binding.toolbar.setTitle(getString(R.string.deadlines_title_course,
                        course.getCode()));
            }
        }

        adapter = new DeadlineAdapter(this, this);
        binding.recyclerDeadlines.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerDeadlines.setAdapter(adapter);

        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipPending) {
                filter = DeadlineRepository.FILTER_PENDING;
            } else if (id == R.id.chipOverdue) {
                filter = DeadlineRepository.FILTER_OVERDUE;
            } else if (id == R.id.chipCompleted) {
                filter = DeadlineRepository.FILTER_COMPLETED;
            } else {
                filter = DeadlineRepository.FILTER_ALL;
            }
            loadDeadlines();
        });

        binding.fabAddDeadline.setOnClickListener(view -> {
            Intent intent = new Intent(this, AddEditDeadlineActivity.class);
            intent.putExtra(AddEditDeadlineActivity.EXTRA_COURSE_ID, courseId);
            startActivity(intent);
        });
    }

    // Reload here (not in onCreate): the list must refresh after add / edit / delete.
    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) {
            loadDeadlines();
        }
    }

    private void loadDeadlines() {
        List<Deadline> list = repo.getList(semesterId, courseId, filter);
        adapter.setData(list);
        binding.textEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDeadlineClick(Deadline deadline) {
        Intent intent = new Intent(this, AddEditDeadlineActivity.class);
        intent.putExtra(AddEditDeadlineActivity.EXTRA_DEADLINE_ID, deadline.getId());
        startActivity(intent);
    }

    @Override
    public void onCompletedChanged(Deadline deadline, boolean completed) {
        repo.setCompleted(deadline.getId(), completed);
        if (completed) {
            // Nothing to remind about anymore.
            ReminderScheduler.cancel(this, deadline.getId());
        } else {
            // Pending again: plan the saved reminders again (past ones are skipped).
            Deadline fresh = repo.getById(deadline.getId());
            if (fresh != null) {
                ReminderScheduler.schedule(this, fresh,
                        repo.getReminderMinutes(fresh.getId()));
            }
        }
        // post(): do not change the list while the CheckBox is still handling its click.
        binding.recyclerDeadlines.post(this::loadDeadlines);
    }
}
