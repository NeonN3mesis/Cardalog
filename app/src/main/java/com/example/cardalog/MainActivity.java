package com.example.cardalog;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
        Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No contacts app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateCsvFile() {
        File file = new File(getExternalFilesDir(null), "contacts_export.csv");
        try (FileWriter writer = new FileWriter(file)) {
            writer.append("Name,Phone,Email\n");

            Uri uri = ContactsContract.Contacts.CONTENT_URI;
            String[] projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME};
            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));

                        String phone = "";
                        try (Cursor phoneCursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                                new String[]{id}, null)) {
                            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                                phone = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                            }
                        }

                        String email = "";
                        try (Cursor emailCursor = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS},
                                ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
                                new String[]{id}, null)) {
                            if (emailCursor != null && emailCursor.moveToFirst()) {
                                email = emailCursor.getString(emailCursor.getColumnIndexOrThrow(
                                        ContactsContract.CommonDataKinds.Email.ADDRESS));
                            }
                        }

                        writer.append(name).append(',')
                                .append(phone).append(',')
                                .append(email).append('\n');
                    }
                }
            }

            writer.flush();
            Toast.makeText(this, "CSV saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to create CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}