package com.eftyg4n.campusmate.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.activities.AddEditResourceActivity;
import com.eftyg4n.campusmate.activities.BookSearchActivity;
import com.eftyg4n.campusmate.adapters.ResourceAdapter;
import com.eftyg4n.campusmate.databinding.FragmentResourcesBinding;
import com.eftyg4n.campusmate.models.Resource;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.repositories.ResourceRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.services.DownloadService;
import com.eftyg4n.campusmate.utils.NetworkUtils;
import com.eftyg4n.campusmate.utils.ResourceFileUtils;
import com.eftyg4n.campusmate.utils.ResourceOpener;
import com.eftyg4n.campusmate.utils.SimpleTextWatcher;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

// Resource Library list. Used as the "Resources" tab AND inside ResourceListActivity.
public class ResourcesFragment extends Fragment implements ResourceAdapter.Listener {

    private static final String ARG_COURSE_ID = "arg_course_id";

    // courseId == null -> the whole library of the current semester.
    public static ResourcesFragment newInstance(@Nullable String courseId) {
        ResourcesFragment fragment = new ResourcesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_COURSE_ID, courseId);
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentResourcesBinding binding;
    private ResourceRepository repo;
    private ResourceAdapter adapter;
    private String semesterId = "";
    private String courseId;
    private String query = "";
    private String categoryFilter;   // null = all categories
    private int mode = ResourceRepository.MODE_ALL;
    private Resource pendingDownload;

    // Tells us when a download ends, so the list can refresh at once.
    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (binding == null) {
                return;
            }
            loadResources();
            boolean success = intent.getBooleanExtra(DownloadService.EXTRA_SUCCESS, false);
            int reason = intent.getIntExtra(DownloadService.EXTRA_REASON,
                    DownloadService.REASON_NETWORK);
            Toast.makeText(context, success ? getString(R.string.resource_toast_downloaded)
                    : getString(DownloadService.reasonText(reason)), Toast.LENGTH_SHORT).show();
        }
    };

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(requireContext(), R.string.resource_toast_download_silent,
                            Toast.LENGTH_LONG).show();
                }
                // The download works either way; the permission only controls the notification.
                if (pendingDownload != null) {
                    startDownload(pendingDownload);
                    pendingDownload = null;
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResourcesBinding.inflate(inflater, container, false);

        repo = new ResourceRepository(requireContext());
        courseId = getArguments() == null ? null : getArguments().getString(ARG_COURSE_ID);
        Semester current = new SemesterRepository(requireContext()).getCurrent();
        semesterId = current == null ? "" : current.getId();

        // Inside ResourceListActivity the toolbar already shows the title.
        if (courseId != null) {
            binding.textTitle.setVisibility(View.GONE);
        }

        adapter = new ResourceAdapter(requireContext(), this);
        binding.recyclerResources.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerResources.setAdapter(adapter);

        setupSearch();
        setupFilters();

        binding.buttonAdd.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), AddEditResourceActivity.class);
            intent.putExtra(AddEditResourceActivity.EXTRA_COURSE_ID, courseId);
            startActivity(intent);
        });
        binding.buttonBooks.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), BookSearchActivity.class);
            intent.putExtra(BookSearchActivity.EXTRA_COURSE_ID, courseId);
            startActivity(intent);
        });

        return binding.getRoot();
    }

    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                query = editable.toString();
                loadResources();
            }
        });
    }

    private void setupFilters() {
        binding.chipGroupMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipBookmarked) {
                mode = ResourceRepository.MODE_BOOKMARKED;
            } else if (id == R.id.chipRecent) {
                mode = ResourceRepository.MODE_RECENT;
            } else {
                mode = ResourceRepository.MODE_ALL;
            }
            loadResources();
        });

        // "All categories" + the 12 categories (same order as Resource.CATEGORIES).
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.resources_all_categories));
        for (String label : getResources().getStringArray(R.array.resource_category_labels)) {
            labels.add(label);
        }
        binding.dropdownCategoryFilter.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, labels));
        binding.dropdownCategoryFilter.setText(labels.get(0), false);
        binding.dropdownCategoryFilter.setOnItemClickListener((parent, view, position, id) -> {
            categoryFilter = position == 0 ? null : Resource.CATEGORIES[position - 1];
            loadResources();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // NOT_EXPORTED: only our own app (the service uses setPackage) can reach it.
        ContextCompat.registerReceiver(requireContext(), downloadReceiver,
                new IntentFilter(DownloadService.ACTION_DOWNLOAD_FINISHED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        requireContext().unregisterReceiver(downloadReceiver);
        super.onStop();
    }

    // Refresh here: the list changes after add / edit / delete / download.
    @Override
    public void onResume() {
        super.onResume();
        loadResources();
    }

    private void loadResources() {
        if (binding == null) {
            return;
        }
        List<Resource> list = repo.search(semesterId, courseId, query, categoryFilter, mode);
        adapter.setData(list);
        binding.textEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------- Row callbacks ----------

    @Override
    public void onResourceClick(Resource resource) {
        ResourceOpener.open(requireContext(), resource);
    }

    @Override
    public void onBookmarkClick(Resource resource) {
        repo.setBookmarked(resource.getId(), !resource.isBookmarked());
        loadResources();
    }

    @Override
    public void onMoreClick(Resource resource, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.inflate(R.menu.resource_item_menu);

        // Download only makes sense for a web link that is not saved on the phone yet.
        boolean canDownload = Resource.SOURCE_LINK.equals(resource.getSourceType())
                && !resource.hasLocalFile() && resource.getUri() != null
                && resource.getUri().startsWith("http");
        menu.getMenu().findItem(R.id.action_download).setVisible(canDownload);

        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_share) {
                ResourceOpener.share(requireContext(), resource);
            } else if (id == R.id.action_download) {
                download(resource);
            } else if (id == R.id.action_edit) {
                Intent intent = new Intent(requireContext(), AddEditResourceActivity.class);
                intent.putExtra(AddEditResourceActivity.EXTRA_RESOURCE_ID, resource.getId());
                startActivity(intent);
            } else if (id == R.id.action_delete) {
                confirmDelete(resource);
            }
            return true;
        });
        menu.show();
    }

    private void confirmDelete(Resource resource) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.resource_delete_title)
                .setMessage(getString(R.string.resource_delete_message, resource.getTitle()))
                .setPositiveButton(R.string.resource_action_delete, (dialog, which) -> {
                    repo.delete(resource.getId());
                    ResourceFileUtils.deleteLocalFile(resource.getLocalPath());
                    loadResources();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------- Download (Foreground Service) ----------

    private void download(Resource resource) {
        if (!NetworkUtils.isOnline(requireContext())) {
            Toast.makeText(requireContext(), R.string.resource_toast_offline,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Android 13+: without this permission the progress notification stays hidden.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingDownload = resource;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.perm_notifications_title)
                    .setMessage(R.string.perm_downloads_message)
                    .setPositiveButton(R.string.perm_allow, (dialog, which) ->
                            notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS))
                    .setNegativeButton(R.string.perm_not_now, (dialog, which) -> {
                        startDownload(resource);
                        pendingDownload = null;
                    })
                    .show();
            return;
        }
        startDownload(resource);
    }

    private void startDownload(Resource resource) {
        Toast.makeText(requireContext(), R.string.resource_toast_download_started,
                Toast.LENGTH_SHORT).show();
        DownloadService.start(requireContext().getApplicationContext(), resource.getId(),
                resource.getUri(), resource.getTitle());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
