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
import com.eftyg4n.campusmate.models.ClassGroup;
import com.eftyg4n.campusmate.models.Classmate;
import com.eftyg4n.campusmate.models.UserProfile;
import com.eftyg4n.campusmate.repositories.ClassGroupRepository;
import com.eftyg4n.campusmate.repositories.ClassmateRepository;
import com.eftyg4n.campusmate.repositories.UserProfileRepository;
import com.eftyg4n.campusmate.utils.RollUtils;
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

        // TEMPORARY: class structure and profile test (remove after Phase 2 testing)
        String dbTag = "CampusMate_DB";
        KuetSeedData.seedIfEmpty(this);
        KuetSeedData.seedClassData(this);

        SemesterRepository semesterRepo = new SemesterRepository(this);
        ClassGroupRepository groupRepo = new ClassGroupRepository(this);
        ClassmateRepository classmateRepo = new ClassmateRepository(this);
        UserProfileRepository profileRepo = new UserProfileRepository(this);

        Log.d(dbTag, "Classmates loaded: " + classmateRepo.count());

        Semester current = semesterRepo.getCurrent();
        if (current != null) {
            for (ClassGroup group : groupRepo.getBySemester(current.getId())) {
                Log.d(dbTag, group.getType() + " " + group.getName() + ": "
                        + group.getRollFrom() + "-" + group.getRollTo());
            }

            int digits = 3;
            String myRoll = "2307100";
            int serial = RollUtils.serialOf(myRoll, digits);
            ClassGroup section = groupRepo.findSection(current.getId(), serial);
            ClassGroup labGroup = groupRepo.findLabGroup(current.getId(), serial);
            Classmate me = classmateRepo.getByRoll(myRoll);
            Log.d(dbTag, "Roll " + myRoll + " serial " + serial + " -> "
                    + (me != null ? me.getFullName() : "not in list")
                    + " | section " + (section != null ? section.getName() : "?")
                    + " | lab group " + (labGroup != null ? labGroup.getName() : "?"));

            int[] samples = {1, 30, 31, 60, 61, 90, 91, 123};
            for (int i = 0; i < samples.length; i++) {
                ClassGroup s = groupRepo.findSection(current.getId(), samples[i]);
                ClassGroup l = groupRepo.findLabGroup(current.getId(), samples[i]);
                Log.d(dbTag, "Serial " + samples[i] + " -> "
                        + (s != null ? s.getName() : "?") + " / "
                        + (l != null ? l.getName() : "?"));
            }

            if (profileRepo.getProfile() == null && me != null) {
                UserProfile profile = new UserProfile();
                profile.setName(me.getFullName());
                profile.setUniversity("Khulna University of Engineering & Technology (KUET)");
                profile.setDepartment("Computer Science and Engineering");
                profile.setSession(current.getSession());
                profile.setYearNo(current.getYearNo());
                profile.setTermNo(current.getTermNo());
                profile.setRollNo(myRoll);
                profile.setRollSerialDigits(digits);
                profile.setSection(section != null ? section.getName() : null);
                profile.setLabGroup(labGroup != null ? labGroup.getName() : null);
                Log.d(dbTag, "Profile saved: " + profileRepo.save(profile));
            }

            UserProfile saved = profileRepo.getProfile();
            if (saved != null) {
                Log.d(dbTag, "Profile: " + saved.getName() + " | " + saved.getRollNo()
                        + " | " + saved.getSection() + " | " + saved.getLabGroup());
            }
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