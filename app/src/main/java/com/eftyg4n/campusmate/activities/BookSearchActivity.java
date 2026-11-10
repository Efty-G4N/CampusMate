package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.adapters.BookAdapter;
import com.eftyg4n.campusmate.databinding.ActivityBookSearchBinding;
import com.eftyg4n.campusmate.models.Book;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Resource;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.network.OpenLibraryClient;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.ResourceRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.utils.NetworkUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

// Searches Open Library with Volley (JSON) and shows covers with Glide.
// "Save" puts the book into the Resource Library (a link, no file).
public class BookSearchActivity extends AppCompatActivity implements BookAdapter.Listener {

    public static final String EXTRA_COURSE_ID = "extra_course_id";
    private static final String STATE_QUERY = "state_query";

    private ActivityBookSearchBinding binding;
    private BookAdapter adapter;
    private List<Course> courses = new ArrayList<>();
    private String preselectedCourseId;
    private String lastQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        preselectedCourseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        Semester current = new SemesterRepository(this).getCurrent();
        if (current != null) {
            courses = new CourseRepository(this).getBySemester(current.getId());
        }

        adapter = new BookAdapter(this, this);
        binding.recyclerBooks.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerBooks.setAdapter(adapter);

        binding.buttonSearch.setOnClickListener(view -> startSearch());
        binding.buttonRetry.setOnClickListener(view -> startSearch());
        binding.editQuery.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                startSearch();
                return true;
            }
            return false;
        });

        showMessage(getString(R.string.books_hint_start), false);

        // Rotation recreates the Activity and the results vanish: search again.
        // (Volley's HTTP cache usually answers instantly.)
        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString(STATE_QUERY, "");
            if (!saved.isEmpty()) {
                binding.editQuery.setText(saved);
                startSearch();
            }
        }
    }

    private void startSearch() {
        String query = binding.editQuery.getText() == null ? ""
                : binding.editQuery.getText().toString().trim();
        if (query.isEmpty()) {
            binding.editQuery.setError(getString(R.string.books_error_empty));
            return;
        }
        lastQuery = query;
        hideKeyboard();

        if (!NetworkUtils.isOnline(this)) {
            showMessage(getString(R.string.books_error_offline), true);
            return;
        }

        showLoading();
        OpenLibraryClient.search(this, query, new OpenLibraryClient.Callback() {
            @Override
            public void onSuccess(List<Book> books) {
                if (isDestroyed()) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                adapter.setData(books);
                if (books.isEmpty()) {
                    showMessage(getString(R.string.books_empty, query), false);
                } else {
                    binding.layoutMessage.setVisibility(View.GONE);
                    binding.recyclerBooks.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(int kind) {
                if (isDestroyed()) {
                    return;
                }
                int text = kind == OpenLibraryClient.ERROR_OFFLINE ? R.string.books_error_offline
                        : kind == OpenLibraryClient.ERROR_TIMEOUT ? R.string.books_error_timeout
                        : R.string.books_error_other;
                showMessage(getString(text), true);
            }
        });
    }

    private void showLoading() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.layoutMessage.setVisibility(View.GONE);
        binding.recyclerBooks.setVisibility(View.GONE);
    }

    private void showMessage(String message, boolean showRetry) {
        binding.progress.setVisibility(View.GONE);
        binding.recyclerBooks.setVisibility(View.GONE);
        binding.layoutMessage.setVisibility(View.VISIBLE);
        binding.textMessage.setText(message);
        binding.buttonRetry.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.editQuery.getWindowToken(), 0);
        }
    }

    // ---------- Save to the library ----------

    @Override
    public void onSaveClick(Book book) {
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.resource_no_course));
        int checked = 0;
        for (int i = 0; i < courses.size(); i++) {
            labels.add(courses.get(i).getCode() + " — " + courses.get(i).getTitle());
            if (courses.get(i).getId().equals(preselectedCourseId)) {
                checked = i + 1;
            }
        }

        final int[] choice = {checked};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.books_choose_course)
                .setSingleChoiceItems(labels.toArray(new String[0]), checked,
                        (dialog, which) -> choice[0] = which)
                .setPositiveButton(R.string.books_save, (dialog, which) ->
                        saveBook(book, choice[0] == 0 ? null : courses.get(choice[0] - 1).getId()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void saveBook(Book book, String courseId) {
        Resource r = new Resource();
        r.setSourceType(Resource.SOURCE_ONLINE_BOOK);
        r.setCategory(Resource.CATEGORY_TEXTBOOK);
        r.setTitle(book.getTitle());
        r.setAuthor(book.getAuthors());
        r.setCoverUrl(book.getCoverUrl());
        r.setUri(book.getWebUrl());
        r.setCourseId(courseId);

        boolean ok = new ResourceRepository(this).insert(r) != null;
        Toast.makeText(this, ok ? R.string.books_toast_saved : R.string.resource_toast_save_failed,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, lastQuery);
    }

    @Override
    protected void onDestroy() {
        // No callback may arrive after the screen is gone.
        OpenLibraryClient.cancel(this);
        super.onDestroy();
    }
}
