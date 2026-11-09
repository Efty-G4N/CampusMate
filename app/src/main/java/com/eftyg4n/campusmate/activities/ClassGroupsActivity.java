package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ActivityClassGroupsBinding;
import com.eftyg4n.campusmate.models.ClassGroup;
import com.eftyg4n.campusmate.models.Classmate;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.models.UserProfile;
import com.eftyg4n.campusmate.repositories.ClassGroupRepository;
import com.eftyg4n.campusmate.repositories.ClassmateRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.repositories.UserProfileRepository;
import com.eftyg4n.campusmate.utils.RollUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ClassGroupsActivity extends AppCompatActivity {

    // A "to" value of 999 or more means "no upper limit" (example: Section B).
    private static final int OPEN_ENDED_ROLL = 999;

    private ActivityClassGroupsBinding binding;
    private ClassmateRepository classmateRepo;

    // Same order as the rows of the ListView, so position = index in this list.
    private final List<ClassGroup> groups = new ArrayList<>();

    private int serialDigits = 3;
    private String mySection;
    private String myLabGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassGroupsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Keep the content away from the status bar and the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        binding.toolbar.setNavigationOnClickListener(view -> finish());

        classmateRepo = new ClassmateRepository(this);

        UserProfile profile = new UserProfileRepository(this).getProfile();
        if (profile != null) {
            serialDigits = profile.getRollSerialDigits();
            mySection = profile.getSection();
            myLabGroup = profile.getLabGroup();
        }

        Semester current = new SemesterRepository(this).getCurrent();
        if (current != null) {
            groups.addAll(new ClassGroupRepository(this).getBySemester(current.getId()));
        }

        // One text line for each group.
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            labels.add(labelOf(groups.get(i)));
        }

        // Simple ListView: ArrayAdapter + Android's ready-made row layout.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, labels);
        binding.listGroups.setAdapter(adapter);

        // Item click: position tells which row was tapped.
        binding.listGroups.setOnItemClickListener((parent, view, position, id) ->
                showStudents(groups.get(position)));

        boolean empty = groups.isEmpty();
        binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.listGroups.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // Example: "Section B · Roll 61 and above · Your group"
    private String labelOf(ClassGroup group) {
        String range;
        if (group.getRollTo() >= OPEN_ENDED_ROLL) {
            range = getString(R.string.class_group_range_open, group.getRollFrom());
        } else {
            range = getString(R.string.class_group_range,
                    group.getRollFrom(), group.getRollTo());
        }

        String label;
        boolean mine;
        if (ClassGroup.TYPE_LAB_GROUP.equals(group.getType())) {
            label = "      " + getString(R.string.class_group_lab, group.getName(), range);
            mine = group.getName().equals(myLabGroup);
        } else {
            label = getString(R.string.class_group_section, group.getName(), range);
            mine = group.getName().equals(mySection);
        }

        if (mine) {
            label = label + " · " + getString(R.string.class_group_yours);
        }
        return label;
    }

    // Pop-up list of all students whose roll serial is inside the group's range.
    private void showStudents(ClassGroup group) {
        List<String> lines = new ArrayList<>();
        for (Classmate classmate : classmateRepo.getAll()) {
            int serial = RollUtils.serialOf(classmate.getRollNo(), serialDigits);
            if (serial >= group.getRollFrom() && serial <= group.getRollTo()) {
                lines.add(classmate.getRollNo() + "  " + classmate.getFullName());
            }
        }

        if (lines.isEmpty()) {
            Toast.makeText(this, R.string.class_group_no_students, Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.class_group_students_title,
                        group.getName(), lines.size()))
                .setItems(lines.toArray(new String[0]), null)
                .setPositiveButton(R.string.dialog_close, null)
                .show();
    }
}