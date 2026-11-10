package com.eftyg4n.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemBookBinding;
import com.eftyg4n.campusmate.models.Book;

import java.util.ArrayList;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    public interface Listener {
        void onSaveClick(Book book);
    }

    private final Context context;
    private final Listener listener;
    private final List<Book> items = new ArrayList<>();

    public BookAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<Book> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BookViewHolder(ItemBookBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = items.get(position);
        ItemBookBinding b = holder.binding;

        b.textTitle.setText(book.getTitle());
        b.textAuthors.setText(book.getAuthors().isEmpty()
                ? context.getString(R.string.books_unknown_author) : book.getAuthors());
        if (book.getFirstYear() > 0) {
            b.textYear.setVisibility(View.VISIBLE);
            b.textYear.setText(context.getString(R.string.books_first_published,
                    book.getFirstYear()));
        } else {
            b.textYear.setVisibility(View.GONE);
        }

        if (book.getCoverUrl() != null) {
            Glide.with(context).load(book.getCoverUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_image_broken)
                    .into(b.imageCover);
        } else {
            Glide.with(context).clear(b.imageCover);
            b.imageCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        b.buttonSave.setOnClickListener(view -> listener.onSaveClick(book));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        final ItemBookBinding binding;

        BookViewHolder(ItemBookBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
