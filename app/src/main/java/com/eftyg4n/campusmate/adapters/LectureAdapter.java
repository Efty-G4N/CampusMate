package com.eftyg4n.campusmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eftyg4n.campusmate.databinding.ItemLectureBinding;
import com.eftyg4n.campusmate.models.Lecture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LectureAdapter extends RecyclerView.Adapter<LectureAdapter.RowHolder> {

    public interface Listener {
        void onLectureClick(Lecture lecture);
    }

    private final Listener listener;
    private final List<Lecture> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());

    public LectureAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<Lecture> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RowHolder(ItemLectureBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        Lecture lecture = items.get(position);
        holder.binding.textTitle.setText(lecture.getTitle());
        String date = dateFormat.format(new Date(lecture.getLectureDate()));
        boolean hasNote = lecture.getNote() != null && !lecture.getNote().isEmpty();
        holder.binding.textSub.setText(hasNote ? date + "\n" + lecture.getNote() : date);
        holder.binding.getRoot().setOnClickListener(view -> listener.onLectureClick(lecture));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final ItemLectureBinding binding;

        RowHolder(ItemLectureBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
