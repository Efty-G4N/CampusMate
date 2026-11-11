package com.eftyg4n.campusmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ActivityCourseDetailBinding;
import com.eftyg4n.campusmate.databinding.ItemTeacherBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Teacher;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.TeacherRepository;
import com.eftyg4n.campusmate.utils.AccessGate;
import com.eftyg4n.campusmate.utils.TeacherCardBinder;

import java.util.List;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "extra_course_id";

    private ActivityCourseDetailBinding binding;
    private String courseId;

    // Opens the edit screen and receives its result.
    // If the course was deleted there, this screen has nothing to show, so it closes.
    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AddEditCourseActivity.RESULT_DELETED) {
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Keep the content away from the status bar and the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        binding.toolbar.setNavigationOnClickListener(view -> finish());

        binding.toolbar.inflateMenu(R.menu.course_detail_menu);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_course) {
                Intent intent = new Intent(this, AddEditCourseActivity.class);
                intent.putExtra(AddEditCourseActivity.EXTRA_COURSE_ID, courseId);
                editLauncher.launch(intent);
                return true;
            }
            return false;
        });

        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        setupWorkspaceButtons();
    }

    // Load here (not in onCreate), so the screen is refreshed after an edit.
    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing()) {
            return;
        }

        Course course = null;
        if (courseId != null) {
            course = new CourseRepository(this).getById(courseId);
        }
        if (course == null) {
            Toast.makeText(this, R.string.course_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showCourse(course);
        showTeachers(course);
    }

    private void showCourse(Course course) {
        binding.toolbar.setTitle(course.getCode());
        binding.textCode.setText(course.getCode());
        binding.textTitle.setText(course.getTitle());
        binding.textMeta.setText(getString(R.string.course_meta,
                typeLabel(course.getType()), String.valueOf(course.getCredits())));

        int color = course.getColor();
        if (color == 0) {
            color = ContextCompat.getColor(this, R.color.cm_primary);
        }
        binding.cardHeader.setCardBackgroundColor(color);
    }

    private void showTeachers(Course course) {
        List<Teacher> teachers = new TeacherRepository(this).getByCourse(course.getId());

        binding.layoutTeachers.removeAllViews();
        binding.textNoTeachers.setVisibility(teachers.isEmpty() ? View.VISIBLE : View.GONE);

        for (Teacher teacher : teachers) {
            ItemTeacherBinding row = ItemTeacherBinding.inflate(
                    getLayoutInflater(), binding.layoutTeachers, false);
            TeacherCardBinder.bind(this, row, teacher);
            binding.layoutTeachers.addView(row.getRoot());
        }
    }

    private void setupWorkspaceButtons() {
        Button[] buttons = {
                binding.buttonAttendance, binding.buttonMarks, binding.buttonSyllabus,
                binding.buttonLectures, binding.buttonResources, binding.buttonNotices
        };
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setOnClickListener(view -> Toast.makeText(this,
                    R.string.workspace_coming_soon, Toast.LENGTH_SHORT).show());
        }

        // Marks, Syllabus and Lectures: each opens its own screen for THIS course.
        binding.buttonMarks.setOnClickListener(view -> {
            if (AccessGate.require(this, R.string.gate_marks)) {
                openForCourse(MarksActivity.class);
            }
        });
        binding.buttonSyllabus.setOnClickListener(view -> openForCourse(SyllabusActivity.class));
        binding.buttonLectures.setOnClickListener(view -> openForCourse(LecturesActivity.class));

        // Attendance is real now: present / absent log of THIS course.
        binding.buttonAttendance.setOnClickListener(view -> {
            if (!AccessGate.require(this, R.string.gate_attendance)) {
                return;
            }
            Intent intent = new Intent(this, AttendanceActivity.class);
            intent.putExtra(AttendanceActivity.EXTRA_COURSE_ID, courseId);
            startActivity(intent);
        });

        // Notices: class-wide announcements stored in Cloud Firestore.
        binding.buttonNotices.setOnClickListener(view -> {
            Intent intent = new Intent(this, NoticesActivity.class);
            intent.putExtra(NoticesActivity.EXTRA_COURSE_ID, courseId);
            startActivity(intent);
        });

        // Resources is real now: the Resource Library filtered to THIS course.
        binding.buttonResources.setOnClickListener(view -> {
            Intent intent = new Intent(this, ResourceListActivity.class);
            intent.putExtra(ResourceListActivity.EXTRA_COURSE_ID, courseId);
            startActivity(intent);
        });

        // Deadlines is real now: open the list filtered to THIS course.
        binding.buttonDeadlines.setOnClickListener(view -> {
            Intent intent = new Intent(this, DeadlinesActivity.class);
            intent.putExtra(DeadlinesActivity.EXTRA_COURSE_ID, courseId);
            startActivity(intent);
        });
    }

    // Every course workspace screen takes the same extra, so one helper starts them all.
    private void openForCourse(Class<?> screen) {
        Intent intent = new Intent(this, screen);
        intent.putExtra(EXTRA_COURSE_ID, courseId);
        startActivity(intent);
    }

    private String typeLabel(String type) {
        if (Course.TYPE_LAB.equals(type)) {
            return getString(R.string.course_type_lab);
        }
        if (Course.TYPE_CUSTOM.equals(type)) {
            return getString(R.string.course_type_custom);
        }
        return getString(R.string.course_type_theory);
    }
}