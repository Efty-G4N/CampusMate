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
- ⬜ ListView + ArrayAdapter + item click
- ⬜ Custom ListView (row XML, custom adapter, getView, LayoutInflater)
- ✅ RecyclerView (adapter, ViewHolder, onCreateViewHolder, onBindViewHolder, getItemCount)
- 🟩 LayoutManager
- ✅ View recycling

## Lecture 10
- ⬜ Service (foreground, notification, progress)
- ⬜ WorkManager
- ⬜ BroadcastReceiver: Airplane mode
- ⬜ BroadcastReceiver: Battery Saver
- ⬜ Image picker and document picker (content URI)
- ⬜ Notifications (channels, tap navigation)
- ⬜ Runtime permissions

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
- ✅ Home counter state kept on rotation (onSaveInstanceState), waiting for phone test

## Phase 2: Local data (SQLite)
- ✅ Database design (tables and relationships)
- ✅ DatabaseHelper (SQLiteOpenHelper, foreign keys, migrations)
- ✅ Semester and Course models and repositories (CRUD, search, current semester)
- ✅ Teacher model and repository, CourseTeacher links (assign, JOIN queries)
- ✅ Initial KUET data (2-2 term courses, teachers with room, phone, email; routine later)
- ✅ Customizable sections and lab groups (class_group, roll serial ranges)
- ✅ Classmate list loaded from assets CSV
- ✅ UserProfile model and repository (roll number, section, lab group)

## Phase 3: Main screens
- ✅ Courses screen (RecyclerView, course cards, teacher short codes)
- ✅ Course detail screen (course header, teachers with room, phone, email; workspace buttons)
- ⬜ Profile screen (form, validation, auto-detect from roll number)