package com.eftyg4n.campusmate.activities;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ActivityAddEditCourseBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class AddEditCourseActivity extends AppCompatActivity {

    // No extra = add mode. With a course id = edit mode.
    public static final String EXTRA_COURSE_ID = "extra_course_id";
    // Sent back to CourseDetailActivity after a delete, so it can close itself.
    public static final int RESULT_DELETED = RESULT_FIRST_USER;

    private static final String KEY_TYPE_INDEX = "key_type_index";
    private static final String KEY_COLOR = "key_color";

    private static final String[] TYPES = {
            Course.TYPE_THEORY, Course.TYPE_LAB, Course.TYPE_CUSTOM
    };
    private static final int[] PALETTE = {
            0xFF1E88E5, 0xFF8E24AA, 0xFF5E35B1, 0xFF00897B, 0xFFF4511E,
            0xFFFB8C00, 0xFF6D4C41, 0xFF43A047, 0xFFE53935, 0xFF00ACC1
    };

    private ActivityAddEditCourseBinding binding;
    private CourseRepository courseRepo;
    private Course editingCourse;   // null in add mode
    private String semesterId;
    private int selectedTypeIndex = 0;
    private int selectedColor = PALETTE[0];
    private View[] swatches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        courseRepo = new CourseRepository(this);

        String courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        if (courseId != null) {
            editingCourse = courseRepo.getById(courseId);
            if (editingCourse == null) {
                Toast.makeText(this, R.string.course_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            semesterId = editingCourse.getSemesterId();
        } else {
            Semester current = new SemesterRepository(this).getCurrent();
            if (current == null) {
                Toast.makeText(this, R.string.course_form_no_semester,
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            semesterId = current.getId();
        }

        setupTypeDropdown();
        setupColorSwatches();

        if (savedInstanceState != null) {
            // Text fields restore themselves; type and color are our own state.
            selectedTypeIndex = savedInstanceState.getInt(KEY_TYPE_INDEX, 0);
            selectedColor = savedInstanceState.getInt(KEY_COLOR, PALETTE[0]);
        } else if (editingCourse != null) {
            fillForm(editingCourse);
        }
        showSelectedType();
        refreshSwatches();

        if (editingCourse != null) {
            binding.toolbar.setTitle(R.string.course_form_title_edit);
            binding.buttonDelete.setVisibility(View.VISIBLE);
        } else {
            binding.toolbar.setTitle(R.string.course_form_title_add);
        }

        binding.buttonSave.setOnClickListener(view -> save());
        binding.buttonDelete.setOnClickListener(view -> confirmDelete());
    }

    private void setupTypeDropdown() {
        String[] labels = {
                getString(R.string.course_type_theory),
                getString(R.string.course_type_lab),
                getString(R.string.course_type_custom)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, labels);
        binding.dropdownType.setAdapter(adapter);
        binding.dropdownType.setOnItemClickListener((parent, view, position, id) ->
                selectedTypeIndex = position);
    }

    // setText(text, false): show the text without filtering the dropdown list.
    private void showSelectedType() {
        String[] labels = {
                getString(R.string.course_type_theory),
                getString(R.string.course_type_lab),
                getString(R.string.course_type_custom)
        };
        binding.dropdownType.setText(labels[selectedTypeIndex], false);
    }

    private void setupColorSwatches() {
        swatches = new View[PALETTE.length];
        for (int i = 0; i < PALETTE.length; i++) {
            final int color = PALETTE[i];
            View swatch = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(44);
            params.height = dp(44);
            params.setMargins(dp(6), dp(6), dp(6), dp(6));
            swatch.setLayoutParams(params);
            swatch.setOnClickListener(view -> {
                selectedColor = color;
                refreshSwatches();
            });
            binding.layoutColors.addView(swatch);
            swatches[i] = swatch;
        }
    }

    // Draws every swatch as a circle; the selected one gets a thick outline.
    private void refreshSwatches() {
        int outline = ContextCompat.getColor(this, R.color.cm_on_surface);
        for (int i = 0; i < swatches.length; i++) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(PALETTE[i]);
            if (PALETTE[i] == selectedColor) {
                circle.setStroke(dp(3), outline);
            }
            swatches[i].setBackground(circle);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void fillForm(Course course) {
        binding.editCode.setText(course.getCode());
        binding.editTitle.setText(course.getTitle());
        binding.editCredits.setText(String.valueOf(course.getCredits()));
        binding.editDescription.setText(course.getDescription());

        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(course.getType())) {
                selectedTypeIndex = i;
            }
        }
        if (course.getColor() != 0) {
            selectedColor = course.getColor();
        }
    }

    private String textOf(TextInputEditText edit) {
        if (edit.getText() == null) {
            return "";
        }
        return edit.getText().toString().trim();
    }

    private void save() {
        binding.layoutCode.setError(null);
        binding.layoutTitle.setError(null);
        binding.layoutCredits.setError(null);

        String code = textOf(binding.editCode);
        String title = textOf(binding.editTitle);
        String creditsText = textOf(binding.editCredits);
        String description = textOf(binding.editDescription);

        boolean valid = true;
        if (code.isEmpty()) {
            binding.layoutCode.setError(getString(R.string.course_form_error_code));
            valid = false;
        }
        if (title.isEmpty()) {
            binding.layoutTitle.setError(getString(R.string.course_form_error_title));
            valid = false;
        }

        double credits = 0;
        try {
            credits = Double.parseDouble(creditsText);
        } catch (NumberFormatException e) {
            credits = 0;
        }
        if (credits < 0.25 || credits > 10) {
            binding.layoutCredits.setError(getString(R.string.course_form_error_credits));
            valid = false;
        }

        if (!valid) {
            return;
        }

        Course course = editingCourse;
        if (course == null) {
            course = new Course();
            course.setSemesterId(semesterId);
        }
        course.setCode(code);
        course.setTitle(title);
        course.setCredits(credits);
        course.setType(TYPES[selectedTypeIndex]);
        course.setDescription(description.isEmpty() ? null : description);
        course.setColor(selectedColor);

        boolean saved;
        if (editingCourse == null) {
            saved = courseRepo.insert(course) != null;
        } else {
            saved = courseRepo.update(course);
        }

        if (!saved) {
            // The only expected reason: the same code already exists in this semester.
            binding.layoutCode.setError(getString(R.string.course_form_error_exists));
            return;
        }

        Toast.makeText(this, R.string.course_form_saved, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.course_delete_title)
                .setMessage(getString(R.string.course_delete_message, editingCourse.getCode()))
                .setNegativeButton(R.string.course_delete_cancel, null)
                .setPositiveButton(R.string.course_delete_confirm, (dialog, which) -> {
                    if (courseRepo.delete(editingCourse.getId())) {
                        Toast.makeText(this, R.string.course_form_deleted,
                                Toast.LENGTH_SHORT).show();
                        setResult(RESULT_DELETED);
                        finish();
                    } else {
                        Toast.makeText(this, R.string.course_form_save_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TYPE_INDEX, selectedTypeIndex);
        outState.putInt(KEY_COLOR, selectedColor);
    }
}