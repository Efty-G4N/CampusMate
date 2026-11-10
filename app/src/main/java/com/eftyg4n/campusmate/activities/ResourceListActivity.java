package com.eftyg4n.campusmate.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ActivityResourceListBinding;
import com.eftyg4n.campusmate.fragments.ResourcesFragment;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.repositories.CourseRepository;

// A thin screen that only hosts ResourcesFragment. The same fragment is also the
// "Resources" tab, so the list code is written once.
public class ResourceListActivity extends AppCompatActivity {

    // Optional: show only one course's resources (sent by CourseDetailActivity).
    public static final String EXTRA_COURSE_ID = "extra_course_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityResourceListBinding binding = ActivityResourceListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        String courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        if (courseId != null) {
            Course course = new CourseRepository(this).getById(courseId);
            if (course != null) {
                binding.toolbar.setTitle(getString(R.string.resources_title_course,
                        course.getCode()));
            }
        }

        // After rotation the FragmentManager restores the fragment by itself.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, ResourcesFragment.newInstance(courseId))
                    .commit();
        }
    }
}
