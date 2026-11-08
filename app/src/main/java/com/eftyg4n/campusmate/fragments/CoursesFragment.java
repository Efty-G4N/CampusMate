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
import com.eftyg4n.campusmate.adapters.CourseAdapter;
import com.eftyg4n.campusmate.databinding.FragmentCoursesBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.models.Teacher;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.repositories.TeacherRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import com.eftyg4n.campusmate.activities.CourseDetailActivity;

public class CoursesFragment extends Fragment {

    private FragmentCoursesBinding binding;
    private CourseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCoursesBinding.inflate(inflater, container, false);

        adapter = new CourseAdapter(requireContext(), course -> {
            Intent intent = new Intent(requireContext(), CourseDetailActivity.class);
            intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.getId());
            startActivity(intent);
        });

        binding.recyclerCourses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCourses.setAdapter(adapter);

        return binding.getRoot();
    }

    // Reload every time the screen becomes visible, so changes are always shown.
    @Override
    public void onResume() {
        super.onResume();
        loadCourses();
    }

    private void loadCourses() {
        if (binding == null) {
            return;
        }

        Semester current = new SemesterRepository(requireContext()).getCurrent();
        if (current == null) {
            binding.textSemester.setText(R.string.courses_no_semester);
            adapter.setData(new ArrayList<>(), new HashMap<>());
            showEmpty(true);
            return;
        }

        CourseRepository courseRepo = new CourseRepository(requireContext());
        TeacherRepository teacherRepo = new TeacherRepository(requireContext());

        List<Course> courses = courseRepo.getBySemester(current.getId());
        Map<String, String> teacherCodes = new HashMap<>();
        for (Course course : courses) {
            StringBuilder codes = new StringBuilder();
            for (Teacher teacher : teacherRepo.getByCourse(course.getId())) {
                if (codes.length() > 0) {
                    codes.append(", ");
                }
                codes.append(teacher.getShortCode());
            }
            teacherCodes.put(course.getId(), codes.toString());
        }

        binding.textSemester.setText(getString(R.string.courses_header,
                current.getName(), current.getSession(), courses.size()));
        adapter.setData(courses, teacherCodes);
        showEmpty(courses.isEmpty());
    }

    private void showEmpty(boolean empty) {
        binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerCourses.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}