package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemCourseBinding;
import com.eftyg4n.campusmate.models.Course;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private static final String TAG = "CampusMate_Recycler";

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    private final Context context;
    private final OnCourseClickListener listener;
    private final List<Course> courses = new ArrayList<>();
    // course id -> teacher short codes (example: "SA, EK")
    private final Map<String, String> teacherCodes = new HashMap<>();

    private int createdCount = 0; // TEMPORARY: only to see view recycling in Logcat

    public CourseAdapter(Context context, OnCourseClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<Course> newCourses, Map<String, String> newTeacherCodes) {
        courses.clear();
        courses.addAll(newCourses);
        teacherCodes.clear();
        teacherCodes.putAll(newTeacherCodes);
        notifyDataSetChanged();
    }

    // Called only when RecyclerView needs a NEW row view.
    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        createdCount++;
        Log.d(TAG, "onCreateViewHolder: new row view #" + createdCount);
        ItemCourseBinding binding = ItemCourseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CourseViewHolder(binding);
    }

    // Called every time a row appears: new view or a recycled one.
    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        Log.d(TAG, "onBindViewHolder: position " + position + " (" + course.getCode() + ")");

        holder.binding.viewColor.setBackgroundColor(course.getColor());
        holder.binding.textCode.setText(course.getCode());
        holder.binding.textTitle.setText(course.getTitle());
        holder.binding.textMeta.setText(context.getString(R.string.course_meta,
                typeLabel(course.getType()), String.valueOf(course.getCredits())));

        String codes = teacherCodes.get(course.getId());
        if (codes == null || codes.isEmpty()) {
            holder.binding.textTeachers.setText(R.string.course_no_teachers);
        } else {
            holder.binding.textTeachers.setText(
                    context.getString(R.string.course_teachers, codes));
        }

        holder.binding.getRoot().setOnClickListener(view -> {
            if (listener != null) {
                listener.onCourseClick(course);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    private String typeLabel(String type) {
        if (Course.TYPE_LAB.equals(type)) {
            return context.getString(R.string.course_type_lab);
        }
        if (Course.TYPE_CUSTOM.equals(type)) {
            return context.getString(R.string.course_type_custom);
        }
        return context.getString(R.string.course_type_theory);
    }

    // Holds the views of ONE row, so findViewById is not repeated while scrolling.
    static class CourseViewHolder extends RecyclerView.ViewHolder {

        final ItemCourseBinding binding;

        CourseViewHolder(ItemCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}