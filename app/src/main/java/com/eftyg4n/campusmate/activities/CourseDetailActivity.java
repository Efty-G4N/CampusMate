package com.eftyg4n.campusmate.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.List;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "extra_course_id";

    private ActivityCourseDetailBinding binding;

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

        String courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
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
        setupWorkspaceButtons();
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
            bindTeacher(row, teacher);
            binding.layoutTeachers.addView(row.getRoot());
        }
    }

    private void bindTeacher(ItemTeacherBinding row, Teacher teacher) {
        row.textTeacherName.setText(teacher.getName());

        StringBuilder role = new StringBuilder();
        appendPart(role, teacher.getShortCode());
        appendPart(role, teacher.getDesignation());
        appendPart(role, teacher.getDepartment());
        row.textTeacherRole.setText(role.toString());

        StringBuilder place = new StringBuilder();
        if (hasText(teacher.getRoomNo())) {
            appendPart(place, getString(R.string.teacher_room, teacher.getRoomNo()));
        }
        if (hasText(teacher.getExtension())) {
            appendPart(place, getString(R.string.teacher_ext, teacher.getExtension()));
        }
        boolean hasPlace = showText(row.textTeacherRoom, place.toString());

        final String phone = teacher.getPhone();
        boolean hasPhone = showText(row.textTeacherPhone,
                hasText(phone) ? getString(R.string.teacher_phone, phone) : null);
        if (hasPhone) {
            // Implicit Intent: open the dialer with this number (does not call).
            row.textTeacherPhone.setOnClickListener(view -> openIntent(
                    new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone)))));
        }

        final String email = teacher.getEmail();
        boolean hasEmail = showText(row.textTeacherEmail,
                hasText(email) ? getString(R.string.teacher_email, email) : null);
        if (hasEmail) {
            // Implicit Intent: open an email app with this address.
            row.textTeacherEmail.setOnClickListener(view -> openIntent(
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email))));
        }

        boolean noInfo = !hasPlace && !hasPhone && !hasEmail;
        row.textTeacherNoInfo.setVisibility(noInfo ? View.VISIBLE : View.GONE);
    }

    private void setupWorkspaceButtons() {
        Button[] buttons = {
                binding.buttonAttendance, binding.buttonMarks, binding.buttonSyllabus,
                binding.buttonLectures, binding.buttonResources, binding.buttonDeadlines,
                binding.buttonNotices
        };
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setOnClickListener(view -> Toast.makeText(this,
                    R.string.workspace_coming_soon, Toast.LENGTH_SHORT).show());
        }
    }

    private void openIntent(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.common_no_app, Toast.LENGTH_SHORT).show();
        }
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

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    // Shows the text and returns true, or hides the view and returns false if it is empty.
    private boolean showText(TextView view, String text) {
        if (hasText(text)) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
            return true;
        }
        view.setVisibility(View.GONE);
        return false;
    }

    // Adds "part" to the text, with " · " between parts.
    private void appendPart(StringBuilder builder, String part) {
        if (hasText(part)) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(part);
        }
    }
}