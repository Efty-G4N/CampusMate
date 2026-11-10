package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemAttendanceBinding;
import com.eftyg4n.campusmate.models.Attendance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.RowHolder> {

    public interface Listener {
        void onRecordClick(Attendance record);
    }

    private final Context context;
    private final Listener listener;
    private final List<Attendance> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());

    public AttendanceAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<Attendance> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RowHolder(ItemAttendanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        Attendance record = items.get(position);
        ItemAttendanceBinding b = holder.binding;

        b.textDate.setText(dateFormat.format(new Date(record.getClassDate())));

        boolean hasNote = record.getNote() != null && !record.getNote().isEmpty();
        b.textNote.setText(hasNote ? record.getNote() : "");
        b.textNote.setVisibility(hasNote ? android.view.View.VISIBLE : android.view.View.GONE);

        int label;
        int color;
        if (Attendance.STATUS_ABSENT.equals(record.getStatus())) {
            label = R.string.attendance_status_absent;
            color = R.color.cm_error;
        } else if (Attendance.STATUS_LATE.equals(record.getStatus())) {
            label = R.string.attendance_status_late;
            color = R.color.cm_primary;
        } else {
            label = R.string.attendance_status_present;
            color = R.color.cm_secondary;
        }
        b.textStatus.setText(label);
        b.textStatus.setTextColor(ContextCompat.getColor(context, color));

        b.getRoot().setOnClickListener(view -> listener.onRecordClick(record));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final ItemAttendanceBinding binding;

        RowHolder(ItemAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
