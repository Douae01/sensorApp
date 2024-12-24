package com.example.sensorApp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.ViewHolder> {

    private List<String> emailList;

    public EmailAdapter(List<String> emailList) {
        this.emailList = emailList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_email, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String email = emailList.get(position);
        holder.emailTextView.setText(email);
    }

    @Override
    public int getItemCount() {
        return emailList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView;

        ViewHolder(View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.email_text_view);
        }
    }
}
