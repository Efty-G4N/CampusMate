package com.eftyg4n.campusmate.firebase;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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

    // Called right after a successful sign in / register.
    public static void onSignedIn(Context context) {
        Context app = context.getApplicationContext();
        FirebaseUser user = currentUser();
        if (user == null) {
            return;
        }
        // A different account on the same phone: this phone's data must be uploaded
        // to the NEW account too, so everything becomes "pending" again.
        String previous = prefs(app).getString(KEY_UID, null);
        if (!user.getUid().equals(previous)) {
            markEverythingPending(app);
            prefs(app).edit().putString(KEY_UID, user.getUid()).apply();
        }
        syncNow(app, null);
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

        // Profile: only restored when this phone has none (a fresh install).
        UserProfileRepository profileRepo = new UserProfileRepository(app);
        DataSnapshot remoteProfile = all.child("profile");
        if (remoteProfile.exists() && profileRepo.getProfile() == null) {
            UserProfile p = new UserProfile();
            p.setName(str(remoteProfile, "name"));
            p.setUniversity(str(remoteProfile, "university"));
            p.setDepartment(str(remoteProfile, "department"));
            p.setSession(str(remoteProfile, "session"));
            p.setYearNo((int) lng(remoteProfile, "yearNo"));
            p.setTermNo((int) lng(remoteProfile, "termNo"));
            p.setSection(str(remoteProfile, "section"));
            p.setLabGroup(str(remoteProfile, "labGroup"));
            p.setRollNo(str(remoteProfile, "rollNo"));
            long digits = lng(remoteProfile, "rollSerialDigits");
            p.setRollSerialDigits(digits > 0 ? (int) digits : 3);
            if (profileRepo.save(p)) {
                count++;
            }
        }
        return count;
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

    private static void markEverythingPending(Context app) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_STATUS, DatabaseHelper.SYNC_PENDING);
        DatabaseHelper helper = DatabaseHelper.getInstance(app);
        helper.getWritableDatabase().update(DatabaseHelper.TABLE_DEADLINE, values, null, null);
        helper.getWritableDatabase().update(DatabaseHelper.TABLE_RESOURCE, values, null, null);
        helper.getWritableDatabase().update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null);
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
