package com.eftyg4n.campusmate.activities;

import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ActivityAddEditResourceBinding;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Resource;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.ResourceRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.utils.ResourceFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Add or edit a resource. Two sources:
//  FILE: Photo Picker (images) or the system document picker (any file, Storage Access Framework)
//  LINK: a web address
// A picked file comes as a content:// Uri owned by ANOTHER app. We copy it into our own
// storage right away ("staging"), so it keeps working offline and after rotation.
public class AddEditResourceActivity extends AppCompatActivity {

    public static final String EXTRA_RESOURCE_ID = "extra_resource_id";
    public static final String EXTRA_COURSE_ID = "extra_course_id";

    private static final String STATE_PATH = "state_path";
    private static final String STATE_NAME = "state_name";
    private static final String STATE_MIME = "state_mime";
    private static final String STATE_SIZE = "state_size";
    private static final String STATE_LINK_MODE = "state_link_mode";
    private static final String STATE_CATEGORY = "state_category";
    private static final String STATE_COURSE = "state_course";

    private ActivityAddEditResourceBinding binding;
    private ResourceRepository repo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<Course> courses = new ArrayList<>();
    private Resource editing;               // null = new resource
    private boolean linkMode;
    private int categoryIndex = 0;
    private int courseIndex = 0;            // 0 = no course, 1.. = courses.get(index - 1)
    private boolean categoryTouched;
    private boolean saved;

    // The staged copy (already inside our app storage).
    private File stagedFile;
    private String stagedName;
    private String stagedMime;
    private long stagedSize;

    // Photo Picker: no permission needed on any Android version it supports.
    private final ActivityResultLauncher<PickVisualMediaRequest> imagePicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    stageFile(uri);
                }
            });

    // System file picker (SAF). The user chooses; we get a temporary read grant, no permission.
    private final ActivityResultLauncher<String[]> documentPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    stageFile(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditResourceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        repo = new ResourceRepository(this);
        Semester current = new SemesterRepository(this).getCurrent();
        if (current != null) {
            courses = new CourseRepository(this).getBySemester(current.getId());
        }

        String editId = getIntent().getStringExtra(EXTRA_RESOURCE_ID);
        if (editId != null) {
            editing = repo.getById(editId);
            if (editing == null) {
                finish();
                return;
            }
        }

        setupDropdowns();
        binding.toggleSource.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                setLinkMode(checkedId == R.id.buttonSourceLink);
            }
        });
        binding.buttonPickImage.setOnClickListener(view -> imagePicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        binding.buttonPickDocument.setOnClickListener(view ->
                documentPicker.launch(new String[]{"*/*"}));
        binding.buttonSave.setOnClickListener(view -> save());

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else if (editing != null) {
            fillFromResource(editing);
        } else {
            preselectCourseFromIntent();
            setLinkMode(false);
        }
        showSelections();
    }

    // ---------- Dropdowns ----------

    private void setupDropdowns() {
        binding.dropdownCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.resource_category_labels)));
        binding.dropdownCategory.setOnItemClickListener((parent, view, position, id) -> {
            categoryIndex = position;
            categoryTouched = true;
        });

        List<String> courseLabels = new ArrayList<>();
        courseLabels.add(getString(R.string.resource_no_course));
        for (Course course : courses) {
            courseLabels.add(course.getCode() + " — " + course.getTitle());
        }
        binding.dropdownCourse.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, courseLabels));
        binding.dropdownCourse.setOnItemClickListener((parent, view, position, id) ->
                courseIndex = position);
    }

    private void showSelections() {
        String[] labels = getResources().getStringArray(R.array.resource_category_labels);
        binding.dropdownCategory.setText(labels[categoryIndex], false);

        String courseLabel = getString(R.string.resource_no_course);
        if (courseIndex > 0 && courseIndex <= courses.size()) {
            Course course = courses.get(courseIndex - 1);
            courseLabel = course.getCode() + " — " + course.getTitle();
        }
        binding.dropdownCourse.setText(courseLabel, false);
    }

    private void preselectCourseFromIntent() {
        String wanted = getIntent().getStringExtra(EXTRA_COURSE_ID);
        for (int i = 0; i < courses.size() && wanted != null; i++) {
            if (courses.get(i).getId().equals(wanted)) {
                courseIndex = i + 1;
            }
        }
    }

    // ---------- Source switch ----------

    private void setLinkMode(boolean link) {
        linkMode = link;
        binding.layoutFile.setVisibility(link ? View.GONE : View.VISIBLE);
        binding.layoutLink.setVisibility(link ? View.VISIBLE : View.GONE);
        if (link && !categoryTouched && editing == null) {
            categoryIndex = Resource.categoryIndex(Resource.CATEGORY_LINK);
            showSelections();
        }
    }

    // ---------- Picking and staging ----------

    // Copy on a background thread: a big PDF would freeze the screen otherwise.
    private void stageFile(Uri uri) {
        ResourceFileUtils.PickedInfo info = ResourceFileUtils.queryInfo(this, uri);
        binding.progressCopy.setVisibility(View.VISIBLE);
        binding.buttonSave.setEnabled(false);

        executor.execute(() -> {
            File copy = null;
            IOException error = null;
            try {
                copy = ResourceFileUtils.copyToAppStorage(this, uri, info.displayName);
            } catch (IOException e) {
                error = e;
            }
            final File result = copy;
            final boolean failed = error != null;
            runOnUiThread(() -> {
                if (isDestroyed()) {
                    ResourceFileUtils.deleteLocalFile(result == null ? null : result.getPath());
                    return;
                }
                binding.progressCopy.setVisibility(View.GONE);
                binding.buttonSave.setEnabled(true);
                if (failed) {
                    Toast.makeText(this, R.string.resource_toast_copy_failed,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                discardStagedFile(); // the user picked a different file
                stagedFile = result;
                stagedName = info.displayName;
                stagedSize = result.length();
                stagedMime = info.mimeType != null ? info.mimeType
                        : ResourceFileUtils.mimeFromName(info.displayName);
                onStaged(true);
            });
        });
    }

    private void onStaged(boolean autoFill) {
        if (stagedFile == null) {
            binding.imagePreview.setVisibility(View.GONE);
            binding.textFileInfo.setText(R.string.resource_no_file);
            return;
        }
        binding.textFileInfo.setText(getString(R.string.resource_file_info, stagedName,
                ResourceFileUtils.formatSize(this, stagedSize)));

        boolean image = stagedMime != null && stagedMime.startsWith("image/");
        if (image) {
            binding.imagePreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(stagedFile)
                    .placeholder(R.drawable.ic_file_placeholder)
                    .error(R.drawable.ic_image_broken)
                    .into(binding.imagePreview);
        } else {
            Glide.with(this).clear(binding.imagePreview);
            binding.imagePreview.setVisibility(View.GONE);
        }

        if (autoFill) {
            if (binding.editTitle.getText() == null
                    || binding.editTitle.getText().toString().trim().isEmpty()) {
                String name = stagedName;
                int dot = name.lastIndexOf('.');
                binding.editTitle.setText(dot > 0 ? name.substring(0, dot) : name);
            }
            if (!categoryTouched) {
                categoryIndex = Resource.categoryIndex(
                        ResourceFileUtils.guessCategory(stagedMime));
                showSelections();
            }
        }
    }

    private void discardStagedFile() {
        if (stagedFile != null) {
            ResourceFileUtils.deleteLocalFile(stagedFile.getPath());
            stagedFile = null;
        }
    }

    // ---------- Edit mode ----------

    private void fillFromResource(Resource r) {
        binding.toolbar.setTitle(R.string.resource_form_title_edit);
        binding.editTitle.setText(r.getTitle());
        binding.editTopic.setText(r.getTopic());
        binding.editDescription.setText(r.getDescription());
        categoryIndex = Resource.categoryIndex(r.getCategory());
        categoryTouched = true;

        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).getId().equals(r.getCourseId())) {
                courseIndex = i + 1;
            }
        }

        // The source cannot change while editing (delete and add again for that).
        binding.toggleSource.setVisibility(View.GONE);
        if (Resource.SOURCE_LINK.equals(r.getSourceType())
                || Resource.SOURCE_ONLINE_BOOK.equals(r.getSourceType())) {
            setLinkMode(true);
            binding.editLink.setText(r.getUri());
        } else {
            setLinkMode(false);
            binding.layoutPickButtons.setVisibility(View.GONE);
            showEditingFileInfo(r);
        }
    }

    // ---------- Save ----------

    private void save() {
        String title = binding.editTitle.getText() == null ? ""
                : binding.editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            binding.layoutTitle.setError(getString(R.string.resource_error_title));
            return;
        }
        binding.layoutTitle.setError(null);

        Resource r = editing != null ? editing : new Resource();

        if (editing == null) {
            if (linkMode) {
                String link = normalizeLink();
                if (link == null) {
                    return; // error already shown
                }
                r.setSourceType(Resource.SOURCE_LINK);
                r.setUri(link);
            } else {
                if (stagedFile == null) {
                    Toast.makeText(this, R.string.resource_error_no_file,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                r.setSourceType(Resource.SOURCE_FILE);
                r.setLocalPath(stagedFile.getAbsolutePath());
                r.setMimeType(stagedMime);
                r.setSizeBytes(stagedSize);
            }
        } else if (linkMode) {
            String link = normalizeLink();
            if (link == null) {
                return;
            }
            r.setUri(link);
        }

        r.setTitle(title);
        r.setCategory(Resource.CATEGORIES[categoryIndex]);
        r.setCourseId(courseIndex == 0 ? null : courses.get(courseIndex - 1).getId());
        r.setTopic(textOf(binding.editTopic));
        r.setDescription(textOf(binding.editDescription));

        boolean ok = editing != null ? repo.update(r) : repo.insert(r) != null;
        if (!ok) {
            Toast.makeText(this, R.string.resource_toast_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        saved = true; // the staged file now belongs to the saved resource
        Toast.makeText(this, R.string.resource_toast_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    // "example.com/a.pdf" -> "https://example.com/a.pdf"; null if it is not a web address.
    private String normalizeLink() {
        String link = textOf(binding.editLink);
        if (link == null) {
            binding.layoutLink.setError(getString(R.string.resource_error_link));
            return null;
        }
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            link = "https://" + link;
        }
        if (!Patterns.WEB_URL.matcher(link).matches()) {
            binding.layoutLink.setError(getString(R.string.resource_error_link));
            return null;
        }
        binding.layoutLink.setError(null);
        return link;
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText edit) {
        String text = edit.getText() == null ? "" : edit.getText().toString().trim();
        return text.isEmpty() ? null : text;
    }

    // ---------- Rotation / cleanup ----------

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PATH, stagedFile == null ? null : stagedFile.getPath());
        outState.putString(STATE_NAME, stagedName);
        outState.putString(STATE_MIME, stagedMime);
        outState.putLong(STATE_SIZE, stagedSize);
        outState.putBoolean(STATE_LINK_MODE, linkMode);
        outState.putInt(STATE_CATEGORY, categoryIndex);
        outState.putInt(STATE_COURSE, courseIndex);
    }

    private void restoreState(Bundle state) {
        String path = state.getString(STATE_PATH);
        if (path != null && new File(path).exists()) {
            stagedFile = new File(path);
            stagedName = state.getString(STATE_NAME);
            stagedMime = state.getString(STATE_MIME);
            stagedSize = state.getLong(STATE_SIZE);
        }
        categoryIndex = state.getInt(STATE_CATEGORY, 0);
        courseIndex = state.getInt(STATE_COURSE, 0);
        categoryTouched = true;
        if (editing != null) {
            binding.toolbar.setTitle(R.string.resource_form_title_edit);
            binding.toggleSource.setVisibility(View.GONE);
            binding.layoutPickButtons.setVisibility(editing.getSourceType()
                    .equals(Resource.SOURCE_FILE) ? View.GONE : View.VISIBLE);
        }
        setLinkMode(state.getBoolean(STATE_LINK_MODE, false));
        binding.toggleSource.check(linkMode ? R.id.buttonSourceLink : R.id.buttonSourceFile);
        if (editing != null && Resource.SOURCE_FILE.equals(editing.getSourceType())) {
            showEditingFileInfo(editing);
        } else {
            onStaged(false);
        }
    }

    private void showEditingFileInfo(Resource r) {
        binding.textFileInfo.setText(getString(R.string.resource_file_info,
                r.getLocalPath() == null ? "" : new File(r.getLocalPath()).getName(),
                ResourceFileUtils.formatSize(this, r.getSizeBytes())));
    }

    @Override
    protected void onDestroy() {
        // Left the screen without saving: do not leave an orphan copy in storage.
        // (isFinishing() is false during rotation, so the copy survives that.)
        if (isFinishing() && !saved) {
            discardStagedFile();
        }
        executor.shutdown();
        super.onDestroy();
    }
}
