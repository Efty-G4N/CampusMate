package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemTeacherRowBinding;
import com.eftyg4n.campusmate.models.Teacher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherAdapter extends BaseAdapter {

    private static final String TAG = "CampusMate_ListView";

    private final Context context;
    private final LayoutInflater inflater;
    private final List<Teacher> teachers = new ArrayList<>();
    // teacher id -> course codes (example: "CSE 2209, CSE 2201")
    private final Map<String, String> courseCodes = new HashMap<>();

    public TeacherAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setData(List<Teacher> newTeachers, Map<String, String> newCourseCodes) {
        teachers.clear();
        teachers.addAll(newTeachers);
        courseCodes.clear();
        courseCodes.putAll(newCourseCodes);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return teachers.size();
    }

    @Override
    public Teacher getItem(int position) {
        return teachers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Called for every visible row. convertView is an old row that can be reused.
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemTeacherRowBinding binding;
        if (convertView == null) {
            Log.d(TAG, "getView position " + position + ": new row view");
            binding = ItemTeacherRowBinding.inflate(inflater, parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding); // ViewHolder pattern: keep the binding with the row
        } else {
            Log.d(TAG, "getView position " + position + ": recycled row view");
            binding = (ItemTeacherRowBinding) convertView.getTag();
        }

        Teacher teacher = teachers.get(position);

        String shortCode = teacher.getShortCode();
        binding.textShortCode.setText(shortCode == null || shortCode.isEmpty() ? "?" : shortCode);
        binding.textTeacherName.setText(teacher.getName());
        binding.textTeacherRole.setText(roleOf(teacher));

        String codes = courseCodes.get(teacher.getId());
        if (codes == null || codes.isEmpty()) {
            binding.textTeacherCourses.setText(R.string.teacher_row_no_courses);
        } else {
            binding.textTeacherCourses.setText(
                    context.getString(R.string.teacher_row_courses, codes));
        }

        return convertView;
    }

    // "Assistant Professor · CSE"
    private String roleOf(Teacher teacher) {
        StringBuilder role = new StringBuilder();
        appendPart(role, teacher.getDesignation());
        appendPart(role, teacher.getDepartment());
        return role.toString();
    }

    private void appendPart(StringBuilder builder, String part) {
        if (part != null && !part.trim().isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(part);
        }
    }
}