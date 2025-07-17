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
        nameEditText = findViewById(R.id.name);
        phoneEditText = findViewById(R.id.phone_number);
        emailEditText = findViewById(R.id.email);
        companyEditText = findViewById(R.id.business_name);
        addressEditText = findViewById(R.id.address);

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

    /**
     * Look up an existing contact using the provided phone number. If a match is
     * found the contact is updated with the new information and image. Otherwise
     * a new contact is created.
     */
    private void searchAndUpdateContact(BusinessCardInfo info, Uri imageUri) {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(info.getPhoneNumber()));
        String[] projection = new String[]{ContactsContract.PhoneLookup._ID};

        try (Cursor cursor = getContentResolver().query(lookupUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long contactId = cursor.getLong(0);
                updateContact(contactId, info, imageUri);
            } else {
                createContact(info, imageUri);
            }
        }
    }

    /**
     * Launches an insert intent pre-populated with the details from the
     * {@link BusinessCardInfo}.  The user can then confirm the creation in the
     * contacts app.
     */
    private void createContact(BusinessCardInfo info, Uri imageUri) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

        intent.putExtra(ContactsContract.Intents.Insert.NAME, info.getName());
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, info.getPhoneNumber());
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, info.getEmail());
        intent.putExtra(ContactsContract.Intents.Insert.POSTAL, info.getAddress());
        intent.putExtra(ContactsContract.Intents.Insert.COMPANY, info.getBusinessName());
        intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, info.getJobTitle());

        // Attach the captured image as the contact photo if available
        try (InputStream stream = getContentResolver().openInputStream(imageUri)) {
            if (stream != null) {
                byte[] photo = IOUtils.toByteArray(stream);
                intent.putExtra(ContactsContract.Intents.Insert.DATA, photo);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading image file", e);
        }

        startActivity(intent);
    }

    /**
     * Updates an existing contact's core fields and replaces the contact photo
     * with the supplied image.
     */
    private void updateContact(long contactId, BusinessCardInfo info, Uri imageUri) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, info.getName());
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, info.getPhoneNumber());
        values.put(ContactsContract.CommonDataKinds.Email.DATA, info.getEmail());
        values.put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, info.getAddress());
        values.put(ContactsContract.CommonDataKinds.Organization.COMPANY, info.getBusinessName());
        values.put(ContactsContract.CommonDataKinds.Organization.TITLE, info.getJobTitle());

        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        getContentResolver().update(contactUri, values, null, null);

        try (InputStream stream = getContentResolver().openInputStream(imageUri)) {
            if (stream != null) {
                byte[] photo = IOUtils.toByteArray(stream);
                ContentValues photoValues = new ContentValues();
                photoValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactId);
                photoValues.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
                photoValues.put(ContactsContract.CommonDataKinds.Photo.PHOTO, photo);
                photoValues.put(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

                getContentResolver().insert(ContactsContract.Data.CONTENT_URI, photoValues);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading image file", e);
        }
    }
}