package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemNoticeBinding;
import com.eftyg4n.campusmate.models.Notice;

import java.util.ArrayList;
import java.util.List;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

    public interface Listener {
        void onDeleteClick(Notice notice);
    }

    private final Context context;
    private final Listener listener;
    private final List<Notice> items = new ArrayList<>();
    private String currentUid;

    public NoticeAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<Notice> newItems, String uid) {
        items.clear();
        items.addAll(newItems);
        currentUid = uid;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoticeViewHolder(ItemNoticeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        Notice n = items.get(position);
        ItemNoticeBinding b = holder.binding;

        b.textTitle.setText(n.getTitle());
        b.textBody.setText(n.getBody());

        String course = n.getCourseCode() == null || n.getCourseCode().isEmpty()
                ? context.getString(R.string.notice_general) : n.getCourseCode();
        String author = n.getAuthorName() == null ? "" : n.getAuthorName();
        CharSequence when = DateUtils.getRelativeTimeSpanString(n.getCreatedAt(),
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        b.textMeta.setText(context.getString(R.string.notice_meta, course, author, when));

        // Only the author sees Delete (the Firestore rules enforce it on the server as well).
        boolean mine = currentUid != null && currentUid.equals(n.getOwnerUid());
        b.buttonDelete.setVisibility(mine ? View.VISIBLE : View.GONE);
        b.buttonDelete.setOnClickListener(view -> listener.onDeleteClick(n));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NoticeViewHolder extends RecyclerView.ViewHolder {
        final ItemNoticeBinding binding;

        NoticeViewHolder(ItemNoticeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
