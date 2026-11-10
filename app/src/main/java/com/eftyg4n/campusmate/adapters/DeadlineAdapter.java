package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemDeadlineBinding;
import com.eftyg4n.campusmate.models.Deadline;

import java.util.ArrayList;
import java.util.List;

public class DeadlineAdapter extends RecyclerView.Adapter<DeadlineAdapter.DeadlineViewHolder> {

    public interface Listener {
        void onDeadlineClick(Deadline deadline);

        void onCompletedChanged(Deadline deadline, boolean completed);
    }

    private final Context context;
    private final Listener listener;
    private final List<Deadline> items = new ArrayList<>();
    private final String[] typeLabels;
    private final String[] priorityLabels;

    public DeadlineAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        typeLabels = context.getResources().getStringArray(R.array.deadline_type_labels);
        priorityLabels = context.getResources().getStringArray(R.array.deadline_priority_labels);
    }

    public void setData(List<Deadline> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeadlineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeadlineBinding binding = ItemDeadlineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DeadlineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DeadlineViewHolder holder, int position) {
        Deadline d = items.get(position);
        ItemDeadlineBinding b = holder.binding;
        boolean completed = d.isCompleted();
        boolean overdue = d.isOverdue(System.currentTimeMillis());

        // A recycled row still has the listener of its PREVIOUS item.
        // Remove it before setChecked(), or the old listener fires by mistake.
        b.checkDone.setOnCheckedChangeListener(null);
        b.checkDone.setChecked(completed);
        b.checkDone.setOnCheckedChangeListener((button, checked) -> {
            if (listener != null) {
                listener.onCompletedChanged(d, checked);
            }
        });

        b.textTitle.setText(d.getTitle());
        // Strike-through must be switched on AND off, because rows are reused.
        int flags = b.textTitle.getPaintFlags();
        if (completed) {
            b.textTitle.setPaintFlags(flags | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            b.textTitle.setPaintFlags(flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        String course = d.getCourseCode() != null
                ? d.getCourseCode() : context.getString(R.string.deadline_no_course_short);
        b.textMeta.setText(context.getString(R.string.deadline_meta, course,
                typeLabels[Deadline.typeIndex(d.getType())],
                priorityLabels[d.getPriority()]));

        String due = DateUtils.formatDateTime(context, d.getDueAt(),
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL);
        b.textDue.setText(context.getString(R.string.deadline_due, due));

        // Always set the color in both cases (recycling again).
        int normal = ContextCompat.getColor(context, R.color.cm_on_surface);
        int error = ContextCompat.getColor(context, R.color.cm_error);
        b.textDue.setTextColor(overdue ? error : normal);

        if (overdue) {
            b.textStatus.setText(R.string.deadline_status_overdue);
            b.textStatus.setTextColor(error);
            b.textStatus.setVisibility(View.VISIBLE);
        } else if (completed) {
            b.textStatus.setText(R.string.deadline_status_completed);
            b.textStatus.setTextColor(ContextCompat.getColor(context, R.color.cm_secondary));
            b.textStatus.setVisibility(View.VISIBLE);
        } else {
            b.textStatus.setVisibility(View.GONE);
        }

        int color = d.getCourseColor();
        if (color == 0) {
            color = ContextCompat.getColor(context, R.color.cm_primary);
        }
        b.viewColor.setBackgroundColor(color);

        b.getRoot().setOnClickListener(view -> {
            if (listener != null) {
                listener.onDeadlineClick(d);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DeadlineViewHolder extends RecyclerView.ViewHolder {

        final ItemDeadlineBinding binding;

        DeadlineViewHolder(ItemDeadlineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
