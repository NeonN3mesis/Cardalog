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

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class ConfirmDetailsActivity extends AppCompatActivity {
    private static final String TAG = "ConfirmDetailsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_details);

        Intent intent = getIntent();
        BinessCardInfo info = (BusinessCardInfo) intent.getExtras().get("info");
        Uri imageUri = Uri.parse(intent.getStringExtra("imageUri"));

        // Populate EditText fields with extracted information
        // ...

        Button confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> {
            // Update info object with user-edited values from EditTexts
            // ...

            searchAndUpdateContact(info, imageUri);
        });

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> finish());
    }

    private void searchAndUpdateContact(BusinessCardInfo info, Uri imageUri) {
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(info.phoneNumber));
            String[] projection = new String[]{ContactsContract.PhoneLookup._ID};
            Cursor cursor = getContentResolver().query(lookupUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                long existingContactId = cursor.getLong(0);
                updateContact(existingContactId, info, imageUri);
                cursor.close();
            } else {
                createContact(info, imageUri);
            }
        }

        private void createContact(BusinessCardInfo info, Uri imageUri) {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

            intent.putExtra(ContactsContract.Intents.Insert.NAME, info.name);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, info.phoneNumber);
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, info.email);
            intent.putExtra(ContactsContract.Intents.Insert.POSTAL, info.address);
            intent.putExtra(ContactsContract.Intents.Insert.COMPANY, info.businessName);
            intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, info.jobTitle);

            // Add the photo to the contact
            try {
                InputStream stream = getContentResolver().openInputStream(imageUri);
                if (stream != null) {
                    byte[] photo = IOUtils.toByteArray(stream);
                    intent.putExtra(ContactsContract.Intents.Insert.DATA, photo);
                    stream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading image file", e);
            }

            startActivity(intent);
        }

        private void updateContact(long contactId, BusinessCardInfo info, Uri imageUri) {
            ContentValues values = new ContentValues();

            values.put(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, info.name);
            values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, info.phoneNumber);
            values.put(ContactsContract.CommonDataKinds.Email.DATA, info.email);
            values.put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, info.address);
            values.put(ContactsContract.CommonDataKinds.Organization.COMPANY, info.businessName);
            values.put(ContactsContract.CommonDataKinds.Organization.TITLE, info.jobTitle);

            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
            getContentResolver().update(contactUri, values, null, null);

            try {
                InputStream stream = getContentResolver().openInputStream(imageUri);
                if (stream != null) {
                    byte[] photo = IOUtils.toByteArray(stream);
                    ContentValues photoValues = new ContentValues();
                    photoValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactId);
                    photoValues.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
                    photoValues.put(ContactsContract.CommonDataKinds.Photo.PHOTO, photo);
                    photoValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

                    getContentResolver().insert(ContactsContract.Data.CONTENT_URI, photoValues);
                    stream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading image file", e);
            }
        }
    }
