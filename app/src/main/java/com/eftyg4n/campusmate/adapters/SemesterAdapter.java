package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemSemesterBinding;
import com.eftyg4n.campusmate.models.Semester;

import java.util.ArrayList;
import java.util.List;

public class SemesterAdapter extends RecyclerView.Adapter<SemesterAdapter.RowHolder> {

    public interface Listener {
        void onSemesterClick(Semester semester);
    }

    private final Context context;
    private final Listener listener;
    private final List<Semester> items = new ArrayList<>();

    public SemesterAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<Semester> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RowHolder(ItemSemesterBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        Semester s = items.get(position);
        holder.binding.textTitle.setText(s.getName());

        StringBuilder sub = new StringBuilder();
        if (s.getSession() != null && !s.getSession().isEmpty()) {
            sub.append(s.getSession());
        }
        if (s.getYearNo() > 0 && s.getTermNo() > 0) {
            if (sub.length() > 0) {
                sub.append(" · ");
            }
            sub.append(context.getString(R.string.semester_year_term, s.getYearNo(), s.getTermNo()));
        }
        if (s.getSection() != null && !s.getSection().isEmpty()) {
            if (sub.length() > 0) {
                sub.append(" · ");
            }
            sub.append(context.getString(R.string.semester_section_line, s.getSection()));
        }
        holder.binding.textSub.setText(sub.length() == 0 ? "-" : sub);

        int flag = s.isCurrent() ? R.string.semester_flag_current
                : s.isArchived() ? R.string.semester_flag_archived : 0;
        holder.binding.textRight.setText(flag == 0 ? "" : context.getString(flag));
        holder.binding.getRoot().setAlpha(s.isArchived() ? 0.6f : 1f);
        holder.binding.getRoot().setOnClickListener(view -> listener.onSemesterClick(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final ItemSemesterBinding binding;

        RowHolder(ItemSemesterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
