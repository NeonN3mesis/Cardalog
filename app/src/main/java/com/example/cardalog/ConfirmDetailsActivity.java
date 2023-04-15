package com.example.cardalog;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class ConfirmDetailsActivity extends AppCompatActivity {
    private static final String TAG = "ConfirmDetailsActivity";

    // Add EditText fields as member variables
    private EditText nameEditText;
    private EditText phoneEditText;
    private EditText emailEditText;
    private EditText companyEditText;
    private EditText addressEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_details);

        Intent intent = getIntent();
        BusinessCardInfo info = (BusinessCardInfo) intent.getExtras().get("info");
        Uri imageUri = Uri.parse(intent.getStringExtra("imageUri"));

        // Find the EditText fields in the layout
        nameEditText = findViewById(R.id.nameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        companyEditText = findViewById(R.id.companyEditText);
        addressEditText = findViewById(R.id.addressEditText);

        // Populate EditText fields with extracted information
        nameEditText.setText(info.getName());
        phoneEditText.setText(info.getPhoneNumber());
        emailEditText.setText(info.getEmail());
        companyEditText.setText(info.getBusinessName());
        addressEditText.setText(info.getAddress());

        Button confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> {
            // Update info object with user-edited values from EditTexts
            info.setName(nameEditText.getText().toString());
            info.setPhoneNumber(phoneEditText.getText().toString());
            info.setEmail(emailEditText.getText().toString());
            info.setBusinessName(companyEditText.getText().toString());
            info.setAddress(addressEditText.getText().toString());

            searchAndUpdateContact(info, imageUri);
        });

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> finish());
    }

    // ... (other methods)
}