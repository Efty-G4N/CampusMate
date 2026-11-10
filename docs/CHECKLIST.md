# CampusMate — Lecture Topic Checklist

Status: ⬜ Not started | 🟨 In progress | 🟩 Implemented | ✅ Tested

Note: Lecture 8-11 here mean course Lecture 1-4.

## Lecture 8
- ✅ Project folder structure
- ✅ Activity lifecycle
- ✅ Explicit Intent
- ✅ Implicit Intent
- ✅ Button click handling
- ✅ ConstraintLayout
- ✅ RelativeLayout
- ✅ LinearLayout
- ✅ View Binding

## Lecture 9
- ✅ ListView + ArrayAdapter + item click
- ✅ Custom ListView (row XML, custom adapter, getView, LayoutInflater)
- ✅ RecyclerView (adapter, ViewHolder, onCreateViewHolder, onBindViewHolder, getItemCount)
- ✅ LayoutManager
- ✅ View recycling

## Lecture 10
- ⬜ Service (foreground, notification, progress)
- ✅ WorkManager
- ✅ BroadcastReceiver: Airplane mode (context-registered in HomeFragment onStart/onStop, status card)
- ✅ BroadcastReceiver: Battery Saver (ACTION_POWER_SAVE_MODE_CHANGED, status card)
- ⬜ Image picker and document picker (content URI)
- ✅ Notifications (channels, tap navigation)
- ✅ Runtime permissions

## Lecture 11
- ⬜ Firebase Authentication (register, login, logout, reset)
- ⬜ Realtime Database CRUD
- ⬜ Firestore create/read (update/delete)
- ⬜ Firebase Storage upload/retrieve
- ⬜ Volley HTTP request + JSON parsing (Open Library)
- ⬜ Glide (placeholder, error)

---

# Project Progress

## Phase 1: Foundation
- ✅ Package structure (activities, fragments, adapters, models, database, repositories, firebase, storage, network, services, receivers, notifications, utils, dialogs)
- ✅ MainActivity with BottomNavigationView and 5 fragments (Home, Courses, Schedule, Resources, Profile)
- ✅ WelcomeActivity (RelativeLayout, explicit Intent)
- ✅ Resources screen (LinearLayout, implicit Intents: open link, share text)
- ✅ Theme and colors (light and dark)
- ✅ Hardcoded strings moved to strings.xml
- ✅ Home counter state kept on rotation (onSaveInstanceState)

## Phase 2: Local data (SQLite)
- ✅ Database design (tables and relationships)
- ✅ DatabaseHelper (SQLiteOpenHelper, foreign keys, migrations)
- ✅ Semester and Course models and repositories (CRUD, search, current semester)
- ✅ Teacher model and repository, CourseTeacher links (assign, JOIN queries)
- ✅ Initial KUET data (2-2 term courses, teachers with room, phone, email; routine later)
- ✅ Customizable sections and lab groups (class_group, roll serial ranges)
- ✅ Classmate list loaded from assets CSV
- ✅ UserProfile model and repository (roll number, section, lab group)
- ✅ Routine table (DB version 4), Routine model and RoutineRepository (JOIN with course, filter by section and lab group)
- ✅ Section B weekly routine seed (22 slots; B2 view shows 18)

## Phase 3: Main screens
- ✅ Courses screen (RecyclerView, course cards, teacher short codes)
- ✅ Course detail screen (course header, teachers with room, phone, email; workspace buttons)
- ✅ Profile screen (form, validation, auto-detect from roll number)
- ✅ Teacher directory (custom ListView, search, contact dialog)
- ✅ Courses list/grid switch (LinearLayoutManager and GridLayoutManager)
- ✅ Sections and lab groups screen (simple ListView, ArrayAdapter, item click, student list dialog)
- ✅ Housekeeping: TeacherAdapter moved to adapters/; seeding moved to CampusMateApp (Application class); temporary test block removed from MainActivity
- ✅ Schedule screen (RecyclerView with two view types: day header and class card; daily/weekly toggle; next class card; filtered by section and lab group)
- ✅ Course add / edit / delete (form validation, type dropdown with ArrayAdapter, color swatches, duplicate code check, delete confirm dialog, ActivityResult for delete)
- ✅ Home system status card (live Airplane mode and Battery Saver state)

## Phase 4: Deadlines and reminders
- ✅ Deadline and Reminder tables (DB version 5), models, DeadlineRepository (transaction, JOIN, filters)
- ✅ Deadlines screen (RecyclerView, filter chips, complete checkbox, course-filtered view)
- ✅ Add / edit / delete deadline (Material date and time pickers, multiple reminders, validation)
- ✅ Reminder scheduling with WorkManager + notification with tap navigation
- ✅ Home deadline card