package com.eftyg4n.campusmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.NoticeAdapter;
import com.eftyg4n.campusmate.databinding.ActivityNoticesBinding;
import com.eftyg4n.campusmate.databinding.DialogAddNoticeBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Notice;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.models.UserProfile;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.repositories.UserProfileRepository;
import com.eftyg4n.campusmate.firebase.SyncManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Class notices in Cloud Firestore. The list updates LIVE: a snapshot listener pushes
// every change (new notice from a classmate, a delete) into the screen.
public class NoticesActivity extends AppCompatActivity implements NoticeAdapter.Listener {

    public static final String EXTRA_COURSE_ID = "extra_course_id";

    private ActivityNoticesBinding binding;
    private NoticeAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration registration;

    private List<Course> courses = new ArrayList<>();
    private String courseCode;   // null = every course

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoticesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        Semester current = new SemesterRepository(this).getCurrent();
        if (current != null) {
            courses = new CourseRepository(this).getBySemester(current.getId());
        }
        String courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        for (Course c : courses) {
            if (c.getId().equals(courseId)) {
                courseCode = c.getCode();
                binding.toolbar.setTitle(getString(R.string.notices_title_course, c.getCode()));
            }
        }

        try {
            db = FirebaseFirestore.getInstance();
        } catch (IllegalStateException e) {
            Toast.makeText(this, R.string.auth_error_not_configured, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        adapter = new NoticeAdapter(this, this);
        binding.recyclerNotices.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerNotices.setAdapter(adapter);

        binding.buttonAction.setOnClickListener(view ->
                startActivity(new Intent(this, AuthActivity.class)));
        binding.fabAddNotice.setOnClickListener(view -> showAddDialog());
    }

    // Listen only while the screen is visible (saves battery and data).
    @Override
    protected void onStart() {
        super.onStart();
        if (db != null) {
            attachListener();
        }
    }

    @Override
    protected void onStop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        super.onStop();
    }

    private void attachListener() {
        FirebaseUser user = SyncManager.currentUser();
        if (user == null) {
            showMessage(getString(R.string.notices_sign_in_needed), true);
            binding.fabAddNotice.setVisibility(View.GONE);
            return;
        }
        binding.fabAddNotice.setVisibility(View.VISIBLE);

        // No orderBy: "where + orderBy" on two fields needs a composite index.
        // The list is small, so it is sorted here instead.
        Query query = db.collection("notices");
        if (courseCode != null) {
            query = query.whereEqualTo("courseCode", courseCode);
        }
        query = query.limit(100);

        final String uid = user.getUid();
        registration = query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                boolean denied = error.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
                showMessage(getString(denied ? R.string.notices_error_rules
                        : R.string.notices_error_load), false);
                return;
            }
            if (snapshot == null) {
                return;
            }
            List<Notice> list = new ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                list.add(Notice.fromDocument(doc));
            }
            Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

            adapter.setData(list, uid);
            if (list.isEmpty()) {
                showMessage(getString(R.string.notices_empty), false);
            } else {
                binding.layoutMessage.setVisibility(View.GONE);
                binding.recyclerNotices.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showMessage(String text, boolean showSignIn) {
        binding.recyclerNotices.setVisibility(View.GONE);
        binding.layoutMessage.setVisibility(View.VISIBLE);
        binding.textMessage.setText(text);
        binding.buttonAction.setVisibility(showSignIn ? View.VISIBLE : View.GONE);
    }

    // ---------- Add ----------

    private void showAddDialog() {
        FirebaseUser user = SyncManager.currentUser();
        if (user == null) {
            return;
        }
        DialogAddNoticeBinding form = DialogAddNoticeBinding.inflate(getLayoutInflater());

        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.notice_general));
        int selected = 0;
        for (int i = 0; i < courses.size(); i++) {
            labels.add(courses.get(i).getCode() + " — " + courses.get(i).getTitle());
            if (courses.get(i).getCode().equals(courseCode)) {
                selected = i + 1;
            }
        }
        final int[] choice = {selected};
        form.dropdownCourse.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, labels));
        form.dropdownCourse.setText(labels.get(selected), false);
        form.dropdownCourse.setOnItemClickListener((parent, view, position, id) ->
                choice[0] = position);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notice_add_title)
                .setView(form.getRoot())
                .setPositiveButton(R.string.notice_post, null) // set below: keep it open on errors
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String title = form.editTitle.getText() == null ? ""
                            : form.editTitle.getText().toString().trim();
                    String body = form.editBody.getText() == null ? ""
                            : form.editBody.getText().toString().trim();
                    form.layoutTitle.setError(title.isEmpty()
                            ? getString(R.string.notice_error_title) : null);
                    form.layoutBody.setError(body.isEmpty()
                            ? getString(R.string.notice_error_body) : null);
                    if (title.isEmpty() || body.isEmpty()) {
                        return;
                    }
                    String code = choice[0] == 0 ? "" : courses.get(choice[0] - 1).getCode();
                    postNotice(user, code, title, body);
                    dialog.dismiss();
                });
    }

    private void postNotice(FirebaseUser user, String code, String title, String body) {
        UserProfile profile = new UserProfileRepository(this).getProfile();
        String author = profile != null && profile.getName() != null ? profile.getName()
                : (user.getEmail() == null ? "?" : user.getEmail());

        // If we are offline, Firestore keeps the notice and sends it later; the list
        // already shows it. The failure listener is for real errors (for example rules).
        db.collection("notices")
                .add(Notice.toMap(code, title, body, author, user.getUid()))
                .addOnFailureListener(e -> Toast.makeText(getApplicationContext(),
                        R.string.notice_toast_failed, Toast.LENGTH_LONG).show());
        Toast.makeText(this, R.string.notice_toast_posted, Toast.LENGTH_SHORT).show();
    }

    // ---------- Delete ----------

    @Override
    public void onDeleteClick(Notice notice) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notice_delete_title)
                .setMessage(R.string.notice_delete_message)
                .setPositiveButton(R.string.resource_action_delete, (dialog, which) ->
                        db.collection("notices").document(notice.getId()).delete()
                                .addOnFailureListener(e -> Toast.makeText(
                                        getApplicationContext(), R.string.notice_toast_failed,
                                        Toast.LENGTH_LONG).show()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
