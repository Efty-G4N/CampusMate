package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.SemesterAdapter;
import com.eftyg4n.campusmate.databinding.ActivitySemestersBinding;
import com.eftyg4n.campusmate.databinding.DialogSemesterBinding;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

// All semesters of the student: add, edit, choose the current one, archive, delete.
// Courses belong to a semester, so deleting one also removes its courses (asked first).
public class SemestersActivity extends AppCompatActivity implements SemesterAdapter.Listener {

    private ActivitySemestersBinding binding;
    private SemesterRepository repository;
    private SemesterAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySemestersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.textEmpty.setText(R.string.semesters_empty);

        repository = new SemesterRepository(this);
        adapter = new SemesterAdapter(this, this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.buttonAddSemester.setOnClickListener(view -> showFormDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<Semester> semesters = repository.getAll();
        adapter.setData(semesters);
        binding.textEmpty.setVisibility(semesters.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSemesterClick(Semester semester) {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        labels.add(getString(R.string.semester_action_edit));
        actions.add(() -> showFormDialog(semester));

        if (!semester.isCurrent() && !semester.isArchived()) {
            labels.add(getString(R.string.semester_action_current));
            actions.add(() -> {
                repository.setCurrent(semester.getId());
                refresh();
            });
        }
        labels.add(getString(semester.isArchived() ? R.string.semester_action_restore
                : R.string.semester_action_archive));
        actions.add(() -> {
            repository.setArchived(semester.getId(), !semester.isArchived());
            refresh();
        });
        labels.add(getString(R.string.delete));
        actions.add(() -> confirmDelete(semester));

        new MaterialAlertDialogBuilder(this)
                .setTitle(semester.getName())
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void showFormDialog(Semester existing) {
        DialogSemesterBinding form = DialogSemesterBinding.inflate(getLayoutInflater());
        if (existing != null) {
            form.editName.setText(existing.getName());
            form.editSession.setText(existing.getSession());
            form.editSection.setText(existing.getSection());
            if (existing.getYearNo() > 0) {
                form.editYear.setText(String.valueOf(existing.getYearNo()));
            }
            if (existing.getTermNo() > 0) {
                form.editTerm.setText(String.valueOf(existing.getTermNo()));
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.semesters_add : R.string.semester_action_edit)
                .setView(form.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String name = String.valueOf(form.editName.getText()).trim();
            if (name.isEmpty()) {
                form.editName.setError(getString(R.string.semester_error_name));
                return;
            }
            int year = parseSmall(form.editYear.getText());
            int term = parseSmall(form.editTerm.getText());
            if (year < 0 || year > 4 || term < 0 || term > 2) {
                form.editYear.setError(getString(R.string.semester_error_year_term));
                return;
            }
            String session = String.valueOf(form.editSession.getText()).trim();
            String section = String.valueOf(form.editSection.getText()).trim();

            Semester s = existing != null ? existing : new Semester();
            s.setName(name);
            s.setSession(session.isEmpty() ? null : session);
            s.setSection(section.isEmpty() ? null : section);
            s.setYearNo(year);
            s.setTermNo(term);

            boolean ok;
            if (existing != null) {
                ok = repository.update(s);
            } else {
                // The first semester ever becomes the current one automatically.
                s.setCurrent(repository.getCurrent() == null);
                ok = repository.insert(s) != null;
            }
            if (!ok) {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            refresh();
        });
    }

    // Empty means 0 (not set); anything that is not a number is rejected with -1.
    private int parseSmall(CharSequence text) {
        String t = String.valueOf(text).trim();
        if (t.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void confirmDelete(Semester semester) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.semester_delete_title)
                .setMessage(getString(R.string.semester_delete_message, semester.getName()))
                .setPositiveButton(R.string.delete, (d, w) -> {
                    repository.delete(semester.getId());
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
