package com.example.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TextResultAdapter extends RecyclerView.Adapter<TextResultAdapter.TextResultViewHolder> {
    private ArrayList<TextResult> ArrayList;
    private OnItemClickListener Listener;

    public interface OnItemClickListener {
        void onItemClick(int Position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        Listener = listener;
    }

    public static class TextResultViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView url;
        public TextView snippet;

        public TextResultViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            title = itemView.findViewById(R.id.title_tv);
            url = itemView.findViewById(R.id.url_tv);
            snippet = itemView.findViewById(R.id.snippet_tv);

            title.setSelected(true);
            url.setSelected(true);
            snippet.setSelected(true);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    public TextResultAdapter(ArrayList<TextResult> arrayList) {
        ArrayList = arrayList;
    }

    @NonNull
    @Override
    public TextResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_item, parent, false);
        TextResultViewHolder textResultViewHolder = new TextResultViewHolder(view, Listener);
        return textResultViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TextResultViewHolder holder, int position) {
        TextResult textResult = ArrayList.get(position);
        holder.title.setText(textResult.getTitle());
        holder.url.setText(textResult.getUrl());
        holder.snippet.setText(textResult.getSnippet());
    }

    @Override
    public int getItemCount() {
        return ArrayList.size();
    }
}
