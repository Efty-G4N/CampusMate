package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ActivityMainBinding;
import com.eftyg4n.campusmate.fragments.CoursesFragment;
import com.eftyg4n.campusmate.fragments.HomeFragment;
import com.eftyg4n.campusmate.fragments.ProfileFragment;
import com.eftyg4n.campusmate.fragments.ResourcesFragment;
import com.eftyg4n.campusmate.fragments.ScheduleFragment;

import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.database.KuetSeedData;
import com.eftyg4n.campusmate.models.Teacher;
import com.eftyg4n.campusmate.repositories.TeacherRepository;
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CampusMate_Lifecycle";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity onCreate");

        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // TEMPORARY: seed and repository test (remove after Phase 2 testing)
        String dbTag = "CampusMate_DB";
        KuetSeedData.seedIfEmpty(this);

        SemesterRepository semesterRepo = new SemesterRepository(this);
        CourseRepository courseRepo = new CourseRepository(this);
        TeacherRepository teacherRepo = new TeacherRepository(this);

        Semester current = semesterRepo.getCurrent();
        if (current != null) {
            Log.d(dbTag, "Current semester: " + current.getName()
                    + " (" + current.getSession() + ")");
            for (Course course : courseRepo.getBySemester(current.getId())) {
                StringBuilder codes = new StringBuilder();
                for (Teacher teacher : teacherRepo.getByCourse(course.getId())) {
                    codes.append(teacher.getShortCode()).append(' ');
                }
                Log.d(dbTag, course.getCode() + " -> " + codes.toString().trim());
            }
        }

        Teacher sa = teacherRepo.getByShortCode("SA");
        if (sa != null) {
            Log.d(dbTag, "SA: " + sa.getName() + " | " + sa.getRoomNo() + " | "
                    + sa.getPhone() + " | " + sa.getEmail());
            for (Course taught : courseRepo.getByTeacher(sa.getId())) {
                Log.d(dbTag, "SA teaches: " + taught.getCode());
            }
        }
        Log.d(dbTag, "Teachers total: " + teacherRepo.getAll().size());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showFragment(new HomeFragment());
            } else if (id == R.id.nav_courses) {
                showFragment(new CoursesFragment());
            } else if (id == R.id.nav_schedule) {
                showFragment(new ScheduleFragment());
            } else if (id == R.id.nav_resources) {
                showFragment(new ResourcesFragment());
            } else if (id == R.id.nav_profile) {
                showFragment(new ProfileFragment());
            } else {
                return false;
            }
            return true;
        });

        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "MainActivity onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity onDestroy");
    }
}