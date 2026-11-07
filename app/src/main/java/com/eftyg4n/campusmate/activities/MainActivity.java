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

        // TEMPORARY: repository test (remove after Phase 2 testing)
        String dbTag = "CampusMate_DB";
        SemesterRepository semesterRepo = new SemesterRepository(this);
        CourseRepository courseRepo = new CourseRepository(this);

        if (semesterRepo.getAll().isEmpty()) {
            Semester semester = new Semester();
            semester.setName("2nd Year 2nd Term");
            semester.setSession("2024-2025");
            semester.setYearNo(2);
            semester.setTermNo(2);
            semester.setSection("A");
            semester.setCurrent(true);
            String semesterId = semesterRepo.insert(semester);

            Course theory = new Course();
            theory.setSemesterId(semesterId);
            theory.setCode("CSE 2209");
            theory.setTitle("Theory of Computation");
            theory.setCredits(2.0);
            theory.setType(Course.TYPE_THEORY);
            courseRepo.insert(theory);

            Course lab = new Course();
            lab.setSemesterId(semesterId);
            lab.setCode("CSE 2200");
            lab.setTitle("Advanced Programming Laboratory");
            lab.setCredits(1.5);
            lab.setType(Course.TYPE_LAB);
            courseRepo.insert(lab);

            Course duplicate = new Course();
            duplicate.setSemesterId(semesterId);
            duplicate.setCode("CSE 2209");
            duplicate.setTitle("Duplicate test");
            Log.d(dbTag, "Duplicate insert result: " + courseRepo.insert(duplicate));
        }

        Semester current = semesterRepo.getCurrent();
        if (current != null) {
            Log.d(dbTag, "Current semester: " + current.getName());
            for (Course item : courseRepo.getBySemester(current.getId())) {
                Log.d(dbTag, "Course: " + item.getCode() + " - " + item.getTitle()
                        + " (" + item.getType() + ", " + item.getCredits() + ")");
            }
            Log.d(dbTag, "Search 2209: "
                    + courseRepo.search(current.getId(), "2209", null).size());
            Log.d(dbTag, "Lab only: "
                    + courseRepo.search(current.getId(), null, Course.TYPE_LAB).size());
        }

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