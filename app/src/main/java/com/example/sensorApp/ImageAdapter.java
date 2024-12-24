package com.example.sensorApp;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    // Interface for handling image deletion
    public interface OnImageClickListener {
        void onDeleteClick(int position);
    }

    private List<Bitmap> imageList;
    private OnImageClickListener listener;

    // Constructor to initialize the adapter with the image list and click listener
    public ImageAdapter(List<Bitmap> imageList, OnImageClickListener listener) {
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each image item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // Bind the image to the ImageView in the ViewHolder
        Bitmap image = imageList.get(position);
        holder.imageThumbnail.setImageBitmap(image);

        // Set the delete button click listener
        holder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    // ViewHolder class to hold the views for each item
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageThumbnail;
        public Button buttonDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumbnail = itemView.findViewById(R.id.imageThumbnail);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
