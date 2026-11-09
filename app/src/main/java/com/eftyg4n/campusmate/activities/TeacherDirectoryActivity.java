package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.TeacherAdapter;
import com.eftyg4n.campusmate.databinding.ActivityTeacherDirectoryBinding;
import com.eftyg4n.campusmate.dialogs.TeacherDetailDialog;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Teacher;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.TeacherRepository;
import com.eftyg4n.campusmate.utils.SimpleTextWatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherDirectoryActivity extends AppCompatActivity {

    private ActivityTeacherDirectoryBinding binding;
    private TeacherRepository teacherRepo;
    private CourseRepository courseRepo;
    private TeacherAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTeacherDirectoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Keep the content away from the status bar and the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        teacherRepo = new TeacherRepository(this);
        courseRepo = new CourseRepository(this);

        binding.toolbar.setNavigationOnClickListener(view -> finish());

        adapter = new TeacherAdapter(this);
        binding.listTeachers.setAdapter(adapter);

        // List item click: show the teacher's contact card.
        binding.listTeachers.setOnItemClickListener((parent, view, position, id) ->
                TeacherDetailDialog.show(this, adapter.getItem(position)));

        binding.editSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                loadTeachers(s.toString());
            }
        });

        loadTeachers("");
    }

    private void loadTeachers(String query) {
        List<Teacher> teachers = teacherRepo.search(query);

        Map<String, String> courseCodes = new HashMap<>();
        for (Teacher teacher : teachers) {
            StringBuilder codes = new StringBuilder();
            for (Course course : courseRepo.getByTeacher(teacher.getId())) {
                if (codes.length() > 0) {
                    codes.append(", ");
                }
                codes.append(course.getCode());
            }
            courseCodes.put(teacher.getId(), codes.toString());
        }

        adapter.setData(teachers, courseCodes);
        binding.textCount.setText(getString(R.string.teachers_count, teachers.size()));

        boolean empty = teachers.isEmpty();
        binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.listTeachers.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}