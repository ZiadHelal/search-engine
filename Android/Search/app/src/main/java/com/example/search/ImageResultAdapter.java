package com.example.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class ImageResultAdapter extends RecyclerView.Adapter<ImageResultAdapter.ImageResultViewHolder> {
    private ArrayList<ImageResult> ArrayList;

    public static class ImageResultViewHolder extends RecyclerView.ViewHolder {
        public ImageView Image;

        public ImageResultViewHolder(@NonNull View itemView) {
            super(itemView);
            Image = itemView.findViewById(R.id.image_iv);
        }
    }

    public ImageResultAdapter(ArrayList<ImageResult> arrayList) {
        ArrayList = arrayList;
    }

    @NonNull
    @Override
    public ImageResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
        ImageResultAdapter.ImageResultViewHolder imageResultViewHolder = new ImageResultViewHolder(view);
        return imageResultViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ImageResultViewHolder holder, int position) {
        ImageResult imageResult = ArrayList.get(position);
        if (imageResult.getImageURL() != null) {
            Glide.with(holder.Image.getContext())
                    .load(imageResult.getImageURL())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.Image);
        }
    }

    @Override
    public int getItemCount() {
        return ArrayList.size();
    }

}
