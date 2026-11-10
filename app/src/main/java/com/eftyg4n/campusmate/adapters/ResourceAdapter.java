package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemResourceBinding;
import com.eftyg4n.campusmate.models.Resource;
import com.eftyg4n.campusmate.utils.ResourceFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {

    public interface Listener {
        void onResourceClick(Resource resource);

        void onBookmarkClick(Resource resource);

        void onMoreClick(Resource resource, View anchor);
    }

    private final Context context;
    private final Listener listener;
    private final List<Resource> items = new ArrayList<>();
    private final String[] categoryLabels;

    public ResourceAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        categoryLabels = context.getResources().getStringArray(R.array.resource_category_labels);
    }

    public void setData(List<Resource> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ResourceViewHolder(ItemResourceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        Resource r = items.get(position);
        ItemResourceBinding b = holder.binding;

        b.textTitle.setText(r.getTitle());

        StringBuilder meta = new StringBuilder(categoryLabels[Resource.categoryIndex(r.getCategory())]);
        if (r.getCourseCode() != null) {
            meta.append(" · ").append(r.getCourseCode());
        }
        String size = ResourceFileUtils.formatSize(context, r.getSizeBytes());
        if (!size.isEmpty() && r.hasLocalFile()) {
            meta.append(" · ").append(size);
        }
        b.textMeta.setText(meta);

        // Second line: book author, otherwise the topic. Hide it when empty (rows are reused!).
        String sub = Resource.SOURCE_ONLINE_BOOK.equals(r.getSourceType())
                ? r.getAuthor() : r.getTopic();
        if (sub == null || sub.trim().isEmpty()) {
            b.textSub.setVisibility(View.GONE);
        } else {
            b.textSub.setVisibility(View.VISIBLE);
            b.textSub.setText(sub);
        }

        bindThumbnail(b, r);

        b.buttonBookmark.setImageResource(r.isBookmarked()
                ? R.drawable.ic_star : R.drawable.ic_star_border);
        b.buttonBookmark.setOnClickListener(view -> listener.onBookmarkClick(r));
        b.buttonMore.setOnClickListener(view -> listener.onMoreClick(r, view));
        b.getRoot().setOnClickListener(view -> listener.onResourceClick(r));
    }

    // Glide: loads + caches + resizes the picture off the main thread.
    // placeholder = shown while loading, error = shown if loading fails.
    // A recycled row may still be loading the PREVIOUS item's picture, so every
    // branch either starts a new load (which replaces the old one) or clear()s it.
    private void bindThumbnail(ItemResourceBinding b, Resource r) {
        if (Resource.SOURCE_ONLINE_BOOK.equals(r.getSourceType()) && r.getCoverUrl() != null) {
            Glide.with(context).load(r.getCoverUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .into(b.imageThumb);
        } else if (r.isImage() && r.hasLocalFile()) {
            Glide.with(context).load(new File(r.getLocalPath()))
                    .centerCrop()
                    .placeholder(R.drawable.ic_file_placeholder)
                    .error(R.drawable.ic_image_broken)
                    .into(b.imageThumb);
        } else {
            Glide.with(context).clear(b.imageThumb);
            b.imageThumb.setImageResource(Resource.SOURCE_ONLINE_BOOK.equals(r.getSourceType())
                    ? R.drawable.ic_book_placeholder : R.drawable.ic_file_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ResourceViewHolder extends RecyclerView.ViewHolder {
        final ItemResourceBinding binding;

        ResourceViewHolder(ItemResourceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
