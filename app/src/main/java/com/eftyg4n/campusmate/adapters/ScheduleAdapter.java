package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemRoutineClassBinding;
import com.eftyg4n.campusmate.databinding.ItemRoutineDayBinding;
import com.eftyg4n.campusmate.models.Routine;
import com.eftyg4n.campusmate.utils.ScheduleUtils;

import java.util.ArrayList;
import java.util.List;

// One RecyclerView with TWO kinds of rows: a day header and a class card.
public class ScheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DAY = 0;
    private static final int TYPE_CLASS = 1;

    // One list row: either a day title (routine == null) or a class (dayTitle == null).
    private static class Row {
        final String dayTitle;
        final Routine routine;

        Row(String dayTitle, Routine routine) {
            this.dayTitle = dayTitle;
            this.routine = routine;
        }
    }

    private final Context context;
    private final List<Row> rows = new ArrayList<>();

    public ScheduleAdapter(Context context) {
        this.context = context;
    }

    // routines must be sorted by day. With showDayHeaders, a header row is added
    // before the first class of each day (weekly view).
    public void setData(List<Routine> routines, boolean showDayHeaders) {
        rows.clear();
        int lastDay = -1;
        for (int i = 0; i < routines.size(); i++) {
            Routine routine = routines.get(i);
            if (showDayHeaders && routine.getDayOfWeek() != lastDay) {
                lastDay = routine.getDayOfWeek();
                rows.add(new Row(ScheduleUtils.dayName(context, lastDay), null));
            }
            rows.add(new Row(null, routine));
        }
        notifyDataSetChanged();
    }

    // RecyclerView asks this first, then calls onCreateViewHolder with the answer.
    @Override
    public int getItemViewType(int position) {
        return rows.get(position).routine == null ? TYPE_DAY : TYPE_CLASS;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_DAY) {
            return new DayViewHolder(ItemRoutineDayBinding.inflate(inflater, parent, false));
        }
        return new ClassViewHolder(ItemRoutineClassBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof DayViewHolder) {
            ((DayViewHolder) holder).binding.textDay.setText(row.dayTitle);
        } else {
            bindClass((ClassViewHolder) holder, row.routine);
        }
    }

    private void bindClass(ClassViewHolder holder, Routine routine) {
        ItemRoutineClassBinding b = holder.binding;

        b.viewColor.setBackgroundColor(routine.getCourseColor());
        b.textStart.setText(ScheduleUtils.formatMinute(context, routine.getStartMinute()));
        b.textEnd.setText(ScheduleUtils.formatMinute(context, routine.getEndMinute()));
        if (routine.getStartPeriod() == routine.getEndPeriod()) {
            b.textPeriod.setText(context.getString(R.string.schedule_period_single,
                    routine.getStartPeriod()));
        } else {
            b.textPeriod.setText(context.getString(R.string.schedule_period_range,
                    routine.getStartPeriod(), routine.getEndPeriod()));
        }

        b.textCode.setText(routine.getCourseCode());
        b.textTitle.setText(routine.getCourseTitle());

        // A recycled row may still hold the old state, so every optional view
        // is set in BOTH cases (shown or hidden).
        if (routine.getRoom() == null || routine.getRoom().isEmpty()) {
            b.textRoom.setVisibility(View.GONE);
        } else {
            b.textRoom.setText(context.getString(R.string.schedule_room, routine.getRoom()));
            b.textRoom.setVisibility(View.VISIBLE);
        }

        if (routine.getTeacherCodes() == null || routine.getTeacherCodes().isEmpty()) {
            b.textTeachers.setVisibility(View.GONE);
        } else {
            b.textTeachers.setText(context.getString(R.string.schedule_teachers,
                    ScheduleUtils.formatTeachers(context, routine.getTeacherCodes())));
            b.textTeachers.setVisibility(View.VISIBLE);
        }

        if (Routine.TYPE_LAB.equals(routine.getClassType())) {
            if (routine.getLabGroup() == null) {
                b.textType.setText(R.string.schedule_type_lab);
            } else {
                b.textType.setText(context.getString(R.string.schedule_type_lab_group,
                        routine.getLabGroup()));
            }
        } else {
            b.textType.setText(R.string.schedule_type_theory);
        }

        boolean cancelled = Routine.STATUS_CANCELLED.equals(routine.getStatus());
        if (cancelled) {
            b.textStatus.setText(R.string.schedule_status_cancelled);
            b.textStatus.setVisibility(View.VISIBLE);
        } else if (Routine.STATUS_RESCHEDULED.equals(routine.getStatus())) {
            b.textStatus.setText(R.string.schedule_status_rescheduled);
            b.textStatus.setVisibility(View.VISIBLE);
        } else {
            b.textStatus.setVisibility(View.GONE);
        }
        b.getRoot().setAlpha(cancelled ? 0.5f : 1f);

        if (routine.getNote() == null || routine.getNote().isEmpty()) {
            b.textNote.setVisibility(View.GONE);
        } else {
            b.textNote.setText(routine.getNote());
            b.textNote.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {

        final ItemRoutineDayBinding binding;

        DayViewHolder(ItemRoutineDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {

        final ItemRoutineClassBinding binding;

        ClassViewHolder(ItemRoutineClassBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}