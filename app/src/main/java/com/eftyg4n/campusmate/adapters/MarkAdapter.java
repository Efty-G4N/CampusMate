package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemMarkBinding;
import com.eftyg4n.campusmate.models.Mark;
import com.eftyg4n.campusmate.utils.MarksUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MarkAdapter extends RecyclerView.Adapter<MarkAdapter.RowHolder> {

    public interface Listener {
        void onMarkClick(Mark mark);
    }

    private final Context context;
    private final Listener listener;
    private final String[] labels;
    private final List<Mark> items = new ArrayList<>();

    public MarkAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        this.labels = context.getResources().getStringArray(R.array.mark_category_labels);
    }

    public void setData(List<Mark> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    // Label of a stored category key ("CT" -> "Class test").
    public static String labelFor(String[] labels, String key) {
        for (int i = 0; i < Mark.CATEGORIES.length && i < labels.length; i++) {
            if (Mark.CATEGORIES[i].equals(key)) {
                return labels[i];
            }
        }
        return key;
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RowHolder(ItemMarkBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        Mark mark = items.get(position);
        holder.binding.textTitle.setText(mark.getTitle());
        holder.binding.textSub.setText(labelFor(labels, mark.getCategory()));
        holder.binding.textRight.setText(context.getString(R.string.mark_score,
                MarksUtils.plain(mark.getObtained()), MarksUtils.plain(mark.getTotal()),
                String.format(Locale.getDefault(), "%.0f",
                        MarksUtils.percent(mark.getObtained(), mark.getTotal()))));
        holder.binding.getRoot().setOnClickListener(view -> listener.onMarkClick(mark));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final ItemMarkBinding binding;

        RowHolder(ItemMarkBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
