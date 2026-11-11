package com.eftyg4n.campusmate.firebase;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Deadline;
import com.eftyg4n.campusmate.models.Resource;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.models.UserProfile;
import com.eftyg4n.campusmate.notifications.ReminderScheduler;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.DeadlineRepository;
import com.eftyg4n.campusmate.repositories.ResourceRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.repositories.UserProfileRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Two-way sync between SQLite (the truth on this phone) and Firebase Realtime Database.
//
//   cloud layout:  users/{uid}/deadlines/{id}, users/{uid}/resources/{id}, users/{uid}/profile
//
//   PUSH: rows with sync_status = PENDING are uploaded in ONE updateChildren() call,
//         then marked SYNCED.
//   PULL: rows that exist in the cloud but not here (or are newer there) are saved locally.
//
// Rules of the game (kept simple on purpose):
//   * newest updatedAt wins, per row
//   * a delete made here is removed from the cloud too (queued while offline)
//   * a delete made on ANOTHER phone is not copied here (no tombstones yet)
//   * course and semester ids differ between phones, so rows travel with the course CODE
//     (for example CSE 2101) and are matched to the local course when they arrive
//   * picked files stay on the phone: no cloud file storage is used
public final class SyncManager {

    private static final String TAG = "CampusMate_Sync";
    private static final String PREFS = "campusmate_sync";
    private static final String KEY_UID = "last_uid";
    private static final String KEY_LAST_SYNC = "last_sync_at";

    public interface Callback {
        void onDone(int pushed, int pulled);

        void onError(int reason);
    }

    public static final int ERROR_NOT_SIGNED_IN = 1;
    public static final int ERROR_BUSY = 2;
    public static final int ERROR_NOT_CONFIGURED = 3;
    public static final int ERROR_NETWORK_OR_RULES = 4;

    // Bumped whenever the profile on this phone was replaced by the cloud copy,
    // so the open screens can show it without waiting for a restart.
    public static final MutableLiveData<Integer> profileChanged = new MutableLiveData<>();
    private static int profileVersion;

    private static boolean running; // main thread only

    private SyncManager() {
    }

    public static FirebaseUser currentUser() {
        try {
            return FirebaseAuth.getInstance().getCurrentUser();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static long lastSyncAt(Context context) {
        return prefs(context).getLong(KEY_LAST_SYNC, 0);
    }

    // Called right after a successful sign in / register. `done` runs once, on the main
    // thread, when the first sync is over (or after a few seconds if the network is slow),
    // so the next screen already finds the account's profile on the phone.
    public static void onSignedIn(Context context, Runnable done) {
        Context app = context.getApplicationContext();
        AtomicBoolean finished = new AtomicBoolean(false);
        Runnable finish = () -> {
            if (finished.compareAndSet(false, true) && done != null) {
                done.run();
            }
        };
        new Handler(Looper.getMainLooper()).postDelayed(finish, 8000);

        FirebaseUser user = currentUser();
        if (user == null) {
            finish.run();
            return;
        }
        Callback whenSynced = new Callback() {
            @Override
            public void onDone(int pushed, int pulled) {
                finish.run();
            }

            @Override
            public void onError(int reason) {
                finish.run();
            }
        };

        String previous = prefs(app).getString(KEY_UID, null);
        if (user.getUid().equals(previous)) {
            syncNow(app, whenSynced);
            return;
        }

        // A different account on this phone: the profile must become the ACCOUNT's profile,
        // so the cloud copy is looked at before anything is uploaded.
        prefs(app).edit().putString(KEY_UID, user.getUid()).apply();
        try {
            FirebaseDatabase.getInstance().getReference("users").child(user.getUid())
                    .child("profile").get().addOnCompleteListener(task -> {
                        UserProfileRepository repo = new UserProfileRepository(app);
                        if (task.isSuccessful() && task.getResult() != null
                                && task.getResult().exists()) {
                            // The account already has a profile: it replaces whatever is here.
                            restoreProfile(app, task.getResult());
                        } else if (task.isSuccessful() && repo.getProfile() != null) {
                            // A brand new account: it adopts the profile made on this phone.
                            markProfilePending(app);
                        }
                        // The rest of this phone's data goes up to the new account as before.
                        markDataPending(app);
                        syncNow(app, whenSynced);
                    });
        } catch (RuntimeException e) {
            Log.e(TAG, "Realtime Database is not configured", e);
            finish.run();
        }
    }

    // Signing out takes the profile off this phone (it stays in the cloud account), so the
    // next person who signs in, or a guest, never sees it.
    public static void signOut(Context context) {
        Context app = context.getApplicationContext();
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (RuntimeException e) {
            Log.w(TAG, "Sign out: " + e.getMessage());
        }
        UserProfileRepository repo = new UserProfileRepository(app);
        UserProfile profile = repo.getProfile();
        if (profile != null) {
            repo.delete(profile.getId());
        }
        prefs(app).edit().remove(KEY_UID).apply();
        profileVersion++;
        profileChanged.postValue(profileVersion);
    }

    public static void syncNow(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        FirebaseUser user = currentUser();
        if (user == null) {
            fail(callback, ERROR_NOT_SIGNED_IN);
            return;
        }
        if (running) {
            fail(callback, ERROR_BUSY);
            return;
        }

        DatabaseReference root;
        try {
            root = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        } catch (RuntimeException e) {
            Log.e(TAG, "Realtime Database is not configured", e);
            fail(callback, ERROR_NOT_CONFIGURED);
            return;
        }
        running = true;

        DeadlineRepository deadlineRepo = new DeadlineRepository(app);
        ResourceRepository resourceRepo = new ResourceRepository(app);
        UserProfileRepository profileRepo = new UserProfileRepository(app);

        // ----- PUSH: collect everything that is pending -----
        Map<String, Object> updates = new HashMap<>();
        List<Deadline> pendingDeadlines = deadlineRepo.getPendingSync();
        for (Deadline d : pendingDeadlines) {
            updates.put("deadlines/" + d.getId(),
                    deadlineToMap(d, deadlineRepo.getReminderMinutes(d.getId())));
        }
        List<Resource> pendingResources = resourceRepo.getPendingSync();
        for (Resource r : pendingResources) {
            updates.put("resources/" + r.getId(), resourceToMap(r));
        }
        UserProfile profile = profileRepo.getProfile();
        boolean pushProfile = profile != null
                && profile.getSyncStatus() == DatabaseHelper.SYNC_PENDING;
        if (pushProfile) {
            updates.put("profile", profileToMap(profile, user.getEmail()));
        }

        final int pushed = updates.size();
        if (updates.isEmpty()) {
            pull(app, root, 0, callback);
            return;
        }

        root.updateChildren(updates).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Push failed", task.getException());
                running = false;
                fail(callback, ERROR_NETWORK_OR_RULES);
                return;
            }
            for (Deadline d : pendingDeadlines) {
                markSynced(app, DatabaseHelper.TABLE_DEADLINE, d.getId());
            }
            for (Resource r : pendingResources) {
                markSynced(app, DatabaseHelper.TABLE_RESOURCE, r.getId());
            }
            if (pushProfile) {
                markSynced(app, DatabaseHelper.TABLE_USER_PROFILE, profile.getId());
            }
            pull(app, root, pushed, callback);
        });
    }

    // ----- PULL -----

    private static void pull(Context app, DatabaseReference root, int pushed, Callback callback) {
        root.get().addOnCompleteListener(task -> {
            running = false;
            if (!task.isSuccessful() || task.getResult() == null) {
                Log.e(TAG, "Pull failed", task.getException());
                fail(callback, ERROR_NETWORK_OR_RULES);
                return;
            }
            int pulled = 0;
            try {
                pulled = applyRemote(app, task.getResult());
            } catch (RuntimeException e) {
                // One broken row must not hide the fact that the push worked.
                Log.e(TAG, "Applying cloud data failed", e);
            }
            prefs(app).edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply();
            if (callback != null) {
                callback.onDone(pushed, pulled);
            }
        });
    }

    private static int applyRemote(Context app, DataSnapshot all) {
        int count = 0;

        // Course codes -> local course ids, for the CURRENT semester of this phone.
        Semester semester = new SemesterRepository(app).getCurrent();
        Map<String, String> courseIdByCode = new HashMap<>();
        if (semester != null) {
            for (Course c : new CourseRepository(app).getBySemester(semester.getId())) {
                courseIdByCode.put(c.getCode(), c.getId());
            }
        }

        // Deadlines (they need a local semester because of the foreign key).
        DeadlineRepository deadlineRepo = new DeadlineRepository(app);
        if (semester != null) {
            for (DataSnapshot child : all.child("deadlines").getChildren()) {
                String id = child.getKey();
                long remoteUpdated = lng(child, "updatedAt");
                Deadline local = deadlineRepo.getById(id);
                if (local != null && local.getUpdatedAt() >= remoteUpdated) {
                    continue;
                }
                Deadline d = new Deadline();
                d.setId(id);
                d.setSemesterId(semester.getId());
                d.setCourseId(courseIdByCode.get(str(child, "courseCode")));
                d.setTitle(str(child, "title") == null ? "?" : str(child, "title"));
                d.setDescription(str(child, "description"));
                d.setType(str(child, "type") == null ? Deadline.TYPE_ASSIGNMENT
                        : str(child, "type"));
                d.setDueAt(lng(child, "dueAt"));
                d.setPriority((int) lng(child, "priority"));
                d.setStatus(str(child, "status") == null ? Deadline.STATUS_PENDING
                        : str(child, "status"));
                d.setCompletedAt(lng(child, "completedAt"));
                d.setCreatedAt(lng(child, "createdAt"));
                d.setUpdatedAt(remoteUpdated);

                List<Integer> minutes = new ArrayList<>();
                for (DataSnapshot m : child.child("reminders").getChildren()) {
                    Long value = m.getValue(Long.class);
                    if (value != null) {
                        minutes.add(value.intValue());
                    }
                }
                deadlineRepo.upsertSynced(d, minutes);
                // Alarms live only on this phone, so plan them again.
                ReminderScheduler.cancel(app, id);
                if (!d.isCompleted()) {
                    ReminderScheduler.schedule(app, d, minutes);
                }
                count++;
            }
        }

        // Resources (links and online books).
        ResourceRepository resourceRepo = new ResourceRepository(app);
        for (DataSnapshot child : all.child("resources").getChildren()) {
            String id = child.getKey();
            long remoteUpdated = lng(child, "updatedAt");
            Resource local = resourceRepo.getById(id);
            if (local != null && local.getUpdatedAt() >= remoteUpdated) {
                continue;
            }
            Resource r = new Resource();
            r.setId(id);
            r.setCourseId(courseIdByCode.get(str(child, "courseCode")));
            r.setTitle(str(child, "title") == null ? "?" : str(child, "title"));
            r.setTopic(str(child, "topic"));
            r.setDescription(str(child, "description"));
            r.setCategory(str(child, "category") == null ? Resource.CATEGORY_OTHER
                    : str(child, "category"));
            r.setSourceType(str(child, "sourceType") == null ? Resource.SOURCE_LINK
                    : str(child, "sourceType"));
            r.setUri(str(child, "uri"));
            r.setAuthor(str(child, "author"));
            r.setCoverUrl(str(child, "coverUrl"));
            Boolean bookmarked = child.child("bookmarked").getValue(Boolean.class);
            r.setBookmarked(bookmarked != null && bookmarked);
            r.setCreatedAt(lng(child, "createdAt"));
            r.setUpdatedAt(remoteUpdated);
            resourceRepo.upsertSynced(r);
            count++;
        }

        // Profile: the account's copy wins when this phone has none, or when the copy in the
        // cloud is newer (it was edited from another phone) and nothing here is waiting to upload.
        UserProfileRepository profileRepo = new UserProfileRepository(app);
        DataSnapshot remoteProfile = all.child("profile");
        if (remoteProfile.exists()) {
            UserProfile local = profileRepo.getProfile();
            boolean localIsSynced = local != null
                    && local.getSyncStatus() == DatabaseHelper.SYNC_SYNCED;
            if (local == null
                    || (localIsSynced && lng(remoteProfile, "updatedAt") > local.getUpdatedAt())) {
                if (restoreProfile(app, remoteProfile)) {
                    count++;
                }
            }
        }
        return count;
    }

    // Writes the cloud profile into the one local profile row (created if missing).
    private static boolean restoreProfile(Context app, DataSnapshot remote) {
        UserProfileRepository repo = new UserProfileRepository(app);
        UserProfile p = repo.getProfile();
        if (p == null) {
            p = new UserProfile();
        }
        p.setName(str(remote, "name"));
        p.setEmail(str(remote, "email"));
        p.setUniversity(str(remote, "university"));
        p.setDepartment(str(remote, "department"));
        p.setSession(str(remote, "session"));
        p.setYearNo((int) lng(remote, "yearNo"));
        p.setTermNo((int) lng(remote, "termNo"));
        p.setSection(str(remote, "section"));
        p.setLabGroup(str(remote, "labGroup"));
        p.setRollNo(str(remote, "rollNo"));
        long digits = lng(remote, "rollSerialDigits");
        p.setRollSerialDigits(digits > 0 ? (int) digits : 3);
        if (!repo.save(p)) {
            return false;
        }
        // It came FROM the cloud, so there is nothing to upload.
        markSynced(app, DatabaseHelper.TABLE_USER_PROFILE, p.getId());
        profileVersion++;
        profileChanged.postValue(profileVersion);
        return true;
    }

    // ----- Deleting -----

    // Called by the repositories after a local delete. With disk persistence on,
    // Firebase keeps this request and sends it later if the phone is offline now.
    public static void deleteRemote(Context context, String node, String id) {
        try {
            FirebaseUser user = currentUser();
            if (user == null) {
                return;
            }
            FirebaseDatabase.getInstance().getReference("users").child(user.getUid())
                    .child(node).child(id).removeValue();
        } catch (RuntimeException e) {
            Log.w(TAG, "Remote delete skipped: " + e.getMessage());
        }
    }

    // ----- Mapping: model -> map for Firebase -----

    private static Map<String, Object> deadlineToMap(Deadline d, List<Integer> reminders) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", d.getTitle());
        map.put("description", d.getDescription());
        map.put("type", d.getType());
        map.put("dueAt", d.getDueAt());
        map.put("priority", d.getPriority());
        map.put("status", d.getStatus());
        map.put("completedAt", d.getCompletedAt());
        map.put("courseCode", d.getCourseCode());
        map.put("createdAt", d.getCreatedAt());
        map.put("updatedAt", d.getUpdatedAt());
        map.put("reminders", new ArrayList<>(reminders));
        return map;
    }

    private static Map<String, Object> resourceToMap(Resource r) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", r.getTitle());
        map.put("topic", r.getTopic());
        map.put("description", r.getDescription());
        map.put("category", r.getCategory());
        map.put("sourceType", r.getSourceType());
        map.put("uri", r.getUri());
        map.put("author", r.getAuthor());
        map.put("coverUrl", r.getCoverUrl());
        map.put("bookmarked", r.isBookmarked());
        map.put("courseCode", r.getCourseCode());
        map.put("createdAt", r.getCreatedAt());
        map.put("updatedAt", r.getUpdatedAt());
        return map;
    }

    private static Map<String, Object> profileToMap(UserProfile p, String email) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", p.getName());
        map.put("email", email);
        map.put("university", p.getUniversity());
        map.put("department", p.getDepartment());
        map.put("session", p.getSession());
        map.put("yearNo", p.getYearNo());
        map.put("termNo", p.getTermNo());
        map.put("section", p.getSection());
        map.put("labGroup", p.getLabGroup());
        map.put("rollNo", p.getRollNo());
        map.put("rollSerialDigits", p.getRollSerialDigits());
        map.put("updatedAt", p.getUpdatedAt());
        return map;
    }

    // ----- Small helpers -----

    // getValue(Long.class) is null when the field is missing: we turn that into 0.
    private static long lng(DataSnapshot snapshot, String key) {
        Long value = snapshot.child(key).getValue(Long.class);
        return value == null ? 0 : value;
    }

    private static String str(DataSnapshot snapshot, String key) {
        return snapshot.child(key).getValue(String.class);
    }

    private static void markSynced(Context app, String table, String id) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_STATUS, DatabaseHelper.SYNC_SYNCED);
        DatabaseHelper.getInstance(app).getWritableDatabase().update(table, values,
                DatabaseHelper.COL_ID + "=?", new String[]{id});
    }

    private static void markDataPending(Context app) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_STATUS, DatabaseHelper.SYNC_PENDING);
        DatabaseHelper helper = DatabaseHelper.getInstance(app);
        helper.getWritableDatabase().update(DatabaseHelper.TABLE_DEADLINE, values, null, null);
        helper.getWritableDatabase().update(DatabaseHelper.TABLE_RESOURCE, values, null, null);
    }

    private static void markProfilePending(Context app) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_STATUS, DatabaseHelper.SYNC_PENDING);
        DatabaseHelper.getInstance(app).getWritableDatabase()
                .update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null);
    }

    private static void fail(Callback callback, int reason) {
        if (callback != null) {
            callback.onError(reason);
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
