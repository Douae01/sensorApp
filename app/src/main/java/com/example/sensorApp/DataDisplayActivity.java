package com.example.sensorApp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DataDisplayActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView noDataText;
    private EmailAdapter emailAdapter;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);

        recyclerView = findViewById(R.id.recycler_view);
        noDataText = findViewById(R.id.no_data_text);
        dbHelper = new DBHelper(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch email data from the database
        List<String> emailList = dbHelper.getAllEmails();

        if (emailList.isEmpty()) {
            noDataText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noDataText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            emailAdapter = new EmailAdapter(emailList);
            recyclerView.setAdapter(emailAdapter);
        }
    }
}
