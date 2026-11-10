package com.eftyg4n.campusmate.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eftyg4n.campusmate.databinding.ItemTopicBinding;
import com.eftyg4n.campusmate.models.SyllabusTopic;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.RowHolder> {

    public interface Listener {
        void onTopicClick(SyllabusTopic topic);      // toggles done

        void onTopicLongClick(SyllabusTopic topic);  // delete
    }

    private final Listener listener;
    private final List<SyllabusTopic> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM", Locale.getDefault());

    public TopicAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<SyllabusTopic> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RowHolder(ItemTopicBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        SyllabusTopic topic = items.get(position);
        ItemTopicBinding b = holder.binding;

        b.textTitle.setText(topic.getTitle());
        b.checkDone.setChecked(topic.isDone());
        // A finished topic is struck through and shows the day it was covered.
        if (topic.isDone()) {
            b.textTitle.setPaintFlags(b.textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            b.textSub.setVisibility(View.VISIBLE);
            b.textSub.setText(dateFormat.format(new Date(topic.getDoneAt())));
        } else {
            b.textTitle.setPaintFlags(b.textTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            b.textSub.setVisibility(View.GONE);
        }
        b.getRoot().setOnClickListener(view -> listener.onTopicClick(topic));
        b.getRoot().setOnLongClickListener(view -> {
            listener.onTopicLongClick(topic);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final ItemTopicBinding binding;

        RowHolder(ItemTopicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
