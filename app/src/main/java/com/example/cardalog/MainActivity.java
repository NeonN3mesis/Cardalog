package com.example.cardalog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the buttons in the layout
        Button scanButton = findViewById(R.id.scanButton);
        Button addContactButton = findViewById(R.id.addContactButton);
        Button generateCsvButton = findViewById(R.id.generateCsvButton);

        // Set up the onClick listener for the scanButton
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the ScanActivity when the scanButton is clicked
                Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                startActivity(intent);
            }
        });

        // Set up the onClick listener for the addContactButton
        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform actions when the addContactButton is clicked
                addNewContact();
            }
        });

        // Set up the onClick listener for the generateCsvButton
        generateCsvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform actions when the generateCsvButton is clicked
                generateCsvFile();
            }
        });
    }

    private void addNewContact() {
        // Implement the functionality to add a new contact
        Toast.makeText(MainActivity.this, "Add Contact clicked!", Toast.LENGTH_SHORT).show();
    }

    private void generateCsvFile() {
        // Implement the functionality to generate a CSV file with the extracted information
        Toast.makeText(MainActivity.this, "Generate CSV clicked!", Toast.LENGTH_SHORT).show();
    }
}