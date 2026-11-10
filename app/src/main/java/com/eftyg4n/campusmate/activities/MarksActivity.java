package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.MarkAdapter;
import com.eftyg4n.campusmate.databinding.ActivityMarksBinding;
import com.eftyg4n.campusmate.databinding.DialogMarkBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Mark;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.MarkRepository;
import com.eftyg4n.campusmate.utils.MarksUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

// CT, quiz, assignment, lab and exam marks of ONE course, with the total and the
// "best 3 CT" average that KUET counts.
public class MarksActivity extends AppCompatActivity implements MarkAdapter.Listener {

    private ActivityMarksBinding binding;
    private MarkRepository repository;
    private MarkAdapter adapter;
    private String courseId;
    private String[] categoryLabels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMarksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        courseId = getIntent().getStringExtra(CourseDetailActivity.EXTRA_COURSE_ID);
        Course course = courseId == null ? null : new CourseRepository(this).getById(courseId);
        if (course == null) {
            Toast.makeText(this, R.string.course_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.textCourse.setText(getString(R.string.attendance_course_line,
                course.getCode(), course.getTitle()));
        binding.textEmpty.setText(R.string.marks_empty);
        categoryLabels = getResources().getStringArray(R.array.mark_category_labels);

        repository = new MarkRepository(this);
        adapter = new MarkAdapter(this, this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.buttonAddMark.setOnClickListener(view -> showMarkDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null) {
            refresh();
        }
    }

    private void refresh() {
        List<Mark> marks = repository.getByCourse(courseId);
        adapter.setData(marks);
        binding.textEmpty.setVisibility(marks.isEmpty() ? View.VISIBLE : View.GONE);

        double obtained = 0;
        double total = 0;
        int ctCount = 0;
        double[] ctPercents = new double[marks.size()];
        for (Mark m : marks) {
            obtained += m.getObtained();
            total += m.getTotal();
            if (Mark.CATEGORY_CT.equals(m.getCategory())) {
                ctPercents[ctCount++] = MarksUtils.percent(m.getObtained(), m.getTotal());
            }
        }

        double percent = MarksUtils.percent(obtained, total);
        binding.textOverall.setText(getString(R.string.marks_overall,
                MarksUtils.plain(obtained), MarksUtils.plain(total)));
        binding.progress.setProgressCompat((int) Math.round(percent), true);
        binding.progress.setIndicatorColor(ContextCompat.getColor(this,
                percent >= 40 ? R.color.cm_secondary : R.color.cm_error));

        if (ctCount == 0) {
            binding.textBestCt.setText(R.string.marks_no_ct);
        } else {
            double best = MarksUtils.bestAveragePercent(
                    java.util.Arrays.copyOf(ctPercents, ctCount), MarksUtils.BEST_CT_COUNT);
            binding.textBestCt.setText(getString(R.string.marks_best_ct,
                    MarksUtils.BEST_CT_COUNT, String.format(Locale.getDefault(), "%.1f", best),
                    ctCount));
        }
    }

    @Override
    public void onMarkClick(Mark mark) {
        showMarkDialog(mark);
    }

    // mark == null means a new one; otherwise the same dialog edits it (with a Delete button).
    private void showMarkDialog(Mark existing) {
        DialogMarkBinding form = DialogMarkBinding.inflate(getLayoutInflater());
        form.dropCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, categoryLabels));

        int categoryIndex = 0;
        if (existing != null) {
            form.editTitle.setText(existing.getTitle());
            form.editObtained.setText(MarksUtils.plain(existing.getObtained()));
            form.editTotal.setText(MarksUtils.plain(existing.getTotal()));
            for (int i = 0; i < Mark.CATEGORIES.length; i++) {
                if (Mark.CATEGORIES[i].equals(existing.getCategory())) {
                    categoryIndex = i;
                }
            }
        }
        form.dropCategory.setText(categoryLabels[categoryIndex], false);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.marks_add : R.string.marks_edit)
                .setView(form.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null);
        if (existing != null) {
            builder.setNeutralButton(R.string.delete, (d, w) -> confirmDelete(existing));
        }
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set after show() so a validation error keeps the dialog open.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String title = String.valueOf(form.editTitle.getText()).trim();
            double obtained = MarksUtils.parseNumber(String.valueOf(form.editObtained.getText()));
            double total = MarksUtils.parseNumber(String.valueOf(form.editTotal.getText()));

            if (title.isEmpty()) {
                form.editTitle.setError(getString(R.string.mark_error_title));
                return;
            }
            if (total <= 0) {
                form.editTotal.setError(getString(R.string.mark_error_total));
                return;
            }
            if (obtained < 0 || obtained > total) {
                form.editObtained.setError(getString(R.string.mark_error_obtained));
                return;
            }

            Mark mark = existing != null ? existing : new Mark();
            mark.setCourseId(courseId);
            mark.setTitle(title);
            mark.setCategory(categoryFromLabel(String.valueOf(form.dropCategory.getText())));
            mark.setObtained(obtained);
            mark.setTotal(total);

            boolean ok = existing != null ? repository.update(mark) : repository.insert(mark);
            if (!ok) {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            refresh();
        });
    }

    private String categoryFromLabel(String label) {
        for (int i = 0; i < categoryLabels.length && i < Mark.CATEGORIES.length; i++) {
            if (categoryLabels[i].equals(label)) {
                return Mark.CATEGORIES[i];
            }
        }
        return Mark.CATEGORIES[Mark.CATEGORIES.length - 1];
    }

    private void confirmDelete(Mark mark) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.marks_delete_title)
                .setMessage(mark.getTitle())
                .setPositiveButton(R.string.delete, (d, w) -> {
                    repository.delete(mark.getId());
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
